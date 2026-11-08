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
                    
