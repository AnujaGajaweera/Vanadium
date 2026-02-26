# Metadata Specification

`metadata.json` is mandatory.

## Required Fields

- `name`: string
- `author`: string
- `description`: string
- `version`: string
- `supportedMinecraftVersion`: string
- `modules`: object

## Optional Fields

- `icon`: string (path inside archive)
- `descriptorLayout`: array
- `renderPass`: object

## modules

Allowed stage keys:

- `vertex`
- `fragment`
- `compute` (optional)
- `geometry` (optional)

Each module entry:

- `path`: string, required
- `entryPoint`: string, optional (defaults to `main`)

Location constraints:

- `vertex`, `fragment`, `geometry` paths must be under `world-*`
- `compute` path must be under `lib/` or `world-*`

## renderPass

- `colorAttachments`: integer in `[1,8]`
- `depthAttachment`: boolean

## icon

If set, `icon` must point to an existing archive entry (for example `icon.png`).
