package net.vim.vulkan;

import net.vim.shader.model.LoadedShaderPack;

public interface VulkanBackend {
    boolean initialize();

    boolean isReady();

    PipelineHandle buildGraphicsPipeline(LoadedShaderPack pack);

    PipelineHandle buildComputePipeline(LoadedShaderPack pack);

    void destroyPipeline(PipelineHandle handle);

    void shutdown();
}
