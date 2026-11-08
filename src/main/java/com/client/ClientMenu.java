package com.client;

import com.client.modules.combat.Aura;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class ClientMenu extends Screen {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Настройки визуалов в стиле Пирожка
    public static boolean visualsEsp = true;
    public static boolean targetHud = true;   // Инфо о цели под прицелом
    public static boolean customBoxes = true; // Подсветка хитбоксов

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
        int startY = this.height / 4 - 30;

        // Кнопка Киллауры
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("KillAura: " + (Aura.toggled ? "§aON" : "§cOFF")),
                button -> {
                    Aura.toggled = !Aura.toggled;
                    button.setMessage(Text.literal("KillAura: " + (Aura.toggled ? "§aON" : "§cOFF")));
                }
        ).dimensions(centerX - 100, startY, 200, 20).build());

        // Кнопка Триггербота
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("TriggerBot: " + (Aura.triggerBot ? "§aON" : "§cOFF")),
                button -> {
                    Aura.triggerBot = !Aura.triggerBot;
                    button.setMessage(Text.literal("TriggerBot: " + (Aura.triggerBot ? "§aON" : "§cOFF")));
                }
        ).dimensions(centerX - 100, startY + 25, 200, 20).build());

        // Кнопка Визуалов (ESP)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Visuals ESP: " + (visualsEsp ? "§aON" : "§cOFF")),
                button -> {
                    visualsEsp = !visualsEsp;
                    button.setMessage(Text.literal("Visuals ESP: " + (visualsEsp ? "§aON" : "§cOFF")));
                }
        ).dimensions(centerX - 100, startY + 50, 200, 20).build());

        // Кнопка TargetHUD (инфо о цели)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("TargetHUD: " + (targetHud ? "§aON" : "§cOFF")),
                button -> {
                    targetHud = !targetHud;
                    button.setMessage(Text.literal("TargetHUD: " + (targetHud ? "§aON" : "§cOFF")));
                }
        ).dimensions(centerX - 100, startY + 75, 200, 20).build());

        // Кнопка закрытия
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"),
                button -> this.close()
        ).dimensions(centerX - 100, startY + 110, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, "§lOppka Client [Pirozhok Style]", this.width / 2, this.height / 4 - 55, 0xFF00E5FF);
        super.render(context, mouseX, mouseY, delta);
    }

    // Отрисовка визуалов на экране игры (TargetHUD в стиле пвп-клиентов)
    public static void onRender(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.world == null) return;

        // Отрисовка ватермарки клиента
        context.drawText(mc.textRenderer, "§bOppka Client v1.0 §7[Pirozhok Visuals]", 10, 10, 0xFFFFFFFF, true);

        // Если включен TargetHUD и мы смотрим на живое существо / игрока
        if (targetHud && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
            if (hit.getEntity() instanceof LivingEntity target) {
                int screenWidth = mc.getWindow().getScaledWidth();
                int screenHeight = mc.getWindow().getScaledHeight();
                int x = screenWidth / 2 + 15;
                int y = screenHeight / 2 - 10;

                // Рисуем мини-плашку с информацией о цели (имя, здоровье)
                context.fill(x - 2, y - 2, x + 120, y + 30, 0x90000000);
                context.drawText(mc.textRenderer, target.getName().getString(), x + 4, y + 4, 0xFFFF55, true);
                
                String hpText = "HP: " + (int)target.getHealth() + " / " + (int)target.getMaxHealth();
                context.drawText(mc.textRenderer, hpText, x + 4, y + 16, 0xFF5555, true);
            }
        }
    }

    public static String key(String raw) { return raw == null ? "unknown" : raw.toLowerCase(); }
    public static int themeColor() { return 0xFF00E5FF; }
    public static void save() {}
}
