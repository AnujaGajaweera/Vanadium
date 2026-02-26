# Shader Format Specification

Vim now supports a structured `.mcshader` layout for modular pipeline packs.

## Container

- Extension: `.mcshader`
- Format: ZIP archive
- Scan path: `<gameDir>/shaderpacks/`
- `.zip` shader packs are ignored

## Required Root Files

- `LICENSE`
- `README.md`
- `metadata.json`

## Required Folders

- `shaders/`
- `lib/`
- `world-0/`
- `world-1/`
- `world-2/`
- `textures/`
- `config/`

## shaders/

Required:

- `pack.json`
- at least one `*.properties.xml`

Typical property files:

- `block.properties.xml`
- `colorwheel.properties.xml`
- `dimension.properties.xml`
- `entity.properties.xml`
- `item.properties.xml`
- `shaders.properties.xml`

## lib/

Reusable precompiled SPIR-V utilities grouped by category, for example:

- `antialiasing/`
- `atmospherics/`
- `colors/`
- `lighting/`
- `materials/`
- `misc/`
- `textRendering/`

## world-*/

World pipeline shader modules are stored per pipeline folder (for example `composite1.vsh`, `composite1.fsh`, `shadow.vsh`).

## textures/

Stores textures consumed by shader pipelines (for example blue-noise, cloud, and water maps). Optional `.mcmeta` texture metadata is supported.

## config/

Required files:

- `defaults.json`
- `toggles.json`

## Validation

Vim validates:

- archive safety limits/path traversal
- structured layout requirements listed above
- module location rules:
  - graphics stages in `world-*`
  - compute stage in `lib/` or `world-*`
- SPIR-V module validity (magic, stream shape, entrypoint, execution model)
