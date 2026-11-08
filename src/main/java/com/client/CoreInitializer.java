package com.client;

import com.client.modules.combat.Aura;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CoreInitializer implements ClientModInitializer {
    public static final List<Long> clicks = new ArrayList<>();
    private static KeyBinding menuKeyBinding;

    @Override
    public void onInitializeClient() {
        // Биндим открытие меню на правый Shift
        menuKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oppka.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.oppka"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null) {
                while (menuKeyBinding.wasPressed()) {
                    client.setScreen(new ClientMenu());
                }
                Aura.onTick();
            }
        });
    }
}
