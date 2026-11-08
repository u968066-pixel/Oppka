package com.client;

import com.client.modules.combat.Aura;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class CoreInitializer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        System.out.println("[Oppka Client] Инициализация модулей запущена...");

        // Регистрируем эвент конца клиентского тика для работы Ауры
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null) {
                // Вызываем логику нашей боевой ауры каждый тик
                Aura.onTick();
            }
        });

        System.out.println("[Oppka Client] Аура успешно интегрирована!");
    }
}
