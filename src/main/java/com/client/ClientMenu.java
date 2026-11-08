package com.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import com.client.modules.combat.Aura;

public class ClientMenu {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Метод отрисовки интерфейса (HUD / Меню клиента) на экране
    public static void onRender(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.world == null) return;

        // Пример отрисовки текста на экране (название клиента и статус Ауры)
        int color = Aura.toggled ? 0x55FF55 : 0xFF5555; // Зеленый если включена, красный если выключена
        
        // Рисуем плашку или текст в левом верхнем углу
        context.drawText(mc.textRenderer, "§bOppka Client v1.0", 10, 10, 0xFFFFFFFF, true);
        context.drawText(mc.textRenderer, "Aura: " + (Aura.toggled ? "§aEnabled" : "§cDisabled"), 10, 22, color, true);
        
        // Отображение текущего CPS (если используется)
        int cpsSize = CoreInitializer.clicks != null ? CoreInitializer.clicks.size() : 0;
        context.drawText(mc.textRenderer, "CPS: " + cpsSize, 10, 34, 0xFFFF55, true);
    }
}
