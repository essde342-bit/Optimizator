package com.essde.optimizator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class OptimizatorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OptimizatorConfig.load();
        OptimizatorRuntime.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(OptimizatorRuntime::tick);
    }
}
