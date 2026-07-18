package com.modernizegameframework.securecontainer;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

/**
 * 安全箱容器物品
 * 手持右键打开对应容器的库存界面
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
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // 构造菜单标题
            Component title = Component.translatable(
                    "container.modernizegameframework.secure_container." + type.getName());

            MenuProvider provider = new SimpleMenuProvider(
                    (id, inv, p) -> new SecureContainerMenu(id, inv, type),
                    title);

            NetworkHooks.openScreen(serverPlayer, provider, buf -> buf.writeEnum(type));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.translatable(
                "item.modernizegameframework.secure_container." + type.getName());
    }
}