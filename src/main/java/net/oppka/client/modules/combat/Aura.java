package net.oppka.client.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

public class Aura {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Настройки ауры Оппка клиента
    public static boolean toggled = false;
    public static double range = 4.2;
    public static float fov = 360.0f;
    public static boolean targetPlayers = true;
    public static boolean targetMobs = true;
    public static boolean rayTrace = false;
    public static int cps = 12;

    private static long lastAttack = 0;

    public static void onTick() {
        if (!toggled || mc.player == null || mc.world == null) return;
        if (mc.player.isSpectator()) return;

        LivingEntity target = findTarget();
        if (target == null) return;

        lockRotation(target);

        long delay = 1000L / Math.max(cps, 1);
        if (System.currentTimeMillis() - lastAttack >= delay) {
            executeAttack(target);
            lastAttack = System.currentTimeMillis();
        }
    }

    private static LivingEntity findTarget() {
        LivingEntity best = null;
        double shortestDistance = range;

        // В Fabric метод получения сущностей мира может называться getEntities()
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity) || e == mc.player) continue;
            LivingEntity living = (LivingEntity) e;

            if (living.isDead() || living.getHealth() <= 0) continue;
            
            if (living instanceof PlayerEntity && !targetPlayers) continue;
            if (!(living instanceof PlayerEntity) && !targetMobs) continue;

            double dist = mc.player.distanceTo(living);
            if (dist > range) continue;

            if (!checkFov(living, fov)) continue;

            if (rayTrace && !mc.player.canSee(living)) continue;

            if (dist < shortestDistance) {
                shortestDistance = dist;
                best = living;
            }
        }
        return best;
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

    private static float[] getRotations(Entity entity) {
        double x = entity.getX() - mc.player.getX();
        double y = (entity.getY() + entity.getHeight() * 0.5) - (mc.player.getY() + mc.player.getEyeHeight());
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
