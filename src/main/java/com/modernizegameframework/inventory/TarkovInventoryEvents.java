package com.modernizegameframework.inventory;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 塔科夫背包系统事件监听器
 * 负责拦截原版背包/容器打开、玩家登录与重生数据同步
 */
public class TarkovInventoryEvents {

    /** 记录需要重建塔科夫背包界面的玩家 UUID，由服务端 tick 事件延迟处理 */
    private static final Map<UUID, Boolean> pendingRebuild = new HashMap<>();

    /**
     * 标记指定玩家需要在下一服务端 tick 重建塔科夫背包界面
     */
    public static void markForRebuild(UUID playerId) {
        pendingRebuild.put(playerId, Boolean.TRUE);
    }

    /**
     * 服务端事件：打开容器方块时替换为塔科夫背包界面
     */
    @Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID)
    public static class ServerEvents {

        /**
         * 玩家登录时同步塔科夫背包数据到客户端
         */
        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                TarkovInventoryNetwork.syncAll(player);
            }
        }

        /**
         * 玩家死亡/切换维度后同步塔科夫背包数据到客户端
         */
        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            Player newPlayer = event.getEntity();
            if (newPlayer instanceof ServerPlayer serverPlayer) {
                TarkovInventoryNetwork.syncAll(serverPlayer);
            }
        }

        /**
         * 服务端 tick 结束时处理被标记需要重建界面的玩家
         * 延迟一 tick 重建可避免在 slot 操作过程中直接切换菜单导致的状态异常
         */
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Iterator<Map.Entry<UUID, Boolean>> iterator = pendingRebuild.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Boolean> entry = iterator.next();
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
                if (player != null && player.containerMenu instanceof TarkovInventoryMenu menu) {
                    NetworkHooks.openScreen(player,
                            new TarkovInventoryMenuProvider(menu.getExternalContainer(), menu.getExternalTitle()),
                            TarkovInventoryMenuProvider.extraDataWriter(menu.getContainerSlotCount(), menu.getExternalTitle()));
                }
                iterator.remove();
            }
        }

        /**
         * 右键指定容器方块时打开塔科夫背包界面，并在右侧显示该容器。
         * 仅覆盖箱子、陷阱箱、末影箱、潜影盒与木桶，其他方块保持原版界面。
         */
        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            if (event.getLevel().isClientSide()) {
                return;
            }
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }

            Level level = event.getLevel();
            BlockPos pos = event.getPos();
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();

            if (!isSupportedContainerBlock(block)) {
                return;
            }

            Container container = getContainerFromBlock(level, pos, state, player);
            if (container == null || container.getContainerSize() <= 0) {
                return;
            }

            Component title = getContainerTitle(level, pos, state, container);

            // 先同步装备槽与扩展格数据，再打开界面
            TarkovInventoryNetwork.syncAll(player);

            NetworkHooks.openScreen(player, new TarkovInventoryMenuProvider(container, title), buf -> {
                buf.writeInt(container.getContainerSize());
                buf.writeComponent(title);
            });

            event.setCanceled(true);
        }

        /**
         * 判断是否属于需要被塔科夫背包覆盖的容器方块
         */
        private static boolean isSupportedContainerBlock(Block block) {
            return block instanceof ChestBlock
                    || block instanceof EnderChestBlock
                    || block instanceof ShulkerBoxBlock
                    || block instanceof BarrelBlock;
        }

        /**
         * 根据方块类型获取对应的容器实例
         */
        private static Container getContainerFromBlock(Level level, BlockPos pos, BlockState state, ServerPlayer player) {
            Block block = state.getBlock();
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (block instanceof ChestBlock) {
                if (blockEntity instanceof ChestBlockEntity chestEntity) {
                    Direction connection = ChestBlock.getConnectedDirection(state);
                    BlockPos otherPos = pos.relative(connection);
                    BlockEntity otherBE = level.getBlockEntity(otherPos);
                    if (otherBE instanceof ChestBlockEntity otherChest) {
                        return new CompoundContainer(chestEntity, otherChest);
                    }
                    return chestEntity;
                }
                return null;
            }

            if (block instanceof EnderChestBlock) {
                return player.getEnderChestInventory();
            }

            if (blockEntity instanceof Container container) {
                return container;
            }

            return null;
        }

        /**
         * 根据方块与容器获取容器标题
         */
        private static Component getContainerTitle(Level level, BlockPos pos, BlockState state, Container container) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Nameable nameable && nameable.hasCustomName()) {
                return nameable.getDisplayName();
            }

            Block block = state.getBlock();
            if (block instanceof ChestBlock) {
                return Component.translatable(container.getContainerSize() > 27 ? "container.chestDouble" : "container.chest");
            }
            if (block instanceof EnderChestBlock) {
                return Component.translatable("container.enderchest");
            }
            if (block instanceof ShulkerBoxBlock) {
                return Component.translatable("container.shulkerBox");
            }
            if (block instanceof BarrelBlock) {
                return Component.translatable("container.barrel");
            }
            return Component.translatable("container.inventory");
        }
    }

    /**
     * 客户端事件：拦截原版背包界面并替换为塔科夫背包界面
     */
    @Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID, value = Dist.CLIENT)
    public static class ClientEvents {

        /**
         * 当游戏尝试打开原版 InventoryScreen 时，取消并请求打开塔科夫背包
         */
        @SubscribeEvent
        public static void onGuiOpen(ScreenEvent.Opening event) {
            if (!(event.getScreen() instanceof InventoryScreen)) {
                return;
            }
            // 创造模式背包保持原版，不被替换
            if (event.getScreen() instanceof CreativeModeInventoryScreen) {
                return;
            }
            // 玩家处于创造模式时也不替换
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.isCreative()) {
                return;
            }
            if (event.getScreen() instanceof TarkovInventoryScreen) {
                return;
            }

            event.setCanceled(true);
            TarkovInventoryNetwork.CHANNEL.sendToServer(new TarkovInventoryNetwork.OpenInventoryPacket());
        }
    }
}
