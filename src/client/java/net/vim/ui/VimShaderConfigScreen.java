package net.vim.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.vim.hotreload.HotReloadService;
import net.vim.shader.ShaderManager;
import net.vim.shader.model.LoadedShaderPack;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class VimShaderConfigScreen extends Screen {
    private static final int PACKS_PER_PAGE = 8;

    private final Screen parent;
    private final ShaderManager shaderManager;
    private final HotReloadService hotReloadService;

    private final List<LoadedShaderPack> packs = new ArrayList<>();

    private ButtonWidget enableDisableButton;
    private ButtonWidget pageUpButton;
    private ButtonWidget pageDownButton;

    private int pageStartIndex;
    private String selectedPackId;
    private String statusLine = "";

    private NativeImageBackedTexture iconTexture;
    private Identifier iconTextureId;

    public VimShaderConfigScreen(Screen parent, ShaderManager shaderManager, HotReloadService hotReloadService) {
        super(Text.literal("Vim Shaders"));
        this.parent = parent;
        this.shaderManager = shaderManager;
        this.hotReloadService = hotReloadService;
    }

    @Override
    protected void init() {
        super.init();
        refreshPackCache();
        buildPackButtons();

        this.enableDisableButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
                    if (shaderManager.activePackId().isPresent()) {
                        shaderManager.deactivateActivePack();
                        statusLine = "Shaders disabled; fallback renderer active.";
                    } else {
                        if (selectedPackId != null && shaderManager.activatePack(selectedPackId)) {
                            statusLine = "Activated pack: " + selectedPackId;
                        } else {
                            statusLine = "Enable failed.";
                        }
                    }
                    refreshPackCache();
                    refreshButtonState();
                })
                .dimensions(this.width / 2 + 12, 40, this.width / 2 - 24, 20)
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reload Packs"), button -> {
                    hotReloadService.triggerReload();
                    refreshPackCache();
                    refreshButtonState();
                    statusLine = "Shader pack scan completed.";
                })
                .dimensions(this.width / 2 + 12, 66, this.width / 2 - 24, 20)
                .build());

        this.pageUpButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Previous"), button -> {
                    pageStartIndex = Math.max(0, pageStartIndex - PACKS_PER_PAGE);
                    rebuild();
                })
                .dimensions(20, this.height - 66, 90, 20)
                .build());

        this.pageDownButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Next"), button -> {
                    int maxStart = Math.max(0, packs.size() - PACKS_PER_PAGE);
                    pageStartIndex = Math.min(maxStart, pageStartIndex + PACKS_PER_PAGE);
                    rebuild();
                })
                .dimensions(116, this.height - 66, 90, 20)
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> this.close())
                .dimensions(this.width / 2 - 60, this.height - 30, 120, 20)
                .build());

        refreshButtonState();
    }

    @Override
    public void close() {
        clearIconTexture();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void removed() {
        clearIconTexture();
        super.removed();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);

        LoadedShaderPack selected = selectedPack();
        int rightX = this.width / 2 + 12;
        int y = 96;

        if (selected != null) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("Name: " + selected.metadata().name()), rightX, y, 0xE0E0E0);
            y += 12;
            context.drawTextWithShadow(this.textRenderer, Text.literal("Author: " + selected.metadata().author()), rightX, y, 0xE0E0E0);
            y += 12;
            context.drawTextWithShadow(this.textRenderer, Text.literal("Version: " + selected.metadata().version()), rightX, y, 0xE0E0E0);
            y += 12;
            context.drawTextWithShadow(this.textRenderer, Text.literal("MC: " + selected.metadata().supportedMinecraftVersion()), rightX, y, 0xE0E0E0);
            y += 12;
            context.drawTextWithShadow(this.textRenderer, Text.literal("Description:"), rightX, y, 0xE0E0E0);
            y += 12;
            context.drawTextWithShadow(this.textRenderer, Text.literal(trim(selected.metadata().description(), 48)), rightX, y, 0xB8B8B8);
            y += 18;

            if (iconTextureId != null) {
                context.drawTextWithShadow(this.textRenderer, Text.literal("Icon:"), rightX, y, 0xE0E0E0);
                y += 12;
                context.drawTexture(iconTextureId, rightX, y, 0.0F, 0.0F, 64, 64, 64, 64);
            } else {
                context.drawTextWithShadow(this.textRenderer, Text.literal("Icon: none"), rightX, y, 0x808080);
            }
        } else {
            context.drawTextWithShadow(this.textRenderer, Text.literal("No valid shader packs loaded."), rightX, y, 0xFF7777);
        }

        int errY = this.height - 110;
        context.drawTextWithShadow(this.textRenderer, Text.literal("Error Panel"), rightX, errY, 0xFF9999);
        errY += 12;

        if (!statusLine.isBlank()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal(trim(statusLine, 52)), rightX, errY, 0xFFB266);
            errY += 12;
        }

        String lastError = shaderManager.lastActionError().orElse(null);
        if (lastError != null) {
            context.drawTextWithShadow(
                    this.textRenderer,
                    Text.literal(trim(lastError, 52)),
                    rightX,
                    errY,
                    0xFF6666
            );
        }

        Map<String, String> packErrors = shaderManager.packErrors();
        int lines = 0;
        for (Map.Entry<String, String> entry : packErrors.entrySet()) {
            if (lines >= 2) {
                break;
            }
            context.drawTextWithShadow(
                    this.textRenderer,
                    Text.literal(trim(entry.getKey() + ": " + entry.getValue(), 52)),
                    rightX,
                    errY + 12 + (lines * 12),
                    0xFF6666
            );
            lines++;
        }
    }

    private void rebuild() {
        clearAndInit();
    }

    private void refreshPackCache() {
        packs.clear();
        packs.addAll(shaderManager.listPacks().stream()
                .sorted(Comparator.comparing(pack -> pack.metadata().name().toLowerCase()))
                .toList());

        if (selectedPackId == null && shaderManager.activePackId().isPresent()) {
            selectedPackId = shaderManager.activePackId().orElse(null);
        }

        if (selectedPackId == null && !packs.isEmpty()) {
            selectedPackId = packs.getFirst().id();
        }

        if (selectedPackId != null && packs.stream().noneMatch(pack -> pack.id().equals(selectedPackId))) {
            selectedPackId = packs.isEmpty() ? null : packs.getFirst().id();
        }

        int maxStart = Math.max(0, packs.size() - PACKS_PER_PAGE);
        pageStartIndex = Math.min(pageStartIndex, maxStart);
        loadSelectedIcon();
    }

    private void buildPackButtons() {
        int y = 40;
        for (int i = 0; i < PACKS_PER_PAGE; i++) {
            int index = pageStartIndex + i;
            if (index >= packs.size()) {
                break;
            }

            LoadedShaderPack pack = packs.get(index);
            boolean active = shaderManager.activePackId().map(id -> id.equals(pack.id())).orElse(false);
            String prefix = active ? "* " : "";
            String label = trim(prefix + pack.metadata().name(), 26);

            this.addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> {
                        selectedPackId = pack.id();
                        loadSelectedIcon();
                        statusLine = "Selected: " + pack.id();
                        rebuild();
                    })
                    .dimensions(20, y, this.width / 2 - 32, 20)
                    .build());
            y += 22;
        }
    }

    private void refreshButtonState() {
        boolean enabled = shaderManager.activePackId().isPresent();
        this.enableDisableButton.setMessage(Text.literal(enabled ? "Disable Shaders" : "Enable Shaders"));

        this.pageUpButton.active = pageStartIndex > 0;
        this.pageDownButton.active = pageStartIndex + PACKS_PER_PAGE < packs.size();
    }

    private LoadedShaderPack selectedPack() {
        if (selectedPackId == null) {
            return null;
        }
        for (LoadedShaderPack pack : packs) {
            if (pack.id().equals(selectedPackId)) {
                return pack;
            }
        }
        return null;
    }

    private void loadSelectedIcon() {
        clearIconTexture();
        if (selectedPackId == null || this.client == null) {
            return;
        }

        shaderManager.loadIconBytes(selectedPackId).ifPresent(bytes -> {
            try {
                NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
                NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                Identifier id = this.client.getTextureManager().registerDynamicTexture("vim_icon_" + selectedPackId, texture);
                this.iconTexture = texture;
                this.iconTextureId = id;
            } catch (Exception ignored) {
            }
        });
    }

    private void clearIconTexture() {
        MinecraftClient client = this.client;
        if (client != null && this.iconTextureId != null) {
            client.getTextureManager().destroyTexture(this.iconTextureId);
        }
        this.iconTextureId = null;

        if (this.iconTexture != null) {
            this.iconTexture.close();
            this.iconTexture = null;
        }
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
