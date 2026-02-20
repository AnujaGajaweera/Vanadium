# Metadata Specification

`metadata.json` is mandatory.

## Required top-level fields

- `name`: string
- `author`: string
- `description`: string
- `version`: string
- `supportedMinecraftVersion`: string
- `modules`: object

## Optional top-level fields

- `icon`: string
- `descriptorLayout`: array
- `renderPass`: object

## modules object

Accepted keys:

- `vertex`
- `fragment`
- `compute` (required)
- `geometry`

Each module definition:

- `path`: string (required)
- `entryPoint`: string (optional, defaults to `main`)

## renderPass object

- `colorAttachments`: integer, range `[1,8]`
- `depthAttachment`: boolean

Missing required fields cause hard validation failure for that pack only.
