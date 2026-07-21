package com.modernizegameframework.inventory;

import com.modernizegameframework.securecontainer.SecureContainerItem;
import com.modernizegameframework.securecontainer.SecureContainerType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 塔科夫三段式背包界面渲染
 * 包含顶部标签页、左侧装备区、中段主仓库/扩展格、右侧容器区
 */
public class TarkovInventoryScreen extends AbstractContainerScreen<TarkovInventoryMenu> {

    // 布局常量（与 TarkovInventoryMenu 保持一致）
    private static final int SLOT_SIZE = 16;
    private static final int SLOT_GAP = 1;
    private static final int SECTION_PADDING = 8;
    private static final int LEFT_WIDTH = 90;
    private static final int MIDDLE_WIDTH = 152;
    private static final int RIGHT_WIDTH = 90;

    private static final int BACKGROUND_COLOR = 0xFF1E1E1E;
    private static final int PANEL_COLOR = 0xFF2A2A2A;
    private static final int BORDER_COLOR = 0xFF555555;
    private static final int LOCKED_OVERLAY = 0x99000000;
    private static final int TAB_ACTIVE_COLOR = 0xFF4A90D9;
    private static final int TAB_INACTIVE_COLOR = 0xFF3A3A3A;
    private static final int TAB_TEXT_COLOR = 0xFFFFFFFF;

    private static final int TAB_WIDTH = 60;
    private static final int TAB_HEIGHT = 20;

    private final String[] tabs = {"equipment_tab", "medical_tab", "skill_tab"};
    private final Component[] tabLabels;

    private int currentMouseX;
    private int currentMouseY;

    public TarkovInventoryScreen(TarkovInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.tabLabels = new Component[] {
                Component.translatable("gui.modernizegameframework.tarkov_inventory.equipment_tab"),
                Component.translatable("gui.modernizegameframework.tarkov_inventory.medical_tab"),
                Component.translatable("gui.modernizegameframework.tarkov_inventory.skill_tab")
        };
    }

    @Override
    protected void init() {
        // 全屏布局：界面占满整个 Minecraft 窗口，不受 UI 缩放影响
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        super.init();
        this.leftPos = 0;
        this.topPos = 0;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderSlotOutlines(graphics);
        this.renderLockedOverlays(graphics);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // 整体背景
        graphics.fill(x, y, x + imageWidth, y + imageHeight, BACKGROUND_COLOR);

        // 左侧装备区面板
        graphics.fill(x + 4, y + 28, x + LEFT_WIDTH, y + imageHeight - 8, PANEL_COLOR);
        graphics.renderOutline(x + 4, y + 28, LEFT_WIDTH - 4, imageHeight - 36, BORDER_COLOR);

        // 中段主仓库+扩展格面板
        int middleX = x + LEFT_WIDTH + SECTION_PADDING;
        graphics.fill(middleX - SECTION_PADDING, y + 28, middleX + MIDDLE_WIDTH + SECTION_PADDING, y + imageHeight - 8, PANEL_COLOR);
        graphics.renderOutline(middleX - SECTION_PADDING, y + 28, MIDDLE_WIDTH + SECTION_PADDING * 2, imageHeight - 36, BORDER_COLOR);

        // 右侧容器区面板（始终保留占位，无容器时显示空背景）
        int containerX = middleX + MIDDLE_WIDTH + SECTION_PADDING * 2;
        graphics.fill(containerX - SECTION_PADDING, y + 28, x + imageWidth - 4, y + imageHeight - 8, PANEL_COLOR);
        graphics.renderOutline(containerX - SECTION_PADDING, y + 28, x + imageWidth - 4 - (containerX - SECTION_PADDING), imageHeight - 36, BORDER_COLOR);

        // 容器标题（仅打开容器时显示）
        if (menu.hasExternalContainer()) {
            Component containerTitle = menu.getExternalTitle();
            if (containerTitle != null && !containerTitle.getString().isEmpty()) {
                graphics.drawString(this.font, containerTitle, containerX, y + 14, 0xFFFFFFFF, false);
            }
        }

        // 标签页背景条
        graphics.fill(x + 4, y + 4, x + imageWidth - 4, y + 24, 0xFF151515);

        // 绘制标签页
        for (int i = 0; i < tabs.length; i++) {
            int tabX = x + 8 + i * (TAB_WIDTH + 4);
            int tabY = y + 4;
            int color = i == 0 ? TAB_ACTIVE_COLOR : TAB_INACTIVE_COLOR;
            graphics.fill(tabX, tabY, tabX + TAB_WIDTH, tabY + TAB_HEIGHT, color);
            graphics.renderOutline(tabX, tabY, TAB_WIDTH, TAB_HEIGHT, BORDER_COLOR);

            int textWidth = this.font.width(tabLabels[i]);
            int textX = tabX + (TAB_WIDTH - textWidth) / 2;
            int textY = tabY + (TAB_HEIGHT - 8) / 2;
            graphics.drawString(this.font, tabLabels[i], textX, textY, TAB_TEXT_COLOR, false);
        }

        // 左侧 3D 玩家模型（居中偏上）
        renderPlayerModel(graphics, x + 50, y + 105, 28);

        // 玩家昵称显示在小人头上，经验等级显示在小人正下方
        renderPlayerNameAndLevel(graphics, x + 50, y + 40, y + 115);

        // 安全箱标签（居中显示在安全箱格子区域上方）
        ItemStack secureCase = menu.getSecureCase();
        if (!secureCase.isEmpty()) {
            SecureContainerType type = null;
            if (secureCase.getItem() instanceof SecureContainerItem sci) {
                type = sci.getType();
            }
            Component label;
            if (type != null) {
                label = Component.translatable(
                        "item.modernizegameframework.secure_container." + type.getName());
            } else {
                label = Component.literal("安全箱");
            }
            // 安全箱起始 Y 与 menu 保持一致
            int secureStartY = y + 34 + 3 * (SLOT_SIZE + SLOT_GAP) + 8 + 3 * (SLOT_SIZE + SLOT_GAP);
            int secureLabelX = x + LEFT_WIDTH + SECTION_PADDING;
            int labelWidth = this.font.width(label);
            graphics.drawString(this.font, label,
                    secureLabelX + (MIDDLE_WIDTH - labelWidth) / 2,
                    secureStartY - 12, 0xFFFFFFFF, false);
        }

        // 状态条占位
        renderStatusPlaceholders(graphics, x + 10, y + imageHeight - 55);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // 标题不额外渲染，已在背景中处理
    }

    /**
     * 绘制每个物品槽的灰色边框线
     */
    private void renderSlotOutlines(GuiGraphics graphics) {
        for (Slot slot : menu.slots) {
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            graphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, BORDER_COLOR);
        }
    }

    /**
     * 绘制锁定格半透明遮罩
     */
    private void renderLockedOverlays(GuiGraphics graphics) {
        for (Slot slot : menu.slots) {
            if (menu.isLockedSlot(slot)) {
                int x = this.leftPos + slot.x;
                int y = this.topPos + slot.y;
                graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, LOCKED_OVERLAY);
                graphics.drawCenteredString(this.font, "X", x + SLOT_SIZE / 2, y + SLOT_SIZE / 2 - 4, 0xFF888888);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 标签页点击检测
        if (handleTabClick(mouseX, mouseY)) {
            return true;
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

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 拦截原版背包键（E）已被 GuiOpenEvent 处理，这里不做额外处理
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 处理顶部标签页点击
     */
    private boolean handleTabClick(double mouseX, double mouseY) {
        if (mouseY < this.topPos + 4 || mouseY > this.topPos + 24) {
            return false;
        }
        for (int i = 0; i < tabs.length; i++) {
            int tabX = this.leftPos + 8 + i * (TAB_WIDTH + 4);
            if (mouseX >= tabX && mouseX <= tabX + TAB_WIDTH) {
                if (i == 0) {
                    // 装备标签已在当前页，无需操作
                    return true;
                }
                // 医疗/技能标签弹出占位提示
                String key = i == 1 ? "gui.modernizegameframework.tarkov_inventory.medical_tab"
                        : "gui.modernizegameframework.tarkov_inventory.skill_tab";
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("gui.modernizegameframework.tarkov_inventory.coming_soon",
                                Component.translatable(key)), true);
                return true;
            }
        }
        return false;
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
     * 绘制玩家昵称和经验等级
     *
     * @param nameY  昵称显示高度（小人头上）
     * @param levelY 经验等级显示高度（小人正下方）
     */
    private void renderPlayerNameAndLevel(GuiGraphics graphics, int centerX, int nameY, int levelY) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        Component name = this.minecraft.player.getDisplayName();
        if (name == null) {
            name = Component.literal(this.minecraft.player.getScoreboardName());
        }
        Component level = Component.literal("Lv." + this.minecraft.player.experienceLevel);

        int nameWidth = this.font.width(name);
        int levelWidth = this.font.width(level);

        graphics.drawString(this.font, name, centerX - nameWidth / 2, nameY, 0xFFFFFFFF, true);
        graphics.drawString(this.font, level, centerX - levelWidth / 2, levelY, 0xFFFFFF00, true);
    }

    /**
     * 绘制左下角状态条占位
     */
    private void renderStatusPlaceholders(GuiGraphics graphics, int x, int y) {
        String[] keys = {"hp", "hunger", "thirst", "weight"};
        for (int i = 0; i < keys.length; i++) {
            Component label = Component.translatable("gui.modernizegameframework.tarkov_inventory.status." + keys[i]);
            Component value = Component.literal(": --");
            graphics.drawString(this.font, label.copy().append(value), x, y + i * 12, 0xFFAAAAAA, false);
        }
    }
}
