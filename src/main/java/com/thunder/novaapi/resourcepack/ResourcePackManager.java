package com.thunder.novaapi.resourcepack;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.thunder.novaapi.Core.NovaAPI;
import com.thunder.novaapi.cache.ModDataCache;
import com.thunder.novaapi.cache.ModDataCacheConfig;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.thunder.novaapi.Core.NovaAPI.MOD_ID;

/**
 * Loads cached external resource packs and registers them with the pack repository.
 */
@EventBusSubscriber(modid = MOD_ID)
public final class ResourcePackManager {
    private static final Gson GSON = new Gson();
    private static final Path MANIFEST_PATH = FMLPaths.GAMEDIR.get()
            .resolve("config")
            .resolve("wilderness_odyssey")
            .resolve("resource_packs.json");
    private static final Map<String, CachedPack> PACK_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ResourcePackEntry> ENTRY_CACHE = new ConcurrentHashMap<>();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static volatile boolean manifestLoaded = false;
    private static volatile long manifestLastModified = 0L;

    private ResourcePackManager() {
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (!ResourcePackOptimizationConfig.isOptimizationEnabled()) {
            return;
        }
        loadManifestIfNeeded();
        if (ENTRY_CACHE.isEmpty()) {
            return;
        }
        PackType packType = event.getPackType();
        RepositorySource source = consumer -> ENTRY_CACHE.values().stream()
                .filter(entry -> matchesPackType(entry, packType))
                .map(entry -> resolvePack(entry, packType))
                .flatMap(Optional::stream)
                .forEach(consumer);
        event.addRepositorySource(source);
    }

    private static void loadManifestIfNeeded() {
        if (Files.exists(MANIFEST_PATH)) {
            try {
                long modified = Files.getLastModifiedTime(MANIFEST_PATH).toMillis();
                if (manifestLoaded && modified == manifestLastModified) {
                    return;
                }
                manifestLastModified = modified;
            } catch (IOException e) {
                NovaAPI.LOGGER.warn("[ResourcePackManager] Failed to read manifest timestamp {}", MANIFEST_PATH);
            }
        } else if (manifestLoaded) {
            return;
        }
        ENTRY_CACHE.clear();
        if (!Files.exists(MANIFEST_PATH)) {
            NovaAPI.LOGGER.info("[ResourcePackManager] No resource pack manifest found at {}", MANIFEST_PATH);
            manifestLoaded = true;
            return;
        }
        try (Reader reader = Files.newBufferedReader(MANIFEST_PATH)) {
            ResourcePackManifest manifest = GSON.fromJson(reader, ResourcePackManifest.class);
            if (manifest == null || manifest.packs().isEmpty()) {
                NovaAPI.LOGGER.info("[ResourcePackManager] Resource pack manifest was empty");
                manifestLoaded = true;
                return;
            }
            for (ResourcePackEntry entry : manifest.packs()) {
                if (entry != null && entry.id() != null && entry.uri() != null) {
                    ENTRY_CACHE.put(entry.id(), entry);
                }
            }
            manifestLoaded = true;
        } catch (IOException | JsonParseException e) {
            NovaAPI.LOGGER.error("[ResourcePackManager] Failed to read resource pack manifest {}: {}", MANIFEST_PATH, e.getMessage());
            if (ModDataCacheConfig.isVerboseLogging()) {
                NovaAPI.LOGGER.debug("[ResourcePackManager] Manifest parsing exception", e);
            }
        }
    }

    private static Optional<Pack> resolvePack(ResourcePackEntry entry, PackType packType) {
        String cacheKey = packType.getDirectory() + ":" + entry.id();
        String checksum = entry.checksum() == null ? "" : entry.checksum();
        Optional<Path> cachedPath = resolvePackPath(entry, checksum);
        if (cachedPath.isEmpty()) {
            return Optional.empty();
        }
        Path path = cachedPath.get();
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        String signature = buildSignature(entry, packType);
        long lastModified = getLastModified(path);
        CachedPack cached = PACK_CACHE.get(cacheKey);
        if (cached != null && cached.lastModified() == lastModified && Objects.equals(cached.signature(), signature)) {
            return Optional.of(cached.pack());
        }
        Pack pack = buildPack(entry, packType, path);
        if (pack != null) {
            PACK_CACHE.put(cacheKey, new CachedPack(pack, lastModified, signature));
        }
        return Optional.ofNullable(pack);
    }

    private static Optional<Path> resolvePackPath(ResourcePackEntry entry, String checksum) {
        String key = "resourcepack:" + entry.id();
        ModDataCache.initialize();
        if (!ResourcePackOptimizationConfig.isRemoteDownloadAllowed()) {
            return ModDataCache.getCachedResource(key, checksum);
        }
        URI uri;
        try {
            uri = URI.create(entry.uri());
        } catch (IllegalArgumentException ex) {
            NovaAPI.LOGGER.warn("[ResourcePackManager] Invalid URI for {}: {}", entry.id(), entry.uri());
            return Optional.empty();
        }
        return ModDataCache.getOrDownload(key, checksum, () -> openStream(uri));
    }

    private static Pack buildPack(ResourcePackEntry entry, PackType packType, Path path) {
        PackSource source = createPackSource();
        Component title = Component.literal(entry.title() != null ? entry.title() : entry.id());
        PackLocationInfo locationInfo = new PackLocationInfo(MOD_ID + ":" + entry.id(), title, source, Optional.empty());
        Pack.ResourcesSupplier supplier = Files.isDirectory(path)
                ? new PathPackResources.PathResourcesSupplier(path)
                : new FilePackResources.FileResourcesSupplier(path);
        PackSelectionConfig selectionConfig = new PackSelectionConfig(resolveRequired(entry), resolvePosition(entry), resolveFixedPosition(entry));
        int packVersion = SharedConstants.getCurrentVersion().getPackVersion(packType);
        Pack.Metadata metadata = Pack.readPackMetadata(locationInfo, supplier, packVersion);
        if (metadata == null) {
            return null;
        }
        if (ResourcePackOptimizationConfig.isForceCompatible() && metadata.compatibility() != PackCompatibility.COMPATIBLE) {
            metadata = new Pack.Metadata(metadata.description(), PackCompatibility.COMPATIBLE, metadata.requestedFeatures(),
                    metadata.overlays(), metadata.isHidden());
        }
        return new Pack(locationInfo, supplier, metadata, selectionConfig);
    }

    private static PackSource createPackSource() {
        return PackSource.create(name -> Component.literal("NovaAPI").append(Component.literal(" ")).append(name),
                ResourcePackOptimizationConfig.isAutoEnablePacks());
    }

    private static boolean resolveRequired(ResourcePackEntry entry) {
        return entry.required() != null ? entry.required() : ResourcePackOptimizationConfig.getDefaultRequired();
    }

    private static boolean resolveFixedPosition(ResourcePackEntry entry) {
        return entry.fixedPosition() != null ? entry.fixedPosition() : ResourcePackOptimizationConfig.getDefaultFixedPosition();
    }

    private static Pack.Position resolvePosition(ResourcePackEntry entry) {
        String position = entry.position();
        if (position == null) {
            return mapPosition(ResourcePackOptimizationConfig.getDefaultPosition());
        }
        return "bottom".equalsIgnoreCase(position) ? Pack.Position.BOTTOM : Pack.Position.TOP;
    }

    private static Pack.Position mapPosition(ResourcePackOptimizationConfig.PackPositionPreference preference) {
        return preference == ResourcePackOptimizationConfig.PackPositionPreference.BOTTOM ? Pack.Position.BOTTOM : Pack.Position.TOP;
    }

    private static boolean matchesPackType(ResourcePackEntry entry, PackType packType) {
        PackType entryType = resolvePackType(entry);
        return entryType == packType;
    }

    private static PackType resolvePackType(ResourcePackEntry entry) {
        String value = entry.packType();
        if (value == null || value.isBlank()) {
            return mapPackType(ResourcePackOptimizationConfig.getDefaultPackType());
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "server", "server_data", "data", "data_pack" -> PackType.SERVER_DATA;
            default -> PackType.CLIENT_RESOURCES;
        };
    }

    private static PackType mapPackType(ResourcePackOptimizationConfig.PackTypePreference preference) {
        return preference == ResourcePackOptimizationConfig.PackTypePreference.SERVER_DATA
                ? PackType.SERVER_DATA
                : PackType.CLIENT_RESOURCES;
    }

    private static long getLastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static String buildSignature(ResourcePackEntry entry, PackType packType) {
        return String.join("|",
                packType.getDirectory(),
                entry.id(),
                entry.checksum() == null ? "" : entry.checksum(),
                entry.title() == null ? "" : entry.title(),
                String.valueOf(resolveRequired(entry)),
                String.valueOf(resolveFixedPosition(entry)),
                resolvePosition(entry).name(),
                String.valueOf(ResourcePackOptimizationConfig.isAutoEnablePacks()));
    }

    private static InputStream openStream(URI uri) throws IOException {
        if ("file".equalsIgnoreCase(uri.getScheme()) || uri.getScheme() == null) {
            Path local = Path.of(uri.getPath());
            return Files.newInputStream(local);
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET().build();
            HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            throw new IOException("Unexpected HTTP status " + response.statusCode() + " for " + uri);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading " + uri, e);
        }
    }

    private record ResourcePackManifest(List<ResourcePackEntry> packs) {
        ResourcePackManifest {
            packs = packs == null ? Collections.emptyList() : List.copyOf(packs);
        }
    }

    private record ResourcePackEntry(String id, String uri, String checksum, String packType, String title,
                                     Boolean required, Boolean fixedPosition, String position) {
    }

    private record CachedPack(Pack pack, long lastModified, String signature) {
    }
}
