package net.vanadium.compute;

import net.vanadium.shader.PipelineBuilder;
import net.vanadium.shader.model.LoadedShaderPack;
import net.vanadium.util.StructuredLog;
import net.vanadium.vulkan.PipelineHandle;
import net.vanadium.vulkan.VulkanBackend;
import org.slf4j.Logger;

import java.util.Optional;

public final class ComputeManager {
    private final Logger logger;
    private final PipelineBuilder pipelineBuilder;
    private final VulkanBackend backend;
    private PipelineHandle currentHandle;
    private volatile String lastFailureReason;

    public ComputeManager(Logger logger, PipelineBuilder pipelineBuilder, VulkanBackend backend) {
        this.logger = logger;
        this.pipelineBuilder = pipelineBuilder;
        this.backend = backend;
    }

    public boolean activate(LoadedShaderPack pack) {
        PipelineBuilder.BuildResult result = pipelineBuilder.buildCompute(pack);
        if (!result.success()) {
            lastFailureReason = result.reason();
            StructuredLog.warn(logger, "compute-build-failed", StructuredLog.kv("pack", pack.id(), "reason", result.reason()));
            return false;
        }

        if (currentHandle != null) {
            backend.destroyPipeline(currentHandle);
        }
        currentHandle = result.pipelineHandle();
        lastFailureReason = null;

        StructuredLog.info(logger, "compute-activated", StructuredLog.kv("pack", pack.id(), "pipeline", currentHandle.id()));
        return true;
    }

    public void shutdown() {
        if (currentHandle != null) {
            backend.destroyPipeline(currentHandle);
            currentHandle = null;
        }
        lastFailureReason = null;
    }

    public Optional<String> lastFailureReason() {
        return Optional.ofNullable(lastFailureReason);
    }
}
