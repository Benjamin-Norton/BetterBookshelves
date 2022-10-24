package com.bawnorton.betterbookshelves.client;

import com.bawnorton.betterbookshelves.render.BetterBookshelvesModelProvider;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;

public class BetterBookshelvesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLoadingRegistry.INSTANCE.registerResourceProvider(manager -> new BetterBookshelvesModelProvider());
    }
}