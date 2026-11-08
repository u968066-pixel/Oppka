package com.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CoreInitializer implements ClientModInitializer {
    public static final String MOD_ID = "internal_client";
    private static KeyBinding toggleKey;
    private static boolean active = false;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.internal_client.toggle",
                InputUtil.Type.KEY_SYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.internal_client"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                active = !active;
                // Место для вызова модулей или скрытого интерфейса
            }
        });
    }

    public static boolean isActive() {
        return active;
    }
}
