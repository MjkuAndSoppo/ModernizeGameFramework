package com.modernizegameframework.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * 塔科夫背包菜单提供者
 * 用于服务端通过 NetworkHooks.openScreen 打开自定义背包界面
 */
public class TarkovInventoryMenuProvider implements MenuProvider {

    private final Container externalContainer;
    private final Component title;

    /**
     * @param externalContainer 右侧容器，可为 null（表示自己背包）
     * @param title             界面标题
     */
    public TarkovInventoryMenuProvider(Container externalContainer, Component title) {
        this.externalContainer = externalContainer;
        this.title = title;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return title;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new TarkovInventoryMenu(id, playerInv, externalContainer, title);
    }

    /**
     * 生成用于 NetworkHooks.openScreen 的额外数据写入器
     *
     * @param containerSlotCount 右侧容器槽位数量，无容器时为 0
     * @param title              右侧容器标题
     */
    public static Consumer<FriendlyByteBuf> extraDataWriter(int containerSlotCount, Component title) {
        return buf -> {
            buf.writeInt(containerSlotCount);
            buf.writeComponent(title);
        };
    }
}
