package com.thunder.NovaAPI.debug.CrashHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class LiveLogMonitor {
    private static final Path outputLog = FMLPaths.GAMEDIR.get().resolve("logs/runtime_issues.log");
    private static final String reportLink = "https://github.com/YourModpack/issues"; // Replace this

    // Ordered map so first match wins
    private static final Map<String, String> errorMessages = new LinkedHashMap<>() {{
        put("Mixin apply failed", "A mod failed to apply a Mixin. This could cause crashes or broken features.");
        put("ClassNotFoundException", "A required class is missing. This usually means a dependency is missing or outdated.");
        put("NoClassDefFoundError", "A mod tried to use a class that failed to load. This can happen due to a missing or mismatched mod.");
        put("OutOfMemoryError", "The game has run out of memory. Try allocating more RAM or reducing mod load.");
        put("NullPointerException", "A mod ran into a null value where it shouldn't. This may cause instability or crashes.");
        put("Exception in server tick loop", "A fatal error occurred during world tick. This may crash or corrupt the world.");
    }};

    public static void start() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.Logger coreLogger = context.getRootLogger();
        coreLogger.addAppender(new IssueCatchingAppender("LiveLogMonitor"));
    }

    public static class IssueCatchingAppender extends AbstractAppender {
        protected IssueCatchingAppender(String name) {
            super(name, null, PatternLayout.createDefaultLayout(), false);
            start();
        }

        @Override
        public void append(LogEvent event) {
            String message = event.getMessage().getFormattedMessage();

            for (Map.Entry<String, String> entry : errorMessages.entrySet()) {
                if (message.contains(entry.getKey())) {
                    writeToLog(message);
                    sendToChat(message, entry.getValue());
                    break;
                }
            }
        }

        private void writeToLog(String message) {
            try {
                Files.writeString(outputLog,
                        "[" + Instant.now() + "] " + message + "\n",
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendToChat(String logLine, String warningMessage) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            mc.execute(() -> {
                mc.player.sendSystemMessage(Component.literal("§c[NovaAPI Warning] A potential error was detected:"));
                mc.player.sendSystemMessage(Component.literal("§7" + logLine));
                mc.player.sendSystemMessage(Component.literal("§6" + warningMessage));
                mc.player.sendSystemMessage(Component.literal("§9[Click here to report the issue]")
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, reportLink))
                                .withUnderlined(true)));
            });
        }
    }
}