package com.modernizegameframework.inventory;

import com.modernizegameframework.securecontainer.SecureContainerItem;
import com.modernizegameframework.securecontainer.SecureContainerType;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.layout.Position;
import com.sighs.apricityui.render.Base;
import com.sighs.apricityui.render.Mask;
import com.sighs.apricityui.screen.AuiLinkedScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 塔科夫三段式背包界面 - AUI 纯 CSS 布局版本
 * AUI 负责所有视觉渲染（面板、槽位背景、文字、滚动）
 * Java 负责物品图标覆盖层和玩家模型渲染
 */
public class TarkovInventoryScreen extends AbstractContainerScreen<TarkovInventoryMenu> implements AuiLinkedScreen {

    /** AUI 文档模板路径 */
    private static final String AUI_TEMPLATE = "screens/tarkov_inventory.html";

    /** AUI 背景文档 */
    private Document auiDocument = null;

    /** 槽位屏幕坐标缓存：slotIndex -> {x, y, width, height} */
    private final Map<Integer, SlotRect> slotRects = new HashMap<>();

    /** 玩家模型屏幕坐标 */
    private int modelCenterX, modelCenterY, modelSize;

    /** 当前鼠标位置（用于玩家模型跟随） */
    private int currentMouseX;
    private int currentMouseY;

    public TarkovInventoryScreen(TarkovInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    // ========================================================================
    // AUI 文档管理
    // ========================================================================

    @Override
    public Document getLinkedDocument() {
        return auiDocument;
    }

    @Override
    protected void init() {
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        super.init();
        this.leftPos = 0;
        this.topPos = 0;
        initAuiDocument();
    }

    /**
     * 创建或重建 AUI 文档
     */
    private void initAuiDocument() {
        // 清理旧文档
        if (auiDocument != null) {
            auiDocument.remove();
            auiDocument = null;
        }
        slotRects.clear();
        modelCenterX = modelCenterY = modelSize = 0;

        // 创建新文档
        auiDocument = Document.create(AUI_TEMPLATE);
        if (auiDocument == null) return;

        auiDocument.applyViewport(false);

        // 在 AUI 网格容器中创建动态槽位元素
        createAuiSlotElements();

        // 更新玩家信息
        updatePlayerInfo();

        // 计算所有槽位和模型位置的屏幕坐标
        refreshLayout();
    }

    /**
     * 在 AUI 文档的网格容器中创建动态槽位元素
     */
    private void createAuiSlotElements() {
        if (auiDocument == null) return;

        Element middleGrid = auiDocument.getElementById("middleGrid");
        Element rightGrid = auiDocument.getElementById("rightGrid");
        Element hotbarGrid = auiDocument.getElementById("hotbarGrid");
        Element rightPanel = auiDocument.getElementById("rightPanel");
        boolean hasContainer = menu.hasExternalContainer();

        // 右侧面板始终可见，无容器时留空占位
        if (rightPanel != null) {
            // 不设 hidden 类，始终保留空间
        }

        // 容器标题
        if (hasContainer) {
            Element titleEl = auiDocument.getElementById("containerTitle");
            if (titleEl != null) {
                Component title = menu.getExternalTitle();
                titleEl.setTextContent(title != null ? title.getString() : "容器");
            }
        }

        // 为每个菜单槽位创建 AUI 槽位元素
        // 左侧面板的槽位已在 HTML 中静态定义，只需处理中部/右侧/快捷栏
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);

            // 跳过已在 HTML 中定义的左侧面板槽位
            if (menu.isLeftPanelSlot(slot)) continue;

            Element slotEl = auiDocument.createElement("div");
            slotEl.setAttribute("class", "slot-bg");
            slotEl.setAttribute("data-slot-index", String.valueOf(i));

            if (i >= menu.getMiddlePanelSlotStart() && i < menu.getMiddlePanelSlotEnd()) {
                // 中部面板槽位
                if (middleGrid != null) middleGrid.appendChild(slotEl);
            } else if (hasContainer && i >= menu.getRightPanelSlotStart() && i < menu.getRightPanelSlotEnd()) {
                // 右侧面板槽位
                if (rightGrid != null) rightGrid.appendChild(slotEl);
            } else if (menu.isHotbarSlot(slot)) {
                // 快捷栏槽位
                if (hotbarGrid != null) hotbarGrid.appendChild(slotEl);
            }
        }
    }

    /**
     * 更新玩家名称、经验等级等信息
     */
    private void updatePlayerInfo() {
        if (auiDocument == null || minecraft == null || minecraft.player == null) return;

        Element nameEl = auiDocument.getElementById("playerName");
        if (nameEl != null) {
            nameEl.setTextContent(minecraft.player.getDisplayName().getString());
        }

        Element levelEl = auiDocument.getElementById("playerLevel");
        if (levelEl != null) {
            levelEl.setTextContent("Lv." + minecraft.player.experienceLevel);
        }

        // 状态栏信息
        Element hpEl = auiDocument.getElementById("statusHp");
        if (hpEl != null) {
            hpEl.setTextContent(String.valueOf((int) minecraft.player.getHealth()));
        }
        Element hungerEl = auiDocument.getElementById("statusHunger");
        if (hungerEl != null) {
            hungerEl.setTextContent(String.valueOf(minecraft.player.getFoodData().getFoodLevel()));
        }
    }

    /**
     * 刷新布局：读取 AUI 元素位置，计算屏幕坐标
     */
    private void refreshLayout() {
        if (auiDocument == null) return;

        double scaleX = auiDocument.getViewportScaleX();
        double scaleY = auiDocument.getViewportScaleY();

        // 更新槽位位置
        slotRects.clear();
        List<Element> slotElements = auiDocument.getElementsByClassName("slot-bg");
        for (Element el : slotElements) {
            String idxStr = el.getAttribute("data-slot-index");
            if (idxStr == null || idxStr.isEmpty()) continue;
            try {
                int idx = Integer.parseInt(idxStr);
                Element.DOMRect rect = el.getBoundingClientRect();
                Position docPos = new Position(rect.x, rect.y);
                Position screenPos = auiDocument.documentToScreenPosition(docPos);
                slotRects.put(idx, new SlotRect(
                        (int) Math.round(screenPos.x),
                        (int) Math.round(screenPos.y),
                        (int) Math.round(rect.width * scaleX),
                        (int) Math.round(rect.height * scaleY)
                ));
            } catch (NumberFormatException ignored) {
            }
        }

        // 更新玩家模型位置
        Element placeholder = auiDocument.getElementById("modelPlaceholder");
        if (placeholder != null) {
            Element.DOMRect rect = placeholder.getBoundingClientRect();
            Position docPos = new Position(rect.x, rect.y);
            Position screenPos = auiDocument.documentToScreenPosition(docPos);
            modelCenterX = (int) Math.round(screenPos.x + rect.width * scaleX / 2);
            // 原版 3D 小人渲染尺寸：占位区高度经视口缩放后再放大 2 倍（仅放大渲染，不影响布局占位）
            modelSize = (int) Math.round(rect.height * scaleY) * 2;
            // 模型底部对齐"等级"文字顶部（下移 10px）：读取等级元素顶部 Y，反推模型中心 Y
            int levelTopY = (int) Math.round(screenPos.y + rect.height * scaleY / 2);
            Element levelEl = auiDocument.getElementById("playerLevel");
            if (levelEl != null) {
                Element.DOMRect levelRect = levelEl.getBoundingClientRect();
                Position levelDocPos = new Position(levelRect.x, levelRect.y);
                Position levelScreenPos = auiDocument.documentToScreenPosition(levelDocPos);
                levelTopY = (int) Math.round(levelScreenPos.y);
            }
            modelCenterY = levelTopY - modelSize / 2;
        }
    }

    // ========================================================================
    // 渲染
    // ========================================================================

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
        this.renderBackground(graphics);

        // 1. 渲染 AUI 文档（面板、槽位背景、文字、滚动）
        if (auiDocument != null) {
            com.sighs.apricityui.viewport.ApricityViewport viewport = auiDocument.getViewport();
            if (viewport != null) {
                graphics.pose().pushPose();
                Mask.pushScissorScale(viewport.scissorScale());
                try {
                    graphics.pose().scale(viewport.renderScale(), viewport.renderScale(), 1.0f);
                    Base.drawScreenDocument(graphics.pose(), auiDocument);
                } finally {
                    Mask.popScissorScale();
                    graphics.pose().popPose();
                }
                Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
            }
        }

        // 2. 刷新布局（AUI 滚动后槽位位置会变化）
        refreshLayout();

        // 3. 渲染物品和交互（super.render 会调用 renderSlot / isHovering）
        super.render(graphics, mouseX, mouseY, partialTick);

        // 4. 渲染锁定槽位覆盖层
        renderLockedOverlays(graphics);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 玩家模型渲染（在 AUI 文档之上，物品图标之下）
        renderPlayerModel(graphics);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // 所有文字标签由 AUI 文档渲染，Java 侧不渲染
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics) {
        // 深色半透明背景
        graphics.fillGradient(0, 0, width, height, 0xCC0A0A0A, 0xCC0A0A0A);
    }

    // ========================================================================
    // 物品图标渲染（覆盖层）
    // ========================================================================

    @Override
    protected void renderSlot(@NotNull GuiGraphics graphics, Slot slot) {
        int index = menu.slots.indexOf(slot);
        SlotRect rect = slotRects.get(index);
        if (rect != null) {
            // 临时应用 AUI 位置以渲染物品图标
            int oldX = slot.x;
            int oldY = slot.y;
            slot.x = rect.x;
            slot.y = rect.y;
            super.renderSlot(graphics, slot);
            slot.x = oldX;
            slot.y = oldY;
        } else {
            super.renderSlot(graphics, slot);
        }
    }

    @Override
    protected boolean isHovering(Slot slot, double mouseX, double mouseY) {
        int index = menu.slots.indexOf(slot);
        SlotRect rect = slotRects.get(index);
        if (rect != null) {
            return mouseX >= rect.x && mouseX < rect.x + rect.width
                    && mouseY >= rect.y && mouseY < rect.y + rect.height;
        }
        return super.isHovering(slot, mouseX, mouseY);
    }

    // ========================================================================
    // 锁定槽位覆盖层
    // ========================================================================

    private void renderLockedOverlays(GuiGraphics graphics) {
        for (Slot slot : menu.slots) {
            if (menu.isLockedSlot(slot)) {
                int index = menu.slots.indexOf(slot);
                SlotRect rect = slotRects.get(index);
                if (rect != null) {
                    graphics.fill(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, 0x99000000);
                }
            }
        }
    }

    // ========================================================================
    // 玩家模型渲染
    // ========================================================================

    private void renderPlayerModel(GuiGraphics graphics) {
        if (minecraft == null || minecraft.player == null || modelSize <= 0) return;

        LivingEntity entity = minecraft.player;
        float lookX = (float) (modelCenterX - currentMouseX);
        float lookY = (float) (modelCenterY - 50 - currentMouseY);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, modelCenterX, modelCenterY, modelSize, lookX, lookY, entity);
    }

    // ========================================================================
    // 鼠标事件
    // ========================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 检查是否点击了关闭按钮
        if (button == 0 && auiDocument != null) {
            Element closeBtn = auiDocument.getElementById("btnClose");
            if (closeBtn != null) {
                Element.DOMRect rect = closeBtn.getBoundingClientRect();
                Position docPos = new Position(rect.x, rect.y);
                Position screenPos = auiDocument.documentToScreenPosition(docPos);
                double scaleX = auiDocument.getViewportScaleX();
                double scaleY = auiDocument.getViewportScaleY();
                int btnX = (int) Math.round(screenPos.x);
                int btnY = (int) Math.round(screenPos.y);
                int btnW = (int) Math.round(rect.width * scaleX);
                int btnH = (int) Math.round(rect.height * scaleY);
                if (mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                    onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ========================================================================
    // 窗口尺寸变化
    // ========================================================================

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        if (auiDocument != null) {
            auiDocument.applyViewport(true);
        }
        super.resize(minecraft, width, height);
    }

    @Override
    public void onClose() {
        if (auiDocument != null) {
            auiDocument.remove();
            auiDocument = null;
        }
        slotRects.clear();
        super.onClose();
    }

    // ========================================================================
    // 内部数据结构
    // ========================================================================

    /** 槽位屏幕坐标 */
    private static final class SlotRect {
        final int x, y, width, height;

        SlotRect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}