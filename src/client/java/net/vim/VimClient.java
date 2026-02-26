package net.vim;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VimClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vim/Client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Vim client entrypoint initialized");
    }
}
