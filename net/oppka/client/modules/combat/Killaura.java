package net.oppka.client.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class Killaura {
    private static final Minecraft mc = Minecraft.getInstance();

    // Настройки модуля
    public static boolean toggled = false;
    public static double range = 4.0; // Дистанция удара
    public static boolean autoBlock = true;
    public static float fov = 180.0f; // Угол обзора
    public static int cps = 10; // Кликов в секунду
    
    private static long lastClickTime = 0;

    // Вызывается каждый тик игрока (ClientTickEvent)
    public static void onTick() {
        if (!toggled || mc.player == null || mc.world == null) return;
        if (mc.player.isSpectator() || mc.player.isDead()) return;

        LivingEntity target = getTarget();
        if (target == null) return;

        // 1. Наводим камеру на цель (ротация)
        aimAt(target);

        // 2. Проверяем задержку кликов (CPS) и кулдаун атаки
        long delay = 1000L / Math.max(cps, 1);
        if (System.currentTimeMillis() - lastClickTime >= delay) {
            attack(target);
            lastClickTime = System.currentTimeMillis();
        }
    }

    // Поиск лучшей цели в радиусе
    private static LivingEntity getTarget() {
        LivingEntity bestEntity = null;
        double minDistance = range;

        for (Entity entity : mc.world.getLoadedEntityList()) {
            if (entity instanceof LivingEntity && entity != mc.player) {
                LivingEntity living = (LivingEntity) entity;

                // Проверки на валидность цели
                if (living.isDead() || living.getHealth() <= 0) continue;
                if (living instanceof PlayerEntity && ((PlayerEntity) living).isCreative()) continue;

                // Проверка дистанции
                double distance = mc.player.getDistance(living);
                if (distance > range) continue;

                // Проверка угла обзора (FOV)
                if (!isInFOV(living, fov)) continue;

                // Выбираем ближайшую
                if (distance < minDistance) {
                    minDistance = distance;
                    bestEntity = living;
                }
            }
        }
        return bestEntity;
    }

    // Расчет угла обзора
    private static boolean isInFOV(Entity entity, float maxFov) {
        float[] neededRotations = getRotations(entity);
        float yawDiff = MathHelper.wrapDegrees(mc.player.rotationYaw - neededRotations[0]);
        return Math.abs(yawDiff) <= maxFov / 2.0f;
    }

    // Плавный или моментальный поворот к цели
    private static void aimAt(Entity entity) {
        float[] rot = getRotations(entity);
        mc.player.rotationYaw = rot[0];
        mc.player.rotationPitch = rot[1];
    }

    // Математика вычисления Yaw и Pitch до цели
    private static float[] getRotations(Entity entity) {
        double x = entity.getPosX() - mc.player.getPosX();
        double y = (entity.getPosY() + (entity.getHeight() / 2.0)) - (mc.player.getPosY() + mc.player.getEyeHeight());
        double z = entity.getPosZ() - mc.player.getPosZ();
        double flatDistance = Math.sqrt(x * x + z * z);

        float yaw = (float) (Math.atan2(z, x) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) (-(Math.atan2(y, flatDistance) * (180.0 / Math.PI)));

        return new float[]{
            mc.player.rotationYaw + MathHelper.wrapDegrees(yaw - mc.player.rotationYaw),
            MathHelper.clamp(pitch, -90.0f, 90.0f)
        };
    }

    // Сам процесс удара
    private static void attack(LivingEntity target) {
        // Отправка пакета/вызов контроллера удара
        mc.playerController.attackEntity(mc.player, target);
        // Анимация взмаха руки
        mc.player.swingArm(Hand.MAIN_HAND);
    }
}
