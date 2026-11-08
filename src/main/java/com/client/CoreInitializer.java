package com.client;

import com.client.modules.combat.Aura;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.ArrayList;
import java.util.List;

public class CoreInitializer implements ClientModInitializer {
    // Явно объявляем список кликов, чтобы HudManager мог к нему обращаться
    public static final List<Long> clicks = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        System.out.println("[Oppka Client] Инициализация модулей запущена...");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null) {
                Aura.onTick();
            }
        });

        System.out.println("[Oppka Client] Аура успешно интегрирована!");
    }
}
