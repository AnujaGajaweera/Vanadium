package net.vim.shader;

import net.vim.compute.ComputeManager;
import net.vim.fallback.FallbackRenderer;
import net.vim.shader.model.LoadedShaderPack;
import net.vim.shader.model.PackLoadResult;
import net.vim.shader.pack.ShaderPackLoader;
import net.vim.util.StructuredLog;
import net.vim.vulkan.PipelineHandle;
import net.vim.vulkan.VulkanBackend;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class ShaderManager {
    private final Logger logger;
    private final Path shaderpacksDir;
    private final ShaderPackLoader packLoader;
    private final DescriptorManager descriptorManager;
    private final PipelineBuilder pipelineBuilder;
    private final ComputeManager computeManager;
    private final VulkanBackend backend;
    private final FallbackRenderer fallbackRenderer;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, LoadedShaderPack> loadedPacks = new LinkedHashMap<>();
    private final Map<String, String> packErrors = new LinkedHashMap<>();

    private volatile String activePackId;
    private volatile PipelineHandle graphicsPipeline;
    private volatile String lastActionError;

    public ShaderManager(
            Logger logger,
            Path shaderpacksDir,
            ShaderPackLoader packLoader,
            DescriptorManager descriptorManager,
            PipelineBuilder pipelineBuilder,
            ComputeManager computeManager,
            VulkanBackend backend,
            FallbackRenderer fallbackRenderer
    ) {
        this.logger = logger;
        this.shaderpacksDir = shaderpacksDir;
        this.packLoader = packLoader;
        this.descriptorManager = descriptorManager;
        this.pipelineBuilder = pipelineBuilder;
        this.computeManager = computeManager;
        this.backend = backend;
        this.fallbackRenderer = fallbackRenderer;
    }

    public void reloadPacks() {
        lock.writeLock().lock();
        try {
            List<PackLoadResult> results = packLoader.scan(shaderpacksDir);
            String previouslyActivePack = activePackId;
            loadedPacks.clear();
            packErrors.clear();
            for (PackLoadResult result : results) {
                if (result.success()) {
                    LoadedShaderPack pack = result.pack();
                    DescriptorManager.DescriptorValidation validation = descriptorManager.validate(pack);
                    if (validation.valid()) {
                        loadedPacks.put(pack.id(), pack);
                    } else {
                        packErrors.put(pack.id(), validation.reason());
                        StructuredLog.warn(logger, "descriptor-validation-failed", StructuredLog.kv("pack", pack.id(), "reason", validation.reason()));
                    }
                } else {
                    packErrors.put(result.id(), result.message());
                    StructuredLog.warn(logger, "pack-invalid", StructuredLog.kv("pack", result.id(), "reason", result.message()));
                }
            }

            if (previouslyActivePack != null && !loadedPacks.containsKey(previouslyActivePack)) {
                deactivateActivePack();
            }

            if (previouslyActivePack != null && loadedPacks.containsKey(previouslyActivePack)) {
                if (!activatePack(previouslyActivePack)) {
                    StructuredLog.warn(logger, "active-pack-reload-failed", StructuredLog.kv("pack", previouslyActivePack));
                    fallbackRenderer.enable();
                }
            } else if (activePackId == null) {
                fallbackRenderer.enable();
            }

            StructuredLog.info(logger, "packs-reloaded", StructuredLog.kv("count", loadedPacks.size()));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean activatePack(String id) {
        lock.writeLock().lock();
        try {
            LoadedShaderPack pack = loadedPacks.get(id);
            if (pack == null) {
                lastActionError = "Pack not found: " + id;
                return false;
            }
            if (!backend.isReady()) {
                lastActionError = "Vulkan backend is not ready";
                fallbackRenderer.enable();
                return false;
            }

            PipelineBuilder.BuildResult graphics = pipelineBuilder.buildGraphics(pack);
            if (!graphics.success()) {
                lastActionError = graphics.reason();
                StructuredLog.warn(logger, "graphics-build-failed", StructuredLog.kv("pack", id, "reason", graphics.reason()));
                fallbackRenderer.enable();
                return false;
            }

            if (!computeManager.activate(pack)) {
                lastActionError = computeManager.lastFailureReason().orElse("Compute pipeline activation failed");
                fallbackRenderer.enable();
                return false;
            }

            if (graphicsPipeline != null) {
                backend.destroyPipeline(graphicsPipeline);
            }

            graphicsPipeline = graphics.pipelineHandle();
            activePackId = id;
            lastActionError = null;
            fallbackRenderer.disable();
            StructuredLog.info(logger, "pack-activated", StructuredLog.kv("pack", id, "pipeline", graphicsPipeline.id()));
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deactivateActivePack() {
        lock.writeLock().lock();
        try {
            if (graphicsPipeline != null) {
                backend.destroyPipeline(graphicsPipeline);
                graphicsPipeline = null;
            }
            computeManager.shutdown();
            activePackId = null;
            lastActionError = null;
            fallbackRenderer.enable();
            StructuredLog.info(logger, "pack-deactivated", StructuredLog.kv("active", false));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<LoadedShaderPack> listPacks() {
        lock.readLock().lock();
        try {
            return loadedPacks.values().stream()
                    .sorted(Comparator.comparing(pack -> pack.metadata().name().toLowerCase()))
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<String> activePackId() {
        return Optional.ofNullable(activePackId);
    }

    public boolean isFallbackActive() {
        return fallbackRenderer.isActive();
    }

    public Map<String, String> packErrors() {
        lock.readLock().lock();
        try {
            return new LinkedHashMap<>(packErrors);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<String> lastActionError() {
        return Optional.ofNullable(lastActionError);
    }

    public Optional<byte[]> loadIconBytes(String packId) {
        lock.readLock().lock();
        try {
            LoadedShaderPack pack = loadedPacks.get(packId);
            if (pack == null) {
                return Optional.empty();
            }

            String iconPath = pack.metadata().icon();
            if (iconPath == null || iconPath.isBlank() || iconPath.contains("..") || iconPath.startsWith("/")) {
                return Optional.empty();
            }
            return Optional.ofNullable(pack.iconBytes());
        } finally {
            lock.readLock().unlock();
        }
    }
}
