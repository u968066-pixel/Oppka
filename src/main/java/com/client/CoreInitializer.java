package com.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CoreInitializer implements ClientModInitializer {
    private static final String CAT = "Oppka Client";

    static class Setting {
        final String name; final int type; boolean b; float v, min, max, step; String[] opts; int idx;
        Setting(String n, int t) { name = n; type = t; }
        static Setting bool(String n, boolean d) { Setting s = new Setting(n, 0); s.b = d; return s; }
        static Setting slider(String n, float min, float max, float step, float d) { Setting s = new Setting(n, 1); s.min = min; s.max = max; s.step = step; s.v = d; return s; }
        static Setting mode(String n, int d, String... o) { Setting s = new Setting(n, 2); s.opts = o; s.idx = d; return s; }
        String shown() {
            if (type == 0) return b ? "ON" : "OFF";
            if (type == 1) return step < 1f ? String.format("%.1f", v) : String.valueOf(Math.round(v));
            return opts[idx];
        }
        String ser() { return type == 0 ? String.valueOf(b) : (type == 1 ? String.valueOf(v) : String.valueOf(idx)); }
        void des(String t) {
            try {
                if (type == 0) b = Boolean.parseBoolean(t);
                else if (type == 1) v = Math.max(min, Math.min(max, Float.parseFloat(t)));
                else idx = Math.max(0, Math.min(opts.length - 1, Integer.parseInt(t)));
            } catch (Exception ignored) {}
        }
    }

    static class Mod {
        final String name, cat; final boolean always; boolean enabled; final List<Setting> sets = new ArrayList<>();
        Mod(String n, String c, boolean def, boolean always) { name = n; cat = c; enabled = def; this.always = always; }
        Mod add(Setting s) { sets.add(s); return this; }
    }

    static final Setting S_ZOOM = Setting.slider("FOV", 10, 70, 5, 30), S_SMOOTH = Setting.bool("Smooth", true);
    static final Setting S_CROSS = Setting.mode("Style", 2, "Dot", "Ticks", "Both");
    static final Setting S_COLOR = Setting.mode("Color", 2, "White", "Green", "Cyan", "Pink", "Orange", "Rainbow");
    static final Setting S_HSOUND = Setting.mode("Sound", 3, "Pling", "Bell", "Bit", "Orb", "Arrow"), S_HPITCH = Setting.slider("Pitch", 0.5f, 2.0f, 0.1f, 1.2f);
    static final Setting S_SOUND = Setting.bool("Sounds", true);

    static final Mod WATERMARK = new Mod("Watermark", "HUD", true, false);
    static final Mod COORDS = new Mod("Coordinates", "HUD", true, false);
    static final Mod FPS = new Mod("FPS", "HUD", true, false);
    static final Mod PING = new Mod("Ping", "HUD", true, false);
    static final Mod CPS = new Mod("CPS", "HUD", true, false);
    static final Mod SPEED = new Mod("Speed", "HUD", false, false);
    static final Mod TIME = new Mod("Time", "HUD", false, false);
    static final Mod TOTEMS = new Mod("Totems", "HUD", true, false);
    static final Mod ARMOR = new Mod("Armor", "HUD", true, false);
    static final Mod EFFECTS = new Mod("Effects", "HUD", false, false);
    static final Mod ARRAYLIST = new Mod("ArrayList", "HUD", true, false);
    
    static final Mod NIGHT = new Mod("Night Vision", "Visuals", false, false);
    static final Mod BRIGHT = new Mod("Brightness", "Visuals", false, false);
    static final Mod ZOOM = new Mod("Zoom", "Visuals", true, false).add(S_ZOOM).add(S_SMOOTH);
    static final Mod CROSS = new Mod("Crosshair", "Visuals", false, false).add(S_CROSS);
    static final Mod NO_HURT_CAM = new Mod("NoHurtCam", "Visuals", true, false);
    
    static final Mod SPRINT = new Mod("Auto Sprint", "Player", false, false);
    static final Mod AUTO_RESPAWN = new Mod("Auto Respawn", "Player", true, false);
    static final Mod HITSOUND = new Mod("Hit Sound", "Player", true, false).add(S_HSOUND).add(S_HPITCH);
    static final Mod TRIGGER_BOT = new Mod("TriggerBot", "Combat", false, false);
    
    static final Mod IFACE = new Mod("Interface", "Client", true, true).add(S_COLOR).add(S_SOUND);
    static final Mod LITE = new Mod("Lite Mode", "Client", false, false);

    static final Mod[] ALL = {WATERMARK, COORDS, FPS, PING, CPS, SPEED, TIME, TOTEMS, ARMOR, EFFECTS, ARRAYLIST, NIGHT, BRIGHT, ZOOM, CROSS, NO_HURT_CAM, SPRINT, AUTO_RESPAWN, HITSOUND, TRIGGER_BOT, IFACE, LITE};
    static final String[] CATS = {"HUD", "Visuals", "Player", "Combat", "Client"};
    static final int[] COLORS = {0xFFFFFF, 0x55FF55, 0x55FFFF, 0xFF77CC, 0xFFAA00, 0};
    
    public static boolean hudVisible = true;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private KeyBinding menuKey, hudKey, nightKey, zoomKey, hudEditorKey, triggerKey;
    private boolean zoomActive = false, wasNight = false, wasBright = false, lastAttack = false;
    private int savedFov = 70; private float curFov = 70f; private double savedGamma = 0.5;
    private final List<Long> clicks = new ArrayList<>();
    private float speed = 0f; private double prevX, prevZ;

    @Override
    public void onInitializeClient() {
        menuKey = reg("Open menu", GLFW.GLFW_KEY_M);
        hudKey = reg("HUD on/off", GLFW.GLFW_KEY_H);
        nightKey = reg("Night vision", GLFW.GLFW_KEY_N);
        zoomKey = reg("Zoom (hold)", GLFW.GLFW_KEY_C);
        hudEditorKey = reg("HUD Editor", GLFW.GLFW_KEY_RIGHT_SHIFT);
        triggerKey = reg("TriggerBot Toggle", GLFW.GLFW_KEY_R);

        HudDrag.register("Watermark", 4, 4, 120, 14);
        HudDrag.register("Coordinates", 4, 22, 110, 11);
        HudDrag.register("FPS", 4, 34, 70, 11);
        HudDrag.register("Ping", 4, 46, 90, 11);
        HudDrag.register("CPS", 4, 58, 70, 11);
        HudDrag.register("Speed", 4, 70, 100, 11);
        HudDrag.register("Time", 4, 82, 60, 11);
        HudDrag.register("Effects", 4, 96, 130, 11);
        HudDrag.register("ArrayList", 300, 4, 100, 11);

        load();

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((ctx, tc) -> render(ctx));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && HITSOUND.enabled) playHit();
            return ActionResult.PASS;
        });
    }

    private KeyBinding reg(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(name, InputUtil.Type.KEYSYM, key, CAT));
    }

    public static int themeColor() {
        if (S_COLOR.idx == COLORS.length - 1) {
            double t = (System.currentTimeMillis() % 4000) / 4000.0 * Math.PI * 2;
            return 0xFF000000 | ((int) (127 + 127 * Math.sin(t)) << 16) | ((int) (127 + 127 * Math.sin(t + 2.094)) << 8) | (int) (127 + 127 * Math.sin(t + 4.188));
        }
        return 0xFF000000 | COLORS[S_COLOR.idx];
    }

    static String key(String s) { return s.replace(' ', '_'); }
    static Path cfgPath() { return FabricLoader.getInstance().getConfigDir().resolve("oppka_client.properties"); }

    public static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("hudVisible", String.valueOf(hudVisible));
            for (Mod m : ALL) {
                p.setProperty("m." + key(m.name), String.valueOf(m.enabled));
                for (Setting s : m.sets) p.setProperty("s." + key(m.name) + "." + key(s.name), s.ser());
            }
            HudDrag.saveAll(p);
            try (OutputStream o = Files.newOutputStream(cfgPath())) { p.store(o, "Oppka Client Config"); }
        } catch (Exception ignored) {}
    }

    static void load() {
        Path f = cfgPath(); if (!Files.exists(f)) return;
        try (InputStream i = Files.newInputStream(f)) {
            Properties p = new Properties(); p.load(i);
            String h = p.getProperty("hudVisible"); if (h != null) hudVisible = Boolean.parseBoolean(h);
            for (Mod m : ALL) {
                String e = p.getProperty("m." + key(m.name));
                if (e != null && !m.always) m.enabled = Boolean.parseBoolean(e);
                for (Setting s : m.sets) {
                    String v = p.getProperty("s." + key(m.name) + "." + key(s.name));
                    if (v != null) s.des(v);
                }
            }
            HudDrag.loadAll(p);
        } catch (Exception ignored) {}
    }

    static void playHit() {
        var sm = MinecraftClient.getInstance().getSoundManager(); float p = S_HPITCH.v;
        switch (S_HSOUND.idx) {
            case 0 -> sm.play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, p));
            case 1 -> sm.play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BELL, p));
            case 2 -> sm.play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BIT, p));
            case 3 -> sm.play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, p));
            default -> sm.play(PositionedSoundInstance.master(SoundEvents.ENTITY_ARROW_HIT_PLAYER, p));
        }
    }

    private void tick(MinecraftClient mc) {
        if (mc.player == null) return;

        while (menuKey.wasPressed()) { if (mc.currentScreen == null) mc.setScreen(new MenuScreen()); }
        while (hudEditorKey.wasPressed()) { if (mc.currentScreen == null) mc.setScreen(new HudEditorScreen()); }
        while (hudKey.wasPressed()) { hudVisible = !hudVisible; }
        while (nightKey.wasPressed()) { NIGHT.enabled = !NIGHT.enabled; }
        while (triggerKey.wasPressed()) {
            TRIGGER_BOT.enabled = !TRIGGER_BOT.enabled;
            mc.player.sendMessage(Text.literal("§7[Oppka] TriggerBot: " + (TRIGGER_BOT.enabled ? "§aON" : "§cOFF")), true);
        }

        long now = System.currentTimeMillis(); boolean atk = mc.options.attackKey.isPressed();
        if (atk && !lastAttack) clicks.add(now); lastAttack = atk; clicks.removeIf(t -> now - t > 1000);

        double dx = mc.player.getX() - prevX, dz = mc.player.getZ() - prevZ;
        prevX = mc.player.getX(); prevZ = mc.player.getZ();
        speed = speed * 0.8f + (float) (Math.sqrt(dx * dx + dz * dz) * 20) * 0.2f;

        if (NIGHT.enabled) {
            StatusEffectInstance e = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (e == null || e.getDuration() < 220) mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
        } else if (wasNight) { mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION); }
        wasNight = NIGHT.enabled;

        var gamma = mc.options.getGamma();
        if (BRIGHT.enabled != wasBright) {
            if (BRIGHT.enabled) { double g = gamma.getValue(); savedGamma = g >= 1.0 ? 0.5 : g; gamma.setValue(1.0); }
            else { gamma.setValue(savedGamma); } wasBright = BRIGHT.enabled;
        }

        if (SPRINT.enabled && mc.options.forwardKey.isPressed() && !mc.player.isSneaking() && !mc.player.horizontalCollision) mc.player.setSprinting(true);
        if (AUTO_RESPAWN.enabled && mc.player.isDead()) mc.player.requestRespawn();
        if (NO_HURT_CAM.enabled) { mc.player.hurtTime = 0; mc.player.timeUntilRegen = 0; }

        var fov = mc.options.getFov(); boolean held = ZOOM.enabled && mc.currentScreen == null && zoomKey.isPressed();
        if (held && !zoomActive) { savedFov = fov.getValue(); curFov = savedFov; zoomActive = true; }
        if (zoomActive) {
            float target = held ? S_ZOOM.v : savedFov; curFov = S_SMOOTH.b ? curFov + (target - curFov) * 0.4f : target; fov.setValue(Math.round(curFov));
            if (!held && Math.abs(curFov - savedFov) < 1f) { fov.setValue(savedFov); zoomActive = false; }
        }

        if (TRIGGER_BOT.enabled && mc.interactionManager != null) {
            HitResult hit = mc.crosshairTarget;
            if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
                Entity target = ((EntityHitResult) hit).getEntity();
                if (target instanceof LivingEntity && target != mc.player) {
                    if (mc.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
                        mc.interactionManager.attackEntity(mc.player, target);
                        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                    }
                }
            }
        }
    }

    private void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || !hudVisible) return;
        TextRenderer tr = mc.textRenderer; int col = themeColor();

        int[] wp = HudDrag.pos("Watermark");
        if (WATERMARK.enabled) {
            String wm = "Oppka Client  |  " + mc.getCurrentFps() + " FPS";
            int w = tr.getWidth(wm);
            ctx.fill(wp[0], wp[1], wp[0] + w + 16, wp[1] + 14, 0xAA101018);
            ctx.fill(wp[0], wp[1], wp[0] + 2, wp[1] + 14, col);
            ctx.drawTextWithShadow(tr, wm, wp[0] + 6, wp[1] + 3, col);
        }

        int[] cp = HudDrag.pos("Coordinates");
        if (COORDS.enabled) {
            String s = String.format("XYZ: %.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            int w = tr.getWidth(s);
            ctx.fill(cp[0], cp[1], cp[0] + w + 8, cp[1] + 11, 0x80101018);
            ctx.fill(cp[0], cp[1], cp[0] + 1, cp[1] + 11, col);
            ctx.drawTextWithShadow(tr, s, cp[0] + 4, cp[1] + 2, 0xFFFFFFFF);
        }

        int[] fp = HudDrag.pos("FPS");
        if (FPS.enabled) {
            String s = "FPS: " + mc.getCurrentFps(); int w = tr.getWidth(s);
            ctx.fill(fp[0], fp[1], fp[0] + w + 8, fp[1] + 11, 0x80101018);
            ctx.fill(fp[0], fp[1], fp[0] + 1, fp[1] + 11, col);
            ctx.drawTextWithShadow(tr, s, fp[0] + 4, fp[1] + 2, 0xFFFFFFFF);
        }

        int[] cpsp = HudDrag.pos("CPS");
        if (CPS.enabled) {
            String s = "CPS: " + clicks.size(); int w = tr.getWidth(s);
            ctx.fill(cpsp[0], cpsp[1], cpsp[0] + w + 8, cpsp[1] + 11, 0x80101018);
            ctx.fill(cpsp[0], cpsp[1], cpsp[0] + 1, cpsp[1] + 11, col);
            ctx.drawTextWithShadow(tr, s, cpsp[0] + 4, cpsp[1] + 2, 0xFFFFFFFF);
        }

        int[] tp = HudDrag.pos("Time");
        if (TIME.enabled) {
            String s = LocalTime.now().format(TIME_FMT); int w = tr.getWidth(s);
            ctx.fill(tp[0], tp[1], tp[0] + w + 8, tp[1] + 11, 0x80101018);
            ctx.fill(tp[0], tp[1], tp[0] + 1, tp[1] + 11, col);
            ctx.drawTextWithShadow(tr, s, tp[0] + 4, tp[1] + 2, 0xFFFFFFFF);
        }
    }

    public static class HudDrag {
        public static final Map<String, int[]> POS = new HashMap<>();
        public static final Map<String, int[]> DEFAULT = new HashMap<>();
        public static boolean editorMode = false;
        public static String dragging = null;
        public static int dragDX, dragDY;

        public static void register(String name, int x, int y, int w, int h) {
            DEFAULT.put(name, new int[]{x, y, w, h});
            if (!POS.containsKey(name)) POS.put(name, new int[]{x, y, w, h});
        }

        public static int[] pos(String name) {
            int[] p = POS.get(name);
            if (p == null) {
                int[] d = DEFAULT.get(name);
                p = d != null ? new int[]{d[0], d[1], d[2], d[3]} : new int[]{0, 0, 10, 10};
                POS.put(name, p);
            }
            return p;
        }

        public static void drawBox(DrawContext ctx, String name, int color) {
            int[] p = pos(name);
            ctx.fill(p[0] - 1, p[1] - 1, p[0] + p[2] + 1, p[1], color);
            ctx.fill(p[0] - 1, p[1] + p[3], p[0] + p[2] + 1, p[1] + p[3] + 1, color);
            ctx.fill(p[0] - 1, p[1] - 1, p[0], p[1] + p[3] + 1, color);
            ctx.fill(p[0] + p[2], p[1] - 1, p[0] + p[2] + 1, p[1] + p[3] + 1, color);
            ctx.fill(p[0], p[1], p[0] + p[2], p[1] + p[3], 0x30FFFFFF);
        }

        public static boolean mouseClicked(double mx, double my) {
            if (!editorMode) return false;
            for (Map.Entry<String, int[]> e : POS.entrySet()) {
                int[] p = e.getValue();
                if (mx >= p[0] && mx <= p[0] + p[2] && my >= p[1] && my <= p[1] + p[3]) {
                    dragging = e.getKey();
                    dragDX = (int) mx - p[0];
                    dragDY = (int) my - p[1];
                    return true;
                }
            }
            return false;
        }

        public static boolean mouseDragged(double mx, double my) {
            if (dragging == null) return false;
            int[] p = POS.get(dragging);
            if (p == null) return false;
            p[0] = (int) mx - dragDX;
            p[1] = (int) my - dragDY;
            return true;
        }

        public static boolean mouseReleased() {
            if (dragging != null) { dragging = null; save(); return true; }
            return false;
        }

        public static void saveAll(Properties p) {
            for (Map.Entry<String, int[]> e : POS.entrySet()) {
                int[] v = e.getValue();
                p.setProperty("hud." + key(e.getKey()) + ".x", String.valueOf(v[0]));
                p.setProperty("hud." + key(e.getKey()) + ".y", String.valueOf(v[1]));
            }
        }

        public static void loadAll(Properties p) {
            for (Map.Entry<String, int[]> e : POS.entrySet()) {
                try {
                    String x = p.getProperty("hud." + key(e.getKey()) + ".x");
                    String y = p.getProperty("hud." + key(e.getKey()) + ".y");
                    if (x != null && y != null) { e.getValue()[0] = Integer.parseInt(x); e.getValue()[1] = Integer.parseInt(y); }
                } catch (Exception ignored) {}
            }
        }
    }

    public static class HudEditorScreen extends Screen {
        public HudEditorScreen() { super(Text.literal("HUD Editor")); }
        @Override protected void init() { HudDrag.editorMode = true; }
        @Override public boolean shouldPause() { return false; }
        @Override public void removed() { HudDrag.editorMode = false; HudDrag.dragging = null; save(); }
        @Override public boolean keyPressed(int key, int scan, int mod) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
            return super.keyPressed(key, scan, mod);
        }
        @Override public boolean mouseClicked(double mx, double my, int button) {
            if (button == 0 && HudDrag.mouseClicked(mx, my)) return true;
            return super.mouseClicked(mx, my, button)
