# Vim

<p align="center">
  <img src="assets/vim-icon.png" alt="Vim Logo" width="140"/>
</p>

<p align="center">
  <strong>A Vulkan-native SPIR-V shader pipeline for Minecraft Java Edition</strong><br>
  Deterministic rendering • Strict validation • Developer-first architecture
</p>

<p align="center">
  <a href="https://github.com/AnujaGajaweera/vim/releases">
    <img src="https://img.shields.io/github/v/release/AnujaGajaweera/vim?include_prereleases&style=for-the-badge" alt="Release">
  </a>
  <a href="https://github.com/AnujaGajaweera/vim/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/AnujaGajaweera/vim/build.yml?style=for-the-badge" alt="Build">
  </a>
  <a href="https://github.com/AnujaGajaweera/vim/issues">
    <img src="https://img.shields.io/github/issues/AnujaGajaweera/vim?style=for-the-badge" alt="Issues">
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/github/license/AnujaGajaweera/vim?style=for-the-badge" alt="License">
  </a>

  <img src="https://img.shields.io/badge/Minecraft-1.20–1.21+-brightgreen?style=for-the-badge" alt="Minecraft">
  <img src="https://img.shields.io/badge/Modloader-Fabric-blue?style=for-the-badge" alt="Fabric">
  <img src="https://img.shields.io/badge/Renderer-VulkanMod-purple?style=for-the-badge" alt="VulkanMod">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge" alt="Java">
</p>

---

## What is Vim?

**Vim** is a modern shader runtime for **Minecraft Java Edition (Fabric)** that executes **precompiled SPIR-V pipelines** through **VulkanMod**.

The project exists to solve long-standing problems in the Minecraft shader ecosystem:

* Runtime GLSL compilation stutter
* Undefined behavior from loosely structured packs
* Fragile mod interactions
* Poor validation and diagnostics
* Lack of compute pipeline support

Vim replaces those weaknesses with a **deterministic, validated, Vulkan-first architecture** designed for both players and graphics developers.

---

## Design Goals

Vim is built around a few core engineering principles:

* **Determinism** — Pipelines are precompiled and validated before activation.
* **Safety** — Guarded initialization with automatic fallback prevents client crashes.
* **Isolation** — `.mcshader` packs do not interfere with legacy `.zip` shader systems.
* **Observability** — Structured logs and status reporting make debugging practical.
* **Extensibility** — Architecture supports compute stages and future rendering features.
* **Professional workflow** — Shader development behaves closer to real graphics engines.

---

## Features

### Vulkan SPIR-V Runtime

* Executes `.spv` modules instead of compiling GLSL at runtime
* Integrates directly with VulkanMod backend
* Predictable pipeline creation with reduced hitching

### Hot Reload

* File watcher monitors pack changes
* Reload without restart:

```
/vim reload
```

### In-Game Management UI

Location:

```
Options → Video Settings → Vim Shaders
```

Capabilities:

* Pack discovery and activation
* Metadata display
* Compatibility status
* Runtime reload controls

### Compute Shader Support

Compute pipelines are supported when:

* Device features allow
* Descriptor layout rules are satisfied

### Safe Fallback Renderer

If anything fails during load or initialization:

* Activation is aborted safely
* A fallback renderer is used
* Errors are logged with context

### Structured Diagnostics

Developer-oriented logging covers:

* Pack validation
* Descriptor mismatches
* Pipeline creation
* Runtime transitions
* Reload events

---

## Installation

### Requirements

* Minecraft Java Edition
* Fabric Loader
* VulkanMod (**required**)
* Vulkan-capable GPU with updated drivers
* Java 17+

### Steps

1. Install Fabric Loader
2. Install VulkanMod
3. Place Vim jar into:

```
.minecraft/mods/
```

4. Place `.mcshader` packs into:

```
<gameDir>/shaderpacks/vim/
```

5. Launch Minecraft

---

## Commands

```
/vim list
/vim status
/vim activate <pack_id>
/vim reload
```

---

## Shader Pack Format (`.mcshader`)

Vim uses a structured archive format with strict validation.

A pack is a ZIP renamed to `.mcshader`.

### Required Structure

```
pack.mcshader
├── metadata.json
├── README.md
├── LICENSE
├── shaders/
│   ├── pack.json
│   └── *.properties.xml
├── lib/
├── world-0/
├── world-1/
├── world-2/
├── textures/
└── config/
    ├── defaults.json
    └── toggles.json
```

### Key Properties

* Precompiled SPIR-V modules (`.spv`)
* Explicit descriptor layouts
* Pipeline stage separation
* Validation before activation

Specifications are documented in `/docs`.

---

## Development

### Build From Source

```bash
git clone https://github.com/AnujaGajaweera/vim.git
cd Vim
./gradlew clean build
```

Output:

```
build/libs/
```

### Dev Environment

Use Fabric Loom run configs:

```
./gradlew runClient
```

Place test packs into the dev instance shader directory.

---

## Architecture Overview

High-level system layout:

```
Vim
 ├── Shader System
 │    ├── ShaderManager
 │    ├── PipelineBuilder
 │    ├── DescriptorManager
 │
 ├── Vulkan Backend
 │    ├── VulkanBackend
 │    ├── SafeVulkanBackend
 │    └── PipelineHandle
 │
 ├── Pack Loader
 │    ├── MetadataParser
 │    └── ShaderPackLoader
 │
 ├── Hot Reload
 │    ├── FileWatcherService
 │    └── HotReloadService
 │
 ├── Compatibility
 │    └── CompatibilityGuard
 │
 ├── Fallback Renderer
 │
 └── Client UI
```

The architecture intentionally separates **validation**, **resource management**, and **activation** phases to reduce failure risk.

---

## Compatibility

* ✔ Fabric
* ✔ VulkanMod
* ✔ Coexists with Iris/Sodium environments
* ✖ Does not load GLSL `.zip` shaderpacks (by design)

---

## Contributing

Contributions are welcome.

Engineering priorities:

* Vulkan resource lifecycle correctness
* Deterministic pipeline initialization
* Strict pack validation
* Thread-safe hot reload
* Clear diagnostics

Typical workflow:

1. Fork repository
2. Create feature branch
3. Implement changes
4. Run build + tests
5. Submit pull request

---

## Debugging

Useful tools:

```
/vim status
```

Logs:

```
net.vim
```

If a pack fails:

* Validation errors will be logged
* Fallback renderer activates automatically

---

## Roadmap

Planned directions include:

* Shader validation CLI tooling
* Descriptor automation helpers
* Performance telemetry overlay
* Advanced compute orchestration
* Multi-GPU awareness
* Additional example packs

---

## License

Licensed under **LGPL-3.0-only**.

See:

```
LICENSE
docs/license-compliance.md
```

---

## Support

GitHub Issues:

https://github.com/AnujaGajaweera/vim/issues

Please include:

* GPU + driver
* Logs

---

## Acknowledgements

* VulkanMod developers
* Fabric ecosystem
* SPIR-V tooling community
* Contributors and testers

---

<p align="center">
  Built for stability, determinism, and modern graphics workflows.
</p>
