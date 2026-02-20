# Developer Integration Guide

## Runtime Boot Order

1. `CompatibilityGuard` checks installed mods.
2. Vanadium remains dormant if VulkanMod is absent.
3. Vulkan backend initializes only after compatibility passes.
4. Shader packs are scanned and validated.
5. Hot reload and UI command layer are registered.

## Core Subsystems

- `ShaderManager`
- `ShaderPackLoader`
- `MetadataParser`
- `VulkanBackend`
- `PipelineBuilder`
- `DescriptorManager`
- `ComputeManager`
- `UILayer`
- `FallbackRenderer`
- `CompatibilityGuard`
- `HotReloadService`
- `FileWatcherService`

## Extending

- Add a new validation step in `ShaderPackLoader` before `PackLoadResult.success`.
- Extend reflection in `SpirvInspector` for additional SPIR-V opcodes.
- Replace `SafeVulkanBackend` with direct VulkanMod bridge code as needed.
