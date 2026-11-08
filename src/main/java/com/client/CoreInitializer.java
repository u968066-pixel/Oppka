package com.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class CoreInitializer implements ClientModInitializer {

    // --- Полный набор модулей ---
    public static boolean killAura = true;
    public static boolean triggerBot = true;
    public static boolean flyMod = false;
    public static boolean autoSprint = true;
    public static boolean noHurtCam = true;
    public static boolean autoCrit = true; // Автоматические критические удары
    public static boolean strictBypass = true;

    public static double auraRange = 3.1;
    private static KeyBinding menuKey;
    private static long lastAttackTime = 0;
    private static final Random random = new Random();

    @Override
    public void onInitializeClient() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oppka.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.oppka"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register(this::onRenderHud);
    }

    private void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;

        while (menuKey.wasPressed()) {
            if (mc.currentScreen == null) {
                mc.setScreen(new OppkaMenuScreen());
            }
        }

        // Модуль: NoHurtCam
        if (noHurtCam && mc.player.hurtTime > 0) {
            mc.player.hurtTime = 0;
            mc.player.timeUntilRegen = 0;
        }

        // Модуль: AutoSprint
        if (autoSprint && !mc.player.isSneaking() && !mc.player.horizontalCollision && mc.player.input.movementForward > 0f) {
            mc.player.setSprinting(true);
        }

        // Продвинутый Motion-Fly с пакетной симуляцией опоры
        if (flyMod) {
            double vY = 0.0;
            if (mc.options.jumpKey.isPressed()) {
                vY = 0.42;
            } else if (mc.options.sneakKey.isPressed()) {
                vY = -0.42;
            } else {
                vY = (mc.player.age % 2 == 0) ? -0.04 : 0.0;
            }

            double forward = mc.player.input.movementForward;
            double sideways = mc.player.input.movementSideways;
            float yaw = mc.player.getYaw();

            if (forward != 0.0 || sideways != 0.0) {
                double cos = Math.cos(Math.toRadians(yaw + 90.0f));
                double sin = Math.sin(Math.toRadians(yaw + 90.0f));
                double speed = 0.26;
                
                double vX = (forward * speed * cos + sideways * speed * sin);
                double vZ = (forward * speed * sin - sideways * speed * cos);
                mc.player.setVelocity(vX, vY, vZ);
            } else {
                mc.player.setVelocity(0.0, vY, 0.0);
            }
            mc.player.fallDistance = 0f;
            
            if (mc.getNetworkHandler() != null && strictBypass) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
            }
        }

        if (mc.player.isSpectator()) return;

        LivingEntity target = null;

        // TriggerBot проверка
        if (triggerBot && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
            if (hit.getEntity() instanceof LivingEntity living && isValidTarget(mc, living)) {
                target = living;
            }
        }

        // KillAura поиск и приоритизация целей
        if (target == null && killAura) {
            List<LivingEntity> targets = new ArrayList<>();
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof LivingEntity living)) continue;
                if (isValidTarget(mc, living)) {
                    targets.add(living);
                }
            }

            if (!targets.isEmpty()) {
                targets.sort(Comparator.comparingDouble(LivingEntity::getHealth));
                target = targets.get(0);
            }
        }

        // Исполнительный блок атаки
        if (target != null && (killAura || (triggerBot && target != null))) {
            float[] rotations = getSmoothRotations(mc, target);
            float targetYaw = rotations[0];
            float targetPitch = rotations[1];

            long currentMillis = System.currentTimeMillis();
            long dynamicDelay = 260 + random.nextInt(120); 

            if (currentMillis - lastAttackTime >= dynamicDelay && mc.player.getAttackCooldownProgress(0.5f) >= 0.85f) {
                
                // Легитный микро-прыжок для критических ударов (AutoCrit)
                if (autoCrit && mc.player.isOnGround() && !mc.player.isSubmergedInWater() && !mc.player.isGliding() && !mc.player.isSprinting()) {
                    mc.player.jump();
                }

                mc.player.setYaw(targetYaw);
                mc.player.setPitch(targetPitch);

                if (mc.interactionManager != null) {
                    mc.interactionManager.attackEntity(mc.player, target);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    lastAttackTime = currentMillis;
                }
            }
        }
    }

    private boolean isValidTarget(MinecraftClient mc, LivingEntity entity) {
        return entity != mc.player && !entity.isDead() && entity.getHealth() > 0 && 
               !entity.isSpectator() && mc.player.distanceTo(entity) <= auraRange;
    }

    private float[] getSmoothRotations(MinecraftClient mc, Entity entity) {
        Vec3d eyePos = mc.player.getEyePos();
        double targetX = entity.getX();
        double targetY = entity.getY() + (entity.getHeight() * 0.5);
        double targetZ = entity.getZ();

        double dx = targetX - eyePos.x;
        double dy = targetY - eyePos.y;
        double dz = targetZ - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float rawYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float rawPitch = (float) (-(Math.atan2(dy, dist) * (180.0 / Math.PI)));

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float diffYaw = MathHelper.wrapDegrees(rawYaw - currentYaw);
        float diffPitch = rawPitch - currentPitch;

        float smoothFactor = 0.7f; 
        float smoothedYaw = currentYaw + diffYaw * smoothFactor;
        float smoothedPitch = MathHelper.clamp(currentPitch + diffPitch * smoothFactor, -90.0f, 90.0f);

        return new float[]{smoothedYaw, smoothedPitch};
    }

    private void onRenderHud(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        String status = "Oppka [MAX Edition] | KA: " + (killAura ? "ON" : "OFF") +
                        " | TB: " + (triggerBot ? "ON" : "OFF") +
                        " | Crit: " + (autoCrit ? "ON" : "OFF") +
                        " | Fly: " + (flyMod ? "ON" : "OFF");
        ctx.drawTextWithShadow(tr, status, 4, 4, 0x00FF99);
    }

    public static class OppkaMenuScreen extends Screen {
        public OppkaMenuScreen() { super(Text.literal("Oppka Max Menu")); }

        @Override
        protected void init() {
            int cx = this.width / 2;
            int cy = this.height / 4 - 40;

            addBtn(cx, cy, "KillAura", () -> killAura, () -> killAura = !killAura);
            addBtn(cx, cy + 22, "TriggerBot", () -> triggerBot, () -> triggerBot = !triggerBot);
            addBtn(cx, cy + 44, "AutoCrit", () -> autoCrit, () -> autoCrit = !autoCrit);
            addBtn(cx, cy + 66, "Motion-Fly", () -> flyMod, () -> flyMod = !flyMod);
            addBtn(cx, cy + 88, "AutoSprint", () -> autoSprint, () -> autoSprint = !autoSprint);
            addBtn(cx, cy + 110, "Strict Bypass", () -> strictBypass, () -> strictBypass = !strictBypass);

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Close"), b -> this.close()
            ).dimensions(cx - 100, cy + 138, 200, 20).build());
        }

        private void addBtn(int cx, int y, String name, java.util.function.BooleanSupplier getter, Runnable toggle) {
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(name + ": " + (getter.getAsBoolean() ? "§aON" : "§cOFF")), b -> {
                        toggle.run();
                        b.setMessage(Text.literal(name + ": " + (getter.getAsBoolean() ? "§aON" : "§cOFF")));
                    }).dimensions(cx - 100, y, 200, 20).build());
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            this.renderBackground(ctx, mouseX, mouseY, delta);
            super.render(ctx, mouseX, mouseY, delta);
        }
    }
                }
