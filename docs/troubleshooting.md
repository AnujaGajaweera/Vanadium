# Troubleshooting

## Vanadium stays inactive

Cause:

- VulkanMod is not loaded.

Fix:

- Install VulkanMod and verify Fabric loads it.

## Pack rejected (structured layout)

Check required files exist:

- `LICENSE`
- `README.md`
- `metadata.json`
- `shaders/pack.json`
- `config/defaults.json`
- `config/toggles.json`

Check required folders exist:

- `shaders/`, `lib/`, `world-0/`, `world-1/`, `world-2/`, `textures/`, `config/`

Check at least one shader property file exists:

- `shaders/*.properties.xml`

## Pack rejected (module placement)

Check module paths in `metadata.json`:

- graphics modules in `world-*`
- compute module in `lib/` or `world-*`

## SPIR-V validation failure

Check:

- binaries are valid SPIR-V
- entrypoint names match metadata
- stage execution model matches module type

## Gradle permission/lock failure

Cause:

- restricted access to `~/.gradle` in sandbox

Fix:

- run build with permissions that allow Gradle wrapper cache access.
