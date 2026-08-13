package com.modernizegameframework.hollowhouse;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 照明工作方块界面
 * 显示当前照明等级并允许玩家选择 1~3 级亮度
 */
public class LightingScreen extends Screen {

    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 140;
    private static final int BUTTON_WIDTH = 50;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;

    private final int unlockedLevel;
    private int selectedLevel;

    private int leftPos;
    private int topPos;
    private final Button[] levelButtons = new Button[3];

    public LightingScreen(int unlockedLevel, int selectedLevel) {
        super(Component.literal("照明"));
        this.unlockedLevel = Math.max(1, Math.min(3, unlockedLevel));
        this.selectedLevel = Math.max(1, Math.min(this.unlockedLevel, selectedLevel));
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - PANEL_HEIGHT) / 2;

        int totalButtonWidth = 3 * BUTTON_WIDTH + 2 * BUTTON_GAP;
        int startX = leftPos + (PANEL_WIDTH - totalButtonWidth) / 2;
        int buttonY = topPos + 70;

        for (int i = 0; i < 3; i++) {
            final int level = i + 1;
            int x = startX + i * (BUTTON_WIDTH + BUTTON_GAP);
            levelButtons[i] = Button.builder(Component.literal("§e" + level + "级"), btn -> selectLevel(level))
                    .bounds(x, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();
            this.addRenderableWidget(levelButtons[i]);
        }

        updateButtonStates();
    }

    private void selectLevel(int level) {
        if (level > unlockedLevel) {
            return;
        }
        selectedLevel = level;
        updateButtonStates();
        LightingNetwork.CHANNEL.sendToServer(new LightingNetwork.SelectLevelPacket(level));
    }

    private void updateButtonStates() {
        for (int i = 0; i < 3; i++) {
            int level = i + 1;
            Button button = levelButtons[i];
            boolean locked = level > unlockedLevel;
            boolean selected = level == selectedLevel;

            button.active = !locked && !selected;

            String label;
            if (locked) {
                label = "§7" + level + "级🔒";
            } else if (selected) {
                label = "§a[ " + level + "级 ]";
            } else {
                label = "§e" + level + "级";
            }
            button.setMessage(Component.literal(label));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        // 绘制面板背景
        graphics.fill(leftPos, topPos, leftPos + PANEL_WIDTH, topPos + PANEL_HEIGHT, 0xB02A2A2A);
        graphics.renderOutline(leftPos, topPos, PANEL_WIDTH, PANEL_HEIGHT, 0xFF555555);

        super.render(graphics, mouseX, mouseY, partialTick);
        renderPanelText(graphics);
    }

    private void renderPanelText(GuiGraphics graphics) {
        int x = leftPos + 8;
        int y = topPos + 8;
        int lineHeight = 12;

        graphics.drawString(this.font, Component.literal("§n照明"), x, y, 0xFFFFFFFF, false);
        y += lineHeight + 6;

        graphics.drawString(this.font, Component.literal("已解锁等级：§e" + unlockedLevel + " / 3"), x, y, 0xFFFFFFFF, false);
        y += lineHeight;

        graphics.drawString(this.font, Component.literal("当前选择：§e" + selectedLevel + "级"), x, y, 0xFFFFFFFF, false);
        y += lineHeight;

        int lightLevel = getLightLevelForLightingLevel(selectedLevel);
        graphics.drawString(this.font, Component.literal("亮度：§e" + lightLevel), x, y, 0xFFFFFFFF, false);
        y += lineHeight + 6;

        graphics.drawString(this.font, Component.literal("需要：§7供电站发电 + 红石信号"), x, y, 0xFFFFFFFF, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 根据照明等级获取对应的光照等级
     * 1 级 = 4，2 级 = 8，3 级 = 12
     */
    private static int getLightLevelForLightingLevel(int lightingLevel) {
        return switch (lightingLevel) {
            case 1 -> 4;
            case 2 -> 8;
            case 3 -> 12;
            default -> 0;
        };
    }
}