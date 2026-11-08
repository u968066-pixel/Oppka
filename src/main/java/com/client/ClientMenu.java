package com.client;

import com.client.modules.combat.Aura;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ClientMenu extends Screen {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Заглушки элементов HUD для совместимости
    public static final HudElement WATERMARK = new HudElement(true);
    public static final HudElement COORDS = new HudElement(true);
    public static final HudElement FPS = new HudElement(true);
    public static final HudElement CPS = new HudElement(true);
    public static final HudElement TIME = new HudElement(true);

    public static class HudElement {
        public boolean enabled;
        public HudElement(boolean enabled) { this.enabled = enabled; }
    }

    public ClientMenu() {
        super(Text.literal("Oppka Client Menu"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4;

        // Кнопка включения/выключения Ауры прямо в меню
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Aura: " + (Aura.toggled ? "§aON" : "§cOFF")),
                button -> {
                    Aura.toggled = !Aura.toggled;
                    button.setMessage(Text.literal("Aura: " + (Aura.toggled ? "§aON" : "§cOFF")));
                }
        ).dimensions(centerX - 100, startY, 200, 20).build());

        // Кнопка закрытия
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"),
                button -> this.close()
        ).dimensions(centerX - 100, startY + 30, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, "§lOppka Client", this.width / 2, this.height / 4 - 25, 0xFF00E5FF);
        super.render(context, mouseX, mouseY, delta);
    }

    public static String key(String raw) { return raw == null ? "unknown" : raw.toLowerCase(); }
    public static int themeColor() { return 0xFF00E5FF; }
    public static void save() {}
}
