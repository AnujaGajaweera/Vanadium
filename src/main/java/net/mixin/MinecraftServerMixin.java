package net.mixin;

import net.minecraft.server.MinecraftServer;
import net.vim.util.StructuredLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    private static final Logger Vim_MIXIN_LOGGER = LoggerFactory.getLogger("Vim/Mixin");

    @Inject(method = "loadWorld", at = @At("HEAD"))
    private void vim$onServerWorldLoad(CallbackInfo ci) {
        StructuredLog.info(Vim_MIXIN_LOGGER, "mixin-load-world", StructuredLog.kv("hook", "minecraft-server-load-world"));
    }
}
