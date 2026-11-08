package com.client;

import com.client.modules.combat.Aura;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class ClientMenu extends Screen {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // --- Функционал из PirozhokVisuals (разрешенные визуалы и QoL) ---
    public static boolean noHurtCam = true;   // Отключение тряски камеры при получении урона
    public static boolean fullbright = true;  // Полная яркость (ночное зрение)
    public static boolean targetHud = true;   // Красивый TargetHUD на экране
    public static boolean visualsEsp = true;  // Общий тумблер визуалов

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
        super(Text.literal("Oppka x PirozhokVisuals"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4 - 40;

        // 1. Киллаура
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("KillAura: " + (Aura.toggled ? "§aON" : "§cOFF")),
                button -> {
                    Aura.toggled = !Aura.toggled;
                    button.setMessage(Text.literal("KillAura: " + (Aura.toggled ? "§aON" : "§cOFF")));
                }
        ).dimensions(centerX - 100, startY, 200, 20).build());

        // 2. Триггербот
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("TriggerBot: " + (Aura.triggerBot ? "§aON" : "§cOFF")),
                button -> {
                    Aura.triggerBot = !Aura.triggerBot;
                    button.setMessage(Text.literal("TriggerBot: " + (Aura.triggerBot ? "§aON" : "§cOFF")));
                }
        ).dimensions(centerX - 100, startY + 25, 200, 20).build());

        // 3. NoHurtCam (Фича из PirozhokVisuals)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("NoHurtCam: " + (noHurtCam ? "§aON" : "§cOFF")),
                button -> {
                    noHurtCam = !noHurtCam;
                    button.setMessage(Text.literal("NoHurtCam: " + (noHurtCam ? "§aON" : "§cOFF")));
                }
        ).dimensions(centerX - 100, startY + 50, 200, 20).build());

        // 4. Fullbright (Яркость)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Fullbright: " + (fullbright ? "§aON" : "§cOFF")),
                button -> {
                    fullbright = !fullbright;
                    button.setMessage(Text.literal("Fullbright: " + (fullbright ? "§aON" : "§cOFF")));
                }
        ).dimensions(centerX - 100, startY + 75, 200, 20).build());

        // 5. TargetHUD (Визуал цели)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("TargetHUD: " + (targetHud ? "§aON" : "§cOFF")),
                button -> {
                    targetHud = !targetHud;
                    button.setMessage(Text.literal("TargetHUD: " + (targetHud ? "§aON" : "§cOFF")));
                }
        ).dimensions(centerX - 100, startY + 100, 200, 20).build());

        // Кнопка закрытия
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"),
                button -> this.close()
        ).dimensions(centerX - 100, startY + 130, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, "§lOppka Client §7+ PirozhokVisuals", this.width / 2, this.height / 4 - 65, 0xFF00E5FF);
        super.render(context, mouseX, mouseY, delta);
    }

    // Рендер элементов интерфейса прямо на экране игры
    public static void onRender(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.world == null) return;

        // Ватермарка объединенного клиента
        context.drawText(mc.textRenderer, "§bOppka §7| PirozhokVisuals Edition", 10, 10, 0xFFFFFFFF, true);

        // Применяем Fullbright (выкручиваем гамму, если включено)
        if (fullbright && mc.options !=
            
