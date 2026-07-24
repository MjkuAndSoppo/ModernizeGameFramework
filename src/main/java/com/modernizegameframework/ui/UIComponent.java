package com.modernizegameframework.ui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * UI 组件基类
 * 所有自研 UI 组件都继承此类，统一提供位置、尺寸、可见性、启用状态等基础属性
 */
public abstract class UIComponent {

    /** 组件左上角 X 坐标（相对于父容器或屏幕） */
    protected int x;
    /** 组件左上角 Y 坐标（相对于父容器或屏幕） */
    protected int y;
    /** 组件宽度 */
    protected int width;
    /** 组件高度 */
    protected int height;
    /** 是否可见 */
    protected boolean visible = true;
    /** 是否启用（启用时才接收交互事件） */
    protected boolean enabled = true;

    public UIComponent(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * 渲染组件
     *
     * @param graphics   渲染上下文
     * @param mouseX     鼠标 X 坐标
     * @param mouseY     鼠标 Y 坐标
     * @param partialTick 部分 tick，用于动画插值
     */
    public abstract void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    /**
     * 处理鼠标点击
     *
     * @param mouseX 鼠标 X 坐标
     * @param mouseY 鼠标 Y 坐标
     * @param button 鼠标按键
     * @return 是否消费了此次点击
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    /**
     * 处理鼠标释放
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    /**
     * 处理鼠标拖动
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    /**
     * 处理鼠标滚动
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    /**
     * 判断坐标是否在组件范围内
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
