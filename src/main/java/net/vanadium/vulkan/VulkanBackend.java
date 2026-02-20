package net.vanadium.vulkan;

import net.vanadium.shader.model.LoadedShaderPack;

public interface VulkanBackend {
    boolean initialize();

    boolean isReady();

    PipelineHandle buildGraphicsPipeline(LoadedShaderPack pack);

    PipelineHandle buildComputePipeline(LoadedShaderPack pack);

    void destroyPipeline(PipelineHandle handle);

    void shutdown();
}
