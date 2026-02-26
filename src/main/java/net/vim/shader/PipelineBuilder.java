package net.vim.shader;

import net.vim.shader.model.LoadedShaderPack;
import net.vim.shader.model.RenderPassSpec;
import net.vim.shader.model.ShaderStage;
import net.vim.vulkan.PipelineHandle;
import net.vim.vulkan.VulkanBackend;

public final class PipelineBuilder {
    private final VulkanBackend backend;

    public PipelineBuilder(VulkanBackend backend) {
        this.backend = backend;
    }

    public BuildResult buildGraphics(LoadedShaderPack pack) {
        if (!backend.isReady()) {
            return BuildResult.failed("vulkan backend is not ready");
        }

        boolean hasVertex = pack.modules().containsKey(ShaderStage.VERTEX);
        boolean hasFragment = pack.modules().containsKey(ShaderStage.FRAGMENT);
        if (!hasVertex || !hasFragment) {
            return BuildResult.failed("graphics pipeline requires vertex+fragment modules");
        }

        RenderPassSpec renderPass = pack.metadata().renderPass();
        if (renderPass.colorAttachments() < 1) {
            return BuildResult.failed("renderPass.colorAttachments must be >= 1");
        }
        if (renderPass.colorAttachments() > 8) {
            return BuildResult.failed("renderPass.colorAttachments must be <= 8");
        }

        return BuildResult.success(backend.buildGraphicsPipeline(pack));
    }

    public BuildResult buildCompute(LoadedShaderPack pack) {
        if (!backend.isReady()) {
            return BuildResult.failed("vulkan backend is not ready");
        }
        if (!pack.modules().containsKey(ShaderStage.COMPUTE)) {
            return BuildResult.failed("compute module is missing");
        }
        return BuildResult.success(backend.buildComputePipeline(pack));
    }

    public record BuildResult(boolean success, String reason, PipelineHandle pipelineHandle) {
        public static BuildResult success(PipelineHandle pipelineHandle) {
            return new BuildResult(true, "ok", pipelineHandle);
        }

        public static BuildResult failed(String reason) {
            return new BuildResult(false, reason, null);
        }
    }
}
