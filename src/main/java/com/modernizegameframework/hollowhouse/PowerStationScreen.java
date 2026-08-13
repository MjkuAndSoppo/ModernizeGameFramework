package com.modernizegameframework.hollowhouse;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 供电站界面
 * 主面板显示预测剩余发电时间、燃油槽位与发电开关；
 * 右侧为仓库中可用燃料的滚动列表。
 */
public class PowerStationScreen extends Screen {

    /** 主面板尺寸 */
    private static final int MAIN_PANEL_WIDTH = 176;
    private static final int MAIN_PANEL_HEIGHT = 166;

    /** 右侧面板宽度 */
    private static final int RIGHT_PANEL_WIDTH = 100;
    /** 面板间隙 */
    private static final int PANEL_GAP = 4;

    /** 燃油槽位尺寸 */
    private static final int FUEL_SLOT_SIZE = 18;
    /** 右侧燃料条目高度 */
    private static final int FUEL_ENTRY_HEIGHT = 20;
    /** 右侧燃料条目图标尺寸 */
    private static final int FUEL_ICON_SIZE = 16;

    /** 滚动条宽度 */
    private static final int SCROLLBAR_WIDTH = 6;

    /** 界面整体左上角坐标 */
    private int leftPos;
    private int topPos;

    /** 主面板坐标与尺寸 */
    private int mainPanelX, mainPanelY, mainPanelW, mainPanelH;
    /** 右侧燃料面板坐标与尺寸 */
    private int fuelPanelX, fuelPanelY, fuelPanelW, fuelPanelH;

    private final int powerLevel;
    private PowerStationData powerStationData;
    private final List<ItemStack> storehouseFuelItems;

    private Button toggleButton;

    private final List<FuelSlotEntry> fuelSlotEntries = new ArrayList<>();
    private final List<FuelItemEntry> fuelItemEntries = new ArrayList<>();

    /** 当前选中的燃油槽位索引，-1 表示未选中 */
    private int selectedSlotIndex = -1;

    // ===== 右侧燃料面板滚动状态 =====
    private int fuelScrollOffset = 0;
    private int fuelContentHeight = 0;
    private boolean fuelDraggingScrollbar = false;
    private double fuelDragStartYOffset = 0;

    public PowerStationScreen(int powerLevel, PowerStationData data, List<ItemStack> storehouseFuelItems) {
        super(Component.literal("供电站"));
        this.powerLevel = powerLevel;
        this.powerStationData = data;
        this.storehouseFuelItems = new ArrayList<>(storehouseFuelItems != null ? storehouseFuelItems : Collections.emptyList());
    }

    @Override
    protected void init() {
        int totalWidth = MAIN_PANEL_WIDTH + PANEL_GAP + RIGHT_PANEL_WIDTH;
        this.leftPos = (this.width - totalWidth) / 2;
        this.topPos = (this.height - MAIN_PANEL_HEIGHT) / 2;

        // 主面板
        mainPanelX = leftPos;
        mainPanelY = topPos;
        mainPanelW = MAIN_PANEL_WIDTH;
        mainPanelH = MAIN_PANEL_HEIGHT;

        // 右侧燃料面板
        fuelPanelX = leftPos + MAIN_PANEL_WIDTH + PANEL_GAP;
        fuelPanelY = topPos;
        fuelPanelW = RIGHT_PANEL_WIDTH;
        fuelPanelH = MAIN_PANEL_HEIGHT;

        buildFuelSlotEntries();
        buildFuelItemEntries();

        // 发电开关按钮
        toggleButton = Button.builder(Component.literal("开启发电"), btn -> togglePower())
                .bounds(leftPos + (MAIN_PANEL_WIDTH - 70) / 2,
                        topPos + MAIN_PANEL_HEIGHT - 32, 70, 20)
                .build();
        this.addRenderableWidget(toggleButton);

        updateToggleButtonText();
    }

    /**
     * 构建主面板上的燃油槽位组件
     */
    private void buildFuelSlotEntries() {
        fuelSlotEntries.clear();

        int slotCount = powerLevel;
        int totalSlotWidth = slotCount * FUEL_SLOT_SIZE + (slotCount - 1) * 4;
        int startX = mainPanelX + (MAIN_PANEL_WIDTH - totalSlotWidth) / 2;
        int slotY = mainPanelY + 92;

        List<ItemStack> slots = powerStationData.getFuelSlots();
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = i < slots.size() ? slots.get(i) : ItemStack.EMPTY;
            FuelSlotEntry entry = new FuelSlotEntry(startX + i * (FUEL_SLOT_SIZE + 4), slotY,
                    FUEL_SLOT_SIZE, FUEL_SLOT_SIZE, stack, i);
            entry.setSelected(i == selectedSlotIndex);
            entry.setOnClick(index -> onFuelSlotClicked(index));
            fuelSlotEntries.add(entry);
        }
    }

    /**
     * 构建右侧仓库燃料列表
     */
    private void buildFuelItemEntries() {
        fuelItemEntries.clear();

        int entryWidth = RIGHT_PANEL_WIDTH - SCROLLBAR_WIDTH - 1;
        int contentHeight = Math.max(MAIN_PANEL_HEIGHT, storehouseFuelItems.size() * FUEL_ENTRY_HEIGHT);
        fuelContentHeight = contentHeight;
        fuelScrollOffset = 0;

        for (int i = 0; i < storehouseFuelItems.size(); i++) {
            ItemStack stack = storehouseFuelItems.get(i);
            FuelItemEntry entry = new FuelItemEntry(
                    fuelPanelX, fuelPanelY + i * FUEL_ENTRY_HEIGHT,
                    entryWidth, FUEL_ENTRY_HEIGHT, stack, i);
            entry.setOnClick(index -> onFuelItemClicked(index));
            fuelItemEntries.add(entry);
        }
    }

    /**
     * 燃油槽位被点击
     */
    private void onFuelSlotClicked(int index) {
        ItemStack current = powerStationData.getFuelSlot(index);
        if (!current.isEmpty()) {
            // 槽位已有物品，点击则取回仓库
            sendSetFuelSlot(index, ItemStack.EMPTY);
            selectedSlotIndex = -1;
        } else {
            // 空槽位则选中
            selectedSlotIndex = index;
        }
        refreshSlotSelection();
    }

    /**
     * 右侧燃料条目被点击，放入当前选中的燃油槽位
     */
    private void onFuelItemClicked(int index) {
        if (selectedSlotIndex < 0) {
            return;
        }
        if (index < 0 || index >= storehouseFuelItems.size()) {
            return;
        }
        ItemStack stack = storehouseFuelItems.get(index);
        if (stack.isEmpty() || PowerStationData.computeFuelValue(stack) <= 0) {
            return;
        }
        sendSetFuelSlot(selectedSlotIndex, stack);
        selectedSlotIndex = -1;
        refreshSlotSelection();
    }

    /**
     * 发送设置燃油槽位请求到服务端
     */
    private void sendSetFuelSlot(int slotIndex, ItemStack stack) {
        PowerStationNetwork.CHANNEL.sendToServer(new PowerStationNetwork.SetFuelSlotPacket(slotIndex, stack));
    }

    /**
     * 切换发电开关
     */
    private void togglePower() {
        PowerStationNetwork.CHANNEL.sendToServer(new PowerStationNetwork.TogglePowerPacket());
    }

    /**
     * 刷新槽位选中状态显示
     */
    private void refreshSlotSelection() {
        for (FuelSlotEntry entry : fuelSlotEntries) {
            entry.setSelected(entry.getIndex() == selectedSlotIndex);
        }
    }

    /**
     * 更新发电开关按钮文字
     */
    private void updateToggleButtonText() {
        if (toggleButton == null) {
            return;
        }
        if (powerStationData.isGenerating()) {
            toggleButton.setMessage(Component.literal("停止发电"));
        } else {
            toggleButton.setMessage(Component.literal("开启发电"));
        }
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 绘制毛玻璃背景
        graphics.fillGradient(0, 0, this.width, this.height, 0xCC0A0A0A, 0xCC0A0A0A);

        // 标题
        graphics.drawString(this.font, this.title,
                leftPos + (MAIN_PANEL_WIDTH + PANEL_GAP + RIGHT_PANEL_WIDTH) / 2
                        - this.font.width(this.title) / 2,
                topPos - 14, 0xFFFFFFFF, false);

        // 渲染主面板
        renderMainPanel(graphics, mouseX, mouseY, partialTick);

        // 渲染右侧燃料面板
        renderFuelPanel(graphics, mouseX, mouseY, partialTick);

        // 渲染主面板文本内容
        renderMainPanelText(graphics);

        // 渲染原版组件（按钮）
        super.render(graphics, mouseX, mouseY, partialTick);

        // 绘制提示框（必须在最后，确保在 UI 上方）
        renderTooltips(graphics, mouseX, mouseY);
    }

    /**
     * 渲染主面板背景、边框与燃油槽位
     */
    private void renderMainPanel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 主面板背景
        graphics.fill(mainPanelX, mainPanelY, mainPanelX + mainPanelW, mainPanelY + mainPanelH, 0xB02A2A2A);
        // 主面板边框
        graphics.renderOutline(mainPanelX, mainPanelY, mainPanelW, mainPanelH, 0xFF555555);

        // 渲染燃油槽位
        for (FuelSlotEntry entry : fuelSlotEntries) {
            entry.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * 渲染右侧燃料面板背景、边框、燃料条目（带滚动裁剪）与滚动条
     */
    private void renderFuelPanel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 燃料面板背景
        graphics.fill(fuelPanelX, fuelPanelY, fuelPanelX + fuelPanelW, fuelPanelY + fuelPanelH, 0xFF2A2A2A);
        // 燃料面板边框
        graphics.renderOutline(fuelPanelX, fuelPanelY, fuelPanelW, fuelPanelH, 0xFF555555);

        // 渲染燃料条目（带滚动裁剪）
        graphics.pose().pushPose();
        graphics.enableScissor(fuelPanelX, fuelPanelY, fuelPanelX + fuelPanelW, fuelPanelY + fuelPanelH);
        graphics.pose().translate(0, -fuelScrollOffset, 0);
        for (FuelItemEntry entry : fuelItemEntries) {
            entry.render(graphics, mouseX, mouseY, partialTick);
        }
        graphics.disableScissor();
        graphics.pose().popPose();

        // 渲染滚动条
        renderFuelScrollbar(graphics);
    }

    /**
     * 渲染右侧燃料面板的滚动条
     */
    private void renderFuelScrollbar(GuiGraphics graphics) {
        if (fuelContentHeight <= fuelPanelH) {
            return;
        }
        int scrollbarX = fuelPanelX + fuelPanelW - SCROLLBAR_WIDTH - 1;
        int scrollbarY = fuelPanelY + 1;
        int scrollbarHeight = fuelPanelH - 2;
        // 滚动条背景
        graphics.fill(scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, 0xFF222222);
        // 滚动条滑块
        float ratio = (float) fuelPanelH / fuelContentHeight;
        int thumbHeight = Math.max(10, (int) (scrollbarHeight * ratio));
        int maxOffset = fuelContentHeight - fuelPanelH;
        int thumbY = scrollbarY + (maxOffset == 0 ? 0
                : (int) ((scrollbarHeight - thumbHeight) * ((float) fuelScrollOffset / maxOffset)));
        graphics.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFF777777);
    }

    /**
     * 渲染主面板文本信息
     */
    private void renderMainPanelText(GuiGraphics graphics) {
        int x = mainPanelX + 8;
        int y = mainPanelY + 8;
        int lineHeight = 12;

        // 名称
        graphics.drawString(this.font, Component.literal("§n供电站"), x, y, 0xFFFFFFFF, false);
        y += lineHeight + 4;

        // 等级
        graphics.drawString(this.font, Component.literal("等级：§e" + powerLevel), x, y, 0xFFFFFFFF, false);
        y += lineHeight;

        // 状态
        String status = powerStationData.isGenerating() ? "§a发电中" : "§c已停止";
        graphics.drawString(this.font, Component.literal("状态：" + status), x, y, 0xFFFFFFFF, false);
        y += lineHeight;

        // 预测剩余时间
        int predictedSeconds = powerStationData.getPredictedTotalSeconds(powerLevel);
        graphics.drawString(this.font, Component.literal("预测剩余：§e" + formatTime(predictedSeconds)), x, y, 0xFFFFFFFF, false);
        y += lineHeight;

        // 发电倍率说明
        int secondsPerUnit = PowerStationData.getSecondsPerPowerUnit(powerLevel);
        graphics.drawString(this.font, Component.literal("每单位：§7" + secondsPerUnit + " 秒"), x, y, 0xFFFFFFFF, false);
        y += lineHeight + 6;

        // 燃油槽位标签
        String slotLabel = "燃油槽位 (" + powerLevel + "/" + powerLevel + ")";
        graphics.drawString(this.font, Component.literal(slotLabel),
                mainPanelX + (MAIN_PANEL_WIDTH - this.font.width(slotLabel)) / 2,
                y, 0xFFFFFFFF, false);

        // 右侧面板标题（绘制在面板上方，避免遮挡滚动内容）
        graphics.drawString(this.font, Component.literal("可用燃料"),
                fuelPanelX + 4, fuelPanelY - 12, 0xFFFFFFFF, false);
    }

    /**
     * 渲染物品提示框
     */
    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        // 燃油槽位提示
        for (FuelSlotEntry entry : fuelSlotEntries) {
            if (entry.isMouseOver(mouseX, mouseY) && !entry.getStack().isEmpty()) {
                graphics.renderTooltip(this.font, entry.getStack(), mouseX, mouseY);
                return;
            }
        }
        // 右侧燃料条目提示（考虑滚动偏移）
        if (mouseX >= fuelPanelX && mouseX < fuelPanelX + fuelPanelW
                && mouseY >= fuelPanelY && mouseY < fuelPanelY + fuelPanelH) {
            double adjustedMouseY = mouseY + fuelScrollOffset;
            for (FuelItemEntry entry : fuelItemEntries) {
                if (entry.isMouseOver(mouseX, adjustedMouseY) && !entry.getStack().isEmpty()) {
                    graphics.renderTooltip(this.font, entry.getStack(), mouseX, mouseY);
                    return;
                }
            }
        }
    }

    /**
     * 将秒数格式化为 HH:MM:SS
     */
    private String formatTime(int totalSeconds) {
        int total = Math.max(0, totalSeconds);
        int hours = total / 3600;
        int minutes = (total % 3600) / 60;
        int seconds = total % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 检查燃油槽位点击
        for (FuelSlotEntry entry : fuelSlotEntries) {
            if (entry.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        // 检查右侧燃料面板交互
        if (mouseX >= fuelPanelX && mouseX < fuelPanelX + fuelPanelW
                && mouseY >= fuelPanelY && mouseY < fuelPanelY + fuelPanelH) {
            // 检查滚动条点击
            if (fuelContentHeight > fuelPanelH) {
                int scrollbarX = fuelPanelX + fuelPanelW - SCROLLBAR_WIDTH - 1;
                int scrollbarY = fuelPanelY + 1;
                int scrollbarHeight = fuelPanelH - 2;
                if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH
                        && mouseY >= scrollbarY && mouseY < scrollbarY + scrollbarHeight) {
                    fuelDraggingScrollbar = true;
                    fuelDragStartYOffset = mouseY - getFuelScrollbarThumbY();
                    return true;
                }
            }
            // 检查燃料条目点击（考虑滚动偏移）
            double adjustedMouseY = mouseY + fuelScrollOffset;
            for (FuelItemEntry entry : fuelItemEntries) {
                if (entry.mouseClicked(mouseX, adjustedMouseY, button)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        fuelDraggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (fuelDraggingScrollbar && fuelContentHeight > fuelPanelH) {
            int scrollbarY = fuelPanelY + 1;
            int scrollbarHeight = fuelPanelH - 2;
            float ratio = (float) fuelPanelH / fuelContentHeight;
            int thumbHeight = Math.max(10, (int) (scrollbarHeight * ratio));
            int trackHeight = scrollbarHeight - thumbHeight;
            int relativeY = (int) (mouseY - fuelDragStartYOffset - scrollbarY);
            if (trackHeight > 0) {
                int maxOffset = fuelContentHeight - fuelPanelH;
                fuelScrollOffset = (int) ((double) relativeY / trackHeight * maxOffset);
                clampFuelScrollOffset();
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= fuelPanelX && mouseX < fuelPanelX + fuelPanelW
                && mouseY >= fuelPanelY && mouseY < fuelPanelY + fuelPanelH
                && fuelContentHeight > fuelPanelH) {
            fuelScrollOffset -= (int) (delta * 10);
            clampFuelScrollOffset();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /**
     * 限制燃料面板滚动偏移在有效范围内
     */
    private void clampFuelScrollOffset() {
        int maxOffset = Math.max(0, fuelContentHeight - fuelPanelH);
        if (fuelScrollOffset < 0) fuelScrollOffset = 0;
        if (fuelScrollOffset > maxOffset) fuelScrollOffset = maxOffset;
    }

    /**
     * 获取燃料面板滚动条滑块当前 Y 坐标
     */
    private int getFuelScrollbarThumbY() {
        int scrollbarY = fuelPanelY + 1;
        int scrollbarHeight = fuelPanelH - 2;
        float ratio = (float) fuelPanelH / fuelContentHeight;
        int thumbHeight = Math.max(10, (int) (scrollbarHeight * ratio));
        int maxOffset = fuelContentHeight - fuelPanelH;
        return scrollbarY + (maxOffset == 0 ? 0
                : (int) ((scrollbarHeight - thumbHeight) * ((float) fuelScrollOffset / maxOffset)));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 服务端同步供电站数据后刷新界面
     */
    public void updateData(PowerStationData data) {
        this.powerStationData = data;
        buildFuelSlotEntries();
        updateToggleButtonText();
    }

    // ===== 燃油槽位显示组件 =====

    /**
     * 燃油槽位显示组件（不继承 UI 组件，自主实现渲染与交互）
     */
    private class FuelSlotEntry {

        private final int x, y, width, height;
        private final ItemStack stack;
        private final int index;
        private boolean selected;
        private Consumer<Integer> onClick;
        private int backgroundColor = 0xFF333333;
        private int borderColor = 0xFF555555;

        public FuelSlotEntry(int x, int y, int width, int height, ItemStack stack, int index) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.stack = stack;
            this.index = index;
        }

        public ItemStack getStack() {
            return stack;
        }

        public int getIndex() {
            return index;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            this.borderColor = selected ? 0xFFFFFF00 : 0xFF555555;
        }

        public void setOnClick(Consumer<Integer> onClick) {
            this.onClick = onClick;
        }

        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // 绘制背景
            graphics.fill(x, y, x + width, y + height, backgroundColor);
            // 绘制边框
            graphics.renderOutline(x, y, width, height, borderColor);

            // 绘制物品图标
            if (!stack.isEmpty()) {
                int iconX = x + (width - 16) / 2;
                int iconY = y + (height - 16) / 2;
                graphics.renderItem(stack, iconX, iconY);
                graphics.renderItemDecorations(font, stack, iconX, iconY);
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isMouseOver(mouseX, mouseY) && onClick != null) {
                onClick.accept(index);
                return true;
            }
            return false;
        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    // ===== 右侧仓库燃料条目组件 =====

    /**
     * 右侧仓库燃料条目组件（不继承 UI 组件，自主实现渲染与交互）
     */
    private class FuelItemEntry {

        private final int x, y, width, height;
        private final ItemStack stack;
        private final int index;
        private Consumer<Integer> onClick;
        private int backgroundColor = 0xFF333333;
        private int borderColor = 0xFF555555;

        public FuelItemEntry(int x, int y, int width, int height, ItemStack stack, int index) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.stack = stack;
            this.index = index;
        }

        public ItemStack getStack() {
            return stack;
        }

        public void setOnClick(Consumer<Integer> onClick) {
            this.onClick = onClick;
        }

        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // 绘制背景
            graphics.fill(x, y, x + width, y + height, backgroundColor);
            // 绘制边框
            graphics.renderOutline(x, y, width, height, borderColor);

            // 图标
            int iconX = x + 2;
            int iconY = y + (height - FUEL_ICON_SIZE) / 2;
            graphics.renderItem(stack, iconX, iconY);
            graphics.renderItemDecorations(font, stack, iconX, iconY);

            // 名称
            String name = stack.getHoverName().getString();
            if (font.width(name) > width - FUEL_ICON_SIZE - 8) {
                name = font.plainSubstrByWidth(name, width - FUEL_ICON_SIZE - 12) + "...";
            }
            graphics.drawString(font, Component.literal(name), x + FUEL_ICON_SIZE + 4,
                    y + 2, 0xFFFFFFFF, false);

            // 数量或耐久
            String valueText;
            if (stack.isDamageableItem()) {
                valueText = "§7耐久 " + (stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage();
            } else {
                valueText = "§7数量 " + stack.getCount();
            }
            graphics.drawString(font, Component.literal(valueText), x + FUEL_ICON_SIZE + 4,
                    y + height - 10, 0xFFAAAAAA, false);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isMouseOver(mouseX, mouseY) && onClick != null) {
                onClick.accept(index);
                return true;
            }
            return false;
        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}