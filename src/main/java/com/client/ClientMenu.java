package com.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ClientMenu {
    public static class Setting {
        public final String name;
        public final int type;
        public boolean b;
        public float v, min, max, step;
        public String[] opts;
        public int idx;

        Setting(String n, int t) {
            name = n;
            type = t;
        }

        public static Setting bool(String n, boolean d) {
            Setting s = new Setting(n, 0);
            s.b = d;
            return s;
        }

        public static Setting slider(String n, float min, float max, float step, float d) {
            Setting s = new Setting(n, 1);
            s.min = min;
            s.max = max;
            s.step = step;
            s.v = d;
            return s;
        }

        public static Setting mode(String n, int d, String... o) {
            Setting s = new Setting(n, 2);
            s.opts = o;
            s.idx = d;
            return s;
        }

        public String shown() {
            if (type == 0) return b ? "ON" : "OFF";
            if (type == 1) return step < 1f ? String.format("%.1f", v) : String.valueOf(Math.round(v));
            return opts[idx];
        }

        public String ser() {
            return type == 0 ? String.valueOf(b) : (type == 1 ? String.valueOf(v) : String.valueOf(idx));
        }

        public void des(String t) {
            try {
                if (type == 0) b = Boolean.parseBoolean(t);
                else if (type == 1) v = Math.max(min, Math.min(max, Float.parseFloat(t)));
                else idx = Math.max(0, Math.min(opts.length - 1, Integer.parseInt(t)));
            } catch (Exception ignored) {}
        }
    }

    public static class Mod {
        public final String name, cat;
        public final boolean always;
        public boolean enabled;
        public final List<Setting> sets = new ArrayList<>();

        Mod(String n, String c, boolean def, boolean always) {
            name = n;
            cat = c;
            enabled = def;
            this.always = always;
        }

        public Mod add(Setting s) {
            sets.add(s);
            return this;
        }
    }

    public static final Setting S_ZOOM = Setting.slider("FOV", 10, 70, 5, 30);
    public static final Setting S_SMOOTH = Setting.bool("Smooth", true);
    public static final Setting S_CROSS = Setting.mode("Style", 2, "Dot", "Ticks", "Both");
    public static final Setting S_COLOR = Setting.mode("Color", 2, "White", "Green", "Cyan", "Pink", "Orange", "Rainbow");
    public static final Setting S_HSOUND = Setting.mode("Sound", 3, "Pling", "Bell", "Bit", "Orb", "Arrow");
    public static final Setting S_HPITCH = Setting.slider("Pitch", 0.5f, 2.0f, 0.1f, 1.2f);
    public static final Setting S_SOUND = Setting.bool("Sounds", true);

    public static final Mod WATERMARK = new Mod("Watermark", "HUD", true, false);
    public static final Mod COORDS = new Mod("Coordinates", "HUD", true, false);
    public static final Mod FPS = new Mod("FPS", "HUD", true, false);
    public static final Mod PING = new Mod("Ping", "HUD", true, false);
    public static final Mod CPS = new Mod("CPS", "HUD", true, false);
    public static final Mod SPEED = new Mod("Speed", "HUD", false, false);
    public static final Mod TIME = new Mod("Time", "HUD", false, false);
    public static final Mod TOTEMS = new Mod("Totems", "HUD", true, false);
    public static final Mod ARMOR = new Mod("Armor", "HUD", true, false);
    public static final Mod EFFECTS = new Mod("Effects", "HUD", false, false);
    public static final Mod ARRAYLIST = new Mod("ArrayList", "HUD", true, false);

    public static final Mod NIGHT = new Mod("Night Vision", "Visuals", false, false);
    public static final Mod BRIGHT = new Mod("Brightness", "Visuals", false, false);
    public static final Mod ZOOM = new Mod("Zoom", "Visuals", true, false).add(S_ZOOM).add(S_SMOOTH);
    public static final Mod CROSS = new Mod("Crosshair", "Visuals", false, false).add(S_CROSS);
    public static final Mod NO_HURT_CAM = new Mod("NoHurtCam", "Visuals", true, false);

    public static final Mod SPRINT = new Mod("Auto Sprint", "Player", false, false);
    public static final Mod AUTO_RESPAWN = new Mod("Auto Respawn", "Player", true, false);
    public static final Mod HITSOUND = new Mod("Hit Sound", "Player", true, false).add(S_HSOUND).add(S_HPITCH);
    public static final Mod TRIGGER_BOT = new Mod("TriggerBot", "Combat", false, false);

    public static final Mod IFACE = new Mod("Interface", "Client", true, true).add(S_COLOR).add(S_SOUND);
    public static final Mod LITE = new Mod("Lite Mode", "Client", false, false);

    public static final Mod[] ALL = {WATERMARK, COORDS, FPS, PING, CPS, SPEED, TIME, TOTEMS, ARMOR, EFFECTS, ARRAYLIST, NIGHT, BRIGHT, ZOOM, CROSS, NO_HURT_CAM, SPRINT, AUTO_RESPAWN, HITSOUND, TRIGGER_BOT, IFACE, LITE};
    public static final String[] CATS = {"HUD", "Visuals", "Player", "Combat", "Client"};
    public static final int[] COLORS = {0xFFFFFF, 0x55FF55, 0x55FFFF, 0xFF77CC, 0xFFAA00, 0};

    public static int themeColor() {
        if (S_COLOR.idx == COLORS.length - 1) {
            double t = (System.currentTimeMillis() % 4000) / 4000.0 * Math.PI * 2;
            return 0xFF000000 | ((int) (127 + 127 * Math.sin(t)) << 16) | ((int) (127 + 127 * Math.sin(t + 2.094)) << 8) | (int) (127 + 127 * Math.sin(t + 4.188));
        }
        return 0xFF000000 | COLORS[S_COLOR.idx];
    }

    public static String key(String s) {
        return s.replace(' ', '_');
    }

    public static Path cfgPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("oppka_client.properties");
    }

    public static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("hudVisible", String.valueOf(HudManager.hudVisible));
            for (Mod m : ALL) {
                p.setProperty("m." + key(m.name), String.valueOf(m.enabled));
                for (Setting s : m.sets) p.setProperty("s." + key(m.name) + "." + key(s.name), s.ser());
            }
            HudManager.saveAll(p);
            try (OutputStream o = Files.newOutputStream(cfgPath())) {
                p.store(o, "Oppka Client Config");
            }
        } catch (Exception ignored) {}
    }

    public static void load() {
        Path f = cfgPath();
        if (!Files.exists(f)) return;
        try (InputStream i = Files.newInputStream(f)) {
            Properties p = new Properties();
            p.load(i);
            String h = p.getProperty("hudVisible");
            if (h != null) HudManager.hudVisible = Boolean.parseBoolean(h);
            for (Mod m : ALL) {
                String e = p.getProperty("m." + key(m.name));
                if (e != null && !m.always) m.enabled = Boolean.parseBoolean(e);
                for (Setting s : m.sets) {
                    String v = p.getProperty("s." + key(m.name) + "." + key(s.name));
                    if (v != null) s.des(v);
                }
            }
            HudManager.loadAll(p);
        } catch (Exception ignored) {}
    }

    public static class HudEditorScreen extends Screen {
        public HudEditorScreen() {
            super(Text.literal("HUD Editor"));
        }

        @Override
        protected void init() {
            HudManager.editorMode = true;
        }

        @Override
        public boolean shouldPause() {
            return false;
        }

        @Override
        public void removed() {
            HudManager.editorMode = false;
            HudManager.dragging = null;
            save();
        }

        @Override
        public boolean keyPressed(int key, int scan, int mod) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                close();
                return true;
            }
            return super.keyPressed(key, scan, mod);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button == 0 && HudManager.mouseClicked(mx, my)) return true;
            return super.mouseClicked(mx, my, button);
        }

        @Override
        public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
            if (HudManager.mouseDragged(mx, my)) return true;
            return super.mouseDragged(mx, my, btn, dx, dy);
        }

        @Override
        public boolean mouseReleased(double mx, double my, int btn) {
            if (HudManager.mouseReleased()) return true;
            return super.mouseReleased(mx, my, btn);
        }

        @Override
        public void render(DrawContext ctx, int mx, int my, float delta) {
            ctx.fill(0, 0, width, height, 0x88000000);
            for (String name : HudManager.POS.keySet()) {
                if (isHudVisible(name)) HudManager.drawBox(ctx, name, themeColor());
            }
            ctx.drawCenteredTextWithShadow(textRenderer, "HUD EDITOR - Drag items to move", width / 2, 15, themeColor());
            super.render(ctx, mx, my, delta);
        }

        private boolean isHudVisible(String name) {
            return switch (name) {
                case "Watermark" -> WATERMARK.enabled;
                case "Coordinates" -> COORDS.enabled;
                case "FPS" -> FPS.enabled;
                case "Ping" -> PING.enabled;
                case "CPS" -> CPS.enabled;
                case "Speed" -> SPEED.enabled;
                case "Time" -> TIME.enabled;
                case "Effects" -> EFFECTS.enabled;
                case "ArrayList" -> ARRAYLIST.enabled;
                default -> false;
            };
        }
    }

    public static class MenuScreen extends Screen {
        private String currentCat = "HUD";

        public MenuScreen() {
            super(Text.literal("Oppka Client Menu"));
        }

        @Override
        protected void init() {
            super.init();
            rebuildWidgets();
        }

        private void rebuildWidgets() {
            this.clearChildren();
            int startX = this.width / 2 - 160;
            int startY = 40;

            for (int i = 0; i < CATS.length; i++) {
                String cat = CATS[i];
                this.addDrawableChild(ButtonWidget.builder(Text.literal(cat), b -> {
                    currentCat = cat;
                    rebuildWidgets();
                }).dimensions(startX + (i * 65), startY, 60, 20).build());
            }

            List<Mod> mods = modsOf(currentCat);
            int mY = startY + 35;
            for (Mod m : mods) {
                String status = m.enabled ? "§aON" : "§cOFF";
                this.addDrawableChild(ButtonWidget.builder(Text.literal(m.name + ": " + status), b -> {
                    if (!m.always) m.enabled = !m.enabled;
                    save();
                    rebuildWidgets();
                }).dimensions(startX, mY, 150, 20).build());

                int sX = startX + 160;
                for (Setting s : m.sets) {
                    this.addDrawableChild(ButtonWidget.builder(Text.literal(s.name + ": " + s.shown()), b -> {
                        if (s.type == 0) s.b = !s.b;
                        else if (s.type == 1) {
                            s.v += s.step;
                            if (s.v > s.max) s.v = s.min;
                        } else if (s.type == 2) {
                            s.idx = (s.idx + 1) % s.opts.length;
                        }
                        save();
                        rebuildWidgets();
                    }).dimensions(sX, mY, 150, 20).build());
                }
                mY += 24;
            }
        }

        private List<Mod> modsOf(String cat) {
            List<Mod> l = new ArrayList<>();
            for (Mod m : ALL) if (m.cat.equals(cat)) l.add(m);
            return l;
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            this.renderBackground(ctx, mouseX, mouseY, delta);
            ctx.drawCenteredTextWithShadow(textRenderer, "Oppka Client - v1.0.0", width / 2, 15, themeColor());
            super.render(ctx, mouseX, mouseY, delta);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}
