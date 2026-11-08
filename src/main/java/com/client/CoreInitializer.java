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
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class CoreInitializer implements ClientModInitializer {

    // --- 5 модулей в одном монолитном файле ---
    public static boolean killAura = false;     // Модуль 1 (+30 баллов, с AutoCrit и RayTrace)
    public static boolean triggerBot = false;   // Модуль 2 (+10 баллов)
    public static boolean flyMod = false;       // Модуль 3 (+15 баллов)
    public static boolean autoSprint = true;    // Модуль 4 (Авто-спринт)
    public static boolean noHurtCam = true;     // Модуль 5 (Анти-тряска при получении урона)
    public static boolean silentMode = true;    // Пакетные тихие ротации без рывков камеры

    public static double auraRange = 3.2;       // Легитная дистанция для честного RayTrace
    private static KeyBinding menuKey;
    private static long lastAttackTime = 0;
    private static float lastServerYaw = 0f;
    private static float lastServerPitch = 0f;
    private static final Random random = new Random();

    @Override
    public void onInitializeClient() {
        // Регистрируем клавишу открытия меню (Правый Shift)
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oppka.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.oppka"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register(this::onRenderHud);
    }

    private void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;

        // Открытие GUI по нажатию клавиши
        while (menuKey.wasPressed()) {
            if (mc.currentScreen == null) {
                mc.setScreen(new OppkaMenuScreen());
            }
        }

        // Модуль 5: Убираем тряску экрана при уроне
        if (noHurtCam && mc.player.hurtTime > 0) {
            mc.player.hurtTime = 0;
            mc.player.timeUntilRegen = 0;
        }

        // Модуль 4: Авто-спринт
        if (autoSprint && !mc.player.isSneaking() && !mc.player.horizontalCollision && mc.player.input.movementForward > 0f) {
            mc.player.setSprinting(true);
        }

        // Модуль 3: Fly
        if (flyMod) {
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().setFlySpeed(0.05f);
        } else if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().flying = false;
        }

        if (mc.player.isSpectator()) return;

        LivingEntity target = null;

        // Модуль 2: TriggerBot (+10 баллов) — бьет только при твоем наведении на хитбокс
        if (triggerBot && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
            if (hit.getEntity() instanceof LivingEntity living && isValidTarget(mc, living)) {
                target = living;
            }
        }

        // Модуль 1: KillAura (+30 баллов) — продвинутый поиск целей, если триггер не сработал
        if (target == null && killAura) {
            List<LivingEntity> targets = new ArrayList<>();
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof LivingEntity living)) continue;
                if (isValidTarget(mc, living)) {
                    targets.add(living);
                }
            }

            if (!targets.isEmpty()) {
                // Сортируем по здоровью (добивание шотных в стиле Wurst/Meteor)
                targets.sort(Comparator.comparingDouble(LivingEntity::getHealth));
                target = targets.get(0);
            }
        }

        // Логика атаки для Киллауры
        if (target != null && killAura) {
            float[] rotations = getGcdFixedRotations(mc, target);
            float targetYaw = rotations[0];
            float targetPitch = rotations[1];

            // Проверка луча (RayTrace) перед нанесением удара
            if (raycastHit(mc, target, targetYaw, targetPitch, auraRange)) {
                long delay = 380 + random.nextInt(90); // Быстрый тайминг ударов с рандомом
                
                if (System.currentTimeMillis() - lastAttackTime >= delay && mc.player.getAttackCooldownProgress(0.5f) >= 0.85f) {
                    
                    // Авто-крит через микро-прыжок (если стоим на земле)
                    if (mc.player.isOnGround() && !mc.player.isSubmergedInWater() && !mc.player.isFallFlying()) {
                        mc.player.jump();
                    }

                    // Отправка тихого пакета поворота или визуальный поворот
                    if (silentMode && mc.getNetworkHandler() != null) {
                        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                                targetYaw, targetPitch, mc.player.isOnGround(), mc.player.horizontalCollision
                        ));
                    } else {
                        mc.player.setYaw(targetYaw);
                        mc.player.setPitch(targetPitch);
                    }

                    // Сам удар
                    if (mc.interactionManager != null) {
                        mc.interactionManager.attackEntity(mc.player, target);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        lastAttackTime = System.currentTimeMillis();
                    }

                    // Возврат взгляда в исходное состояние при Silent-режиме
                    if (silentMode && mc.getNetworkHandler() != null) {
                        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                                mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision
                        ));
                    }
                }
            }
        }
    }

    private boolean isValidTarget(MinecraftClient mc, LivingEntity entity) {
        return entity != mc.player && !entity.isDead() && entity.getHealth() > 0 && 
               !entity.isSpectator() && mc.player.distanceTo(entity) <= auraRange;
    }

    // --- Математика GCD Fix (Привязка к сетке сенсы мыши) ---
    private float[] getGcdFixedRotations(MinecraftClient mc, Entity entity) {
        double offsetX = (random.nextDouble() - 0.5) * 0.2;
        double offsetY = entity.getHeight() * (0.4 + random.nextDouble() * 0.35);
        double offsetZ = (random.nextDouble() - 0.5) * 0.2;

        Vec3d eyePos = mc.player.getEyePos();
        double dx = (entity.getX() + offsetX) - eyePos.x;
        double dy = (entity.getY() + offsetY) - eyePos.y;
        double dz = (entity.getZ() + offsetZ) - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float rawYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float rawPitch = (float) (-(Math.atan2(dy, dist) * (180.0 / Math.PI)));

        double sens = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        double gcd = sens * sens * sens * 8.0 * 0.15;

        float currentYaw = silentMode ? lastServerYaw : mc.player.getYaw();
        float currentPitch = silentMode ? lastServerPitch : mc.player.getPitch();

        float deltaYaw = MathHelper.wrapDegrees(rawYaw - currentYaw);
        float deltaPitch = rawPitch - currentPitch;

        float fixedYaw = currentYaw + (float) (Math.round(deltaYaw / gcd) * gcd);
        float fixedPitch = MathHelper.clamp(currentPitch + (float) (Math.round(deltaPitch / gcd) * gcd), -90.0f, 90.0f);

        lastServerYaw = fixedYaw;
        lastServerPitch = fixedPitch;

        return new float[]{fixedYaw, fixedPitch};
    }

    // --- Точный RayTrace хитбокса ---
    private boolean raycastHit(MinecraftClient mc, Entity target, float yaw, float pitch, double reach) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = Vec3d.fromPolar(pitch, yaw);
        Vec3d endPos = eyePos.add(lookVec.multiply(reach));
        Box searchBox = mc.player.getBoundingBox().stretch(lookVec.multiply(reach)).expand(1.0);

        EntityHitResult result = ProjectileUtil.raycast(
                mc.player, eyePos, endPos, searchBox,
                e -> e == target && !e.isSpectator() && e.canHit(),
                reach * reach
        );
        return result != null && result.getEntity() == target;
    }

    // HUD Статус в левом верхнем углу
    private void onRenderHud(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        String status = "Oppka 5-in-1 | KA: " + (killAura ? "ON" : "OFF") +
                        " | TB: " + (triggerBot ? "ON" : "OFF") +
                        " | Fly: " + (flyMod ? "ON" : "OFF") +
                        " | Sprint: " + (autoSprint ? "ON" : "OFF");
        ctx.drawTextWithShadow(tr, status, 4, 4, 0x00FF99);
    }

    // GUI Меню на Правый Shift
    public static class OppkaMenuScreen extends Screen {
        public OppkaMenuScreen() { super(Text.literal("Oppka 5-in-1 Menu")); }

        @Override
        protected void init() {
            int cx = this.width / 2;
            int cy = this.height / 4 - 20;

            addBtn(cx, cy, "KillAura (+30)", () -> killAura, () -> killAura = !killAura);
            addBtn(cx, cy + 24, "TriggerBot (+10)", () -> triggerBot, () -> triggerBot = !triggerBot);
            addBtn(cx, cy + 48, "Fly (+15)", () -> flyMod, () -> flyMod = !flyMod);
            addBtn(cx, cy + 72, "AutoSprint", () -> autoSprint, () -> autoSprint = !autoSprint);
            addBtn(cx, cy + 96, "NoHurtCam", () -> noHurtCam, () -> noHurtCam = !noHurtCam);

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Close"), b -> this.close()
            ).dimensions(cx - 100, cy + 128, 200, 20).build());
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
