package com.modernizegameframework.ui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 可滚动面板组件
 * 内部内容高度超过面板高度时可垂直滚动，支持鼠标滚轮与拖拽滚动条
 */
public class UIScrollPanel extends UIPanel {

    /** 当前垂直滚动偏移量（像素） */
    private int scrollOffset = 0;
    /** 内容总高度 */
    private int contentHeight = 0;
    /** 滚动条宽度 */
    public static final int SCROLLBAR_WIDTH = 6;
    /** 滚动条颜色 */
    private int scrollbarColor = 0xFF777777;
    /** 滚动条背景颜色 */
    private int scrollbarBgColor = 0xFF222222;
    /** 是否正在拖动滚动条 */
    private boolean draggingScrollbar = false;
    /** 拖动开始时鼠标 Y 与滚动条顶部的偏移 */
    private double dragStartYOffset = 0;

    public UIScrollPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /**
     * 设置内容总高度， scrollOffset 会自动限制在合法范围内
     */
    public void setContentHeight(int contentHeight) {
        this.contentHeight = Math.max(contentHeight, height);
        clampScrollOffset();
    }

    public int getContentHeight() {
        return contentHeight;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    /**
     * 直接设置滚动偏移
     */
    public void setScrollOffset(int offset) {
        this.scrollOffset = offset;
        clampScrollOffset();
    }

    private void clampScrollOffset() {
        int maxOffset = Math.max(0, contentHeight - height);
        if (scrollOffset < 0) {
            scrollOffset = 0;
        } else if (scrollOffset > maxOffset) {
            scrollOffset = maxOffset;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }
        renderBackground(graphics);

        // 使用剪刀区域裁剪子组件，超出面板范围的内容不显示
        graphics.enableScissor(x, y, x + width, y + height);
        graphics.pose().pushPose();
        graphics.pose().translate(0, -scrollOffset, 0);
        for (UIComponent child : children) {
            if (child.isVisible()) {
                child.render(graphics, mouseX, mouseY + scrollOffset, partialTick);
            }
        }
        graphics.pose().popPose();
        graphics.disableScissor();

        renderBorder(graphics);
        renderScrollbar(graphics);
    }

    /**
     * 绘制垂直滚动条
     */
    private void renderScrollbar(GuiGraphics graphics) {
        if (contentHeight <= height) {
            return;
        }
        int scrollbarX = x + width - SCROLLBAR_WIDTH - 1;
        int scrollbarY = y + 1;
        int scrollbarHeight = height - 2;
        // 滚动条背景
        graphics.fill(scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, scrollbarBgColor);
        // 滚动条滑块
        float ratio = (float) height / contentHeight;
        int thumbHeight = Math.max(10, (int) (scrollbarHeight * ratio));
        int maxOffset = contentHeight - height;
        int thumbY = scrollbarY + (maxOffset == 0 ? 0 : (int) ((scrollbarHeight - thumbHeight) * ((float) scrollOffset / maxOffset)));
        graphics.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, scrollbarColor);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!visible || !enabled || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        // 子组件优先消费滚动事件
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (child.isVisible() && child.isEnabled() && child.mouseScrolled(mouseX, mouseY + scrollOffset, delta)) {
                return true;
            }
        }
        if (contentHeight > height) {
            scrollOffset -= (int) (delta * 10);
            clampScrollOffset();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        // 先检查是否点在滚动条上
        if (contentHeight > height && isMouseOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            int scrollbarY = y + 1;
            int scrollbarHeight = height - 2;
            float ratio = (float) height / contentHeight;
            int thumbHeight = Math.max(10, (int) (scrollbarHeight * ratio));
            int maxOffset = contentHeight - height;
            int thumbY = scrollbarY + (maxOffset == 0 ? 0 : (int) ((scrollbarHeight - thumbHeight) * ((float) scrollOffset / maxOffset)));
            dragStartYOffset = mouseY - thumbY;
            return true;
        }
        // 子组件在滚动坐标系中接收事件
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (child.isVisible() && child.isEnabled() && child.mouseClicked(mouseX, mouseY + scrollOffset, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        if (!visible || !enabled) {
            return false;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (child.isVisible() && child.isEnabled() && child.mouseReleased(mouseX, mouseY + scrollOffset, button)) {
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
        if (draggingScrollbar && contentHeight > height) {
            int scrollbarY = y + 1;
            int scrollbarHeight = height - 2;
            float ratio = (float) height / contentHeight;
            int thumbHeight = Math.max(10, (int) (scrollbarHeight * ratio));
            int trackHeight = scrollbarHeight - thumbHeight;
            int relativeY = (int) (mouseY - dragStartYOffset - scrollbarY);
            if (trackHeight > 0) {
                int maxOffset = contentHeight - height;
                scrollOffset = (int) ((double) relativeY / trackHeight * maxOffset);
                clampScrollOffset();
            }
            return true;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (child.isVisible() && child.isEnabled() && child.mouseDragged(mouseX, mouseY + scrollOffset, button, dragX, dragY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断鼠标是否位于滚动条滑块区域
     */
    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        int scrollbarX = x + width - SCROLLBAR_WIDTH - 1;
        int scrollbarY = y + 1;
        int scrollbarHeight = height - 2;
        return mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH
                && mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight;
    }

    public int getScrollbarWidth() {
        return SCROLLBAR_WIDTH;
    }

    public void setScrollbarColor(int scrollbarColor) {
        this.scrollbarColor = scrollbarColor;
    }

    public void setScrollbarBgColor(int scrollbarBgColor) {
        this.scrollbarBgColor = scrollbarBgColor;
    }
}
