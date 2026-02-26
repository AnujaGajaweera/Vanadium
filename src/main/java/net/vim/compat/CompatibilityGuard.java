package net.vim.compat;

import net.fabricmc.loader.api.FabricLoader;

public final class CompatibilityGuard {
    private final FabricLoader loader;

    public CompatibilityGuard(FabricLoader loader) {
        this.loader = loader;
    }

    public CompatibilityStatus inspect() {
        boolean vulkanModLoaded = loader.isModLoaded("vulkanmod");
        return new CompatibilityStatus(vulkanModLoaded);
    }
}
