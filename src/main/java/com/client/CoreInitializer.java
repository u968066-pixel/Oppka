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

    // Состояния всех модулей
    public static boolean watermark = true;
    public static boolean coordinates = true;
    public static boolean fps = true;
    public static boolean ping = true;
    public static boolean cpsMod = true;
    public static boolean speedMod = true;
    public static boolean timeMod = true;
    public static boolean totems = true;
    public static boolean armor = true;
    public static boolean effects = true;
    public static boolean arrayList = true;

    public static boolean nightVision = false;
    public static boolean brightness = false;
    public static boolean zoom = true;
    public static boolean crosshair = false;
    public static boolean noHurtCam = true;
    public static boolean targetHud = true;

    public static boolean autoSprint = false;
    public static boolean autoRespawn = false;
    public static boolean hitSound = false;

    public static boolean killAura = false;
    public static boolean triggerBot = true;

    public static boolean interfaceMod = true;
    public static boolean liteMode = false;

    public static double auraRange = 4.2;

    private static KeyBinding menuKey;
    private static long lastAttack = 0;
    private static final List<Long> clicks = new ArrayList<>();
    private static boolean lastMouseState = false;

    @Override
    public void onInitializeClient() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oppka.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.oppka"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            onRenderHud(drawContext, tickCounter);
        });
    }

    private void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;

        while (menuKey.wasPressed()) {
            if (mc.currentScreen == null) {
                mc.setScreen(new OppkaMenuScreen());
            }
        }

        long now = System.currentTimeMillis();
        boolean isAttacking = mc.options.attackKey.isPressed();
        if (isAttacking && !lastMouseState) clicks.add(now);
        lastMouseState = isAttacking;
        clicks.removeIf(t -> now - t > 1000);

        if (noHurtCam && mc.player.hurtTime > 0) {
            mc.player.hurtTime = 0;
            mc.player.timeUntilRegen = 0;
        }

        // Боевая логика: жесткий снап на цель + кулдаун (без автопрыжков)
        if (!mc.player.isSpectator()) {
            LivingEntity target = null;

            if (triggerBot && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
                if (hit.getEntity() instanceof LivingEntity living && living != mc.player && !living.isDead() && living.getHealth() > 0) {
                    target = living;
                }
            }

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

            if (target != null) {
                // Удар по кулдауну ванильного оружия
                if (mc.player.getAttackCooldown_Progress(0.5f) >= 0.9f) {
                    if (mc.interactionManager != null) {
                        mc.interactionManager.attackEntity(mc.player, target);
                        mc.player.swingHand(Hand.MAIN_HAND);
                    }
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

    private void onRenderHud(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        if (watermark) {
            String wm = "Oppka Client - v1.0.0 | " + mc.getCurrentFps() + " FPS";
            ctx.fill(4, 4, 4 + tr.getWidth(wm) + 16, 18, 0xAA101018);
            ctx.fill(4, 4, 6, 18, 0xFF00E5FF);
            ctx.drawTextWithShadow(tr, wm, 10, 7, 0xFFFFFF);
        }

        if (coordinates) {
            String coords = String.format("XYZ: %.1f, %.1f, %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            ctx.drawTextWithShadow(tr, coords, 4, 24, 0xFFFFFFFF);
        }

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

    public static class OppkaMenuScreen extends Screen {
        private String currentTab = "HUD";

        public OppkaMenuScreen() {
            super(Text.literal("Oppka Client Menu"));
        }

        @Override
        protected void init() {
            int cx = this.width / 2;
            int cy = this.height / 4;

            String[] tabs = {"HUD", "Visuals", "Player", "Combat", "Client"};
            int startX = cx - 125;
            for (int i = 0; i < tabs.length; i++) {
                final String tab = tabs[i];
                this.addDrawableChild(ButtonWidget.builder(
                        Text.literal(tab), b -> { currentTab = tab; this.clearAndInit(); }
                ).dimensions(startX + (i * 52), cy - 35, 50, 18).build());
            }

            if (currentTab.equals("HUD")) {
                addToggle("Watermark", watermark, b -> watermark = !watermark);
                addToggle("Coordinates", coordinates, b -> coordinates = !coordinates);
                addToggle("FPS", fps, b -> fps = !fps);
                addToggle("Ping", ping, b -> ping = !ping);
                addToggle("CPS", cpsMod, b -> cpsMod = !cpsMod);
                addToggle("Speed", speedMod, b -> speedMod = !speedMod);
                addToggle("Time", timeMod, b -> timeMod = !timeMod);
                addToggle("Totems", totems, b -> totems = !totems);
                addToggle("Armor", armor, b -> armor = !armor);
                addToggle("Effects", effects, b -> effects = !effects);
                addToggle("ArrayList", arrayList, b -> arrayList = !arrayList);
            } else if (currentTab.equals("Visuals")) {
                addToggle("Night Vision", nightVision, b -> nightVision = !nightVision);
                addToggle("Brightness", brightness, b -> brightness = !brightness);
                addToggle("Zoom", zoom, b -> zoom = !zoom);
                addToggle("Crosshair", crosshair, b -> crosshair = !crosshair);
                addToggle("NoHurtCam", noHurtCam, b -> noHurtCam = !noHurtCam);
                addToggle("TargetHUD", targetHud, b -> targetHud = !targetHud);
            } else if (currentTab.equals("Player")) {
                addToggle("Auto Sprint", autoSprint, b -> autoSprint = !autoSprint);
                addToggle("Auto Respawn", autoRespawn, b -> autoRespawn = !autoRespawn);
                addToggle("HitSound", hitSound, b -> hitSound = !hitSound);
            } else if (currentTab.equals("Combat")) {
                addToggle("KillAura", killAura, b -> killAura = !killAura);
                addToggle("TriggerBot", triggerBot, b -> triggerBot = !triggerBot);
            } else if (currentTab.equals("Client")) {
                addToggle("Interface", interfaceMod, b -> interfaceMod = !interfaceMod);
                addToggle("Lite Mode", liteMode, b -> liteMode = !liteMode);
            }

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Done"), b -> this.close()
            ).dimensions(cx - 100, cy + 130, 200, 20).build());
        }

        private void addToggle(String name, boolean val, java.util.function.Consumer<ButtonWidget> action) {
            int cx = this.width / 2;
            int yPos = this.height / 4 - 5 + getTrackedY();
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(name + ": " + (val ? "§aON" : "§cOFF")), b -> {
                        action.accept(b);
                        b.setMessage(Text.literal(name + ": " + (val ? "§cOFF" : "§aON")));
                        this.clearAndInit();
                    }
            ).dimensions(cx - 100, yPos, 200, 20).build());
            incrementTrackedY();
        }

        private static int trackedY = 0;
        private int getTrackedY() { return trackedY; }
        private void incrementTrackedY() { trackedY += 24; }
        @Override
        public void clearAndInst() {}
        @Override
        public void clearAndInit() {
            trackedY = 0;
            super.clearAndInit();
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            this.renderBackground(ctx, mouseX, mouseY, delta);
            ctx.drawCenteredTextWithShadow(this.textRenderer, "§lOppka Client - v1.0.0", this.width / 2, this.height / 4 - 55, 0xFF00E5FF);
            super.render(ctx, mouseX, mouseY, delta);
        }
    }
}
