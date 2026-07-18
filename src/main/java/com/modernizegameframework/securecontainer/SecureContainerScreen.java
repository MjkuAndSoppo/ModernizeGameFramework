package com.modernizegameframework.securecontainer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * 安全箱右键打开界面
 * 投掷器风格，容器槽位居中，玩家背包在下方
 */
public class SecureContainerScreen extends AbstractContainerScreen<SecureContainerMenu> {

    private static final ResourceLocation CONTAINER_BG =
            ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");

    private final SecureContainerType type;

    public SecureContainerScreen(SecureContainerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.type = menu.getContainerType();
        this.imageHeight = menu.getScreenHeight();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 绘制背景
        graphics.blit(CONTAINER_BG, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 动态绘制容器槽位区域（覆盖默认的 3×3 槽位纹理）
        int cols = type.getCols();
        int rows = type.getRows();
        int slotStartX = (176 - cols * 18) / 2 + 1;
        int slotStartY = 18;

        // 用槽位背景色覆盖可能多余的默认槽位
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (row >= rows || col >= cols) {
                    // 这个位置没有槽位，用背景色覆盖
                    graphics.fill(x + slotStartX + col * 18 - 1, y + slotStartY + row * 18 - 1,
                            x + slotStartX + col * 18 + 17, y + slotStartY + row * 18 + 17,
                            0xFF8B8B8B);
                }
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}