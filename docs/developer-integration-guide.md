# Developer Integration Guide

## Source Layout

- Common code: `src/main/java`
- Client code: `src/client/java`
- Common resources: `src/main/resources`
- Client resources: `src/client/resources`

## Fabric Registration

From `fabric.mod.json`:

- `main` entrypoint: `net.vim.mod`
- `client` entrypoint: `net.vim.VimClient`
- mixin configs:
  - `vim.mixins.json`
  - `vim.client.mixins.json` (client-only)

## Runtime Boot Order

1. `CompatibilityGuard` verifies `vulkanmod` is loaded.
2. Vim remains dormant when VulkanMod is absent.
3. Backend and shader subsystems initialize.
4. Shader packs are loaded and validated.
5. Hot reload watcher + UI hooks are registered.

## Core Components

- `ShaderManager`
- `ShaderPackLoader`
- `MetadataParser`
- `SpirvInspector`
- `VulkanBackend`
- `PipelineBuilder`
- `DescriptorManager`
- `ComputeManager`
- `FallbackRenderer`
- `FileWatcherService`
- `HotReloadService`
- `UILayer`

## Mixin Layer

- Common mixin package: `net.mixin`
- Client mixin package: `net.vim.mixin.client`

Current mixins:

- `src/main/java/net/mixin/MinecraftServerMixin.java`
- `src/client/java/net/vim/mixin/client/VimClientMixin.java`
