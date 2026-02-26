package net.vim.vulkan;

import java.util.UUID;

public record PipelineHandle(UUID id, String type, String packId) {
    public static PipelineHandle of(String type, String packId) {
        return new PipelineHandle(UUID.randomUUID(), type, packId);
    }
}
