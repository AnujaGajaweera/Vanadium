package net.vanadium.hotreload;

import net.vanadium.shader.ShaderManager;

import java.util.concurrent.atomic.AtomicBoolean;

public final class HotReloadService {
    private final ShaderManager shaderManager;
    private final AtomicBoolean inProgress = new AtomicBoolean(false);

    public HotReloadService(ShaderManager shaderManager) {
        this.shaderManager = shaderManager;
    }

    public void triggerReload() {
        if (!inProgress.compareAndSet(false, true)) {
            return;
        }

        try {
            shaderManager.reloadPacks();
        } finally {
            inProgress.set(false);
        }
    }
}
