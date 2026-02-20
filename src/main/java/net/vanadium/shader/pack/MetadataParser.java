package net.vanadium.shader.pack;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.vanadium.shader.model.DescriptorBindingSpec;
import net.vanadium.shader.model.RenderPassSpec;
import net.vanadium.shader.model.ShaderModuleConfig;
import net.vanadium.shader.model.ShaderPackMetadata;
import net.vanadium.shader.model.ShaderStage;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MetadataParser {
    private static final Gson GSON = new Gson();

    public ShaderPackMetadata parse(InputStream inputStream) {
        JsonObject root = GSON.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), JsonObject.class);
        if (root == null) {
            throw new JsonParseException("metadata.json is empty or malformed");
        }

        String name = required(root, "name");
        String author = required(root, "author");
        String description = required(root, "description");
        String version = required(root, "version");
        String supportedMinecraftVersion = required(root, "supportedMinecraftVersion");
        String icon = optional(root, "icon");

        JsonObject modulesNode = objectNode(root, "modules");
        Map<ShaderStage, ShaderModuleConfig> modules = new EnumMap<>(ShaderStage.class);
        readModule(modulesNode, modules, "vertex", ShaderStage.VERTEX);
        readModule(modulesNode, modules, "fragment", ShaderStage.FRAGMENT);
        readModule(modulesNode, modules, "compute", ShaderStage.COMPUTE);
        readModule(modulesNode, modules, "geometry", ShaderStage.GEOMETRY);

        if (!modules.containsKey(ShaderStage.COMPUTE)) {
            throw new JsonParseException("modules.compute is required");
        }

        List<DescriptorBindingSpec> descriptorLayout = parseDescriptorLayout(root.get("descriptorLayout"));
        RenderPassSpec renderPass = parseRenderPass(root.get("renderPass"));

        return new ShaderPackMetadata(
                name,
                author,
                description,
                version,
                supportedMinecraftVersion,
                icon,
                modules,
                descriptorLayout,
                renderPass
        );
    }

    private static void readModule(JsonObject modulesNode, Map<ShaderStage, ShaderModuleConfig> modules, String key, ShaderStage stage) {
        if (!modulesNode.has(key)) {
            return;
        }
        JsonObject moduleNode = modulesNode.getAsJsonObject(key);
        String path = required(moduleNode, "path");
        String entry = optional(moduleNode, "entryPoint");
        modules.put(stage, new ShaderModuleConfig(stage, path, entry));
    }

    private static List<DescriptorBindingSpec> parseDescriptorLayout(JsonElement element) {
        List<DescriptorBindingSpec> bindings = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return bindings;
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("descriptorLayout must be an array");
        }

        for (JsonElement e : element.getAsJsonArray()) {
            JsonObject obj = e.getAsJsonObject();
            bindings.add(new DescriptorBindingSpec(
                    intRequired(obj, "set"),
                    intRequired(obj, "binding"),
                    required(obj, "type"),
                    required(obj, "name")
            ));
        }
        return bindings;
    }

    private static RenderPassSpec parseRenderPass(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return new RenderPassSpec(1, true);
        }
        JsonObject obj = element.getAsJsonObject();
        int colorAttachments = intRequired(obj, "colorAttachments");
        boolean depthAttachment = obj.has("depthAttachment") && obj.get("depthAttachment").getAsBoolean();

        if (colorAttachments < 1 || colorAttachments > 8) {
            throw new JsonParseException("renderPass.colorAttachments must be in [1,8]");
        }

        return new RenderPassSpec(colorAttachments, depthAttachment);
    }

    private static String required(JsonObject root, String key) {
        if (!root.has(key) || root.get(key).isJsonNull()) {
            throw new JsonParseException("Missing required metadata field: " + key);
        }
        String value = root.get(key).getAsString();
        if (value == null || value.isBlank()) {
            throw new JsonParseException("Metadata field is blank: " + key);
        }
        return value;
    }

    private static int intRequired(JsonObject root, String key) {
        if (!root.has(key) || root.get(key).isJsonNull()) {
            throw new JsonParseException("Missing required metadata field: " + key);
        }
        return root.get(key).getAsInt();
    }

    private static String optional(JsonObject root, String key) {
        if (!root.has(key) || root.get(key).isJsonNull()) {
            return null;
        }
        String value = root.get(key).getAsString();
        return value == null || value.isBlank() ? null : value;
    }

    private static JsonObject objectNode(JsonObject root, String key) {
        if (!root.has(key) || root.get(key).isJsonNull()) {
            throw new JsonParseException("Missing required object field: " + key);
        }
        JsonElement element = root.get(key);
        if (!element.isJsonObject()) {
            throw new JsonParseException(key + " must be an object");
        }
        return element.getAsJsonObject();
    }
}
