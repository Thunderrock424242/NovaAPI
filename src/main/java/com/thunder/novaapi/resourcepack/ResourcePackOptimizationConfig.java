package com.thunder.novaapi.resourcepack;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration toggles for optimizing resource pack caching and activation.
 */
public final class ResourcePackOptimizationConfig {
    public static final ModConfigSpec CONFIG_SPEC;
    public static final ModConfigSpec.BooleanValue enableOptimization;
    public static final ModConfigSpec.BooleanValue allowRemoteDownloads;
    public static final ModConfigSpec.BooleanValue autoEnablePacks;
    public static final ModConfigSpec.BooleanValue defaultRequired;
    public static final ModConfigSpec.BooleanValue defaultFixedPosition;
    public static final ModConfigSpec.EnumValue<PackPositionPreference> defaultPosition;
    public static final ModConfigSpec.EnumValue<PackTypePreference> defaultPackType;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        enableOptimization = builder.comment("Enable cached resource pack loading via NovaAPI")
                .define("enableOptimization", true);
        allowRemoteDownloads = builder.comment("Allow NovaAPI to download packs that are not already cached")
                .define("allowRemoteDownloads", true);
        autoEnablePacks = builder.comment("Automatically enable cached packs without user intervention")
                .define("autoEnablePacks", true);
        defaultRequired = builder.comment("Require packs by default when manifest entries omit the flag")
                .define("defaultRequired", false);
        defaultFixedPosition = builder.comment("Fix pack position by default when manifest entries omit the flag")
                .define("defaultFixedPosition", false);
        defaultPosition = builder.comment("Default position for packs when manifest entries omit the position")
                .defineEnum("defaultPosition", PackPositionPreference.TOP);
        defaultPackType = builder.comment("Default pack type when manifest entries omit the packType field")
                .defineEnum("defaultPackType", PackTypePreference.CLIENT_RESOURCES);
        CONFIG_SPEC = builder.build();
    }

    private ResourcePackOptimizationConfig() {
    }

    public static boolean isOptimizationEnabled() {
        return enableOptimization.get();
    }

    public static boolean isRemoteDownloadAllowed() {
        return allowRemoteDownloads.get();
    }

    public static boolean isAutoEnablePacks() {
        return autoEnablePacks.get();
    }

    public static boolean getDefaultRequired() {
        return defaultRequired.get();
    }

    public static boolean getDefaultFixedPosition() {
        return defaultFixedPosition.get();
    }

    public static PackPositionPreference getDefaultPosition() {
        return defaultPosition.get();
    }

    public static PackTypePreference getDefaultPackType() {
        return defaultPackType.get();
    }

    public enum PackPositionPreference {
        TOP,
        BOTTOM
    }

    public enum PackTypePreference {
        CLIENT_RESOURCES,
        SERVER_DATA
    }
}
