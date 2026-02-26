package net.vim.shader.model;

import java.util.Set;

public record SpirvModule(
        ShaderStage stage,
        String entryPoint,
        byte[] bytes,
        Set<Integer> descriptorKeys
) {
}
