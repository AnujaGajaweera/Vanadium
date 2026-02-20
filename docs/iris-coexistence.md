# Iris Coexistence Strategy

When Iris is installed:

- Vanadium logs Iris presence
- Vanadium does not attempt OpenGL registration
- Vanadium keeps Vulkan-only behavior
- No duplicate Iris shaderpack registration is attempted

When VulkanMod is missing:

- Vanadium does not initialize rendering subsystems even if Iris is present
