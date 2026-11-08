package com.client.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Aura {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean toggled = true;
    public static boolean triggerBot = true; // Тот самый рабочий триггербот
    public static double range = 4.2;
    public static int cps = 12;
    private static long lastAttack = 0;

    public static void onTick() {
        if (!toggled || mc.player == null || mc.world == null) return;
        if (mc.player.isSpectator()) return;

        // Триггербот: бьёт цель под прицелом в момент кулдауна
        if (triggerBot && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
            if (hit.getEntity() instanceof LivingEntity target) {
                if (target != mc.player && !target.isDead() && target.getHealth() > 0) {
                    tryAttack(target);
                    return;
                }
            }
        }

        // Киллаура (если цель не по центру прицела, но рядом)
        LivingEntity target = selectTarget();
        if (target == null) return;

        float[] rot = getRotations(target);
        mc.player.setYaw(rot[0]);
        mc.player.setPitch(rot[1]);

        tryAttack(target);
    }

    private static void tryAttack(LivingEntity target) {
        long delay = 1000L / Math.max(cps, 1);
        if (System.currentTimeMillis() - lastAttack >= delay) {
            if (mc.interactionManager != null) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            lastAttack = System.currentTimeMillis();
        }
    }

    private static LivingEntity selectTarget() {
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity living)) continue;
            if (living == mc.player || living.isDead() || living.getHealth() <= 0) continue;
            if (mc.player.distanceTo(living) > range) continue;
            targets.add(living);
        }
        if (targets.isEmpty()) return null;
        targets.sort(Comparator.comparingDouble(e -> mc.player.distanceTo(e)));
        return targets.get(0);
    }

    private static float[] getRotations(Entity entity) {
        double x = entity.getX() - mc.player.getX();
        double y = (entity.getY() + entity.getHeight() * 0.5) - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double z = entity.getZ() - mc.player.getZ();
        double dist = Math.sqrt(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) (-(Math.atan2(y, dist) * (180.0 / Math.PI)));
        return new float[]{
            mc.player.getYaw() + MathHelper.wrapDegrees(yaw - mc.player.getYaw()),
            MathHelper.clamp(pitch, -90.0f, 90.0f)
        };
    }
}
