package net.vanadium.shader;

import net.vanadium.compute.ComputeManager;
import net.vanadium.fallback.FallbackRenderer;
import net.vanadium.shader.model.LoadedShaderPack;
import net.vanadium.shader.model.PackLoadResult;
import net.vanadium.shader.pack.ShaderPackLoader;
import net.vanadium.util.StructuredLog;
import net.vanadium.vulkan.PipelineHandle;
import net.vanadium.vulkan.VulkanBackend;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<String, LoadedShaderPack> loadedPacks = new ConcurrentHashMap<>();

    private volatile String activePackId;
    private volatile PipelineHandle graphicsPipeline;

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
            loadedPacks.clear();
            for (PackLoadResult result : results) {
                if (result.success()) {
                    LoadedShaderPack pack = result.pack();
                    DescriptorManager.DescriptorValidation validation = descriptorManager.validate(pack);
                    if (validation.valid()) {
                        loadedPacks.put(pack.id(), pack);
                    } else {
                        StructuredLog.warn(logger, "descriptor-validation-failed", StructuredLog.kv("pack", pack.id(), "reason", validation.reason()));
                    }
                } else {
                    StructuredLog.warn(logger, "pack-invalid", StructuredLog.kv("pack", result.id(), "reason", result.message()));
                }
            }

            if (activePackId != null && !loadedPacks.containsKey(activePackId)) {
                deactivateActivePack();
            }

            if (activePackId == null && !loadedPacks.isEmpty()) {
                String first = loadedPacks.values().stream()
                        .sorted(Comparator.comparing(pack -> pack.metadata().name().toLowerCase()))
                        .findFirst()
                        .map(LoadedShaderPack::id)
                        .orElse(null);

                if (first != null) {
                    activatePack(first);
                }
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
                return false;
            }

            PipelineBuilder.BuildResult graphics = pipelineBuilder.buildGraphics(pack);
            if (!graphics.success()) {
                StructuredLog.warn(logger, "graphics-build-failed", StructuredLog.kv("pack", id, "reason", graphics.reason()));
                fallbackRenderer.enable();
                return false;
            }

            if (!computeManager.activate(pack)) {
                fallbackRenderer.enable();
                return false;
            }

            if (graphicsPipeline != null) {
                backend.destroyPipeline(graphicsPipeline);
            }

            graphicsPipeline = graphics.pipelineHandle();
            activePackId = id;
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
            fallbackRenderer.enable();
            StructuredLog.info(logger, "pack-deactivated", StructuredLog.kv("active", false));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<LoadedShaderPack> listPacks() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(loadedPacks.values());
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
}
