package net.vim.shader.model;

import java.nio.file.Path;
import java.util.Map;

public record LoadedShaderPack(
        String id,
        Path archivePath,
        ShaderPackMetadata metadata,
        Map<ShaderStage, SpirvModule> modules,
        byte[] iconBytes
) {
}
