package com.modernizegameframework.securecontainer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 安全箱容器物品
 * 仅通过物品栏附加面板访问容器内容，右键不再打开界面
 * 每种容器类型有独立的物品实例
 */
public class SecureContainerItem extends Item {

    private final SecureContainerType type;

    public SecureContainerItem(SecureContainerType type) {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE));
        this.type = type;
    }

    /**
     * 获取此物品对应的容器类型
     */
    public SecureContainerType getType() {
        return type;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        // 右键不再打开安全箱界面，交由原版处理
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.translatable(
                "item.modernizegameframework.secure_container." + type.getName());
    }
}