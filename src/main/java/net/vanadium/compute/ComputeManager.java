package net.vanadium.compute;

import net.vanadium.shader.PipelineBuilder;
import net.vanadium.shader.model.LoadedShaderPack;
import net.vanadium.util.StructuredLog;
import net.vanadium.vulkan.PipelineHandle;
import net.vanadium.vulkan.VulkanBackend;
import org.slf4j.Logger;

public final class ComputeManager {
    private final Logger logger;
    private final PipelineBuilder pipelineBuilder;
    private final VulkanBackend backend;
    private PipelineHandle currentHandle;

    public ComputeManager(Logger logger, PipelineBuilder pipelineBuilder, VulkanBackend backend) {
        this.logger = logger;
        this.pipelineBuilder = pipelineBuilder;
        this.backend = backend;
    }

    public boolean activate(LoadedShaderPack pack) {
        PipelineBuilder.BuildResult result = pipelineBuilder.buildCompute(pack);
        if (!result.success()) {
            StructuredLog.warn(logger, "compute-build-failed", StructuredLog.kv("pack", pack.id(), "reason", result.reason()));
            return false;
        }

        if (currentHandle != null) {
            backend.destroyPipeline(currentHandle);
        }
        currentHandle = result.pipelineHandle();

        StructuredLog.info(logger, "compute-activated", StructuredLog.kv("pack", pack.id(), "pipeline", currentHandle.id()));
        return true;
    }

    public void shutdown() {
        if (currentHandle != null) {
            backend.destroyPipeline(currentHandle);
            currentHandle = null;
        }
    }
}
