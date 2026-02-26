package net.vim.shader.model;

import java.util.Optional;

public record PackLoadResult(String id, boolean success, String message, LoadedShaderPack pack) {
    public static PackLoadResult success(LoadedShaderPack pack) {
        return new PackLoadResult(pack.id(), true, "ok", pack);
    }

    public static PackLoadResult failed(String id, String message) {
        return new PackLoadResult(id, false, message, null);
    }

    public Optional<LoadedShaderPack> asOptional() {
        return Optional.ofNullable(pack);
    }
}
