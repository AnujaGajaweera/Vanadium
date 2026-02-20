# Troubleshooting

## Vanadium stays inactive

Cause: VulkanMod not installed.

Action: install VulkanMod; Vanadium intentionally remains dormant otherwise.

## Pack rejected

Check:

- file extension is `.mcshader`
- `metadata.json` has required fields
- `modules.compute` exists
- SPIR-V binaries include expected entry points

## Fallback renderer remains active

Cause: graphics or compute pipeline validation failed.

Action: run `/vanadium status` and inspect log output for structured rejection reason.
