package com.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CoreInitializer implements ClientModInitializer {
    private static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        // Регистрируем клавишу Правый Shift (GLFW.GLFW_KEY_RIGHT_SHIFT) для открытия меню
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.internal_client.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.internal_client.general"
        ));

        // Слушаем нажатия в каждом кадре игры
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                // Открываем наше меню на клиенте
                client.setScreen(new MyMenuScreen());
            }
        });
    }
}
