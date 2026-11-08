package com.client; // Твой пакет Oppka Client

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
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CoreInitializer implements ClientModInitializer {
    
    // Переключатели функций
    public static boolean killAura = true;
    public static boolean triggerBot = true;
    public static boolean targetHud = true;
    public static boolean noHurtCam = true;
    public static boolean watermark = true;

    public static double auraRange = 4.2;
    public static int auraCps = 12;

    private static KeyBinding menuKey;
    private static long lastAttack = 0;
    private static final List<Long> clicks = new ArrayList<>();
    private static boolean lastMouseState = false;

    @Override
    public void onInitializeClient() {
        // Открытие меню на Правый Shift
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oppka.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.oppka"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register(this::onRenderHud);
    }

    private void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;

        // Открытие меню
        while (menuKey.wasPressed()) {
            if (mc.currentScreen == null) {
                mc.setScreen(new OppkaMenuScreen());
            }
        }

        // CPS подсчет
        long now = System.currentTimeMillis();
        boolean isAttacking = mc.options.attackKey.isPressed();
        if (isAttacking && !lastMouseState) clicks.add(now);
        lastMouseState = isAttacking;
        clicks.removeIf(t -> now - t > 1000);

        // NoHurtCam (убираем тряску экрана при ударе)
        if (noHurtCam && mc.player.hurtTime > 0) {
            mc.player.hurtTime = 0;
            mc.player.timeUntilRegen = 0;
        }

        // Боевая логика: Киллаура и Триггербот
        if (!mc.player.isSpectator()) {
            LivingEntity target = null;

            // Триггербот
            if (triggerBot && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
                if (hit.getEntity() instanceof LivingEntity living && living != mc.player && !living.isDead() && living.getHealth() > 0) {
                    target = living;
                }
            }

            // Киллаура
            if (target == null && killAura) {
                List<LivingEntity> targets = new ArrayList<>();
                for (Entity e : mc.world.getEntities()) {
                    if (!(e instanceof LivingEntity living)) continue;
                    if (living == mc.player || living.isDead() || living.getHealth() <= 0) continue;
                    if (mc.player.distanceTo(living) > auraRange) continue;
                    targets.add(living);
                }
                if (!targets.isEmpty()) {
                    targets.sort(Comparator.comparingDouble(e -> mc.player.distanceTo(e)));
                    target = targets.get(0);

                    float[] rot = getRotations(mc, target);
                    mc.player.setYaw(rot[0]);
                    mc.player.setPitch(rot[1]);
                }
            }

            // Удар по цели
            if (target != null) {
                long delay = 1000L / Math.max(1, auraCps);
                if (System.currentTimeMillis() - lastAttack >= delay) {
                    if (mc.interactionManager != null) {
                        mc.interactionManager.attackEntity(mc.player, target);
                        mc.player.swingHand(Hand.MAIN_HAND);
                    }
                    lastAttack = System.currentTimeMillis();
                }
            }
        }
    }

    private float[] getRotations(MinecraftClient mc, Entity entity) {
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

    private void onRenderHud(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        // Ватермарка
        if (watermark) {
            String wm = "Oppka Client | " + mc.getCurrentFps() + " FPS";
            ctx.fill(4, 4, 4 + tr.getWidth(wm) + 16, 18, 0xAA101018);
            ctx.fill(4, 4, 6, 18, 0xFF00E5FF);
            ctx.drawTextWithShadow(tr, wm, 10, 7, 0xFFFFFF);
        }

        // TargetHUD (плашка здоровья цели как в Пирожке)
        if (targetHud && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
            if (hit.getEntity() instanceof LivingEntity target && target != mc.player) {
                int tx = sw / 2 + 15;
                int ty = sh / 2 - 10;
                ctx.fill(tx - 2, ty - 2, tx + 120, ty + 30, 0x90101018);
                ctx.drawTextWithShadow(tr, target.getName().getString(), tx + 4, ty + 4, 0xFFFF55);
                ctx.drawTextWithShadow(tr, "HP: " + (int)target.getHealth() + " / " + (int)target.getMaxHealth(), tx + 4, ty + 16, 0xFF5555);
            }
        }
    }

    // Менюшка клиента
    public static class OppkaMenuScreen extends Screen {
        public OppkaMenuScreen() {
            super(Text.literal("Oppka Client Menu"));
        }

        @Override
        protected void init() {
            int cx = this.width / 2;
            int cy = this.height / 4;

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("KillAura: " + (killAura ? "§aON" : "§cOFF")), b -> {
                        killAura = !killAura;
                        b.setMessage(Text.literal("KillAura: " + (killAura ? "§aON" : "§cOFF")));
                    }).dimensions(cx - 100, cy - 30, 200, 20).build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("TriggerBot: " + (triggerBot ? "§aON" : "§cOFF")), b -> {
                        triggerBot = !triggerBot;
                        b.setMessage(Text.literal("TriggerBot: " + (triggerBot ? "§aON" : "§cOFF")));
                    }).dimensions(cx - 100, cy - 5, 200, 20).build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("TargetHUD: " + (targetHud ? "§aON" : "§cOFF")), b -> {
                        targetHud = !targetHud;
                        b.setMessage(Text.literal("TargetHUD: " + (targetHud ? "§aON" : "§cOFF")));
                    }).dimensions(cx - 100, cy + 20, 200, 20).build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("NoHurtCam: " + (noHurtCam ? "§aON" : "§cOFF")), b -> {
                        noHurtCam = !noHurtCam;
                        b.setMessage(Text.literal("NoHurtCam: " + (noHurtCam ? "§aON" : "§cOFF")));
                    }).dimensions(cx - 100, cy + 45, 200, 20).build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Done"), b -> this.close()
            ).dimensions(cx - 100, cy + 80, 200, 20).build());
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            this.renderBackground(ctx, mouseX, mouseY, delta);
            ctx.drawCenteredTextWithShadow(this.textRenderer, "§lOppka Client [Menu]", this.width / 2, this.height / 4 - 50, 0xFF00E5FF);
            super.render(ctx, mouseX, mouseY, delta);
        }
    }
            }
                                                   
