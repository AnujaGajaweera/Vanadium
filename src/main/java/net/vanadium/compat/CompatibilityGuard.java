package net.vanadium.compat;

import net.fabricmc.loader.api.FabricLoader;

public final class CompatibilityGuard {
    private final FabricLoader loader;

    public CompatibilityGuard(FabricLoader loader) {
        this.loader = loader;
    }

    public CompatibilityStatus inspect() {
        boolean vulkanModLoaded = loader.isModLoaded("vulkanmod");
        boolean irisLoaded = loader.isModLoaded("iris");
        boolean sodiumLoaded = loader.isModLoaded("sodium");
        boolean lithiumLoaded = loader.isModLoaded("lithium");
        boolean phosphorLoaded = loader.isModLoaded("phosphor");

        return new CompatibilityStatus(vulkanModLoaded, irisLoaded, sodiumLoaded, lithiumLoaded, phosphorLoaded);
    }
}
