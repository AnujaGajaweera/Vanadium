package net.vim.hotreload;

import net.vim.shader.ShaderManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HotReloadService {
    private final ShaderManager shaderManager;
    private final AtomicBoolean inProgress = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "vim-reloader");
        thread.setDaemon(true);
        return thread;
    });

    public HotReloadService(ShaderManager shaderManager) {
        this.shaderManager = shaderManager;
    }

    public void triggerReload() {
        if (!inProgress.compareAndSet(false, true)) {
            return;
        }

        executor.execute(() -> {
            try {
                shaderManager.reloadPacks();
            } finally {
                inProgress.set(false);
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
