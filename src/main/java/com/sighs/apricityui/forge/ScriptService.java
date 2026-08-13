package com.sighs.apricityui.forge;

import com.sighs.apricityui.event.Event;
import com.sighs.apricityui.spi.AuiScriptService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Forge implementation of {@link AuiScriptService}, no-op when KubeJS/Rhino
 * scripting engine is not bundled.
 */
public final class ScriptService implements AuiScriptService {
    public static final ScriptService INSTANCE = new ScriptService();
    private static final Logger LOGGER = LogManager.getLogger("ApricityUI/ScriptService");

    private ScriptService() {
    }

    @Override
    public void eval(String code, Event event, String source) {
        LOGGER.warn("Script eval requested but scripting is not available (KubeJS not loaded)");
    }

    @Override
    public void evalGlobal(String code, String documentUuid) {
        LOGGER.warn("Global script eval requested but scripting is not available (KubeJS not loaded)");
    }

    @Override
    public void reload() {
        // No-op: scripting engine not available
    }

    @Override
    public void warmUp() {
        // No-op: scripting engine not available
    }
}