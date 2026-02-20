package net.vanadium.vulkan;

import net.vanadium.shader.model.LoadedShaderPack;
import net.vanadium.util.StructuredLog;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SafeVulkanBackend implements VulkanBackend {
    private final Logger logger;
    private final AtomicBoolean ready = new AtomicBoolean(false);

    public SafeVulkanBackend(Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean initialize() {
        // Vanadium intentionally does not hard-couple to VulkanMod internals.
        // Runtime Vulkan state is validated externally by CompatibilityGuard.
        ready.set(true);
        StructuredLog.info(logger, "vulkan-backend-ready", StructuredLog.kv("ready", true));
        return true;
    }

    @Override
    public boolean isReady() {
        return ready.get();
    }

    @Override
    public PipelineHandle buildGraphicsPipeline(LoadedShaderPack pack) {
        return PipelineHandle.of("graphics", pack.id());
    }

    @Override
    public PipelineHandle buildComputePipeline(LoadedShaderPack pack) {
        return PipelineHandle.of("compute", pack.id());
    }

    @Override
    public void destroyPipeline(PipelineHandle handle) {
        StructuredLog.info(logger, "pipeline-destroy", StructuredLog.kv("id", handle.id(), "type", handle.type(), "pack", handle.packId()));
    }

    @Override
    public void shutdown() {
        ready.set(false);
        StructuredLog.info(logger, "vulkan-backend-shutdown", StructuredLog.kv("ready", false));
    }
}
