package com.modernizegameframework.hollowhouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Consumer;

/**
 * 藏身处工作方块左侧栏条目组件
 * 显示工作方块名称、锁定/解锁状态，并支持点击选中
 */
public class HollowHouseWorkBlockEntry {

    /** 条目高度 */
    public static final int ENTRY_HEIGHT = 28;

    /** 位置与尺寸 */
    protected int x, y, width, height;
    /** 是否可见可用 */
    protected boolean visible = true;
    protected boolean enabled = true;

    /** 当前条目对应的工作方块类型 */
    private final HollowHouseWorkBlockType workBlockType;
    /** 当前等级，0 表示未解锁 */
    private int currentLevel;
    /** 是否被选中 */
    private boolean selected = false;
    /** 点击回调 */
    private Consumer<HollowHouseWorkBlockType> onClick;

    /** 选中背景色 */
    private static final int SELECTED_BG = 0xFF4A90D9;
    /** 悬停背景色 */
    private static final int HOVER_BG = 0xFF3A3A3A;
    /** 默认背景色 */
    private static final int DEFAULT_BG = 0xFF2A2A2A;
    /** 锁定状态文字颜色 */
    private static final int LOCKED_TEXT = 0xFFAAAAAA;
    /** 解锁状态文字颜色 */
    private static final int UNLOCKED_TEXT = 0xFFFFFFFF;
    /** 边框颜色 */
    private static final int BORDER_COLOR = 0xFF555555;

    public HollowHouseWorkBlockEntry(int x, int y, int width,
                                     HollowHouseWorkBlockType type, int currentLevel) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = ENTRY_HEIGHT;
        this.workBlockType = type;
        this.currentLevel = currentLevel;
    }

    /**
     * 设置当前等级
     */
    public void setCurrentLevel(int level) {
        this.currentLevel = level;
    }

    /**
     * 设置是否选中
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * 判断是否选中
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * 获取当前条目对应的工作方块类型
     */
    public HollowHouseWorkBlockType getWorkBlockType() {
        return workBlockType;
    }

    /**
     * 设置点击回调
     */
    public void setOnClick(Consumer<HollowHouseWorkBlockType> onClick) {
        this.onClick = onClick;
    }

    /**
     * 判断鼠标是否悬停在此组件上
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        boolean hovered = isMouseOver(mouseX, mouseY);
        int bgColor = selected ? SELECTED_BG : (hovered ? HOVER_BG : DEFAULT_BG);

        // 绘制背景
        graphics.fill(x, y, x + width, y + height, bgColor);
        // 绘制底部边框
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR);

        // 名称
        String name = workBlockType.getDisplayName();
        graphics.drawString(Minecraft.getInstance().font, name, x + 4, y + 5, UNLOCKED_TEXT, false);

        // 状态文字
        String statusText = currentLevel == 0 ? "§c未解锁" : ("§aLv." + currentLevel + "/" + workBlockType.getMaxLevel());
        graphics.drawString(Minecraft.getInstance().font, statusText, x + 4, y + 16,
                currentLevel == 0 ? LOCKED_TEXT : UNLOCKED_TEXT, false);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (onClick != null) {
            onClick.accept(workBlockType);
        }
        return true;
    }
}