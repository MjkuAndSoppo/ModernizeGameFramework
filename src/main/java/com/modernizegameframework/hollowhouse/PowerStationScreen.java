package com.modernizegameframework.hollowhouse;

import com.modernizegameframework.ui.UIBlurBackground;
import com.modernizegameframework.ui.UIPanel;
import com.modernizegameframework.ui.UIScrollPanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    /** 界面整体左上角坐标 */
    private int leftPos;
    private int topPos;

    private final int powerLevel;
    private PowerStationData powerStationData;
    private final List<ItemStack> storehouseFuelItems;

    private UIPanel mainPanel;
    private UIScrollPanel fuelInventoryPanel;
    private Button toggleButton;

    private final List<FuelSlotEntry> fuelSlotEntries = new ArrayList<>();
    private final List<FuelItemEntry> fuelItemEntries = new ArrayList<>();

    /** 当前选中的燃油槽位索引，-1 表示未选中 */
    private int selectedSlotIndex = -1;

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
        mainPanel = new UIPanel(leftPos, topPos, MAIN_PANEL_WIDTH, MAIN_PANEL_HEIGHT);
        mainPanel.setBackgroundColor(0xB02A2A2A);
        mainPanel.setBorderColor(0xFF555555);

        // 右侧燃料列表面板（可滚动）
        fuelInventoryPanel = new UIScrollPanel(leftPos + MAIN_PANEL_WIDTH + PANEL_GAP, topPos,
                RIGHT_PANEL_WIDTH, MAIN_PANEL_HEIGHT);
        fuelInventoryPanel.setBackgroundColor(0xFF2A2A2A);
        fuelInventoryPanel.setBorderColor(0xFF555555);
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
        mainPanel.clearChildren();

        int slotCount = powerLevel;
        int totalSlotWidth = slotCount * FUEL_SLOT_SIZE + (slotCount - 1) * 4;
        int startX = mainPanel.getX() + (MAIN_PANEL_WIDTH - totalSlotWidth) / 2;
        int slotY = mainPanel.getY() + 92;

        List<ItemStack> slots = powerStationData.getFuelSlots();
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = i < slots.size() ? slots.get(i) : ItemStack.EMPTY;
            FuelSlotEntry entry = new FuelSlotEntry(startX + i * (FUEL_SLOT_SIZE + 4), slotY,
                    FUEL_SLOT_SIZE, FUEL_SLOT_SIZE, stack, i);
            entry.setSelected(i == selectedSlotIndex);
            entry.setOnClick(index -> onFuelSlotClicked(index));
            mainPanel.addChild(entry);
            fuelSlotEntries.add(entry);
        }
    }

    /**
     * 构建右侧仓库燃料列表
     */
    private void buildFuelItemEntries() {
        fuelItemEntries.clear();
        fuelInventoryPanel.clearChildren();

        int entryWidth = RIGHT_PANEL_WIDTH - UIScrollPanel.SCROLLBAR_WIDTH - 1;
        int contentHeight = Math.max(MAIN_PANEL_HEIGHT, storehouseFuelItems.size() * FUEL_ENTRY_HEIGHT);
        fuelInventoryPanel.setContentHeight(contentHeight);

        for (int i = 0; i < storehouseFuelItems.size(); i++) {
            ItemStack stack = storehouseFuelItems.get(i);
            FuelItemEntry entry = new FuelItemEntry(
                    fuelInventoryPanel.getX(), fuelInventoryPanel.getY() + i * FUEL_ENTRY_HEIGHT,
                    entryWidth, FUEL_ENTRY_HEIGHT, stack, i);
            entry.setOnClick(index -> onFuelItemClicked(index));
            fuelInventoryPanel.addChild(entry);
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
        UIBlurBackground.render(graphics, this.width, this.height, UIBlurBackground.LIGHT_OVERLAY);

        // 标题
        graphics.drawString(this.font, this.title,
                leftPos + (MAIN_PANEL_WIDTH + PANEL_GAP + RIGHT_PANEL_WIDTH) / 2
                        - this.font.width(this.title) / 2,
                topPos - 14, 0xFFFFFFFF, false);

        // 渲染主面板与右侧滚动面板
        mainPanel.render(graphics, mouseX, mouseY, partialTick);
        fuelInventoryPanel.render(graphics, mouseX, mouseY, partialTick);

        // 渲染主面板文本内容
        renderMainPanelText(graphics);

        // 渲染原版组件（按钮）
        super.render(graphics, mouseX, mouseY, partialTick);

        // 绘制提示框（必须在最后，确保在 UI 上方）
        renderTooltips(graphics, mouseX, mouseY);
    }

    /**
     * 渲染主面板文本信息
     */
    private void renderMainPanelText(GuiGraphics graphics) {
        int x = mainPanel.getX() + 8;
        int y = mainPanel.getY() + 8;
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
                mainPanel.getX() + (MAIN_PANEL_WIDTH - this.font.width(slotLabel)) / 2,
                y, 0xFFFFFFFF, false);

        // 右侧面板标题（绘制在面板上方，避免遮挡滚动内容）
        graphics.drawString(this.font, Component.literal("可用燃料"),
                fuelInventoryPanel.getX() + 4, fuelInventoryPanel.getY() - 12, 0xFFFFFFFF, false);
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
        // 右侧燃料条目提示
        for (FuelItemEntry entry : fuelItemEntries) {
            if (entry.isMouseOver(mouseX, mouseY) && !entry.getStack().isEmpty()) {
                graphics.renderTooltip(this.font, entry.getStack(), mouseX, mouseY);
                return;
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
        if (mainPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (fuelInventoryPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mainPanel.mouseReleased(mouseX, mouseY, button);
        fuelInventoryPanel.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        mainPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        fuelInventoryPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (fuelInventoryPanel.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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

    /**
     * 燃油槽位显示组件
     */
    private class FuelSlotEntry extends UIPanel {

        private final ItemStack stack;
        private final int index;
        private boolean selected;
        private java.util.function.Consumer<Integer> onClick;

        public FuelSlotEntry(int x, int y, int width, int height, ItemStack stack, int index) {
            super(x, y, width, height);
            this.stack = stack;
            this.index = index;
            setBackgroundColor(0xFF333333);
            setBorderColor(0xFF555555);
        }

        public ItemStack getStack() {
            return stack;
        }

        public int getIndex() {
            return index;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            setBorderColor(selected ? 0xFFFFFF00 : 0xFF555555);
        }

        public void setOnClick(java.util.function.Consumer<Integer> onClick) {
            this.onClick = onClick;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);
            if (!stack.isEmpty()) {
                int iconX = x + (width - 16) / 2;
                int iconY = y + (height - 16) / 2;
                graphics.renderItem(stack, iconX, iconY);
                graphics.renderItemDecorations(font, stack, iconX, iconY);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isMouseOver(mouseX, mouseY) && onClick != null) {
                onClick.accept(index);
                return true;
            }
            return false;
        }
    }

    /**
     * 右侧仓库燃料条目组件
     */
    private class FuelItemEntry extends UIPanel {

        private final ItemStack stack;
        private final int index;
        private java.util.function.Consumer<Integer> onClick;

        public FuelItemEntry(int x, int y, int width, int height, ItemStack stack, int index) {
            super(x, y, width, height);
            this.stack = stack;
            this.index = index;
            setBackgroundColor(0xFF333333);
            setBorderColor(0xFF555555);
        }

        public ItemStack getStack() {
            return stack;
        }

        public void setOnClick(java.util.function.Consumer<Integer> onClick) {
            this.onClick = onClick;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);

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

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isMouseOver(mouseX, mouseY) && onClick != null) {
                onClick.accept(index);
                return true;
            }
            return false;
        }
    }
}
