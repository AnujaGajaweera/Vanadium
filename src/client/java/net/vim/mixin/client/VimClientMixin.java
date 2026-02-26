package net.vim.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.vim.util.StructuredLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class VimClientMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vim/ClientMixin");

    @Inject(method = "stop", at = @At("HEAD"))
    private void vim$onClientShutdown(CallbackInfo ci) {
        StructuredLog.info(LOGGER, "client-shutdown", StructuredLog.kv("source", "mixin"));
    }
}
