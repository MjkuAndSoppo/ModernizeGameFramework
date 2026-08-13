package com.modernizegameframework.inventory;

import com.modernizegameframework.securecontainer.SecureContainerItem;
import com.modernizegameframework.securecontainer.SecureContainerType;
import com.modernizegameframework.ui.UILayout;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * 塔科夫三段式背包菜单
 * 基于响应式布局设计：背包面板保持 16:9 并居中，底边栏沿屏幕底部，
 * 面板内部所有槽位按基准坐标（1600×900）等比映射到当前面板尺寸。
 */
public class TarkovInventoryMenu extends AbstractContainerMenu {

    // ===== 面板基准尺寸（16:9） =====
    private static final int PANEL_BASE_WIDTH = 1600;
    private static final int PANEL_BASE_HEIGHT = 900;

    // ===== 面板内基准区域（基于 1600×900 设计） =====
    // 背包面板横向均分为三段，每段宽度约 533，宽高比约为 16:27
    private static final int LEFT_PANEL_X = 20;
    private static final int LEFT_PANEL_Y = 50;
    private static final int MIDDLE_PANEL_X = 553;
    private static final int RIGHT_PANEL_X = 1086;
    private static final int TOP_BAR_H = 40;

    // ===== 左侧面板基准尺寸（用于内部元素相对定位） =====
    // 左侧面板宽度为 533，顶部留出 40 像素顶部边栏，内容区高度为 860
    private static final int LEFT_PANEL_BASE_WIDTH = 533;
    private static final int LEFT_PANEL_BASE_HEIGHT = 860;

    // ===== 左侧面板内部布局常量（基于左侧面板内容区 533×860 设计） =====
    // 左列：护甲槽；右列：装备槽 + 副手槽；中部：原版 3D 小人；底部：2×2 合成槽 + 状态文本
    // 两列槽位居于小人两侧，具体坐标由当前槽位尺寸、间隙与小人间距动态计算
    private static final int LEFT_TOP_PADDING = 30;
    private static final int LEFT_BOTTOM_PADDING = 30;
    private static final int LEFT_MODEL_GAP_BASE = 220; // 模型与左右两列槽位之间的水平间距基准
    private static final float LEFT_MODEL_VERTICAL_RATIO = 0.65f; // 模型中心位于顶部 4 行槽位区中部偏下
    private static final float LEFT_MODEL_SIZE_RATIO = 0.3f;      // 模型尺寸占顶部槽位区高度的比例（降低小人高度）
    private static final int LEFT_NAME_ABOVE_MODEL_BASE = 24;     // 昵称与模型顶部的间距（加大避免被小人挡住）
    private static final int LEFT_LEVEL_BELOW_MODEL_BASE = 4;     // 经验等级与模型底部的间距
    private static final int LEFT_STATUS_LINE_HEIGHT_BASE = 14;
    private static final int LEFT_STATUS_GAP_BASE = 12;

    private static final int MAIN_INV_X = 573;
    private static final int MAIN_INV_Y = 70;
    private static final int EXPANSION_X = 573;
    private static final int EXPANSION_Y = 150;
    private static final int SECURE_X = 573;
    private static final int SECURE_Y = 230;
    private static final int CONTAINER_X = 1106;
    private static final int CONTAINER_Y = 70;

    // ===== 槽位尺寸基准 =====
    private static final int BASE_SLOT_SIZE = 16;
    private static final int BASE_SLOT_GAP = 2;
    private static final int BASE_ROW_GAP = 2;
    private static final int BASE_SECTION_GAP = 12;

    // ===== 槽位范围（按添加顺序） =====
    public static final int EQUIPMENT_START = 0;
    public static final int EQUIPMENT_COUNT = 3;       // 胸挂、背包、安全箱
    public static final int ARMOR_START = 3;
    public static final int ARMOR_COUNT = 5;           // 头、胸、腿、脚、副手
    public static final int OFFHAND_INDEX = ARMOR_START + 4;  // 副手槽位索引（最后一个护甲槽）
    public static final int RESULT_SLOT = 8;
    public static final int CRAFTING_START = 9;
    public static final int CRAFTING_COUNT = 4;
    public static final int CONTAINER_START = 13;

    private final Inventory playerInventory;
    private final TarkovInventoryCapability tarkovInv;
    private final ResultContainer resultContainer = new ResultContainer();
    private final CraftingContainer craftSlots;
    private final Container externalContainer;
    private final Component externalTitle;

    private int containerSlotCount = 0;
    private int secureStart = 0;
    private int mainStart = 0;
    private int expansionStart = 0;
    private int hotbarStart = 0;
    private boolean hasSecureSlots = false;
    /** 安全箱实际槽位行列数（由装备的安全箱类型决定） */
    private int secureCols = 0;
    private int secureRows = 0;
    private int actualSecureSlots = 0;
    /** 菜单创建时记录的安全箱类型，用于服务端检测装备/卸下/更换并重建界面 */
    private SecureContainerType lastSecureType;

    // ===== 当前菜单响应式布局状态 =====
    private int screenWidth;
    private int screenHeight;
    /** 当前背包面板矩形 */
    private UILayout.Rect panelRect;
    /** 当前左侧面板矩形（内容区，已扣除顶部边栏） */
    private UILayout.Rect leftPanelRect;
    /** 普通槽位尺寸（合成等），保持 16 与原版物品图标一致 */
    private int slotSize = BASE_SLOT_SIZE;
    /** 左侧面板槽位尺寸（装备、护甲、副手、合成），按左侧面板宽度动态缩放 */
    private int leftSlotSize = BASE_SLOT_SIZE;
    /** 左侧面板槽位间隙，按左侧面板宽度动态缩放 */
    private int leftSlotGap;
    /** 普通槽位列间距 */
    private int slotGap;
    /** 普通槽位行间距 */
    private int rowGap;
    /** 区域垂直间距 */
    private int sectionGap;

    /** 中部面板槽位尺寸，根据面板宽度动态缩放，固定 8 列 */
    private int middleSlotSize = BASE_SLOT_SIZE;
    /** 中部面板槽位列间距 */
    private int middleSlotGap;
    /** 中部面板槽位行间距 */
    private int middleRowGap;
    /** 中部面板固定列数 */
    private static final int MIDDLE_PANEL_COLUMNS = 8;
    /** 右侧面板容器固定列数 */
    private static final int RIGHT_PANEL_COLUMNS = 8;

    /** 快捷栏槽位尺寸，受底边栏高度限制 */
    private int hotbarSlotSize = BASE_SLOT_SIZE;
    /** 右侧面板容器槽位尺寸，根据右侧面板宽度缩放 */
    private int containerSlotSize = BASE_SLOT_SIZE;
    /** 右侧面板容器槽位列间距 */
    private int containerSlotGap = BASE_SLOT_GAP;
    /** 右侧面板容器槽位行间距 */
    private int containerRowGap = BASE_ROW_GAP;

    /** 中部面板所有槽位的内容总高度，用于滚动范围计算 */
    private int middleContentHeight = 0;
    /** 中部面板槽位在 slots 列表中的起始索引 */
    private int middlePanelSlotStart = 0;
    /** 中部面板槽位在 slots 列表中的结束索引（不含） */
    private int middlePanelSlotEnd = 0;
    /** 中部面板在响应式布局下每行可显示的槽位列数 */
    private int middlePanelColumns = 1;
    /** 流式布局过程中下一个槽位的 Y 坐标 */
    private int nextMiddleY = 0;

    /** 右侧面板容器槽位的内容总高度，用于滚动范围计算 */
    private int rightContentHeight = 0;
    /** 右侧面板容器槽位在 slots 列表中的起始索引 */
    private int rightPanelSlotStart = 0;
    /** 右侧面板容器槽位在 slots 列表中的结束索引（不含） */
    private int rightPanelSlotEnd = 0;

    /**
     * 服务端构造函数
     */
    public TarkovInventoryMenu(int id, Inventory playerInv, Container externalContainer, Component externalTitle) {
        super(TarkovInventoryRegistry.TARKOV_INVENTORY_MENU.get(), id);
        this.playerInventory = playerInv;
        this.tarkovInv = playerInv.player.getCapability(TarkovInventoryCapabilityRegistry.TARKOV_INVENTORY_CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("玩家缺少塔科夫背包能力"));
        this.externalContainer = externalContainer;
        this.externalTitle = externalTitle == null ? Component.empty() : externalTitle;
        this.containerSlotCount = externalContainer == null ? 0 : externalContainer.getContainerSize();
        this.craftSlots = new TransientCraftingContainer(this, 2, 2, NonNullList.withSize(4, ItemStack.EMPTY));

        initializeLayout();
        addAllSlots();
        this.lastSecureType = tarkovInv.getSecureCaseType();
    }

    /**
     * 客户端构造函数（从网络缓冲读取右侧容器信息）
     */
    public TarkovInventoryMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, buf.readInt(), buf.readComponent());
    }

    /**
     * 客户端重建构造函数（窗口尺寸变化时本地重建界面使用）
     */
    public TarkovInventoryMenu(int id, Inventory playerInv, int containerSlotCount, Component externalTitle) {
        super(TarkovInventoryRegistry.TARKOV_INVENTORY_MENU.get(), id);
        this.playerInventory = playerInv;
        this.tarkovInv = playerInv.player.getCapability(TarkovInventoryCapabilityRegistry.TARKOV_INVENTORY_CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("玩家缺少塔科夫背包能力"));
        this.containerSlotCount = containerSlotCount;
        this.externalTitle = externalTitle == null ? Component.empty() : externalTitle;
        this.externalContainer = containerSlotCount > 0 ? new SimpleContainer(containerSlotCount) : null;
        this.craftSlots = new TransientCraftingContainer(this, 2, 2, NonNullList.withSize(4, ItemStack.EMPTY));

        initializeLayout();
        addAllSlots();
        this.lastSecureType = tarkovInv.getSecureCaseType();
    }

    /**
     * 初始化响应式布局状态
     */
    private void initializeLayout() {
        this.screenWidth = getReferenceScreenWidth();
        this.screenHeight = getReferenceScreenHeight();
        this.panelRect = UILayout.backpackPanel(screenWidth, screenHeight);
        this.leftPanelRect = UILayout.leftPanel(screenWidth, screenHeight);

        // 普通槽位保持 16，与原版物品图标保持一致
        this.slotSize = BASE_SLOT_SIZE;
        // 普通槽位间距按面板宽度比例缩放，最小为 1 像素
        float panelScale = (float) panelRect.width() / PANEL_BASE_WIDTH;
        this.slotGap = Math.max(1, Math.round(BASE_SLOT_GAP * panelScale));
        this.rowGap = Math.max(1, Math.round(BASE_ROW_GAP * panelScale));
        this.sectionGap = Math.max(1, Math.round(BASE_SECTION_GAP * panelScale));

        // 中部面板固定 8 列，根据可用宽度计算动态格子尺寸
        computeMiddleSlotSize();
        // 左侧面板槽位尺寸与中部格子保持一致，并按面板宽度均匀分布两列
        computeLeftSlotSize();
        // 右侧面板容器槽尺寸根据右侧面板宽度缩放
        computeContainerSlotSize();
        // 快捷栏尺寸受底边栏高度限制，避免超出底边栏
        computeHotbarSlotSize();
    }

    /**
     * 计算左侧面板槽位尺寸与间隙。
     * 左侧槽位尺寸与中部格子保持一致，确保视觉统一；间隙同步使用中部面板间隙。
     * 若面板极窄导致两列重叠，则将槽位尺寸限制在面板可容纳范围内。
     */
    private void computeLeftSlotSize() {
        this.leftSlotSize = this.middleSlotSize;
        this.leftSlotGap = this.middleSlotGap;
        // 保证两列槽位在面板内不重叠，并留出模型间隙：最大允许宽度为 (面板宽 - 模型间隙) / 2
        int maxSlotSize = Math.max(BASE_SLOT_SIZE, (leftPanelRect.width() - leftModelGap()) / 2);
        if (this.leftSlotSize > maxSlotSize) {
            this.leftSlotSize = maxSlotSize;
        }
    }

    /**
     * 根据中部面板可用宽度计算固定 8 列下的槽位尺寸与间隙。
     * 公式与 ui_preview.html 保持一致：尺寸 = max(16, floor(可用宽度 / 8.875))。
     */
    private void computeMiddleSlotSize() {
        UILayout.Rect middle = UILayout.middlePanel(screenWidth, screenHeight);
        int margin = UILayout.scaled(20, screenHeight);
        int availableWidth = middle.width() - margin * 2 - 6; // SCROLLBAR_WIDTH
        this.middleSlotSize = Math.max(BASE_SLOT_SIZE, (int) Math.floor(availableWidth / 8.875f));
        int maxGap = (availableWidth - MIDDLE_PANEL_COLUMNS * this.middleSlotSize) / (MIDDLE_PANEL_COLUMNS - 1);
        int preferredGap = Math.round(this.middleSlotSize * 0.125f);
        this.middleSlotGap = Math.max(1, Math.min(preferredGap, maxGap));
        this.middleRowGap = this.middleSlotGap;
    }

    /**
     * 根据底边栏高度计算快捷栏槽位尺寸，不能超过底边栏高度减去上下边距。
     */
    private void computeHotbarSlotSize() {
        int bottomBarHeight = UILayout.bottomBar(screenWidth, screenHeight).height();
        this.hotbarSlotSize = Math.min(this.middleSlotSize, bottomBarHeight - 12);
        this.hotbarSlotSize = Math.max(BASE_SLOT_SIZE, this.hotbarSlotSize);
    }

    /**
     * 根据右侧面板可用宽度计算容器槽位尺寸与间隙，固定 8 列并支持换行滚动。
     */
    private void computeContainerSlotSize() {
        if (containerSlotCount == 0) {
            this.containerSlotSize = BASE_SLOT_SIZE;
            return;
        }
        UILayout.Rect right = UILayout.rightPanel(screenWidth, screenHeight, true);
        int margin = UILayout.scaled(20, screenHeight);
        int availableWidth = Math.max(0, right.width() - margin * 2);
        // 容器按 8 列设计，尺寸与间隙计算方式与中部面板保持一致
        this.containerSlotSize = Math.max(BASE_SLOT_SIZE, (int) Math.floor(availableWidth / 8.875f));
        int maxGap = (availableWidth - RIGHT_PANEL_COLUMNS * this.containerSlotSize) / (RIGHT_PANEL_COLUMNS - 1);
        int preferredGap = Math.round(this.containerSlotSize * 0.125f);
        this.containerSlotGap = Math.max(1, Math.min(preferredGap, maxGap));
        this.containerRowGap = this.containerSlotGap;
    }

    /**
     * 按顺序添加所有槽位
     */
    private void addAllSlots() {
        addEquipmentSlots();
        addArmorSlots();
        addCraftingSlots();
        addContainerSlots();
        // 中部面板槽位按视觉顺序添加：主仓库 -> 扩展格 -> 安全箱，便于流式布局计算
        addMainInventorySlots();
        addExpansionSlots();
        addSecureSlots();
        addHotbarSlots();
    }

    /**
     * 将基准面板坐标转换为当前面板内的实际像素坐标
     */
    private int panelX(int baseX) {
        return UILayout.panelPoint(baseX, 0, panelRect).x();
    }

    private int panelY(int baseY) {
        return UILayout.panelPoint(0, baseY, panelRect).y();
    }

    /**
     * 将基准尺寸转换为当前面板内的实际像素尺寸
     */
    private int panelSize(int baseSize) {
        return UILayout.panelSize(baseSize, panelRect);
    }

    /**
     * 将左侧面板内部基准 X 坐标（基于 533 宽度）转换为屏幕实际 X 坐标
     */
    private int leftPanelX(int baseX) {
        return leftPanelRect.x() + Math.round(baseX * ((float) leftPanelRect.width() / LEFT_PANEL_BASE_WIDTH));
    }

    /**
     * 将左侧面板内部基准 Y 坐标（基于 860 高度）转换为屏幕实际 Y 坐标
     */
    private int leftPanelY(int baseY) {
        return leftPanelRect.y() + Math.round(baseY * ((float) leftPanelRect.height() / LEFT_PANEL_BASE_HEIGHT));
    }

    /**
     * 将左侧面板内部基准尺寸（基于 533 宽度）转换为实际像素尺寸
     */
    private int leftPanelSize(int baseSize) {
        return Math.round(baseSize * ((float) leftPanelRect.width() / LEFT_PANEL_BASE_WIDTH));
    }

    /**
     * 将左侧面板内部基准 Y 偏移（基于 860 高度）转换为当前内容区内的像素偏移
     */
    private int leftPanelYOffset(int baseY) {
        return Math.round(baseY * ((float) leftPanelRect.height() / LEFT_PANEL_BASE_HEIGHT));
    }

    /**
     * 计算原版 3D 小人与两列槽位之间的预留间距。
     */
    private int leftModelGap() {
        return Math.max(20, leftPanelSize(LEFT_MODEL_GAP_BASE));
    }

    /**
     * 计算左侧面板两侧留白，确保小人两侧都有足够空隙。
     */
    private int leftSideMargin() {
        int contentWidth = 2 * leftSlotSize + leftModelGap();
        return Math.max(10, (leftPanelRect.width() - contentWidth) / 2);
    }

    /**
     * 计算左侧面板左列槽位 X 坐标（护甲列）。
     */
    private int leftColX() {
        return leftPanelRect.x() + leftSideMargin();
    }

    /**
     * 计算左侧面板右列槽位 X 坐标（装备 + 副手列）。
     */
    private int rightColX() {
        return leftPanelRect.x() + leftPanelRect.width() - leftSideMargin() - leftSlotSize;
    }

    /**
     * 计算左侧面板第 row 行槽位的 Y 坐标，行距由槽位尺寸与间隙决定。
     */
    private int leftRowY(int row) {
        return leftPanelRect.y() + leftPanelYOffset(LEFT_TOP_PADDING)
                + row * (leftSlotSize + leftSlotGap);
    }

    /**
     * 计算顶部 4 行槽位区的总高度。
     */
    private int leftSlotBlockHeight() {
        return 4 * leftSlotSize + 3 * leftSlotGap;
    }

    /**
     * 计算原版 3D 小人渲染尺寸，至少 24 像素。
     */
    private int leftModelSize() {
        return Math.max(24, Math.round(leftSlotBlockHeight() * LEFT_MODEL_SIZE_RATIO));
    }

    /**
     * 计算原版 3D 小人中心 Y 坐标，位于顶部槽位区下半部分，贴合设计稿。
     */
    private int leftModelCenterY() {
        return leftPanelRect.y() + leftPanelYOffset(LEFT_TOP_PADDING)
                + Math.round(leftSlotBlockHeight() * LEFT_MODEL_VERTICAL_RATIO);
    }

    /**
     * 计算玩家昵称 Y 坐标，与头盔槽第一行对齐：名称基线位于头盔栏 Y + 4 像素处。
     */
    private int leftNameY() {
        return leftRowY(0) + 4;
    }

    /**
     * 计算玩家经验等级 Y 坐标，位于模型底部下方居中。
     */
    private int leftLevelY() {
        return leftModelCenterY() + leftModelSize() / 2
                + Math.max(2, leftPanelSize(LEFT_LEVEL_BELOW_MODEL_BASE));
    }

    /**
     * 计算左侧面板底部状态文本行高。
     */
    private int leftStatusLineHeight() {
        return Math.max(10, Math.round(LEFT_STATUS_LINE_HEIGHT_BASE
                * ((float) leftPanelRect.height() / LEFT_PANEL_BASE_HEIGHT)));
    }

    /**
     * 计算左侧面板底部状态文本与合成区之间的间距。
     */
    private int leftStatusGap() {
        return Math.max(8, Math.round(LEFT_STATUS_GAP_BASE
                * ((float) leftPanelRect.height() / LEFT_PANEL_BASE_HEIGHT)));
    }

    /**
     * 计算左侧面板底部状态文本块总高度（实际渲染 2 行：第 1 行 3 项 + 第 2 行 1 项）。
     */
    private int leftStatusBlockHeight() {
        return 2 * leftStatusLineHeight() + 2;
    }

    /**
     * 计算左侧面板底部合成区的底部 Y 坐标。
     * 设计稿中合成区位于左侧面板中下位置，底部留有空白；
     * 这里将名字/等级底部到合成区顶部之间的间距与底部留白按比例分配，
     * 空间不足时回退到底部锚定。
     */
    private int leftCraftBottomY() {
        int contentTop = leftPanelRect.y() + leftPanelYOffset(LEFT_TOP_PADDING);
        int contentBottom = leftPanelRect.y() + leftPanelRect.height()
                - leftPanelYOffset(LEFT_BOTTOM_PADDING);
        int contentHeight = contentBottom - contentTop;

        int upperBottom = leftLevelY() + 9; // 9 像素为默认字体行高，以经验值底部为上半区结束
        int lowerHeight = 2 * leftSlotSize + leftSlotGap
                + leftStatusGap() + leftStatusBlockHeight();
        int available = contentHeight - (upperBottom - contentTop) - lowerHeight;

        if (available <= 0) {
            // 空间不足时回退到底部锚定
            return contentBottom;
        }

        int middleGap = Math.max(leftStatusGap(), Math.round(available * 0.2f));
        return upperBottom + middleGap + lowerHeight;
    }

    private void addEquipmentSlots() {
        ItemStackHandler equipment = tarkovInv.getEquipmentInventory();
        int x = rightColX();
        addSlot(new NotifyingSlotItemHandler(equipment, TarkovInventoryCapability.SLOT_CHEST_RIG, x, leftRowY(0)));
        addSlot(new NotifyingSlotItemHandler(equipment, TarkovInventoryCapability.SLOT_BACKPACK, x, leftRowY(1)));
        addSlot(new NotifyingSlotItemHandler(equipment, TarkovInventoryCapability.SLOT_SECURE_CASE, x, leftRowY(2)));
    }

    private void addArmorSlots() {
        int x = leftColX();
        addSlot(new ArmorSlot(playerInventory, 39, EquipmentSlot.HEAD, x, leftRowY(0)));
        addSlot(new ArmorSlot(playerInventory, 38, EquipmentSlot.CHEST, x, leftRowY(1)));
        addSlot(new ArmorSlot(playerInventory, 37, EquipmentSlot.LEGS, x, leftRowY(2)));
        addSlot(new ArmorSlot(playerInventory, 36, EquipmentSlot.FEET, x, leftRowY(3)));
        addSlot(new Slot(playerInventory, 40, rightColX(), leftRowY(3)));
    }

    private void addCraftingSlots() {
        int matStartX = leftColX();
        int matNextX = matStartX + leftSlotSize + leftSlotGap;
        // 结果槽位于材料右侧；若面板较窄，则吸附到右列，避免超出面板
        int resultX = Math.min(rightColX(), matStartX + 2 * (leftSlotSize + leftSlotGap));
        int bottom = leftCraftBottomY();
        // 2×2 合成材料分上下两行排列
        int topRowY = bottom - 2 * leftSlotSize - leftSlotGap;
        int bottomRowY = bottom - leftSlotSize;
        addSlot(new ResultSlot(playerInventory.player, craftSlots, resultContainer, 0, resultX, topRowY));
        addSlot(new Slot(craftSlots, 0, matStartX, topRowY));
        addSlot(new Slot(craftSlots, 1, matNextX, topRowY));
        addSlot(new Slot(craftSlots, 2, matStartX, bottomRowY));
        addSlot(new Slot(craftSlots, 3, matNextX, bottomRowY));
    }

    private void addContainerSlots() {
        secureStart = CONTAINER_START + containerSlotCount;
        rightPanelSlotStart = slots.size();
        if (externalContainer == null || containerSlotCount == 0) {
            rightPanelSlotEnd = rightPanelSlotStart;
            rightContentHeight = 0;
            return;
        }
        UILayout.Rect right = UILayout.rightPanel(screenWidth, screenHeight, true);
        int margin = UILayout.scaled(20, screenHeight);
        int startX = right.x() + margin;
        // 保持与中部面板第一行在同一水平线（CONTAINER_Y = MAIN_INV_Y = 70）
        int startY = panelY(CONTAINER_Y);
        for (int i = 0; i < containerSlotCount; i++) {
            int row = i / RIGHT_PANEL_COLUMNS;
            int col = i % RIGHT_PANEL_COLUMNS;
            addSlot(new Slot(externalContainer, i,
                    startX + col * (containerSlotSize + containerSlotGap),
                    startY + row * (containerSlotSize + containerRowGap)));
        }
        rightPanelSlotEnd = slots.size();
        rightContentHeight = computeRightContentHeight();
    }

    /**
     * 返回中部面板固定列数（严格 8 列）。
     * 格子尺寸通过 {@link #computeMiddleSlotSize()} 按面板宽度动态缩放。
     */
    private int getMiddlePanelColumns() {
        return MIDDLE_PANEL_COLUMNS;
    }

    /**
     * 将指定数量的槽位按流式布局添加到当前 Y 位置，并返回占用后的下一个 Y 坐标。
     * 中部面板槽位使用动态计算的 {@link #middleSlotSize} 与 {@link #middleSlotGap}。
     */
    private int addFlowSlots(int slotCount, SlotFactory factory, int startX, int startY, int columns) {
        for (int i = 0; i < slotCount; i++) {
            int row = i / columns;
            int col = i % columns;
            addSlot(factory.create(i, startX + col * (middleSlotSize + middleSlotGap),
                    startY + row * (middleSlotSize + middleRowGap)));
        }
        int rows = (slotCount + columns - 1) / columns;
        return rows > 0 ? startY + rows * (middleSlotSize + middleRowGap) : startY;
    }

    private void addMainInventorySlots() {
        mainStart = slots.size();
        middlePanelSlotStart = mainStart;

        middlePanelColumns = getMiddlePanelColumns();
        UILayout.Rect middle = UILayout.middlePanel(screenWidth, screenHeight);
        int margin = UILayout.scaled(20, screenHeight);
        int startX = middle.x() + margin;
        int startY = panelY(MAIN_INV_Y);

        nextMiddleY = addFlowSlots(27, (index, x, y) ->
                new MainInventorySlot(playerInventory, index + 9, index, x, y), startX, startY, middlePanelColumns);
        nextMiddleY += sectionGap;

        expansionStart = mainStart + 27;
    }

    private void addExpansionSlots() {
        expansionStart = slots.size();

        UILayout.Rect middle = UILayout.middlePanel(screenWidth, screenHeight);
        int margin = UILayout.scaled(20, screenHeight);
        int startX = middle.x() + margin;
        int startY = nextMiddleY;

        nextMiddleY = addFlowSlots(27, (index, x, y) ->
                new ExpansionSlot(tarkovInv.getExpansionInventory(), index, x, y), startX, startY, middlePanelColumns);
        nextMiddleY += sectionGap;

        secureStart = expansionStart + 27;
    }

    private void addSecureSlots() {
        ItemStack secureCase = tarkovInv.getSecureCase();
        if (secureCase.isEmpty()) {
            hasSecureSlots = false;
            secureCols = 0;
            secureRows = 0;
            actualSecureSlots = 0;
            hotbarStart = secureStart;
            middlePanelSlotEnd = slots.size();
            middleContentHeight = computeMiddleContentHeight();
            return;
        }

        SecureContainerType type = null;
        if (secureCase.getItem() instanceof SecureContainerItem sci) {
            type = sci.getType();
        }
        if (type == null) {
            secureCols = 2;
            secureRows = 2;
        } else {
            secureCols = type.getCols();
            secureRows = type.getRows();
        }
        actualSecureSlots = secureCols * secureRows;
        hasSecureSlots = true;

        UILayout.Rect middle = UILayout.middlePanel(screenWidth, screenHeight);
        int margin = UILayout.scaled(20, screenHeight);
        int startX = middle.x() + margin;
        int startY = nextMiddleY;
        ItemStackHandler secure = tarkovInv.getSecureInventory();

        nextMiddleY = addFlowSlots(actualSecureSlots, (index, x, y) ->
                new SecureSlot(secure, index, x, y), startX, startY, middlePanelColumns);

        hotbarStart = secureStart + actualSecureSlots;
        middlePanelSlotEnd = slots.size();
        middleContentHeight = computeMiddleContentHeight();
    }

    /**
     * 根据中部面板所有槽位的实际分布计算内容总高度。
     * 以中部面板顶部为基准，计算到最低槽位底部的距离，确保滚动下限正好停在最后一个格子底部。
     * 这样即使主仓库第一行位于面板顶部留空区域下方，也能完整滚动显示最下方的格子。
     */
    private int computeMiddleContentHeight() {
        if (slots.size() <= middlePanelSlotStart) {
            return 0;
        }
        UILayout.Rect middle = UILayout.middlePanel(screenWidth, screenHeight);
        int maxBottom = Integer.MIN_VALUE;
        for (int i = middlePanelSlotStart; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            maxBottom = Math.max(maxBottom, slot.y + middleSlotSize);
        }
        return Math.max(0, maxBottom - middle.y());
    }

    /**
     * 根据右侧面板容器槽位的实际分布计算内容总高度。
     * 以右侧面板顶部为基准，计算到最低槽位底部的距离，超过面板高度时启用滚动。
     */
    private int computeRightContentHeight() {
        if (rightPanelSlotEnd <= rightPanelSlotStart) {
            return 0;
        }
        UILayout.Rect right = UILayout.rightPanel(screenWidth, screenHeight, true);
        int maxBottom = Integer.MIN_VALUE;
        for (int i = rightPanelSlotStart; i < rightPanelSlotEnd; i++) {
            Slot slot = slots.get(i);
            maxBottom = Math.max(maxBottom, slot.y + containerSlotSize);
        }
        return Math.max(0, maxBottom - right.y());
    }

    /**
     * 创建槽位的工厂接口，供流式布局使用。
     */
    private interface SlotFactory {
        Slot create(int index, int x, int y);
    }

    private void addHotbarSlots() {
        int hotbarGap = Math.max(1, Math.round(hotbarSlotSize * 0.125f));
        int startX = UILayout.hotbarStartX(hotbarSlotSize, hotbarGap, screenWidth);
        int startY = UILayout.hotbarStartY(hotbarSlotSize, screenWidth, screenHeight);
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    startX + col * (hotbarSlotSize + hotbarGap),
                    startY));
        }
    }

    // ===== 参考屏幕尺寸获取 =====

    private int getReferenceScreenWidth() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return getClientWindowWidth();
        }
        return 854;
    }

    private int getReferenceScreenHeight() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return getClientWindowHeight();
        }
        return 480;
    }

    private int getClientWindowWidth() {
        return net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    private int getClientWindowHeight() {
        return net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    // ===== crafting 结果更新 =====

    @Override
    public void slotsChanged(@NotNull Container container) {
        super.slotsChanged(container);
        if (container == craftSlots) {
            updateCraftingResult();
        }
        if (container == tarkovInv.getEquipmentInventory()) {
            if (!playerInventory.player.level().isClientSide) {
                SecureContainerType currentType = tarkovInv.getSecureCaseType();
                if (currentType != lastSecureType) {
                    lastSecureType = currentType;
                    TarkovInventoryEvents.markForRebuild(playerInventory.player.getUUID());
                }
                TarkovInventoryHelper.rebalanceItems(playerInventory.player);
            }
        }
    }

    private void updateCraftingResult() {
        if (playerInventory.player.level().isClientSide) {
            return;
        }
        ServerPlayer player = (ServerPlayer) playerInventory.player;
        ItemStack result = ItemStack.EMPTY;
        Optional<CraftingRecipe> recipe = player.server.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftSlots, player.level());
        if (recipe.isPresent()) {
            result = recipe.get().assemble(craftSlots, player.level().registryAccess());
        }
        resultContainer.setItem(0, result);
        broadcastChanges();
    }

    // ===== 快速移动 =====

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();

        int equipmentEnd = OFFHAND_INDEX;
        int containerEnd = externalContainer == null ? CONTAINER_START : CONTAINER_START + containerSlotCount;
        int secureEnd = secureStart + actualSecureSlots;
        int mainEnd = mainStart + 27;
        int expansionEnd = expansionStart + 27;
        int hotbarEnd = hotbarStart + 9;

        // 中部面板槽位在 slots 列表中的顺序为：主仓库 -> 扩展格 -> 安全箱 -> 快捷栏
        // 因此 [mainStart, hotbarEnd) 涵盖了所有玩家可存储物品的槽位
        if (index == RESULT_SLOT) {
            if (!moveItemStackTo(stack, mainStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= CRAFTING_START && index < CRAFTING_START + CRAFTING_COUNT) {
            if (!moveItemStackTo(stack, mainStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= EQUIPMENT_START && index < equipmentEnd) {
            if (!moveItemStackTo(stack, mainStart, hotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (externalContainer != null && index >= CONTAINER_START && index < containerEnd) {
            if (!moveItemStackTo(stack, mainStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (externalContainer != null) {
                if (!moveItemStackTo(stack, CONTAINER_START, containerEnd, false)) {
                    if (!moveItemStackTo(stack, EQUIPMENT_START, equipmentEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else {
                if (!moveItemStackTo(stack, EQUIPMENT_START, equipmentEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    // ===== 关闭菜单时处理 =====

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (externalContainer != null) {
            return externalContainer.stillValid(player);
        }
        return true;
    }

    // ===== getter =====

    public Container getExternalContainer() {
        return externalContainer;
    }

    public Component getExternalTitle() {
        return externalTitle;
    }

    public int getContainerSlotCount() {
        return containerSlotCount;
    }

    public int getMainStart() {
        return mainStart;
    }

    public int getExpansionStart() {
        return expansionStart;
    }

    public int getHotbarStart() {
        return hotbarStart;
    }

    public int getSecureStart() {
        return secureStart;
    }

    public int getSecureCols() {
        return secureCols;
    }

    public int getSecureRows() {
        return secureRows;
    }

    public int getActualSecureSlots() {
        return actualSecureSlots;
    }

    public int getMiddleContentHeight() {
        return middleContentHeight;
    }

    public int getMiddlePanelSlotStart() {
        return middlePanelSlotStart;
    }

    public int getMiddlePanelSlotEnd() {
        return middlePanelSlotEnd;
    }

    public int getSlotSize() {
        return slotSize;
    }

    public int getSlotGap() {
        return slotGap;
    }

    public int getLeftSlotSize() {
        return leftSlotSize;
    }

    public int getLeftSlotGap() {
        return leftSlotGap;
    }

    public UILayout.Rect getLeftPanelRect() {
        return leftPanelRect;
    }

    public int getLeftColX() {
        return leftColX();
    }

    public int getRightColX() {
        return rightColX();
    }

    public int getLeftTopPadding() {
        return leftPanelYOffset(LEFT_TOP_PADDING);
    }

    public int getLeftModelCenterY() {
        return leftModelCenterY();
    }

    public int getLeftModelSize() {
        return leftModelSize();
    }

    public int getLeftNameY() {
        return leftNameY();
    }

    public int getLeftLevelY() {
        return leftLevelY();
    }

    public int getLeftCraftBottomY() {
        return leftCraftBottomY();
    }

    public int getLeftStatusLineHeight() {
        return leftStatusLineHeight();
    }

    public int getLeftStatusGap() {
        return leftStatusGap();
    }

    public int getLeftStatusBlockHeight() {
        return leftStatusBlockHeight();
    }

    public int getMiddleSlotSize() {
        return middleSlotSize;
    }

    public int getMiddleSlotGap() {
        return middleSlotGap;
    }

    public int getHotbarSlotSize() {
        return hotbarSlotSize;
    }

    public int getContainerSlotSize() {
        return containerSlotSize;
    }

    public int getRightContentHeight() {
        return rightContentHeight;
    }

    public int getRightPanelSlotStart() {
        return rightPanelSlotStart;
    }

    public int getRightPanelSlotEnd() {
        return rightPanelSlotEnd;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public UILayout.Rect getPanelRect() {
        return panelRect;
    }

    // ===== 自定义槽位类 =====

    private class NotifyingSlotItemHandler extends SlotItemHandler {
        NotifyingSlotItemHandler(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!playerInventory.player.level().isClientSide) {
                TarkovInventoryHelper.rebalanceItems(playerInventory.player);
            }
        }
    }

    private class MainInventorySlot extends Slot {
        private final int mainIndex;

        MainInventorySlot(Inventory inv, int vanillaIndex, int mainIndex, int x, int y) {
            super(inv, vanillaIndex, x, y);
            this.mainIndex = mainIndex;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return isUnlocked() && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return isUnlocked() && super.mayPickup(player);
        }

        private boolean isUnlocked() {
            return mainIndex < TarkovInventoryHelper.getUnlockedMainSlots(playerInventory.player);
        }
    }

    private class ExpansionSlot extends SlotItemHandler {
        private final int expansionIndex;

        ExpansionSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
            this.expansionIndex = index;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return isUnlocked() && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return isUnlocked() && super.mayPickup(player);
        }

        private boolean isUnlocked() {
            return expansionIndex < TarkovInventoryHelper.getExpansionSlotCount(playerInventory.player);
        }
    }

    private class SecureSlot extends SlotItemHandler {
        SecureSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            if (stack.getItem() instanceof SecureContainerItem) return false;
            return isUnlocked() && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return isUnlocked() && super.mayPickup(player);
        }

        private boolean isUnlocked() {
            return !tarkovInv.getSecureCase().isEmpty();
        }
    }

    private static class ArmorSlot extends Slot {
        private final EquipmentSlot equipmentSlot;

        ArmorSlot(Inventory inv, int index, EquipmentSlot slot, int x, int y) {
            super(inv, index, x, y);
            this.equipmentSlot = slot;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return Mob.getEquipmentSlotForItem(stack) == equipmentSlot;
        }
    }

    // ===== 槽位类型判断 =====

    /**
     * 判断指定槽位是否属于左侧面板（装备、护甲、副手、合成）。
     */
    public boolean isLeftPanelSlot(Slot slot) {
        int index = slots.indexOf(slot);
        if (index < 0) return false;
        return index >= EQUIPMENT_START && index < CONTAINER_START;
    }

    public boolean isEquipmentSlot(Slot slot) {
        int index = slots.indexOf(slot);
        if (index < 0) return false;
        return index >= EQUIPMENT_START && index < CONTAINER_START;
    }

    public boolean isMainInventorySlot(Slot slot) {
        int index = slots.indexOf(slot);
        if (index < 0) return false;
        return index >= mainStart && index < mainStart + 27;
    }

    public boolean isExpansionSlot(Slot slot) {
        int index = slots.indexOf(slot);
        if (index < 0) return false;
        return index >= expansionStart && index < expansionStart + 27;
    }

    public boolean isSecureSlot(Slot slot) {
        if (!hasSecureSlots) return false;
        int index = slots.indexOf(slot);
        if (index < 0) return false;
        return index >= secureStart && index < secureStart + actualSecureSlots;
    }

    public boolean isContainerSlot(Slot slot) {
        int index = slots.indexOf(slot);
        if (index < 0 || externalContainer == null) return false;
        return index >= CONTAINER_START && index < CONTAINER_START + containerSlotCount;
    }

    public boolean isHotbarSlot(Slot slot) {
        int index = slots.indexOf(slot);
        if (index < 0) return false;
        return index >= hotbarStart && index < hotbarStart + 9;
    }

    public boolean isLockedSlot(Slot slot) {
        int index = slots.indexOf(slot);
        if (index < 0) return false;
        if (hasSecureSlots && index >= secureStart && index < secureStart + actualSecureSlots) {
            return tarkovInv.getSecureCase().isEmpty();
        }
        if (index >= mainStart && index < mainStart + 27) {
            int mainIndex = index - mainStart;
            return mainIndex >= TarkovInventoryHelper.getUnlockedMainSlots(playerInventory.player);
        }
        if (index >= expansionStart && index < expansionStart + 27) {
            int expansionIndex = index - expansionStart;
            return expansionIndex >= TarkovInventoryHelper.getExpansionSlotCount(playerInventory.player);
        }
        return false;
    }

    public boolean hasExternalContainer() {
        return externalContainer != null && containerSlotCount > 0;
    }

    public ItemStack getChestRig() {
        return tarkovInv.getChestRig();
    }

    public ItemStack getBackpack() {
        return tarkovInv.getBackpack();
    }

    public ItemStack getSecureCase() {
        return tarkovInv.getSecureCase();
    }
}
