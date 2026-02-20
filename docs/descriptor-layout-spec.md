# Descriptor Layout Specification

`descriptorLayout` is an array of binding declarations.

Each entry:

- `set`: integer >= 0
- `binding`: integer >= 0
- `type`: string
- `name`: string

## Validation behavior

- negative set/binding values are rejected
- if entries are declared, each `set/binding` pair must be reflected in SPIR-V decorations
- SPIR-V reflection uses `OpDecorate` with `DescriptorSet` and `Binding`

Key encoding used internally: `set * 1000 + binding`.
