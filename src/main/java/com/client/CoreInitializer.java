package com.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CoreInitializer implements ClientModInitializer {
    public static final String MOD_ID = "internal_client";

    private static KeyBinding toggleKey;
    private static KeyBinding selfDestructKey;
    private static boolean active = false;
    private static boolean destroyed = false;

    @Override
    public void onInitializeClient() {
        // Регистрация клавиши открытия GUI (Правый Shift) — исправлено на KEYSYM
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.internal_client.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.internal_client"
        ));

        // Регистрация клавиши самоуничтожения (Клавиша DELETE)
        selfDestructKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.internal_client.destruct",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_DELETE,
                "category.internal_client"
        ));

        // Регистрация обработчика тиков клиента
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (destroyed) {
                return;
            }

            while (selfDestructKey.wasPressed()) {
                executeSelfDestruct(client);
                return;
            }

            while (toggleKey.wasPressed()) {
                active = !active;
            }
        });
    }

    /**
     * Процедура полного стирания следов мода в рантайме
     */
    public static void executeSelfDestruct(MinecraftClient client) {
        if (destroyed) return;
        
        destroyed = true;
        active = false;

        // Безопасный сброс биндов на UNKNOWN взамен изменения final-массива allKeys
        if (toggleKey != null && selfDestructKey != null) {
            toggleKey.setBoundKey(InputUtil.UNKNOWN_KEY);
            selfDestructKey.setBoundKey(InputUtil.UNKNOWN_KEY);
        }

        System.gc();
    }

    public static boolean isDestroyed() {
        return destroyed;
    }

    public static boolean isActive() {
        return active && !destroyed;
    }
}
