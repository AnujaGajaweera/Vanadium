package net.vim;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.vim.compat.CompatibilityGuard;
import net.vim.compat.CompatibilityStatus;
import net.vim.compute.ComputeManager;
import net.vim.fallback.FallbackRenderer;
import net.vim.hotreload.FileWatcherService;
import net.vim.hotreload.HotReloadService;
import net.vim.shader.DescriptorManager;
import net.vim.shader.PipelineBuilder;
import net.vim.shader.ShaderManager;
import net.vim.shader.pack.MetadataParser;
import net.vim.shader.pack.ShaderPackLoader;
import net.vim.shader.pack.SpirvInspector;
import net.vim.ui.UILayer;
import net.vim.util.StructuredLog;
import net.vim.vulkan.SafeVulkanBackend;
import net.vim.vulkan.VulkanBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public final class mod implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vim");
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
                "vulkanmod", status.vulkanModLoaded()
        ));

        if (!status.canBootVim()) {
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

        StructuredLog.info(LOGGER, "vim-started", StructuredLog.kv(
                "shaderpacksDir", shaderpacksDir
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
