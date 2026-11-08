package com.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HudManager {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Map<String, int[]> positions = new HashMap<>();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    static {
        load();
    }

    public static String key(String s) {
        return s.toLowerCase().replace(' ', '_');
    }

    public static int themeColor() {
        double t = (System.currentTimeMillis() % 4000) / 4000.0 * Math.PI * 2;
        return 0xFF000000 | ((int) (127 + 127 * Math.sin(t)) << 16) | ((int) (127 + 127 * Math.sin(t + 2.094)) << 8) | (int) (127 + 127 * Math.sin(t + 4.188));
    }

    public static Path cfgPath() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("oppka_hud.properties");
    }

    public static void save() {
        try {
            Properties p = new Properties();
            for (Map.Entry<String, int[]> e : positions.entrySet()) {
                int[] v = e.getValue();
                p.setProperty("hud." + key(e.getKey()) + ".x", String.valueOf(v[0]));
                p.setProperty("hud." + key(e.getKey()) + ".y", String.valueOf(v[1]));
            }
            try (OutputStream o = Files.newOutputStream(cfgPath())) {
                p.store(o, "Oppka HUD Config");
            }
        } catch (Exception ignored) {}
    }

    public static void load() {
        Path f = cfgPath();
        if (!Files.exists(f)) return;
        try (InputStream i = Files.newInputStream(f)) {
            Properties p = new Properties();
            p.load(i);
            for (String name : new String[]{"Watermark", "Coordinates", "FPS", "Ping", "CPS", "Speed", "Time", "Totems", "Armor", "Effects", "ArrayList"}) {
                String xStr = p.getProperty("hud." + key(name) + ".x");
                String yStr = p.getProperty("hud." + key(name) + ".y");
                if (xStr != null && yStr != null) {
                    positions.put(name, new int[]{Integer.parseInt(xStr), Integer.parseInt(yStr)});
                }
            }
        } catch (Exception ignored) {}
    }

    public static void renderHud(DrawContext ctx) {
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        int col = themeColor();

        int yOffset = 10;

        if (CoreInitializer.watermark) {
            String wm = "Oppka Client - v1.0.0";
            ctx.drawTextWithShadow(tr, wm, 10, yOffset, col);
            yOffset += 12;
        }

        if (CoreInitializer.coordinates) {
            String coords = String.format("XYZ: %.1f, %.1f, %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            ctx.drawTextWithShadow(tr, coords, 10, yOffset, 0xFFFFFFFF);
            yOffset += 12;
        }

        if (CoreInitializer.fps) {
            String fps = "FPS: " + mc.getCurrentFps();
            ctx.drawTextWithShadow(tr, fps, 10, yOffset, 0xFFFFFFFF);
            yOffset += 12;
        }

        if (CoreInitializer.timeMod) {
            String time = "Time: " + LocalDateTime.now().format(TIME_FMT);
            ctx.drawTextWithShadow(tr, time, 10, yOffset, 0xFFFFFFFF);
            yOffset += 12;
        }
    }
}
