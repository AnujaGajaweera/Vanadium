package net.vanadium.compat;

public record CompatibilityStatus(
        boolean vulkanModLoaded,
        boolean irisLoaded,
        boolean sodiumLoaded,
        boolean lithiumLoaded,
        boolean phosphorLoaded
) {
    public boolean canBootVanadium() {
        return vulkanModLoaded;
    }
}
