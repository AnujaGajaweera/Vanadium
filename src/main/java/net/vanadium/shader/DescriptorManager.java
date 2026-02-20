package net.vanadium.shader;

import net.vanadium.shader.model.DescriptorBindingSpec;
import net.vanadium.shader.model.LoadedShaderPack;
import net.vanadium.shader.model.SpirvModule;

import java.util.HashSet;
import java.util.Set;

public final class DescriptorManager {
    public DescriptorValidation validate(LoadedShaderPack pack) {
        Set<Integer> declared = new HashSet<>();
        for (DescriptorBindingSpec spec : pack.metadata().descriptorLayout()) {
            if (spec.set() < 0 || spec.binding() < 0) {
                return DescriptorValidation.invalid("Negative descriptor set/binding in metadata");
            }
            declared.add(spec.set() * 1000 + spec.binding());
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
