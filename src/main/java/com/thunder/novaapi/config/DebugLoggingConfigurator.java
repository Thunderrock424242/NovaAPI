package com.thunder.novaapi.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * Applies NovaAPI-specific debug logging settings without changing Minecraft's global log level.
 */
public final class DebugLoggingConfigurator {

    private static boolean debugLoggingEnabled = false;

    private DebugLoggingConfigurator() {
    }

    public static void apply(Logger logger) {
        boolean enableDebug = NovaAPIConfig.ENABLE_DEBUG_LOGGING.get();
        if (enableDebug == debugLoggingEnabled) {
            return;
        }
        debugLoggingEnabled = enableDebug;
        Level targetLevel = enableDebug ? Level.DEBUG : Level.INFO;
        Configurator.setLevel(logger.getName(), targetLevel);
        logger.info("[NovaAPI] {} debug logging.", enableDebug ? "Enabled" : "Disabled");
    }
}
