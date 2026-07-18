package com.modernizegameframework.securecontainer;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Optional;

/**
 * 安全箱物品栏左侧附加面板
 * 当玩家打开物品栏（按 E）时，在左侧显示主槽位和容器库存格子
 *
 * 交互逻辑采用服务端权威模式：
 * - 客户端只发送点击意图
 * - 服务端执行操作并把结果同步回客户端
 * - 客户端不做乐观更新，避免与服务端状态不一致导致物品复制
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SecureContainerOverlay {

    /** 面板宽度 */
    private static final int PANEL_WIDTH = 110;
    /** 槽位大小 */
    private static final int SLOT_SIZE = 18;
    /** 槽位间距 */
    private static final int SLOT_SPACING = 0;

    /** 缓存的 overlay 区域，用于点击检测 */
    private static int cachedOverlayX = 0;
    private static int cachedOverlayY = 0;
    private static int cachedMainSlotX = 0;
    private static int cachedMainSlotY = 0;
    private static int cachedContainerStartX = 0;
    private static int cachedContainerStartY = 0;
    private static SecureContainerType cachedType = null;
    private static boolean overlayVisible = false;

    /**
     * 在物品栏界面渲染之后绘制附加面板
     */
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        if (!MovementConfig_Access.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        getInventory(player).ifPresent(inv -> {
            GuiGraphics graphics = event.getGuiGraphics();
            int invLeft = screen.getGuiLeft();
            int invTop = screen.getGuiTop();
            int gap = SecureContainerConfig.OVERLAY_GAP.get();

            // 面板位置
            int panelX = invLeft - PANEL_WIDTH - gap;
            int panelY = invTop;

            cachedOverlayX = panelX;
            cachedOverlayY = panelY;

            // 获取当前容器类型
            ItemStack containerItem = inv.getContainerItem();
            SecureContainerType type = null;
            if (!containerItem.isEmpty() && containerItem.getItem() instanceof SecureContainerItem sci) {
                type = sci.getType();
            }

            cachedType = type;
            overlayVisible = true;

            // 绘制面板背景
            int bgColor = SecureContainerConfig.OVERLAY_BG_COLOR.get();
            graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + screen.getYSize(), bgColor);

            // 绘制标题
            Component title = Component.translatable("gui.modernizegameframework.secure_container.title");
            int titleWidth = mc.font.width(title);
            graphics.drawString(mc.font, title, panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + 6, 0xFFFFFF, false);

            // 绘制分隔线
            graphics.fill(panelX + 4, panelY + 18, panelX + PANEL_WIDTH - 4, panelY + 19, 0xFF555555);

            // === 主槽位 ===
            int mainSlotX = panelX + (PANEL_WIDTH - SLOT_SIZE) / 2;
            int mainSlotY = panelY + 26;
            cachedMainSlotX = mainSlotX;
            cachedMainSlotY = mainSlotY;

            // 主槽位背景
            drawSlotBackground(graphics, mainSlotX, mainSlotY, 0xFF666666);

            // 主槽位物品
            if (!containerItem.isEmpty()) {
                graphics.renderItem(containerItem, mainSlotX + 1, mainSlotY + 1);
                graphics.renderItemDecorations(mc.font, containerItem, mainSlotX + 1, mainSlotY + 1);
            }

            // 主槽位标签
            Component mainLabel = Component.translatable("gui.modernizegameframework.secure_container.main_slot");
            int mainLabelWidth = mc.font.width(mainLabel);
            graphics.drawString(mc.font, mainLabel, panelX + (PANEL_WIDTH - mainLabelWidth) / 2,
                    mainSlotY + SLOT_SIZE + 2, 0xAAAAAA, false);

            // === 容器槽位（仅当有容器时显示） ===
            if (type != null) {
                int cols = type.getCols();
                int rows = type.getRows();

                // 容器槽位区域居中
                int containerAreaWidth = cols * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING;
                int containerStartX = panelX + (PANEL_WIDTH - containerAreaWidth) / 2;
                int containerStartY = mainSlotY + SLOT_SIZE + 16;

                cachedContainerStartX = containerStartX;
                cachedContainerStartY = containerStartY;

                // 容器标签
                Component containerLabel = Component.translatable(
                        "item.modernizegameframework.secure_container." + type.getName());
                int containerLabelWidth = mc.font.width(containerLabel);
                graphics.drawString(mc.font, containerLabel,
                        panelX + (PANEL_WIDTH - containerLabelWidth) / 2,
                        containerStartY - 12, type.getColor() | 0xFF000000, false);

                // 绘制容器槽位
                ItemStackHandler handler = inv.getInventory(type);
                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < cols; col++) {
                        int slotX = containerStartX + col * (SLOT_SIZE + SLOT_SPACING);
                        int slotY = containerStartY + row * (SLOT_SIZE + SLOT_SPACING);

                        drawSlotBackground(graphics, slotX, slotY, 0xFF8B8B8B);

                        int index = row * cols + col;
                        ItemStack stack = handler.getStackInSlot(index);
                        if (!stack.isEmpty()) {
                            graphics.renderItem(stack, slotX + 1, slotY + 1);
                            graphics.renderItemDecorations(mc.font, stack, slotX + 1, slotY + 1);
                        }
                    }
                }
            }
        });
    }

    /**
     * 处理鼠标点击事件，拦截附加面板区域的点击
     */
    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;
        if (!overlayVisible) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        int button = event.getButton();

        // 只处理左键点击
        if (button != 0) return;

        // 检查是否点击了主槽位
        if (isInSlot(mouseX, mouseY, cachedMainSlotX, cachedMainSlotY)) {
            sendOverlayClickPacket(player, -1);
            event.setCanceled(true);
            return;
        }

        // 检查是否点击了容器槽位
        if (cachedType != null) {
            int cols = cachedType.getCols();
            int rows = cachedType.getRows();

            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    int slotX = cachedContainerStartX + col * (SLOT_SIZE + SLOT_SPACING);
                    int slotY = cachedContainerStartY + row * (SLOT_SIZE + SLOT_SPACING);

                    if (isInSlot(mouseX, mouseY, slotX, slotY)) {
                        int index = row * cols + col;
                        sendOverlayClickPacket(player, index);
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
    }

    /**
     * 发送附加面板点击包到服务端
     * 客户端不自行修改状态，等待服务端同步结果
     */
    private static void sendOverlayClickPacket(LocalPlayer player, int slotIndex) {
        ItemStack carried = player.containerMenu.getCarried();
        SecureContainerNetwork.CHANNEL.sendToServer(
                new SecureContainerNetwork.OverlayClickPacket(slotIndex, carried.copy()));
    }

    /**
     * 在物品栏界面关闭时重置状态
     */
    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof InventoryScreen) {
            overlayVisible = false;
            cachedType = null;
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 获取客户端玩家的安全箱库存能力
     */
    private static Optional<SecureContainerInventory> getInventory(LocalPlayer player) {
        return player.getCapability(SecureContainerRegistry.SECURE_CONTAINER_CAPABILITY).resolve();
    }

    /**
     * 判断鼠标坐标是否在指定槽位区域内
     */
    private static boolean isInSlot(double mouseX, double mouseY, int slotX, int slotY) {
        return mouseX >= slotX && mouseX < slotX + SLOT_SIZE
                && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
    }

    /**
     * 绘制槽位背景
     */
    private static void drawSlotBackground(GuiGraphics graphics, int x, int y, int color) {
        // 槽位外边框（深色）
        graphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, 0xFF373737);
        // 槽位内部（浅色）
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF8B8B8B);
        // 槽位内边框（更暗）
        graphics.fill(x, y, x + SLOT_SIZE, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + SLOT_SIZE, 0xFF373737);
    }

    /**
     * 临时访问 MovementConfig.ENABLED，避免循环依赖
     */
    private static class MovementConfig_Access {
        static boolean isEnabled() {
            return com.modernizegameframework.movement.MovementConfig.ENABLED.get();
        }
    }
}