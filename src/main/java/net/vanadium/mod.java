package net.vanadium;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.vanadium.compat.CompatibilityGuard;
import net.vanadium.compat.CompatibilityStatus;
import net.vanadium.compute.ComputeManager;
import net.vanadium.fallback.FallbackRenderer;
import net.vanadium.hotreload.FileWatcherService;
import net.vanadium.hotreload.HotReloadService;
import net.vanadium.shader.DescriptorManager;
import net.vanadium.shader.PipelineBuilder;
import net.vanadium.shader.ShaderManager;
import net.vanadium.shader.pack.MetadataParser;
import net.vanadium.shader.pack.ShaderPackLoader;
import net.vanadium.shader.pack.SpirvInspector;
import net.vanadium.ui.UILayer;
import net.vanadium.util.StructuredLog;
import net.vanadium.vulkan.SafeVulkanBackend;
import net.vanadium.vulkan.VulkanBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public final class mod implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vanadium");
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private VulkanBackend backend;
    private ShaderManager shaderManager;
    private FileWatcherService fileWatcherService;

    @Override
    public void onInitialize() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        CompatibilityStatus status = new CompatibilityGuard(FabricLoader.getInstance()).inspect();
        StructuredLog.info(LOGGER, "compatibility-status", StructuredLog.kv(
                "vulkanmod", status.vulkanModLoaded(),
                "iris", status.irisLoaded(),
                "sodium", status.sodiumLoaded(),
                "lithium", status.lithiumLoaded(),
                "phosphor", status.phosphorLoaded()
        ));

        if (!status.canBootVanadium()) {
            StructuredLog.warn(LOGGER, "dormant-mode", StructuredLog.kv("reason", "vulkanmod missing"));
            return;
        }

        Path shaderpacksDir = FabricLoader.getInstance().getGameDir().resolve("shaderpacks");
        try {
            Files.createDirectories(shaderpacksDir);
        } catch (Exception e) {
            StructuredLog.error(LOGGER, "shaderpack-dir-create-failed", StructuredLog.kv("dir", shaderpacksDir), e);
            return;
        }

        backend = new SafeVulkanBackend(LOGGER);
        if (!backend.initialize()) {
            StructuredLog.warn(LOGGER, "backend-initialize-failed", StructuredLog.kv("backend", "SafeVulkanBackend"));
            return;
        }

        ShaderPackLoader loader = new ShaderPackLoader(LOGGER, new MetadataParser(), new SpirvInspector());
        DescriptorManager descriptorManager = new DescriptorManager();
        PipelineBuilder pipelineBuilder = new PipelineBuilder(backend);
        ComputeManager computeManager = new ComputeManager(LOGGER, pipelineBuilder, backend);
        FallbackRenderer fallbackRenderer = new FallbackRenderer(LOGGER);

        shaderManager = new ShaderManager(
                LOGGER,
                shaderpacksDir,
                loader,
                descriptorManager,
                pipelineBuilder,
                computeManager,
                backend,
                fallbackRenderer
        );

        HotReloadService hotReloadService = new HotReloadService(shaderManager);
        fileWatcherService = new FileWatcherService(LOGGER, shaderpacksDir, hotReloadService::triggerReload);

        shaderManager.reloadPacks();
        fileWatcherService.start();
        new UILayer(shaderManager, hotReloadService).register();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());

        StructuredLog.info(LOGGER, "vanadium-started", StructuredLog.kv(
                "shaderpacksDir", shaderpacksDir,
                "irisIsolation", status.irisLoaded()
        ));
    }

    private void shutdown() {
        if (fileWatcherService != null) {
            fileWatcherService.stop();
        }
        if (shaderManager != null) {
            shaderManager.deactivateActivePack();
        }
        if (backend != null) {
            backend.shutdown();
        }
    }
}
