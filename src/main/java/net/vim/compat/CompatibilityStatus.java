package net.vim.compat;

public record CompatibilityStatus(
        boolean vulkanModLoaded
) {
    public boolean canBootVim() {
        return vulkanModLoaded;
    }
}
