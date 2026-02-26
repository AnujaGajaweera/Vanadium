package net.vim.ui;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.vim.hotreload.HotReloadService;
import net.vim.shader.ShaderManager;

public final class UILayer {
    private final ShaderManager shaderManager;
    private final HotReloadService hotReloadService;

    public UILayer(ShaderManager shaderManager, HotReloadService hotReloadService) {
        this.shaderManager = shaderManager;
        this.hotReloadService = hotReloadService;
    }

    public void register() {
        registerCommands();
        registerVideoOptionsIntegration();
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("vim")
                        .then(ClientCommandManager.literal("list").executes(context -> {
                            shaderManager.listPacks().forEach(pack -> send("- " + pack.id() + " :: " + pack.metadata().name()));
                            if (shaderManager.listPacks().isEmpty()) {
                                send("No valid .mcshader packs found in /shaderpacks");
                            }
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("status").executes(context -> {
                            String active = shaderManager.activePackId().orElse("none");
                            send("active=" + active + ", fallback=" + shaderManager.isFallbackActive());
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("reload").executes(context -> {
                            hotReloadService.triggerReload();
                            send("Vim shader packs reloaded.");
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("activate")
                                .then(ClientCommandManager.argument("id", StringArgumentType.word())
                                        .executes(context -> {
                                            String id = StringArgumentType.getString(context, "id");
                                            if (shaderManager.activatePack(id)) {
                                                send("Activated pack: " + id);
                                            } else {
                                                send("Failed to activate pack: " + id);
                                            }
                                            return 1;
                                        })))
        ));
    }

    private void registerVideoOptionsIntegration() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof VideoOptionsScreen)) {
                return;
            }
            if (hasVimButton(screen)) {
                return;
            }

            ButtonWidget openButton = ButtonWidget.builder(Text.literal("Vim Shaders"), button ->
                            client.setScreen(new VimShaderConfigScreen(screen, shaderManager, hotReloadService)))
                    .dimensions(screen.width - 154, 6, 148, 20)
                    .build();
            Screens.getButtons(screen).add(openButton);
        });
    }

    private static boolean hasVimButton(net.minecraft.client.gui.screen.Screen screen) {
        for (var element : screen.children()) {
            if (element instanceof ButtonWidget button
                    && "Vim Shaders".equals(button.getMessage().getString())) {
                return true;
            }
        }
        return false;
    }

    private static void send(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("[Vim] " + message), false);
        }
    }
}
