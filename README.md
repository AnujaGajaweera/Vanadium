# Vanadium

Vanadium is a Fabric client mod that loads **precompiled SPIR-V-only** shader packs for a Vulkan renderer in Minecraft.
It is designed for VulkanMod environments and remains dormant if VulkanMod is unavailable.

## License

Vanadium is released under **GNU Lesser General Public License v3.0 (LGPL-3.0-only)**.

- Full license text: `LICENSE`
- Declared in `fabric.mod.json`
- Declared in `gradle.properties`

## Features

- Strict `.mcshader` shader pack format (ZIP archive)
- Defensive archive validation and metadata validation
- SPIR-V module verification (magic, instruction stream, entry point, stage)
- Graphics + compute pipeline activation model
- Hot reload via filesystem watch + `/vanadium reload`
- Safe fallback renderer state when no valid pack is active
- Compatibility guard for VulkanMod/Iris/Sodium/Lithium/Phosphor coexistence

## Hard Isolation Rules

Vanadium intentionally does **not**:

- parse Iris shader formats
- load `.zip` shaderpacks
- scan GLSL shader directories
- register OpenGL shader providers
- modify Iris config/state

Vanadium scans only: `<gameDir>/shaderpacks/*.mcshader`

## Build

```bash
./gradlew build
```

## Run (dev client)

```bash
./gradlew runClient
```

## Install

1. Build JAR from `build/libs`.
2. Place JAR in Minecraft `mods` folder for Fabric.
3. Ensure VulkanMod is present.
4. Copy `example-pack/VanadiumDemo.mcshader` into `<gameDir>/shaderpacks/`.
5. In-game client command usage:

```text
/vanadium list
/vanadium activate vanadiumdemo
/vanadium status
/vanadium reload
```

## Project Layout

- Entrypoint: `net.vanadium.mod`
- Mod resources: `src/main/resources/fabric.mod.json`
- Shader subsystem: `src/main/java/net/vanadium/shader`
- Vulkan backend abstraction: `src/main/java/net/vanadium/vulkan`
- Hot reload services: `src/main/java/net/vanadium/hotreload`
- Docs: `docs/`

## Documentation Index

- `docs/shader-format-spec.md`
- `docs/metadata-spec.md`
- `docs/descriptor-layout-spec.md`
- `docs/developer-integration-guide.md`
- `docs/installation.md`
- `docs/troubleshooting.md`
- `docs/conflict-avoidance.md`
- `docs/iris-coexistence.md`
- `docs/license-compliance.md`
