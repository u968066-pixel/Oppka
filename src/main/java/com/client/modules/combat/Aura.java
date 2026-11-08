package com.client.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Aura {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean toggled = true;
    public static double range = 4.2;
    public static float fov = 360.0f;
    public static boolean targetPlayers = true;
    public static boolean targetMobs = true;
    public static boolean rayTrace = false;
    public static int cps = 12;
    
    public static boolean smoothRotation = true;
    public static float rotationSpeed = 60.0f;
    public static String attackMode = "Switch";

    private static LivingEntity currentTarget = null;
    private static long lastAttack = 0;

    public static void onTick() {
        if (!toggled || mc.player == null || mc.world == null) return;
        if (mc.player.isSpectator()) return;

        currentTarget = selectTarget();
        if (currentTarget == null) return;

        if (smoothRotation) {
            smoothAim(currentTarget);
        } else {
            lockRotation(currentTarget);
        }

        long delay = 1000L / Math.max(cps, 1);
        if (System.currentTimeMillis() - lastAttack >= delay) {
            executeAttack(currentTarget);
            lastAttack = System.currentTimeMillis();
        }
    }

    private static LivingEntity selectTarget() {
        List<LivingEntity> validTargets = new ArrayList<>();

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity)) continue;
            LivingEntity living = (LivingEntity) e;

            if (living == mc.player) continue;
            if (living.isDead() || living.getHealth() <= 0) continue;
            if (living instanceof PlayerEntity && !targetPlayers) continue;
            if (!(living instanceof PlayerEntity) && !targetMobs) continue;

            double dist = mc.player.distanceTo(living);
            if (dist > range) continue;
            if (!checkFov(living, fov)) continue;
            if (rayTrace && !mc.player.canSee(living)) continue;

            validTargets.add(living);
        }

        if (validTargets.isEmpty()) return null;

        validTargets.sort(Comparator.comparingDouble(e -> mc.player.distanceTo(e)));

        if (attackMode.equalsIgnoreCase("Single") && currentTarget != null && validTargets.contains(currentTarget)) {
            return currentTarget;
        }

        return validTargets.get(0);
    }

    private static boolean checkFov(Entity entity, float maxFov) {
        if (maxFov >= 360.0f) return true;
        float[] rot = getRotations(entity);
        float yawDiff = MathHelper.wrapDegrees(mc.player.getYaw() - rot[0]);
        return Math.abs(yawDiff) <= maxFov / 2.0f;
    }

    private static void lockRotation(Entity entity) {
        float[] rot = getRotations(entity);
        mc.player.setYaw(rot[0]);
        mc.player.setPitch(rot[1]);
    }

    private static void smoothAim(Entity entity) {
        float[] targetRot = getRotations(entity);
        
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float yawDiff = MathHelper.wrapDegrees(targetRot[0] - currentYaw);
        float pitchDiff = targetRot[1] - currentPitch;

        float step = rotationSpeed * 0.2f;

        mc.player.setYaw(currentYaw + MathHelper.clamp(yawDiff, -step, step));
        mc.player.setPitch(currentPitch + Math.clamp(pitchDiff, -step, step));
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

    private static void executeAttack(LivingEntity target) {
        if (mc.interactionManager != null && mc.player != null) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}
