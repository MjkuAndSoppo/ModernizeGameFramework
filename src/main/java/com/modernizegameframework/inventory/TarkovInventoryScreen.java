package com.modernizegameframework.inventory;

import com.modernizegameframework.securecontainer.SecureContainerItem;
import com.modernizegameframework.securecontainer.SecureContainerType;
import com.modernizegameframework.ui.UIBlurBackground;
import com.modernizegameframework.ui.UIComponent;
import com.modernizegameframework.ui.UIEventScheduler;
import com.modernizegameframework.ui.UILayout;
import com.modernizegameframework.ui.UIPanel;
import com.modernizegameframework.ui.UIScrollPanel;
import com.modernizegameframework.ui.UITabBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 塔科夫三段式背包界面渲染
 * 使用自研 UI 组件库重构，包含上下边栏、毛玻璃背景、可滚动面板
 */
public class TarkovInventoryScreen extends AbstractContainerScreen<TarkovInventoryMenu> {

    private static final int BORDER_COLOR = 0xFF555555;
    private static final int LOCKED_OVERLAY = 0x99000000;

    /** 窗口 resize 防抖延迟（毫秒），避免拖拽窗口时频繁重建界面 */
    private static final long RESIZE_DEBOUNCE_MS = 150;

    private final String[] tabs = {"equipment_tab", "medical_tab", "skill_tab"};
    private final Component[] tabLabels;

    private int currentMouseX;
    private int currentMouseY;

    // 自研 UI 组件
    private final List<UIComponent> uiComponents = new ArrayList<>();
    private UITabBar tabBar;
    private UIPanel topBar;
    private UIPanel bottomBar;
    private UIPanel leftPanel;
    private UIScrollPanel middleScrollPanel;
    private UIScrollPanel rightScrollPanel;

    /** 防抖的 resize 重建动作，关闭界面时取消 pending 任务 */
    private final UIEventScheduler.DebouncedAction<int[]> debouncedResize;

    public TarkovInventoryScreen(TarkovInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.tabLabels = new Component[]{
                Component.translatable("gui.modernizegameframework.tarkov_inventory.equipment_tab"),
                Component.translatable("gui.modernizegameframework.tarkov_inventory.medical_tab"),
                Component.translatable("gui.modernizegameframework.tarkov_inventory.skill_tab")
        };
        this.debouncedResize = UIEventScheduler.debounce(
                dims -> {
                    if (this.minecraft != null) {
                        this.minecraft.execute(() -> rebuildForResize(dims[0], dims[1]));
                    }
                },
                RESIZE_DEBOUNCE_MS
        );
    }

    @Override
    protected void init() {
        // 全屏布局：界面占满整个 Minecraft 窗口，不受 UI 缩放影响
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        super.init();
        this.leftPos = 0;
        this.topPos = 0;
        rebuildUI();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        // 窗口尺寸变化时通过防抖延迟重建菜单与界面，避免拖拽窗口过程中频繁刷新
        if (width != menu.getScreenWidth() || height != menu.getScreenHeight()) {
            debouncedResize.accept(new int[]{width, height});
            return;
        }
        super.resize(minecraft, width, height);
    }

    /**
     * 在 Minecraft 主线程中执行界面重建，保证 UI 与槽位坐标同步。
     *
     * @param width  新窗口宽度
     * @param height 新窗口高度
     */
    private void rebuildForResize(int width, int height) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        // 如果当前已经不是本界面，则无需重建
        if (this.minecraft.screen != this) {
            return;
        }
        Inventory inv = this.minecraft.player.getInventory();
        TarkovInventoryMenu newMenu = new TarkovInventoryMenu(
                this.menu.containerId,
                inv,
                this.menu.getContainerSlotCount(),
                this.menu.getExternalTitle()
        );
        TarkovInventoryScreen newScreen = new TarkovInventoryScreen(newMenu, inv, this.title);
        this.minecraft.setScreen(newScreen);
    }

    @Override
    public void onClose() {
        // 关闭界面时取消 pending 的 resize 重建任务，防止重建已关闭的界面
        if (debouncedResize != null) {
            debouncedResize.cancel();
        }
        super.onClose();
    }

    /**
     * 根据当前屏幕尺寸重建 UI 组件树
     */
    private void rebuildUI() {
        uiComponents.clear();

        int margin = UILayout.scaled(UILayout.MARGIN, height);
        int topBarHeight = UILayout.scaled(UILayout.TOP_BAR_HEIGHT, height);
        // 底边栏高度使用 UILayout.bottomBar() 计算，确保符合窗口高度 8% 且限制在 40~60 像素
        int bottomBarHeight = UILayout.bottomBar(width, height).height();

        boolean hasContainer = menu.hasExternalContainer();
        UILayout.Rect leftRect = UILayout.leftPanel(width, height);
        UILayout.Rect middleRect = UILayout.middlePanel(width, height);
        UILayout.Rect rightRect = UILayout.rightPanel(width, height, hasContainer);

        // 顶部边栏（标签栏）
        topBar = new UIPanel(margin, 0, width - margin * 2, topBarHeight);
        topBar.setBackgroundColor(0xFF151515);
        topBar.setBorderColor(0);
        tabBar = new UITabBar(0, 0, width - margin * 2, topBarHeight, this.font);
        for (Component label : tabLabels) {
            tabBar.addTab(label);
        }
        tabBar.setSelectedIndex(0);
        tabBar.setOnTabClicked(index -> {
            if (index == 0) {
                return;
            }
            String key = index == 1 ? "gui.modernizegameframework.tarkov_inventory.medical_tab"
                    : "gui.modernizegameframework.tarkov_inventory.skill_tab";
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.translatable("gui.modernizegameframework.tarkov_inventory.coming_soon",
                                Component.translatable(key)), true);
            }
        });
        topBar.addChild(tabBar);

        // 底部边栏（快捷栏背景）
        bottomBar = new UIPanel(margin, height - bottomBarHeight,
                width - margin * 2, bottomBarHeight);
        bottomBar.setBackgroundColor(0xFF1A1A1A);
        bottomBar.setBorderColor(BORDER_COLOR);
        uiComponents.add(bottomBar);

        // 左侧装备区面板
        leftPanel = new UIPanel(leftRect.x(), leftRect.y(), leftRect.width(), leftRect.height());
        leftPanel.setBackgroundColor(0xFF2A2A2A);
        leftPanel.setBorderColor(BORDER_COLOR);
        uiComponents.add(leftPanel);

        // 中部主仓库滚动面板
        middleScrollPanel = new UIScrollPanel(middleRect.x(), middleRect.y(), middleRect.width(), middleRect.height());
        middleScrollPanel.setBackgroundColor(0xFF252525);
        middleScrollPanel.setBorderColor(BORDER_COLOR);
        // 根据流式布局计算出的实际内容高度设置滚动范围；若内容未超出面板高度则不显示滚动条
        int contentHeight = Math.max(middleRect.height(), menu.getMiddleContentHeight());
        middleScrollPanel.setContentHeight(contentHeight);
        uiComponents.add(middleScrollPanel);

        // 右侧容器滚动面板（仅在打开容器时显示）
        if (hasContainer) {
            rightScrollPanel = new UIScrollPanel(rightRect.x(), rightRect.y(), rightRect.width(), rightRect.height());
            rightScrollPanel.setBackgroundColor(0xFF252525);
            rightScrollPanel.setBorderColor(BORDER_COLOR);
            // 根据容器槽位实际内容高度设置滚动范围；若内容未超出面板高度则不显示滚动条
            int rightContentHeight = Math.max(rightRect.height(), menu.getRightContentHeight());
            rightScrollPanel.setContentHeight(rightContentHeight);
            uiComponents.add(rightScrollPanel);
        }

        // 顶部边栏最后加入组件列表，确保渲染顺序在最后，覆盖在背包面板等 UI 上方；
        // 同时鼠标事件从后向前遍历，顶部边栏也会优先消费点击事件。
        uiComponents.add(topBar);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
        this.renderBackground(graphics);

        // 将中部/右侧面板槽位按滚动偏移量上移，renderSlot 会对超出面板边界的槽位进行裁剪，
        // renderSlotOutlines / renderLockedOverlays / renderTooltip 也会跳过越界槽位。
        boolean shouldMiddleScroll = menu.getMiddleContentHeight() > middleScrollPanel.getHeight();
        boolean shouldRightScroll = menu.hasExternalContainer() && rightScrollPanel != null
                && menu.getRightContentHeight() > rightScrollPanel.getHeight();
        if (shouldMiddleScroll) {
            applyMiddleScrollOffset(true);
        }
        if (shouldRightScroll) {
            applyRightScrollOffset(true);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderSlotOutlines(graphics);
        this.renderLockedOverlays(graphics);
        // 原版 renderSlotHighlight 为 static 无法重写，在其上方补绘正确尺寸的高亮框
        this.renderCustomSlotHighlight(graphics);
        // 提示框依赖槽位坐标，必须在恢复偏移前渲染，否则会错位
        this.renderTooltip(graphics, mouseX, mouseY);

        if (shouldMiddleScroll) {
            applyMiddleScrollOffset(false);
        }
        if (shouldRightScroll) {
            applyRightScrollOffset(false);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 渲染各 UI 组件（背景面板、标签栏、底部边栏等）
        for (UIComponent component : uiComponents) {
            if (component.isVisible()) {
                component.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        // 容器标题（仅打开容器时显示在右侧面板顶部）
        if (menu.hasExternalContainer() && rightScrollPanel != null) {
            Component containerTitle = menu.getExternalTitle();
            if (containerTitle != null && !containerTitle.getString().isEmpty()) {
                graphics.drawString(this.font, containerTitle, rightScrollPanel.getX() + 4,
                        rightScrollPanel.getY() + 4, 0xFFFFFFFF, false);
            }
        }

        // 左侧 3D 玩家模型与相关信息按左侧面板尺寸自适应缩放
        UILayout.Rect leftRect = menu.getLeftPanelRect();
        int leftSlotSize = menu.getLeftSlotSize();
        int leftSlotGap = menu.getLeftSlotGap();
        int leftColX = menu.getLeftColX();
        int rightColX = menu.getRightColX();

        // 顶部护甲/装备区共 4 行，小人位于两列之间并与槽位区下半部分对齐
        int modelX = leftRect.x() + leftRect.width() / 2;
        int modelY = menu.getLeftModelCenterY();
        int modelSize = menu.getLeftModelSize();
        renderPlayerModel(graphics, modelX, modelY, modelSize);

        // 玩家昵称显示在小人正上方居中
        int nameY = menu.getLeftNameY();
        renderPlayerName(graphics, modelX, nameY);

        // 经验等级显示在小人正下方居中
        int levelY = menu.getLeftLevelY();
        renderPlayerLevel(graphics, modelX, levelY);



        // 底部 2×2 合成区（左下角），结果槽在材料右侧
        int craftBottom = menu.getLeftCraftBottomY();
        int craftTopRow = craftBottom - 2 * leftSlotSize - leftSlotGap;
        int craftBottomRow = craftBottom - leftSlotSize;
        int resultX = Math.min(rightColX, leftColX + 2 * (leftSlotSize + leftSlotGap));
        int craftingRight = resultX + leftSlotSize;
        int craftingCenterY = craftTopRow + (2 * leftSlotSize + leftSlotGap) / 2;

        // 合成栏标签位于合成格右侧
        if (craftingRight + leftSlotGap + this.font.width("格子（合成栏）") <= leftRect.x() + leftRect.width()) {
            graphics.drawString(this.font, Component.literal("格子（合成栏）"),
                    craftingRight + leftSlotGap,
                    craftingCenterY - this.font.lineHeight / 2,
                    0xFFAAAAAA, false);
        }

        // 底部状态文本占位（血量 / 饥饿 / 口渴 / 重量）
        int statusLineHeight = menu.getLeftStatusLineHeight();
        int statusGap = menu.getLeftStatusGap();
        int statusStartY = craftBottom + statusGap;
        renderStatusPlaceholders(graphics, leftColX, statusStartY, statusLineHeight);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // 标题不额外渲染，已在背景中处理
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics) {
        // 绘制灰色半透明毛玻璃背景
        UIBlurBackground.render(graphics, width, height);
    }

    /**
     * 绘制安全箱类型标签
     */
    private void renderSecureCaseLabel(GuiGraphics graphics) {
        ItemStack secureCase = menu.getSecureCase();
        if (secureCase.isEmpty()) {
            return;
        }
        SecureContainerType type = null;
        if (secureCase.getItem() instanceof SecureContainerItem sci) {
            type = sci.getType();
        }
        Component label = type != null
                ? Component.translatable("item.modernizegameframework.secure_container." + type.getName())
                : Component.literal("安全箱");
        int slotStep = UILayout.slotStep(height);
        int sectionGap = UILayout.scaled(UILayout.SECTION_GAP, height);
        int labelWidth = this.font.width(label);
        int labelX = middleScrollPanel.getX() + (middleScrollPanel.getWidth() - labelWidth) / 2;
        int labelY = middleScrollPanel.getY() + UILayout.scaled(34, height)
                + 3 * slotStep + sectionGap + 3 * slotStep - UILayout.scaled(12, height);
        graphics.drawString(this.font, label, labelX, labelY, 0xFFFFFFFF, false);
    }

    /**
     * 判断指定槽位是否属于中部面板（需要根据滚动偏移进行裁剪）
     */
    private boolean isMiddlePanelSlot(Slot slot) {
        int index = menu.slots.indexOf(slot);
        return index >= menu.getMiddlePanelSlotStart() && index < menu.getMiddlePanelSlotEnd();
    }

    /**
     * 判断指定槽位是否属于右侧面板容器区（需要根据滚动偏移进行裁剪）
     */
    private boolean isRightPanelSlot(Slot slot) {
        if (!menu.hasExternalContainer() || rightScrollPanel == null) {
            return false;
        }
        int index = menu.slots.indexOf(slot);
        return index >= menu.getRightPanelSlotStart() && index < menu.getRightPanelSlotEnd();
    }

    /**
     * 判断槽位在屏幕上的矩形是否与中部面板有交集，用于滚动裁剪。
     */
    private boolean isSlotVisibleInMiddlePanel(Slot slot) {
        int slotSize = menu.getMiddleSlotSize();
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        int panelX = middleScrollPanel.getX();
        int panelY = middleScrollPanel.getY();
        int panelRight = panelX + middleScrollPanel.getWidth();
        int panelBottom = panelY + middleScrollPanel.getHeight();
        return x < panelRight && x + slotSize > panelX && y < panelBottom && y + slotSize > panelY;
    }

    /**
     * 判断槽位在屏幕上的矩形是否与右侧面板有交集，用于滚动裁剪。
     */
    private boolean isSlotVisibleInRightPanel(Slot slot) {
        int slotSize = menu.getContainerSlotSize();
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        int panelX = rightScrollPanel.getX();
        int panelY = rightScrollPanel.getY();
        int panelRight = panelX + rightScrollPanel.getWidth();
        int panelBottom = panelY + rightScrollPanel.getHeight();
        return x < panelRight && x + slotSize > panelX && y < panelBottom && y + slotSize > panelY;
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (isMiddlePanelSlot(slot) && !isSlotVisibleInMiddlePanel(slot)) {
            return;
        }
        if (isRightPanelSlot(slot) && !isSlotVisibleInRightPanel(slot)) {
            return;
        }
        int visualSize = getSlotVisualSize(slot);
        if (visualSize > 16) {
            // 对放大后的槽位应用等比缩放，使物品图标与槽位同步放大
            float scale = visualSize / 16.0f;
            graphics.pose().pushPose();
            graphics.pose().translate(slot.x, slot.y, 0);
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.pose().translate(-slot.x, -slot.y, 0);
            super.renderSlot(graphics, slot);
            graphics.pose().popPose();
        } else {
            super.renderSlot(graphics, slot);
        }
    }

    /**
     * 返回槽位在实际渲染中使用的尺寸。
     * 中部面板、快捷栏、容器槽、左侧面板槽会根据分辨率缩放；其他槽保持 16。
     */
    private int getSlotVisualSize(Slot slot) {
        if (isMiddlePanelSlot(slot)) {
            return menu.getMiddleSlotSize();
        }
        if (menu.isHotbarSlot(slot)) {
            return menu.getHotbarSlotSize();
        }
        if (menu.isContainerSlot(slot)) {
            return menu.getContainerSlotSize();
        }
        if (menu.isLeftPanelSlot(slot)) {
            return menu.getLeftSlotSize();
        }
        return menu.getSlotSize();
    }

    /**
     * 重写命中测试，使放大后的槽位整体区域都能响应鼠标悬停与点击。
     * 原版 isHovering(int,int,int,int,double,double) 为 private，这里直接实现相同的矩形判断。
     */
    @Override
    protected boolean isHovering(Slot slot, double mouseX, double mouseY) {
        int size = getSlotVisualSize(slot);
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    /**
     * 在原版高亮框之上再绘制一层正确尺寸的高亮框。
     * 1.20.1 中 AbstractContainerScreen.renderSlotHighlight 为 private static，无法重写，
     * 因此 super.render() 会先画出 16×16 的高亮框，我们再按其放大后的实际尺寸覆盖绘制。
     */
    private void renderCustomSlotHighlight(GuiGraphics graphics) {
        if (this.hoveredSlot == null || !this.hoveredSlot.isActive()) {
            return;
        }
        Slot slot = this.hoveredSlot;
        if (isMiddlePanelSlot(slot) && !isSlotVisibleInMiddlePanel(slot)) {
            return;
        }
        if (isRightPanelSlot(slot) && !isSlotVisibleInRightPanel(slot)) {
            return;
        }
        int size = getSlotVisualSize(slot);
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        // 1.20.1 原版高亮框使用 HOVER_ITEM_BLIT_OFFSET（200），在其之上绘制即可覆盖
        int z = 200;
        graphics.fillGradient(x, y, x + size, y + size, -2130706433, -2130706433, z);
    }

    /**
     * 绘制每个物品槽的灰色边框线
     */
    private void renderSlotOutlines(GuiGraphics graphics) {
        for (Slot slot : menu.slots) {
            if (isMiddlePanelSlot(slot) && !isSlotVisibleInMiddlePanel(slot)) {
                continue;
            }
            if (isRightPanelSlot(slot) && !isSlotVisibleInRightPanel(slot)) {
                continue;
            }
            int slotSize = getSlotVisualSize(slot);
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            graphics.renderOutline(x, y, slotSize, slotSize, BORDER_COLOR);
        }
    }

    /**
     * 绘制锁定格半透明遮罩
     */
    private void renderLockedOverlays(GuiGraphics graphics) {
        for (Slot slot : menu.slots) {
            if (isMiddlePanelSlot(slot) && !isSlotVisibleInMiddlePanel(slot)) {
                continue;
            }
            if (isRightPanelSlot(slot) && !isSlotVisibleInRightPanel(slot)) {
                continue;
            }
            if (menu.isLockedSlot(slot)) {
                int slotSize = getSlotVisualSize(slot);
                int x = this.leftPos + slot.x;
                int y = this.topPos + slot.y;
                graphics.fill(x, y, x + slotSize, y + slotSize, LOCKED_OVERLAY);
                graphics.drawCenteredString(this.font, "X", x + slotSize / 2, y + slotSize / 2 - 4, 0xFF888888);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // UI 组件优先消费点击事件
        for (int i = uiComponents.size() - 1; i >= 0; i--) {
            UIComponent component = uiComponents.get(i);
            if (component.isVisible() && component.isEnabled() && component.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        // Alt+点击：移到装备区
        if (hasAltDown() && hoveredSlot != null) {
            if (!menu.isEquipmentSlot(hoveredSlot)) {
                int slotIndex = menu.slots.indexOf(hoveredSlot);
                TarkovInventoryNetwork.CHANNEL.sendToServer(new TarkovInventoryNetwork.QuickMovePacket(slotIndex, 0));
                return true;
            }
        }

        // Ctrl+点击：移到容器区
        if (hasControlDown() && hoveredSlot != null) {
            if (!menu.isContainerSlot(hoveredSlot)) {
                int slotIndex = menu.slots.indexOf(hoveredSlot);
                TarkovInventoryNetwork.CHANNEL.sendToServer(new TarkovInventoryNetwork.QuickMovePacket(slotIndex, 1));
                return true;
            }
        }

        // 中部面板内点击需要临时应用滚动偏移，才能正确命中已滚动的槽位
        if (isMouseOverMiddlePanel(mouseX, mouseY)) {
            return dispatchMouseEventToMiddlePanel(mouseX, mouseY, button,
                    (mx, my) -> super.mouseClicked(mx, my, button));
        }

        // 右侧面板内点击同样需要临时应用滚动偏移
        if (isMouseOverRightPanel(mouseX, mouseY)) {
            return dispatchMouseEventToRightPanel(mouseX, mouseY, button,
                    (mx, my) -> super.mouseClicked(mx, my, button));
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (int i = uiComponents.size() - 1; i >= 0; i--) {
            UIComponent component = uiComponents.get(i);
            if (component.isVisible() && component.isEnabled() && component.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (isMouseOverMiddlePanel(mouseX, mouseY)) {
            return dispatchMouseEventToMiddlePanel(mouseX, mouseY, button,
                    (mx, my) -> super.mouseReleased(mx, my, button));
        }
        if (isMouseOverRightPanel(mouseX, mouseY)) {
            return dispatchMouseEventToRightPanel(mouseX, mouseY, button,
                    (mx, my) -> super.mouseReleased(mx, my, button));
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        for (int i = uiComponents.size() - 1; i >= 0; i--) {
            UIComponent component = uiComponents.get(i);
            if (component.isVisible() && component.isEnabled() && component.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        if (isMouseOverMiddlePanel(mouseX, mouseY)) {
            return dispatchMouseEventToMiddlePanel(mouseX, mouseY, button,
                    (mx, my) -> super.mouseDragged(mx, my, button, dragX, dragY));
        }
        if (isMouseOverRightPanel(mouseX, mouseY)) {
            return dispatchMouseEventToRightPanel(mouseX, mouseY, button,
                    (mx, my) -> super.mouseDragged(mx, my, button, dragX, dragY));
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        for (int i = uiComponents.size() - 1; i >= 0; i--) {
            UIComponent component = uiComponents.get(i);
            if (component.isVisible() && component.isEnabled() && component.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 拦截原版背包键（E）已被 GuiOpenEvent 处理，这里不做额外处理
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 判断鼠标是否位于中部滚动面板区域内（用于滚动事件消费与点击坐标转换）
     */
    private boolean isMouseOverMiddlePanel(double mouseX, double mouseY) {
        return mouseX >= middleScrollPanel.getX()
                && mouseX < middleScrollPanel.getX() + middleScrollPanel.getWidth()
                && mouseY >= middleScrollPanel.getY()
                && mouseY < middleScrollPanel.getY() + middleScrollPanel.getHeight();
    }

    /**
     * 对中部面板内的所有槽位应用或恢复滚动偏移。
     * apply 为 true 时将槽位 Y 坐标上移 scrollOffset，用于渲染与交互命中测试；
     * apply 为 false 时恢复到原始基准位置。
     */
    private void applyMiddleScrollOffset(boolean apply) {
        int offset = apply ? middleScrollPanel.getScrollOffset() : -middleScrollPanel.getScrollOffset();
        int start = menu.getMiddlePanelSlotStart();
        int end = menu.getMiddlePanelSlotEnd();
        for (int i = start; i < end; i++) {
            Slot slot = menu.slots.get(i);
            slot.y -= offset;
        }
    }

    /**
     * 在鼠标位于中部面板时，临时应用滚动偏移后调用父类事件处理，随后恢复槽位位置。
     */
    private boolean dispatchMouseEventToMiddlePanel(double mouseX, double mouseY, int button,
                                                     java.util.function.BiFunction<Double, Double, Boolean> handler) {
        applyMiddleScrollOffset(true);
        boolean result = handler.apply(mouseX, mouseY);
        applyMiddleScrollOffset(false);
        return result;
    }

    /**
     * 判断鼠标是否位于右侧滚动面板区域内（用于滚动事件消费与点击坐标转换）
     */
    private boolean isMouseOverRightPanel(double mouseX, double mouseY) {
        if (rightScrollPanel == null) {
            return false;
        }
        return mouseX >= rightScrollPanel.getX()
                && mouseX < rightScrollPanel.getX() + rightScrollPanel.getWidth()
                && mouseY >= rightScrollPanel.getY()
                && mouseY < rightScrollPanel.getY() + rightScrollPanel.getHeight();
    }

    /**
     * 对右侧面板内的所有容器槽位应用或恢复滚动偏移。
     */
    private void applyRightScrollOffset(boolean apply) {
        if (rightScrollPanel == null) {
            return;
        }
        int offset = apply ? rightScrollPanel.getScrollOffset() : -rightScrollPanel.getScrollOffset();
        int start = menu.getRightPanelSlotStart();
        int end = menu.getRightPanelSlotEnd();
        for (int i = start; i < end; i++) {
            Slot slot = menu.slots.get(i);
            slot.y -= offset;
        }
    }

    /**
     * 在鼠标位于右侧面板时，临时应用滚动偏移后调用父类事件处理，随后恢复槽位位置。
     */
    private boolean dispatchMouseEventToRightPanel(double mouseX, double mouseY, int button,
                                                    java.util.function.BiFunction<Double, Double, Boolean> handler) {
        applyRightScrollOffset(true);
        boolean result = handler.apply(mouseX, mouseY);
        applyRightScrollOffset(false);
        return result;
    }

    /**
     * 绘制左侧 3D 玩家模型
     */
    private void renderPlayerModel(GuiGraphics graphics, int centerX, int centerY, int scale) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        LivingEntity entity = this.minecraft.player;
        float lookX = (float) (centerX - currentMouseX);
        float lookY = (float) (centerY - 50 - currentMouseY);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, centerX, centerY, scale, lookX, lookY, entity);
    }

    /**
     * 绘制玩家昵称，水平居中显示在中心点上方。
     */
    private void renderPlayerName(GuiGraphics graphics, int centerX, int y) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        Component name = this.minecraft.player.getDisplayName();
        if (name == null) {
            name = Component.literal(this.minecraft.player.getScoreboardName());
        }
        int nameWidth = this.font.width(name);
        int nameX = centerX - nameWidth / 2;
        graphics.drawString(this.font, name, nameX, y, 0xFFFFFFFF, true);
    }

    /**
     * 绘制玩家经验等级，水平居中显示在中心点下方。
     */
    private void renderPlayerLevel(GuiGraphics graphics, int centerX, int y) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        Component level = Component.literal("Lv." + this.minecraft.player.experienceLevel);
        int levelWidth = this.font.width(level);
        int levelX = centerX - levelWidth / 2;
        graphics.drawString(this.font, level, levelX, y, 0xFFFFFF00, true);
    }

    /**
     * 绘制左下角状态条占位：第一行显示血量 / 饥饿 / 口渴，第二行显示重量。
     */
    private void renderStatusPlaceholders(GuiGraphics graphics, int x, int y, int lineHeight) {
        String[] keys = {"hp", "hunger", "thirst", "weight"};
        Component[] labels = new Component[keys.length];
        int[] widths = new int[keys.length];
        for (int i = 0; i < keys.length; i++) {
            labels[i] = Component.translatable("gui.modernizegameframework.tarkov_inventory.status." + keys[i])
                    .append(Component.literal(": --"));
            widths[i] = this.font.width(labels[i]);
        }

        int gap = 8;
        int row1Y = y;
        int row2Y = y + lineHeight + 2;

        int cursorX = x;
        for (int i = 0; i < 3; i++) {
            graphics.drawString(this.font, labels[i], cursorX, row1Y, 0xFFAAAAAA, false);
            cursorX += widths[i] + gap;
        }

        graphics.drawString(this.font, labels[3], x, row2Y, 0xFFAAAAAA, false);
    }
}
