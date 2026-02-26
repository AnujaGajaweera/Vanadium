package net.vim.shader.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.vim.shader.model.LoadedShaderPack;
import net.vim.shader.model.PackLoadResult;
import net.vim.shader.model.ShaderModuleConfig;
import net.vim.shader.model.ShaderPackMetadata;
import net.vim.shader.model.ShaderStage;
import net.vim.shader.model.SpirvModule;
import net.vim.util.StructuredLog;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ShaderPackLoader {
    private static final String EXTENSION = ".mcshader";
    private static final int MAX_ENTRIES = 2048;
    private static final long MAX_ENTRY_SIZE = 32L * 1024L * 1024L;

    private final Logger logger;
    private final MetadataParser metadataParser;
    private final SpirvInspector spirvInspector;

    public ShaderPackLoader(Logger logger, MetadataParser metadataParser, SpirvInspector spirvInspector) {
        this.logger = logger;
        this.metadataParser = metadataParser;
        this.spirvInspector = spirvInspector;
    }

    public List<PackLoadResult> scan(Path shaderpacksDirectory) {
        if (!Files.exists(shaderpacksDirectory)) {
            return List.of();
        }

        List<PackLoadResult> results = new ArrayList<>();
        try (Stream<Path> files = Files.list(shaderpacksDirectory)) {
            files.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        if (fileName.endsWith(".zip")) {
                            return;
                        }
                        if (!fileName.endsWith(EXTENSION)) {
                            return;
                        }
                        String id = sanitizeId(path.getFileName().toString());
                        results.add(loadArchive(id, path));
                    });
        } catch (IOException e) {
            StructuredLog.error(logger, "scan-failure", StructuredLog.kv("dir", shaderpacksDirectory), e);
        }

        return Collections.unmodifiableList(results);
    }

    private PackLoadResult loadArchive(String id, Path path) {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            validateArchive(zip, id);

            ZipEntry metadataEntry = zip.getEntry("metadata.json");
            if (metadataEntry == null) {
                return PackLoadResult.failed(id, "metadata.json is missing");
            }

            ShaderPackMetadata metadata;
            try (InputStream inputStream = zip.getInputStream(metadataEntry)) {
                metadata = metadataParser.parse(inputStream);
            }

            String layoutError = validateStructuredLayout(zip, metadata);
            if (layoutError != null) {
                return PackLoadResult.failed(id, layoutError);
            }

            Map<ShaderStage, SpirvModule> modules = new EnumMap<>(ShaderStage.class);
            for (Map.Entry<ShaderStage, ShaderModuleConfig> moduleEntry : metadata.modules().entrySet()) {
                ShaderModuleConfig moduleConfig = moduleEntry.getValue();
                if (!moduleConfig.path().toLowerCase(Locale.ROOT).endsWith(".spv")) {
                    return PackLoadResult.failed(id, "Only compiled SPIR-V binaries are allowed (.spv): " + moduleConfig.path());
                }
                ZipEntry binaryEntry = zip.getEntry(moduleConfig.path());
                if (binaryEntry == null) {
                    return PackLoadResult.failed(id, "Module file missing: " + moduleConfig.path());
                }

                byte[] bytes;
                try (InputStream inputStream = zip.getInputStream(binaryEntry)) {
                    bytes = inputStream.readAllBytes();
                }

                SpirvInspector.SpirvInspection inspection = spirvInspector.inspect(
                        bytes,
                        moduleConfig.stage(),
                        moduleConfig.resolvedEntryPoint()
                );

                if (!inspection.valid()) {
                    return PackLoadResult.failed(id, "Invalid SPIR-V module " + moduleConfig.path() + ": " + inspection.message());
                }

                String moduleLayoutError = validateModuleLocation(moduleConfig);
                if (moduleLayoutError != null) {
                    return PackLoadResult.failed(id, moduleLayoutError);
                }

                modules.put(moduleConfig.stage(), new SpirvModule(
                        moduleConfig.stage(),
                        inspection.entryPoint(),
                        bytes,
                        inspection.descriptorKeys()
                ));
            }

            return PackLoadResult.success(new LoadedShaderPack(id, path, metadata, modules));
        } catch (Exception e) {
            StructuredLog.warn(logger, "pack-load-failed", StructuredLog.kv("pack", id, "reason", e.getMessage()));
            return PackLoadResult.failed(id, e.getMessage());
        }
    }

    private static void validateArchive(ZipFile zip, String id) throws IOException {
        int count = 0;
        for (ZipEntry entry : Collections.list(zip.entries())) {
            count++;
            if (count > MAX_ENTRIES) {
                throw new IOException("Archive has too many entries: " + id);
            }

            String name = entry.getName();
            if (name.startsWith("/") || name.contains("..")) {
                throw new IOException("Archive path traversal detected");
            }

            if (!entry.isDirectory() && entry.getSize() > MAX_ENTRY_SIZE) {
                throw new IOException("Archive entry exceeds size limit: " + name);
            }
        }
    }

    private static String sanitizeId(String fileName) {
        String base = fileName.substring(0, fileName.length() - EXTENSION.length());
        return base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }

    private static String validateStructuredLayout(ZipFile zip, ShaderPackMetadata metadata) {
        Set<String> entries = new HashSet<>();
        for (ZipEntry entry : Collections.list(zip.entries())) {
            entries.add(entry.getName());
        }

        String[] requiredFiles = {
                "LICENSE",
                "README.md",
                "metadata.json",
                "shaders/pack.json",
                "config/defaults.json",
                "config/toggles.json"
        };

        for (String requiredFile : requiredFiles) {
            if (!entries.contains(requiredFile)) {
                return "Structured pack is missing required file: " + requiredFile;
            }
        }

        if (!isNonEmptyFile(zip, "LICENSE") || !isNonEmptyFile(zip, "README.md")) {
            return "Structured pack requires non-empty LICENSE and README.md";
        }

        String[] requiredFolders = {
                "shaders/",
                "lib/",
                "world-0/",
                "world-1/",
                "world-2/",
                "textures/",
                "config/"
        };

        for (String folder : requiredFolders) {
            boolean hasFolder = entries.contains(folder) || entries.stream().anyMatch(name -> name.startsWith(folder));
            if (!hasFolder) {
                return "Structured pack is missing required folder: " + folder;
            }
        }

        boolean hasProperties = entries.stream().anyMatch(name -> name.startsWith("shaders/") && name.endsWith(".properties.xml"));
        if (!hasProperties) {
            return "Structured pack requires at least one shaders/*.properties.xml file";
        }

        String jsonError = validateJsonFiles(zip);
        if (jsonError != null) {
            return jsonError;
        }

        String iconPath = metadata.icon();
        if (iconPath != null && !iconPath.isBlank() && !entries.contains(iconPath)) {
            return "metadata.icon points to missing file: " + iconPath;
        }

        return null;
    }

    private static String validateModuleLocation(ShaderModuleConfig module) {
        String path = module.path();
        if (path.startsWith("/") || path.contains("..")) {
            return "Module path is unsafe: " + path;
        }

        return switch (module.stage()) {
            case VERTEX, FRAGMENT, GEOMETRY -> path.startsWith("world-")
                    ? null
                    : "Graphics module must be in world-* folder: " + path;
            case COMPUTE -> (path.startsWith("lib/") || path.startsWith("world-"))
                    ? null
                    : "Compute module must be in lib/ or world-* folder: " + path;
        };
    }

    private static boolean isNonEmptyFile(ZipFile zip, String path) {
        ZipEntry entry = zip.getEntry(path);
        return entry != null && !entry.isDirectory() && entry.getSize() > 0;
    }

    private static String validateJsonFiles(ZipFile zip) {
        String[] files = {"shaders/pack.json", "config/defaults.json", "config/toggles.json"};
        for (String file : files) {
            ZipEntry entry = zip.getEntry(file);
            if (entry == null || entry.isDirectory()) {
                return "Missing required JSON file: " + file;
            }
            try (InputStream in = zip.getInputStream(entry)) {
                JsonElement parsed = JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                if (parsed == null || !parsed.isJsonObject()) {
                    return file + " must contain a JSON object";
                }
            } catch (Exception e) {
                return "Invalid JSON in " + file + ": " + e.getMessage();
            }
        }
        return null;
    }
}
