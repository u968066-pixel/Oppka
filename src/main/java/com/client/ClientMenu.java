package com.client;

import com.client.modules.combat.Aura;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class ClientMenu extends Screen {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final File CONFIG_FILE = new File(mc.runDirectory, "oppka_config.properties");

    public static int currentTheme = 0;
    public static boolean hudEnabled = true;

    public ClientMenu() {
        super(Text.literal("Oppka Client Menu"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4;

        // Кнопка включения/выключения Ауры
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Aura: " + (Aura.toggled ? "§aON" : "§cOFF")),
                button -> {
                    Aura.toggled = !Aura.toggled;
                    button.setMessage(Text.literal("Aura: " + (Aura.toggled ? "§aON" : "§cOFF")));
                    save();
                }
        ).dimensions(centerX - 100, startY, 200, 20).build());

        // Кнопка режима Ауры
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Aura Mode: §e" + Aura.attackMode),
                button -> {
                    Aura.attackMode = Aura.attackMode.equals("Switch") ? "Single" : "Switch";
                    button.setMessage(Text.literal("Aura Mode: §e" + Aura.attackMode));
                    save();
                }
        ).dimensions(centerX - 100, startY + 25, 200, 20).build());

        // Кнопка закрытия меню
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"),
                button -> this.close()
        ).dimensions(centerX - 100, startY + 60, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, "§lOppka Client Settings", this.width / 2, this.height / 4 - 25, themeColor());
        super.render(context, mouseX, mouseY, delta);
    }

    // --- Обязательные методы для HudManager ---

    public static String key(String raw) {
        if (raw == null) return "unknown";
        return raw.toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }

    public static int themeColor() {
        switch (currentTheme) {
            case 1: return 0xFFAA00FF; // Фиолетовый
            case 2: return 0xFF00FF66; // Зеленый
            case 3: return 0xFFFF3333; // Красный
            default: return 0xFF00E5FF; // Голубой
        }
    }

    public static void save() {
        try {
            Properties props = new Properties();
            if (CONFIG_FILE.exists()) {
                try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
                    props.load(in);
                }
            }
            props.setProperty("aura.toggled", String.valueOf(Aura.toggled));
            props.setProperty("aura.mode", Aura.attackMode);
            props.setProperty("ui.theme", String.valueOf(currentTheme));

            try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
                props.store(out, "Oppka Client Configuration");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onRender(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.world == null || !hudEnabled) return;
        int color = themeColor();
        context.drawText(mc.textRenderer, "Oppka Client", 10, 10, color, true);
        context.drawText(mc.textRenderer, "Aura: " + (Aura.toggled ? "§aON" : "§cOFF"), 10, 22, 0xFFFFFFFF, true);
    }
}
