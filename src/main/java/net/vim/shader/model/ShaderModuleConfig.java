package net.vim.shader.model;

public record ShaderModuleConfig(
        ShaderStage stage,
        String path,
        String entryPoint
) {
    public String resolvedEntryPoint() {
        return entryPoint == null || entryPoint.isBlank() ? "main" : entryPoint;
    }
}
