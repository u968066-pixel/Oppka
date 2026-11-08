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
            mc.player.sendMessage(Text.literal("§7[Oppka] TriggerBot: " + (ClientMenu.TRIGGER_BOT.enabled ? "§aON" : "§cOFF")), true);
        }

        long now = System.currentTimeMillis();
        boolean atk = mc.options.attackKey.isPressed();
        if (atk && !lastAttack) clicks.add(now);
        lastAttack = atk;
        clicks.removeIf(t -> now - t > 1000);

        double dx = mc.player.getX() - prevX, dz = mc.player.getZ() - prevZ;
        prevX = mc.player.getX();
        prevZ = mc.player.getZ();
        speed = speed * 0.8f + (float) (Math.sqrt(dx * dx + dz * dz) * 20) * 0.2f;

        if (ClientMenu.NIGHT.enabled) {
            StatusEffectInstance e = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (e == null || e.getDuration() < 220) {
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
            }
        } else if (wasNight) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        wasNight = ClientMenu.NIGHT.enabled;

        var gamma = mc.options.getGamma();
        if (ClientMenu.BRIGHT.enabled != wasBright) {
            if (ClientMenu.BRIGHT.enabled) {
                double g = gamma.getValue();
                savedGamma = g >= 1.0 ? 0.5 : g;
                gamma.setValue(1.0);
            } else {
                gamma.setValue(savedGamma);
            }
            wasBright = ClientMenu.BRIGHT.enabled;
        }

        if (ClientMenu.SPRINT.enabled && mc.options.forwardKey.isPressed() && !mc.player.isSneaking() && !mc.player.horizontalCollision) {
            mc.player.setSprinting(true);
        }
        if (ClientMenu.AUTO_RESPAWN.enabled && mc.player.isDead()) {
            mc.player.requestRespawn();
        }
        if (ClientMenu.NO_HURT_CAM.enabled) {
            mc.player.hurtTime = 0;
            mc.player.timeUntilRegen = 0;
        }

        var fov = mc.options.getFov();
        boolean held = ClientMenu.ZOOM.enabled && mc.currentScreen == null && zoomKey.isPressed();
        if (held && !zoomActive) {
            savedFov = fov.getValue();
            curFov = savedFov;
            zoomActive = true;
        }
        if (zoomActive) {
            float target = held ? ClientMenu.S_ZOOM.v : savedFov;
            curFov = ClientMenu.S_SMOOTH.b ? curFov + (target - curFov) * 0.4f : target;
            fov.setValue(Math.round(curFov));
            if (!held && Math.abs(curFov - savedFov) < 1f) {
                fov.setValue(savedFov);
                zoomActive = false;
            }
        }

        // БЕЗОПАСНЫЙ TRIGGERBOT С РАНДОМИЗАЦИЕЙ КУЛДАУНА
        if (ClientMenu.TRIGGER_BOT.enabled && mc.interactionManager != null) {
            HitResult hit = mc.crosshairTarget;
            if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
                Entity target = ((EntityHitResult) hit).getEntity();
                if (target instanceof LivingEntity && target != mc.player) {
                    float requiredCooldown = 0.85f + (float)(Math.random() * 0.12f);
                    
                    if (mc.player.getAttackCooldownProgress(0.0f) >= requiredCooldown) {
                        mc.interactionManager.attackEntity(mc.player, target);
                        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                    }
                }
            }
        }
    }
            }
