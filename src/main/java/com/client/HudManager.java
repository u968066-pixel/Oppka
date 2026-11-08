package com.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HudManager {
    public static boolean hudVisible = true;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public static final Map<String, int[]> POS = new HashMap<>();
    public static final Map<String, int[]> DEFAULT = new HashMap<>();
    public static boolean editorMode = false;
    public static String dragging = null;
    public static int dragDX, dragDY;

    public static void init() {
        register("Watermark", 4, 4, 120, 14);
        register("Coordinates", 4, 22, 110, 11);
        register("FPS", 4, 34, 70, 11);
        register("Ping", 4, 46, 90, 11);
        register("CPS", 4, 58, 70, 11);
        register("Speed", 4, 70, 100, 11);
        register("Time", 4, 82, 60, 11);
        register("Effects", 4, 96, 130, 11);
        register("ArrayList", 300, 4, 100, 11);
    }

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
        if (dragging != null) {
            dragging = null;
            ClientMenu.save();
            return true;
        }
        return false;
    }

    public static void saveAll(Properties p) {
        for (Map.Entry<String, int[]> e : POS.entrySet()) {
            int[] v = e.getValue();
            p.setProperty("hud." + ClientMenu.key(e.getKey()) + ".x", String.valueOf(v[0]));
            p.setProperty("hud." + ClientMenu.key(e.getKey()) + ".y", String.valueOf(v[1]));
        }
    }

    public static void loadAll(Properties p) {
        for (Map.Entry<String, int[]> e : POS.entrySet()) {
            try {
                String x = p.getProperty("hud." + ClientMenu.key(e.getKey()) + ".x");
                String y = p.getProperty("hud." + ClientMenu.key(e.getKey()) + ".y");
                if (x != null && y != null) {
                    e.getValue()[0] = Integer.parseInt(x);
                    e.getValue()[1] = Integer.parseInt(y);
                }
            } catch (Exception ignored) {}
        }
    }

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || !hudVisible) return;
        TextRenderer tr = mc.textRenderer;
        int col = ClientMenu.themeColor();

        int[] wp = pos("Watermark");
        if (ClientMenu.WATERMARK.enabled) {
            String wm = "Oppka Client  |  " + mc.getCurrentFps() + " FPS";
            int w = tr.getWidth(wm);
            ctx.fill(wp[0], wp[1], wp[0] + w + 16, wp[1] + 14, 0xAA101018);
            ctx.fill(wp[0], wp[1], wp[0] + 2, wp[1] + 14, col);
            ctx.drawTextWithShadow(tr, wm, wp[0] + 6, wp[1] + 3, col);
        }

        int[] cp = pos("Coordinates");
        if (ClientMenu.COORDS.enabled) {
            String s = String.format("XYZ: %.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            int w = tr.getWidth(s);
            ctx.fill(cp[0], cp[1], cp[0] + w + 8, cp[1] + 11, 0x80101018);
            ctx.fill(cp[0], cp[1], cp[0] + 1, cp[1] + 11, col);
            ctx.drawTextWithShadow(tr, s, cp[0] + 4, cp[1] + 2, 0xFFFFFFFF);
        }

        int[] fp = pos("FPS");
        if (ClientMenu.FPS.enabled) {
            String s = "FPS: " + mc.getCurrentFps();
            int w = tr.getWidth(s);
            ctx.fill(fp[0], fp[1], fp[0] + w + 8, fp[1] + 11, 0x80101018);
            ctx.fill(fp[0], fp[1], fp[0] + 1, fp[1] + 11, col);
            ctx.drawTextWithShadow(tr, s, fp[0] + 4, fp[1] + 2, 0xFFFFFFFF);
        }

        int[] cpsp = pos("CPS");
        if (ClientMenu.CPS.enabled) {
            String s = "CPS: " + CoreInitializer.clicks.size();
            int w = tr.getWidth(s);
            ctx.fill(cpsp[0], cpsp[1], cpsp[0] + w + 8, cpsp[1] + 11, 0x80101018);
            ctx.fill(cpsp[0], cpsp[1], cpsp[0] + 1, cpsp[1] + 11, col);
            ctx.drawTextWithShadow(tr, s, cpsp[0] + 4, cpsp[1] + 2, 0xFFFFFFFF);
        }

        int[] tp = pos("Time");
        if (ClientMenu.TIME.enabled) {
            String s = LocalTime.now().format(TIME_FMT);
            int w = tr.getWidth(s);
            ctx.fill(tp[0], tp[1], tp[0] + w + 8, tp[1] + 11, 0x80101018);
            ctx.fill(tp[0], tp[1], tp[0] + 1, tp[1] + 11, col);
            ctx.drawTextWithShadow(tr, s, tp[0] + 4, tp[1] + 2, 0xFFFFFFFF);
        }
    }
  }
