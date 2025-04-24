package com.thunder.NovaAPI.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.thunder.NovaAPI.config.NovaAPIConfig;
import com.thunder.NovaAPI.server.network.NovaAPIServerConnection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;

import static com.thunder.NovaAPI.NovaAPI.PLAYERUUID;

public class NovaAPIAdminCommand {
    private static final String MOD_CREATOR_UUID = PLAYERUUID;
    private static final File REQUEST_FILE = new File("whitelist_requests.txt");
    private static final File WHITELIST_FILE = new File("whitelist.txt");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("novaapi")
                .requires(source -> source.getEntity() != null && source.getEntity().getUUID().toString().equals(MOD_CREATOR_UUID))
                .then(Commands.literal("viewRequests")
                        .executes(ctx -> {
                            List<String> requests = readRequests();
                            if (requests.isEmpty()) {
                                ctx.getSource().sendSuccess((Supplier<Component>) Component.literal("[Nova API] No pending whitelist requests."), false);
                            } else {
                                for (String request : requests) {
                                    ctx.getSource().sendSuccess((Supplier<Component>) Component.literal("[Request ID] " + request), false);
                                }
                            }
                            return 1;
                        }))
                .then(Commands.literal("approve")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id");
                                    boolean success = approveRequest(id);
                                    if (success) {
                                        ctx.getSource().sendSuccess((Supplier<Component>) Component.literal("[Nova API] Approved request ID: " + id), false);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("[Nova API] Invalid request ID."));
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("deny")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id");
                                    boolean success = denyRequest(id);
                                    if (success) {
                                        ctx.getSource().sendSuccess((Supplier<Component>) Component.literal("[Nova API] Denied request ID: " + id), false);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("[Nova API] Invalid request ID."));
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("status")
                        .executes(ctx -> showStatus(ctx.getSource())))
        );
    }

    private static int showStatus(CommandSourceStack source) {
        source.sendSuccess((Supplier<Component>) Component.literal("§6[Nova API Status]"), false);

        boolean isDedicated = NovaAPIConfig.ENABLE_DEDICATED_SERVER.get();
        source.sendSuccess((Supplier<Component>) Component.literal("§eMode: §f" + (isDedicated ? "Dedicated" : "Local")), false);

        boolean connected = isDedicated && NovaAPIServerConnection.isConnected();
        source.sendSuccess((Supplier<Component>) Component.literal("§eConnection: §f" + (connected ? "Connected ✅" : "Disconnected ❌")), false);

        boolean chunkOpt = NovaAPIConfig.ENABLE_CHUNK_OPTIMIZATIONS.get();
        source.sendSuccess((Supplier<Component>) Component.literal("§eChunk Optimization: §f" + (chunkOpt ? "Enabled" : "Disabled")), false);

        boolean aiEnabled = NovaAPIConfig.ENABLE_AI_OPTIMIZATIONS.get();
        int aiThreads = NovaAPIConfig.PATHFINDING_THREAD_COUNT.get();
        source.sendSuccess((Supplier<Component>) Component.literal("§eAI Optimization: §f" + (aiEnabled ? aiThreads + " thread(s)" : "Disabled")), false);

        boolean async = NovaAPIConfig.ASYNC_CHUNK_LOADING.get();
        source.sendSuccess((Supplier<Component>) Component.literal("§eAsync Chunk Loading: §f" + (async ? "Enabled" : "Disabled")), false);

        long ping = NovaAPIServerConnection.getLastPing();
        source.sendSuccess((Supplier<Component>) Component.literal("§ePing: §f" + (connected ? ping + "ms" : "-")), false);

        return 1;
    }

    private static List<String> readRequests() {
        List<String> requests = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(REQUEST_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                requests.add(line);
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }
        return requests;
    }

    private static boolean approveRequest(String id) {
        List<String> requests = readRequests();
        List<String> updatedRequests = new ArrayList<>();
        String approvedIP = null;

        for (String request : requests) {
            if (request.startsWith(id + ":")) {
                approvedIP = request.split(":")[1];
            } else {
                updatedRequests.add(request);
            }
        }

        if (approvedIP == null) return false;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(WHITELIST_FILE, true))) {
            writer.write(approvedIP + "\n");
        } catch (IOException e) {
            return false;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(REQUEST_FILE))) {
            for (String req : updatedRequests) {
                writer.write(req + "\n");
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    private static boolean denyRequest(String id) {
        List<String> requests = readRequests();
        List<String> updatedRequests = new ArrayList<>();
        boolean found = false;

        for (String request : requests) {
            if (request.startsWith(id + ":")) {
                found = true;
            } else {
                updatedRequests.add(request);
            }
        }

        if (!found) return false;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(REQUEST_FILE))) {
            for (String req : updatedRequests) {
                writer.write(req + "\n");
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }
}
