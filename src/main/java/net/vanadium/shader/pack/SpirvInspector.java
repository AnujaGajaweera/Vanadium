package net.vanadium.shader.pack;

import net.vanadium.shader.model.ShaderStage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class SpirvInspector {
    private static final int SPIRV_MAGIC = 0x07230203;
    private static final int OP_ENTRY_POINT = 15;
    private static final int OP_DECORATE = 71;
    private static final int DECORATION_BINDING = 33;
    private static final int DECORATION_DESCRIPTOR_SET = 34;

    public SpirvInspection inspect(byte[] bytes, ShaderStage expectedStage, String expectedEntryPoint) {
        if (bytes.length < 20 || (bytes.length % 4) != 0) {
            return SpirvInspection.invalid("Invalid SPIR-V binary length");
        }

        IntArray words = IntArray.from(bytes);
        if (words.get(0) != SPIRV_MAGIC) {
            return SpirvInspection.invalid("Invalid SPIR-V magic number");
        }

        Set<String> entryPoints = new HashSet<>();
        Set<ShaderStage> entryStages = new HashSet<>();
        Map<Integer, Integer> setsById = new HashMap<>();
        Map<Integer, Integer> bindingsById = new HashMap<>();

        for (int i = 5; i < words.length(); ) {
            int instruction = words.get(i);
            int wordCount = (instruction >>> 16) & 0xFFFF;
            int opcode = instruction & 0xFFFF;
            if (wordCount <= 0 || i + wordCount > words.length()) {
                return SpirvInspection.invalid("Malformed SPIR-V instruction stream");
            }

            if (opcode == OP_ENTRY_POINT) {
                ShaderStage stage = decodeExecutionModel(words.get(i + 1));
                String name = parseString(words, i + 3, wordCount - 3);
                entryStages.add(stage);
                entryPoints.add(name);
            } else if (opcode == OP_DECORATE && wordCount >= 4) {
                int targetId = words.get(i + 1);
                int decoration = words.get(i + 2);
                int literal = words.get(i + 3);
                if (decoration == DECORATION_DESCRIPTOR_SET) {
                    setsById.put(targetId, literal);
                } else if (decoration == DECORATION_BINDING) {
                    bindingsById.put(targetId, literal);
                }
            }

            i += wordCount;
        }

        if (!entryPoints.contains(expectedEntryPoint)) {
            return SpirvInspection.invalid("Missing expected entry point: " + expectedEntryPoint);
        }

        if (!entryStages.contains(expectedStage)) {
            return SpirvInspection.invalid("Entry point stage mismatch: expected " + expectedStage);
        }

        Set<Integer> descriptorKeys = new HashSet<>();
        for (Map.Entry<Integer, Integer> setEntry : setsById.entrySet()) {
            Integer binding = bindingsById.get(setEntry.getKey());
            if (binding != null) {
                int key = setEntry.getValue() * 1000 + binding;
                descriptorKeys.add(key);
            }
        }

        return SpirvInspection.valid(expectedStage, expectedEntryPoint, descriptorKeys);
    }

    private static ShaderStage decodeExecutionModel(int value) {
        return switch (value) {
            case 0 -> ShaderStage.VERTEX;
            case 3 -> ShaderStage.GEOMETRY;
            case 4 -> ShaderStage.FRAGMENT;
            case 5 -> ShaderStage.COMPUTE;
            default -> throw new IllegalArgumentException("Unsupported SPIR-V execution model: " + value);
        };
    }

    private static String parseString(IntArray words, int startIndex, int maxWords) {
        ByteBuffer buffer = ByteBuffer.allocate(maxWords * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int j = 0; j < maxWords; j++) {
            buffer.putInt(words.get(startIndex + j));
        }
        byte[] bytes = buffer.array();
        int end = 0;
        while (end < bytes.length && bytes[end] != 0) {
            end++;
        }
        return new String(bytes, 0, end);
    }

    public record SpirvInspection(boolean valid, String message, ShaderStage stage, String entryPoint, Set<Integer> descriptorKeys) {
        public static SpirvInspection valid(ShaderStage stage, String entryPoint, Set<Integer> descriptorKeys) {
            return new SpirvInspection(true, "ok", stage, entryPoint, Set.copyOf(descriptorKeys));
        }

        public static SpirvInspection invalid(String message) {
            return new SpirvInspection(false, message, null, null, Set.of());
        }

        @Override
        public Set<Integer> descriptorKeys() {
            return Objects.requireNonNullElse(descriptorKeys, Set.of());
        }
    }

    private record IntArray(int[] words) {
        static IntArray from(byte[] bytes) {
            int[] data = new int[bytes.length / 4];
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < data.length; i++) {
                data[i] = buffer.getInt();
            }
            return new IntArray(data);
        }

        int get(int index) {
            return words[index];
        }

        int length() {
            return words.length;
        }
    }
}
