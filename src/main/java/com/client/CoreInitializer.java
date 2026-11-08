package com.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CoreInitializer implements ClientModInitializer {
    private static final String CAT = "Oppka Client";

    public static KeyBinding menuKey, hudKey, nightKey, zoomKey, hudEditorKey, triggerKey;
    public static boolean zoomActive = false, wasNight = false, wasBright = false, lastAttack = false;
    public static int savedFov = 70;
    public static float curFov = 70f;
    public static double savedGamma = 0.5;
    public static final List<Long> clicks = new ArrayList<>();
    public static float speed = 0f;
    public static double prevX, prevZ;

    @Override
    public void onInitializeClient() {
        menuKey = reg("Open menu", GLFW.GLFW_KEY_M);
        hudKey = reg("HUD on/off", GLFW.GLFW_KEY_H);
        nightKey = reg("Night vision", GLFW.GLFW_KEY_N);
        zoomKey = reg("Zoom (hold)", GLFW.GLFW_KEY_C);
        hudEditorKey = reg("HUD Editor", GLFW.GLFW_KEY_RIGHT_SHIFT);
        triggerKey = reg("TriggerBot Toggle", GLFW.GLFW_KEY_R);

        HudManager.init();
        ClientMenu.load();

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((ctx, tc) -> HudManager.render(ctx));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && ClientMenu.HITSOUND.enabled) playHit();
            return ActionResult.PASS;
        });
    }

    private KeyBinding reg(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(name, InputUtil.Type.KEYSYM, key, CAT));
    }

    public static void playHit() {
        var sm = MinecraftClient.getInstance().getSoundManager();
        float p = ClientMenu.S_HPITCH.v;
        switch (ClientMenu.S_HSOUND.idx) {
            case 0 -> sm.play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, p));
            case 1 -> sm.play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BELL, p));
            case 2 -> sm.play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BIT, p));
            case 3 -> sm.play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, p));
            default -> sm.play(PositionedSoundInstance.master(SoundEvents.ENTITY_ARROW_HIT_PLAYER, p));
        }
    }

    private void tick(MinecraftClient mc) {
        if (mc.player == null) return;

        while (menuKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new ClientMenu.MenuScreen());
        }
        while (hudEditorKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new ClientMenu.HudEditorScreen());
        }
        while (hudKey.wasPressed()) {
            HudManager.hudVisible = !HudManager.hudVisible;
        }
        while (nightKey.wasPressed()) {
            ClientMenu.NIGHT.enabled = !ClientMenu.NIGHT.enabled;
        }
        while (triggerKey.wasPressed()) {
            ClientMenu.TRIGGER_BOT.enabled = !ClientMenu.TRIGGER_BOT.enabled;
            mc.player.sendMessage(
                
