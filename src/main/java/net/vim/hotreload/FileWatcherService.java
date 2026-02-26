package net.vim.hotreload;

import net.vim.util.StructuredLog;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FileWatcherService {
    private static final long DEBOUNCE_NANOS = TimeUnit.MILLISECONDS.toNanos(300);

    private final Logger logger;
    private final Path shaderpacksDir;
    private final Runnable onChange;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executor;
    private WatchService watchService;

    public FileWatcherService(Logger logger, Path shaderpacksDir, Runnable onChange) {
        this.logger = logger;
        this.shaderpacksDir = shaderpacksDir;
        this.onChange = onChange;
    }

    public synchronized void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        try {
            Files.createDirectories(shaderpacksDir);
            watchService = FileSystems.getDefault().newWatchService();
            shaderpacksDir.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
            );
        } catch (IOException e) {
            running.set(false);
            StructuredLog.error(logger, "filewatcher-init-failed", StructuredLog.kv("path", shaderpacksDir), e);
            return;
        }

        executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "vim-filewatcher");
            thread.setDaemon(true);
            return thread;
        });

        executor.submit(this::watchLoop);
    }

    public synchronized void stop() {
        running.set(false);

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
        }

        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void watchLoop() {
        long lastTrigger = Long.MIN_VALUE;
        while (running.get()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (Exception e) {
                return;
            }

            boolean relevant = false;
            for (WatchEvent<?> event : key.pollEvents()) {
                Object context = event.context();
                if (context != null && context.toString().toLowerCase().endsWith(".mcshader")) {
                    relevant = true;
                }
            }

            key.reset();

            if (relevant) {
                long now = System.nanoTime();
                if (now - lastTrigger > DEBOUNCE_NANOS) {
                    lastTrigger = now;
                    StructuredLog.info(logger, "filewatcher-change", StructuredLog.kv("path", shaderpacksDir));
                    onChange.run();
                }
            }
        }
    }
}
