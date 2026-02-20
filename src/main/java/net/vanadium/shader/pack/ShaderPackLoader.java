package net.vanadium.shader.pack;

import net.vanadium.shader.model.LoadedShaderPack;
import net.vanadium.shader.model.PackLoadResult;
import net.vanadium.shader.model.ShaderModuleConfig;
import net.vanadium.shader.model.ShaderPackMetadata;
import net.vanadium.shader.model.ShaderStage;
import net.vanadium.shader.model.SpirvModule;
import net.vanadium.util.StructuredLog;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

            Map<ShaderStage, SpirvModule> modules = new EnumMap<>(ShaderStage.class);
            for (Map.Entry<ShaderStage, ShaderModuleConfig> moduleEntry : metadata.modules().entrySet()) {
                ShaderModuleConfig moduleConfig = moduleEntry.getValue();
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
}
