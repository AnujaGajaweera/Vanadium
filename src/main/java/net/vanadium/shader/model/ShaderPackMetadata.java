package net.vanadium.shader.model;

import java.util.List;
import java.util.Map;

public record ShaderPackMetadata(
        String name,
        String author,
        String description,
        String version,
        String supportedMinecraftVersion,
        String icon,
        Map<ShaderStage, ShaderModuleConfig> modules,
        List<DescriptorBindingSpec> descriptorLayout,
        RenderPassSpec renderPass
) {
}
