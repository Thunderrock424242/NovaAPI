package com.thunder.NovaAPI.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;

import static com.thunder.NovaAPI.MainModClass.NovaAPI.PLAYERUUID;

public class NovaAPIAdminCommand {
    private static final String MOD_CREATOR_UUID = PLAYERUUID; // Replace with your actual UUID
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
                        .then(Commands.argument("id", StringArgumentType.string()) // 🔹 FIXED
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
                        .then(Commands.argument("id", StringArgumentType.string()) // 🔹 FIXED
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id");
                                    boolean success = denyRequest(id);
                                    if (success) {
                                        ctx.getSource().sendSuccess((Supplier<Component>) Component.literal("[Nova API] Denied request ID: " + id), false);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("[Nova API] Invalid request ID."));
                                    }
                                    return 1;
                                }))));
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
                approvedIP = request.split(":")[1]; // Extract IP
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
