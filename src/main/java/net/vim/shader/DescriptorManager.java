package net.vim.shader;

import net.vim.shader.model.DescriptorBindingSpec;
import net.vim.shader.model.LoadedShaderPack;
import net.vim.shader.model.SpirvModule;

import java.util.HashSet;
import java.util.Set;

public final class DescriptorManager {
    public DescriptorValidation validate(LoadedShaderPack pack) {
        Set<Integer> declared = new HashSet<>();
        for (DescriptorBindingSpec spec : pack.metadata().descriptorLayout()) {
            if (spec.set() < 0 || spec.binding() < 0) {
                return DescriptorValidation.invalid("Negative descriptor set/binding in metadata");
            }
            if (spec.type() == null || spec.type().isBlank() || spec.name() == null || spec.name().isBlank()) {
                return DescriptorValidation.invalid("Descriptor entries require non-empty type and name");
            }
            int key = spec.set() * 1000 + spec.binding();
            if (!declared.add(key)) {
                return DescriptorValidation.invalid("Duplicate descriptor set/binding in metadata: " + spec.set() + "/" + spec.binding());
            }
        }

        Set<Integer> reflected = new HashSet<>();
        for (SpirvModule module : pack.modules().values()) {
            reflected.addAll(module.descriptorKeys());
        }

        if (!declared.isEmpty() && !reflected.containsAll(declared)) {
            return DescriptorValidation.invalid("metadata descriptorLayout contains entries not found in SPIR-V decorations");
        }

        return DescriptorValidation.ok();
    }

    public record DescriptorValidation(boolean valid, String reason) {
        public static DescriptorValidation ok() {
            return new DescriptorValidation(true, "ok");
        }

        public static DescriptorValidation invalid(String reason) {
            return new DescriptorValidation(false, reason);
        }
    }
}
