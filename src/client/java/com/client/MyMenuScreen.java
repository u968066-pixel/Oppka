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

        // Добавляем кнопку в центр экрана
        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Привет от мода!"), button -> {
                // Действие при нажатии на кнопку
                if (this.client != لوا && this.client.player != null) {
                    this.client.player.sendMessage(Text.literal("§aКнопка в меню нажата!"), false);
                }
            })
            .dimensions(this.width / 2 - 100, this.height / 2 - 20, 200, 20)
            .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Затемнение фона под меню
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        
        // Рисуем заголовок меню по центру
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false; // Игра не ставится на паузу при открытии меню
    }
}
