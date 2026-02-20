# Shader Format Specification

## Container

- File extension: `.mcshader`
- Container format: ZIP archive
- Scan path: `<gameDir>/shaderpacks/`
- `.zip` files are ignored by design.

## Required Files

- `metadata.json`

## SPIR-V Modules

Modules are declared in `metadata.json` under `modules`.
Each module path must reference a binary SPIR-V file in the archive.

Supported stages:

- `vertex`
- `fragment`
- `compute` (mandatory)
- `geometry` (optional)

## Validation

- SPIR-V magic number verification
- instruction stream shape validation
- entry-point name validation
- execution model vs stage validation
- descriptor decoration reflection for layout checks

Invalid packs are rejected without crashing the client.
