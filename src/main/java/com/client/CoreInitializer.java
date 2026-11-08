package com.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

public class CoreInitializer implements ClientModInitializer {
    public static final String MOD_ID = "internal_client";

    private static KeyBinding toggleKey;
    private static KeyBinding selfDestructKey;
    private static boolean active = false;
    private static boolean destroyed = false;

    @Override
    public void onInitializeClient() {
        // Регистрация клавиши открытия GUI (Правый Shift)
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.internal_client.toggle",
                InputUtil.Type.KEY_SYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.internal_client"
        ));

        // Регистрация клавиши самоуничтожения (Клавиша DELETE)
        selfDestructKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.internal_client.destruct",
                InputUtil.Type.KEY_SYM,
                GLFW.GLFW_KEY_DELETE,
                "category.internal_client"
        ));

        // Регистрация обработчика тиков клиента
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Если режим самоуничтожения активирован, полностью игнорируем любые события
            if (destroyed) {
                return;
            }

            // Обработка нажатия клавиши Self-Destruct
            while (selfDestructKey.wasPressed()) {
                executeSelfDestruct(client);
                return;
            }

            // Обработка штатного переключения GUI
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
        
        // 1. Установка флагов отключения
        destroyed = true;
        active = false;

        // 2. Очистка биндов из массива управления Minecraft (удаление из меню настроек)
        if (client.options != null && client.options.allKeys != null) {
            client.options.allKeys = Arrays.stream(client.options.allKeys)
                    .filter(kb -> kb != toggleKey && kb != selfDestructKey)
                    .toArray(KeyBinding[]::new);
        }

        // 3. Очистка ссылок на менеджеры модулей / массивов данных (если они есть)
        // Пример: ModuleManager.getModules().clear();

        // 4. Принудительный вызов сборщика мусора для очистки выделенной памяти
        System.gc();
    }

    public static boolean isDestroyed() {
        return destroyed;
    }

    public static boolean isActive() {
        return active && !destroyed;
    }
}
