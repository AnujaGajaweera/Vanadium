package net.vim.fallback;

import net.vim.util.StructuredLog;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

public final class FallbackRenderer {
    private final Logger logger;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public FallbackRenderer(Logger logger) {
        this.logger = logger;
    }

    public void enable() {
        if (active.compareAndSet(false, true)) {
            StructuredLog.info(logger, "fallback-enabled", StructuredLog.kv("active", true));
        }
    }

    public void disable() {
        if (active.compareAndSet(true, false)) {
            StructuredLog.info(logger, "fallback-disabled", StructuredLog.kv("active", false));
        }
    }

    public boolean isActive() {
        return active.get();
    }
}
