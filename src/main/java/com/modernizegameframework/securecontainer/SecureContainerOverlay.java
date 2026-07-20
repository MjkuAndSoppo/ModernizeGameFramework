package com.modernizegameframework.securecontainer;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Optional;

/**
 * 安全箱物品栏左侧附加面板
 * 支持两种模式：
 * - 无 Curios：面板显示主槽位 + 容器格子
 * - 有 Curios：面板只显示容器格子，主槽位由 Curios 接管
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SecureContainerOverlay {

    private static final int PANEL_WIDTH = 110;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_SPACING = 0;
    private static final int SLOT_BG_U = 8;
    private static final int SLOT_BG_V = 84;

    private static int cachedPanelX = 0;
    private static int cachedPanelY = 0;
    private static int cachedPanelHeight = 0;
    private static int cachedMainSlotX = 0;
    private static int cachedMainSlotY = 0;
    private static int cachedContainerStartX = 0;
    private static int cachedContainerStartY = 0;
    private static SecureContainerType cachedType = null;
    private static boolean overlayVisible = false;
    /** 当前是否处于 Curios 模式 */
    private static boolean curiosMode = false;

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        if (!SecureContainerConfig.ENABLED.get()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        getInventory(player).ifPresent(inv -> {
            GuiGraphics graphics = event.getGuiGraphics();
            int invLeft = screen.getGuiLeft();
            int invTop = screen.getGuiTop();
            int gap = SecureContainerConfig.OVERLAY_GAP.get();

            int panelX = invLeft - PANEL_WIDTH - gap;
            int panelY = invTop;

            cachedPanelX = panelX;
            cachedPanelY = panelY;
            cachedPanelHeight = screen.getYSize();
            curiosMode = SecureContainerCurios.isCuriosLoaded();

            // 获取容器类型：Curios 模式下从 Curios 槽位读取，否则从能力读取
            ItemStack containerItem;
            if (curiosMode) {
                containerItem = SecureContainerCurios.getCuriosSecureContainer(player);
            } else {
                containerItem = inv.getContainerItem();
            }

            SecureContainerType type = null;
            if (!containerItem.isEmpty() && containerItem.getItem() instanceof SecureContainerItem sci) {
                type = sci.getType();
            }

            cachedType = type;
            overlayVisible = true;

            // 面板背景
            int bgColor = SecureContainerConfig.OVERLAY_BG_COLOR.get();
            graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + cachedPanelHeight, bgColor);

            // 标题
            Component title = Component.translatable("gui.modernizegameframework.secure_container.title");
            int titleWidth = mc.font.width(title);
            graphics.drawString(mc.font, title, panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + 6, 0xFFFFFF, false);

            // 分隔线
            graphics.fill(panelX + 4, panelY + 18, panelX + PANEL_WIDTH - 4, panelY + 19, 0xFF555555);

            // 容器格子起始 Y 坐标
            int gridStartY;

            if (curiosMode) {
                // Curios 模式：不显示主槽位，容器格子从分隔线下方开始
                gridStartY = panelY + 24;
            } else {
                // 非 Curios 模式：显示主槽位
                int mainSlotX = panelX + (PANEL_WIDTH - SLOT_SIZE) / 2;
                int mainSlotY = panelY + 22;
                cachedMainSlotX = mainSlotX;
                cachedMainSlotY = mainSlotY;

                drawSlotBackground(graphics, mainSlotX, mainSlotY);

                if (!containerItem.isEmpty()) {
                    graphics.renderItem(containerItem, mainSlotX + 1, mainSlotY + 1);
                    graphics.renderItemDecorations(mc.font, containerItem, mainSlotX + 1, mainSlotY + 1);
                }

                gridStartY = mainSlotY + SLOT_SIZE + 12;
            }

            // === 容器槽位 ===
            if (type != null) {
                int cols = type.getCols();
                int rows = type.getRows();

                int containerAreaWidth = cols * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING;
                int containerStartX = panelX + (PANEL_WIDTH - containerAreaWidth) / 2;
                int containerStartY = gridStartY;

                cachedContainerStartX = containerStartX;
                cachedContainerStartY = containerStartY;

                Component containerLabel = Component.translatable(
                        "item.modernizegameframework.secure_container." + type.getName());
                int containerLabelWidth = mc.font.width(containerLabel);
                graphics.drawString(mc.font, containerLabel,
                        panelX + (PANEL_WIDTH - containerLabelWidth) / 2,
                        containerStartY - 12, type.getColor() | 0xFF000000, false);

                ItemStackHandler handler = inv.getInventory(type);
                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < cols; col++) {
                        int slotX = containerStartX + col * (SLOT_SIZE + SLOT_SPACING);
                        int slotY = containerStartY + row * (SLOT_SIZE + SLOT_SPACING);

                        drawSlotBackground(graphics, slotX, slotY);

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

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        if (!overlayVisible) return;

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        int button = event.getButton();
        boolean shift = Screen.hasShiftDown();

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // === 面板区域内的点击：由 overlay 接管 ===
        if (isInPanel(mouseX, mouseY)) {
            event.setCanceled(true);

            // 非 Curios 模式才处理主槽位点击
            if (!curiosMode && isInSlot(mouseX, mouseY, cachedMainSlotX, cachedMainSlotY)) {
                sendOverlayClickPacket(-1, button, shift);
                return;
            }

            // 容器槽位（两种模式都处理）
            if (cachedType != null) {
                int cols = cachedType.getCols();
                int rows = cachedType.getRows();

                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < cols; col++) {
                        int slotX = cachedContainerStartX + col * (SLOT_SIZE + SLOT_SPACING);
                        int slotY = cachedContainerStartY + row * (SLOT_SIZE + SLOT_SPACING);

                        if (isInSlot(mouseX, mouseY, slotX, slotY)) {
                            int index = row * cols + col;
                            sendOverlayClickPacket(index, button, shift);
                            return;
                        }
                    }
                }
            }
            return;
        }

        // === 面板区域外：shift+右键点击原版玩家物品栏槽位，快速移入安全箱 ===
        if (shift && button == 1) {
            findPlayerInventorySlot(screen, player, mouseX, mouseY).ifPresent(slot -> {
                event.setCanceled(true);
                SecureContainerNetwork.CHANNEL.sendToServer(
                        new SecureContainerNetwork.InventoryQuickMovePacket(slot.index));
            });
        }
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;
        if (!overlayVisible) return;

        if (isInPanel(event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof InventoryScreen) {
            overlayVisible = false;
            cachedType = null;
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // 打开物品栏时主动向服务端请求最新安全箱数据，确保 overlay 立即显示
        SecureContainerNetwork.CHANNEL.sendToServer(new SecureContainerNetwork.RequestSyncPacket());
    }

    // ==================== 工具方法 ====================

    private static void sendOverlayClickPacket(int slotIndex, int button, boolean shift) {
        SecureContainerNetwork.CHANNEL.sendToServer(
                new SecureContainerNetwork.OverlayClickPacket(slotIndex, button, shift));
    }

    private static Optional<Slot> findPlayerInventorySlot(InventoryScreen screen, LocalPlayer player,
                                                          double mouseX, double mouseY) {
        int guiLeft = screen.getGuiLeft();
        int guiTop = screen.getGuiTop();
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) continue;
            int x = guiLeft + slot.x;
            int y = guiTop + slot.y;
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                // 只处理玩家主背包/快捷栏槽位（排除装备、合成、副手等）
                if (slot.container == player.getInventory() && slot.getSlotIndex() < 36) {
                    return Optional.of(slot);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<SecureContainerInventory> getInventory(LocalPlayer player) {
        return player.getCapability(SecureContainerRegistry.SECURE_CONTAINER_CAPABILITY).resolve();
    }

    private static boolean isInPanel(double mouseX, double mouseY) {
        return mouseX >= cachedPanelX && mouseX < cachedPanelX + PANEL_WIDTH
                && mouseY >= cachedPanelY && mouseY < cachedPanelY + cachedPanelHeight;
    }

    private static boolean isInSlot(double mouseX, double mouseY, int slotX, int slotY) {
        return mouseX >= slotX && mouseX < slotX + SLOT_SIZE
                && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
    }

    private static void drawSlotBackground(GuiGraphics graphics, int x, int y) {
        graphics.blit(InventoryScreen.INVENTORY_LOCATION, x, y, SLOT_BG_U, SLOT_BG_V,
                SLOT_SIZE, SLOT_SIZE, 256, 256);
    }
}
