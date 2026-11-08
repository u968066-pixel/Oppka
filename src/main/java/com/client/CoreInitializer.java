package com.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class CoreInitializer implements ClientModInitializer {
    private static KeyBinding openMenuKey;
    private static KeyBinding toggleBotKey;
    
    // Состояние нашего триггер-бота (включен или выключен)
    private static boolean triggerBotEnabled = false;

    @Override
    public void onInitializeClient() {
        // 1. Клавиша для открытия меню (Правый Shift)
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.internal_client.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.internal_client.general"
        ));

        // 2. Клавиша для включения/выключения TriggerBot (например, клавиша R)
        toggleBotKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.internal_client.toggle_bot",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.internal_client.general"
        ));

        // Обработка нажатий в каждом такте игры
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Проверяем открытие меню
            while (openMenuKey.wasPressed()) {
                client.setScreen(new MyMenuScreen());
            }

            // Проверяем включение/выключение бота по клавише R
            while (toggleBotKey.wasPressed()) {
                triggerBotEnabled = !triggerBotEnabled;
                if (client.player != null) {
                    String status = triggerBotEnabled ? "§aВключен" : "§cВыключен";
                    client.player.sendMessage(TextLiteral("§7[Client] TriggerBot: " + status), true);
                }
            }

            // Логика TriggerBot: если включен и игра активна
            if (triggerBotEnabled && client.player != null && client.interactionManager != null) {
                // Смотрим, на кого направлен прицел игрока
                HitResult hit = client.crosshairTarget;
                if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult entityHit = (EntityHitResult) hit;
                    Entity target = entityHit.getEntity();

                    // Бьем только если цель — живое существо (моб или игрок) и это не мы сами
                    if (target instanceof LivingEntity && target != client.player) {
                        // Проверяем, что кулаук атаки полный (чтобы удар был с полным уроном)
                        if (client.player.getAttackCooldown_Progress(0.5f) >= 1.0f) {
                            client.interactionManager.attackEntity(client.player, target);
                            client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                        }
                    }
                }
            }
        });
    }
}
