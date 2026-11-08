package com.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class CoreInitializer implements ClientModInitializer {
    private static KeyBinding openMenuKey;
    private static KeyBinding toggleBotKey;
    public static boolean triggerBotEnabled = false;

    @Override
    public void onInitializeClient() {
        // Кнопка открытия меню (Правый Shift)
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.internal_client.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.internal_client.general"
        ));

        // Кнопка быстрого включения TriggerBot (Клавиша R)
        toggleBotKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.internal_client.toggle_bot",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.internal_client.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                client.setScreen(new MyMenuScreen());
            }

            while (toggleBotKey.wasPressed()) {
                triggerBotEnabled = !triggerBotEnabled;
                if (client.player != null) {
                    String status = triggerBotEnabled ? "§aВключен" : "§cВыключен";
                    client.player.sendMessage(Text.literal("§7[Client] TriggerBot: " + status), true);
                }
            }

            if (triggerBotEnabled && client.player != null && client.interactionManager != null) {
                HitResult hit = client.crosshairTarget;
                if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult entityHit = (EntityHitResult) hit;
                    Entity target = entityHit.getEntity();

                    if (target instanceof LivingEntity && target != client.player) {
                        if (client.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
                            client.interactionManager.attackEntity(client.player, target);
                            client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                        }
                    }
                }
            }
        });
    }

    // Встроенный класс графического меню прямо здесь, чтобы не плодить файлы
    public static class MyMenuScreen extends Screen {
        public MyMenuScreen() {
            super(Text.literal("Internal Client Menu"));
        }

        @Override
        protected void init() {
            super.init();
            updateMenuButtons();
        }

        private void updateMenuButtons() {
            this.clearChildren();
            String botStatusText = CoreInitializer.triggerBotEnabled ? "§aTriggerBot: ВКЛ" : "§cTriggerBot: ВЫКЛ";

            this.addDrawableChild(
                ButtonWidget.builder(Text.literal(botStatusText), button -> {
                    CoreInitializer.triggerBotEnabled = !CoreInitializer.triggerBotEnabled;
                    updateMenuButtons();
                })
                .dimensions(this.width / 2 - 100, this.height / 2 - 20, 200, 20)
                .build()
            );
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context, mouseX, mouseY, delta);
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}
