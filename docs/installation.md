# Installation Instructions

## Requirements

- Fabric Loader
- Fabric API
- VulkanMod
- Minecraft version configured in `gradle.properties`

## Steps

1. Build with `./gradlew build`.
2. Place resulting JAR into `mods/`.
3. Copy `example-pack/VanadiumDemo.mcshader` to `<gameDir>/shaderpacks/`.
4. Launch game with VulkanMod enabled.
5. Use `/vanadium list` and `/vanadium activate vanadiumdemo`.
