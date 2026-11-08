package com.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class MyMenuScreen extends Screen {

    public MyMenuScreen() {
        super(Text.literal("Internal Client Menu"));
    }

    @Override
    protected void init() {
        super.init();

        // Динамический текст для кнопки в зависимости от статуса бота
        updateMenuButtons();
    }

    private void updateMenuButtons() {
        // Очищаем старые видреты, чтобы кнопка корректно перерисовывалась при клике
        this.clearChildren();

        String botStatusText = CoreInitializer.triggerBotEnabled ? "§aTriggerBot: ВКЛ" : "§cTriggerBot: ВЫКЛ";

        // Кнопка переключения TriggerBot
        this.addDrawableChild(
            ButtonWidget.builder(Text.literal(botStatusText), button -> {
                CoreInitializer.triggerBotEnabled = !CoreInitializer.triggerBotEnabled;
                updateMenuButtons(); // Пересоздаем кнопку, чтобы обновился её текст и цвет
            })
            .dimensions(this.width / 2 - 100, this.height / 2 - 20, 200, 20)
            .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        
        // Заголовок меню
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
