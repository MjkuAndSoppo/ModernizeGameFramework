package com.modernizegameframework.ui;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * 面板容器组件
 * 可包含多个子组件，提供背景填充、边框、内边距等基础功能
 */
public class UIPanel extends UIComponent {

    /** 背景颜色，含 Alpha 通道 */
    protected int backgroundColor = 0xFF2A2A2A;
    /** 边框颜色，为 0 时不绘制边框 */
    protected int borderColor = 0xFF555555;
    /** 内边距 */
    protected int padding = 0;
    /** 子组件列表 */
    protected final List<UIComponent> children = new ArrayList<>();

    public UIPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /**
     * 添加子组件
     */
    public UIPanel addChild(UIComponent child) {
        children.add(child);
        return this;
    }

    /**
     * 移除子组件
     */
    public UIPanel removeChild(UIComponent child) {
        children.remove(child);
        return this;
    }

    /**
     * 清空所有子组件
     */
    public void clearChildren() {
        children.clear();
    }

    public List<UIComponent> getChildren() {
        return children;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }
        renderBackground(graphics);
        renderBorder(graphics);
        for (UIComponent child : children) {
            if (child.isVisible()) {
                child.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    /**
     * 绘制面板背景
     */
    protected void renderBackground(GuiGraphics graphics) {
        graphics.fill(x, y, x + width, y + height, backgroundColor);
    }

    /**
     * 绘制面板边框
     */
    protected void renderBorder(GuiGraphics graphics) {
        if (borderColor != 0) {
            graphics.renderOutline(x, y, width, height, borderColor);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        // 逆序遍历，让后添加的子组件优先接收事件
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (child.isVisible() && child.isEnabled() && child.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) {
            return false;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (child.isVisible() && child.isEnabled() && child.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!visible || !enabled) {
            return false;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (child.isVisible() && child.isEnabled() && child.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!visible || !enabled) {
            return false;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (child.isVisible() && child.isEnabled() && child.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return false;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    public int getPadding() {
        return padding;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }
}
