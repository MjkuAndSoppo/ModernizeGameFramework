package com.modernizegameframework.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 标签栏组件
 * 支持横向排列多个标签页，可设置当前选中索引，点击时触发回调
 */
public class UITabBar extends UIComponent {

    /** 标签页标题列表 */
    private final List<Component> labels = new ArrayList<>();
    /** 当前选中标签索引 */
    private int selectedIndex = 0;
    /** 标签页宽度 */
    private int tabWidth = 60;
    /** 标签页高度 */
    private int tabHeight = 20;
    /** 标签页之间的间距 */
    private int tabGap = 4;
    /** 字体渲染器 */
    private final Font font;
    /** 选中标签背景色 */
    private int activeColor = 0xFF4A90D9;
    /** 未选中标签背景色 */
    private int inactiveColor = 0xFF3A3A3A;
    /** 标签边框颜色 */
    private int borderColor = 0xFF555555;
    /** 文字颜色 */
    private int textColor = 0xFFFFFFFF;
    /** 点击回调 */
    private Consumer<Integer> onTabClicked;

    public UITabBar(int x, int y, int width, int height, Font font) {
        super(x, y, width, height);
        this.font = font;
    }

    /**
     * 添加一个标签页
     */
    public UITabBar addTab(Component label) {
        labels.add(label);
        return this;
    }

    /**
     * 清空所有标签页
     */
    public void clearTabs() {
        labels.clear();
        selectedIndex = 0;
    }

    public List<Component> getLabels() {
        return labels;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    public void setOnTabClicked(Consumer<Integer> onTabClicked) {
        this.onTabClicked = onTabClicked;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }
        // 标签栏背景条
        graphics.fill(x, y, x + width, y + height, 0xFF151515);
        int startX = x + 4;
        int startY = y + (height - tabHeight) / 2;
        for (int i = 0; i < labels.size(); i++) {
            int tabX = startX + i * (tabWidth + tabGap);
            int color = i == selectedIndex ? activeColor : inactiveColor;
            graphics.fill(tabX, startY, tabX + tabWidth, startY + tabHeight, color);
            graphics.renderOutline(tabX, startY, tabWidth, tabHeight, borderColor);

            Component label = labels.get(i);
            int textWidth = font.width(label);
            int textX = tabX + (tabWidth - textWidth) / 2;
            int textY = startY + (tabHeight - 8) / 2;
            graphics.drawString(font, label, textX, textY, textColor, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled || labels.isEmpty()) {
            return false;
        }
        int startX = x + 4;
        int startY = y + (height - tabHeight) / 2;
        for (int i = 0; i < labels.size(); i++) {
            int tabX = startX + i * (tabWidth + tabGap);
            if (mouseX >= tabX && mouseX <= tabX + tabWidth
                    && mouseY >= startY && mouseY <= startY + tabHeight) {
                selectedIndex = i;
                if (onTabClicked != null) {
                    onTabClicked.accept(i);
                }
                return true;
            }
        }
        return false;
    }

    public int getTabWidth() {
        return tabWidth;
    }

    public void setTabWidth(int tabWidth) {
        this.tabWidth = tabWidth;
    }

    public int getTabHeight() {
        return tabHeight;
    }

    public void setTabHeight(int tabHeight) {
        this.tabHeight = tabHeight;
    }

    public int getTabGap() {
        return tabGap;
    }

    public void setTabGap(int tabGap) {
        this.tabGap = tabGap;
    }

    public int getActiveColor() {
        return activeColor;
    }

    public void setActiveColor(int activeColor) {
        this.activeColor = activeColor;
    }

    public int getInactiveColor() {
        return inactiveColor;
    }

    public void setInactiveColor(int inactiveColor) {
        this.inactiveColor = inactiveColor;
    }
}
