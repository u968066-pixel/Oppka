package net.oppka.client.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

public class Aura {
    private static final Minecraft mc = Minecraft.getInstance();

    // Настройки ауры Оппка клиента
    public static boolean toggled = false; // Включена / Выключена
    public static double range = 4.2;      // Дистанция удара
    public static float fov = 360.0f;      // Угол обзора
    public static boolean targetPlayers = true; // Бить игроков
    public static boolean targetMobs = true;    // Бить мобов
    public static boolean rayTrace = false;     // Проверка стен (сквозь стены не бьет)
    public static int cps = 12;                 // Кликов в секунду

    private static long lastAttack = 0;

    // Главный метод, который вызывается каждый тик игры
    public static void onTick() {
        if (!toggled || mc.player == null || mc.world == null) return;
        if (mc.player.isSpectator()) return;

        LivingEntity target = findTarget();
        if (target == null) return;

        // Доводка камеры (ротация)
        lockRotation(target);

        // Тайминги удара (CPS)
        long delay = 1000L / Math.max(cps, 1);
        if (System.currentTimeMillis() - lastAttack >= delay) {
            executeAttack(target);
            lastAttack = System.currentTimeMillis();
        }
    }

    // Поиск ближайшей цели
    private static LivingEntity findTarget() {
        LivingEntity best = null;
        double shortestDistance = range;

        for (Entity e : mc.world.getLoadedEntityList()) {
            if (!(e instanceof LivingEntity) || e == mc.player) continue;
            LivingEntity living = (LivingEntity) e;

            if (living.isDead() || living.getHealth() <= 0) continue;
            
            // Фильтры целей
            if (living instanceof PlayerEntity && !targetPlayers) continue;
            if (!(living instanceof PlayerEntity) && !targetMobs) continue;

            double dist = mc.player.getDistance(living);
            if (dist > range) continue;

            // Проверка угла FOV
            if (!checkFov(living, fov)) continue;

            // Проверка видимости (если включен rayTrace)
            if (rayTrace && !mc.player.canEntityBeSeen(living)) continue;

            if (dist < shortestDistance) {
                shortestDistance = dist;
                best = living;
            }
        }
        return best;
    }

    // Проверка угла обзора
    private static boolean checkFov(Entity entity, float maxFov) {
        if (maxFov >= 360.0f) return true;
        float[] rot = getRotations(entity);
        float yawDiff = MathHelper.wrapDegrees(mc.player.rotationYaw - rot[0]);
        return Math.abs(yawDiff) <= maxFov / 2.0f;
    }

    // Плавный или точный поворот к цели
    private static void lockRotation(Entity entity) {
        float[] rot = getRotations(entity);
        mc.player.rotationYaw = rot[0];
        mc.player.rotationPitch = rot[1];
    }

    // Математика расчёта углов (Yaw и Pitch)
    private static float[] getRotations(Entity entity) {
        double x = entity.getPosX() - mc.player.getPosX();
        double y = (entity.getPosY() + entity.getHeight() * 0.5) - (mc.player.getPosY() + mc.player.getEyeHeight());
        double z = entity.getPosZ() - mc.player.getPosZ();
        double dist = Math.sqrt(x * x + z * z);

        float yaw = (float) (Math.atan2(z, x) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) (-(Math.atan2(y, dist) * (180.0 / Math.PI)));

        return new float[]{
            mc.player.rotationYaw + MathHelper.wrapDegrees(yaw - mc.player.rotationYaw),
            MathHelper.clamp(pitch, -90.0f, 90.0f)
        };
    }

    // Удар по цели
    private static void executeAttack(LivingEntity target) {
        mc.playerController.attackEntity(mc.player, target);
        mc.player.swingArm(Hand.MAIN_HAND);
    }
}
