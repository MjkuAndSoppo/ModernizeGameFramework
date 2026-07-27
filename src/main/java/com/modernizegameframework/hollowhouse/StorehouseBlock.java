package com.modernizegameframework.hollowhouse;

import com.modernizegameframework.inventory.TarkovInventoryMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

/**
 * 藏身处仓库方块
 * 右键打开塔科夫背包界面，仓库内容显示在右侧栏；
 * 仅能在藏身处维度内触发。
 */
public class StorehouseBlock extends Block {

    public StorehouseBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level,
                                          @NotNull BlockPos pos, @NotNull Player player,
                                          @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!HollowHouseConfig.ENABLED.get()) {
            return InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        // 工作方块仅在藏身处维度内生效
        if (level.dimension() != HollowHouseDimensionManager.HOLLOW_HOUSE_DIMENSION) {
            return InteractionResult.PASS;
        }

        HollowHouseData data = HollowHouseDimensionManager.getData(serverPlayer);
        if (data == null) {
            return InteractionResult.PASS;
        }

        int storehouseLevel = HollowHouseStorehouseHelper.getStorehouseLevel(serverPlayer);
        if (storehouseLevel <= 0) {
            player.displayClientMessage(Component.literal("§c仓库尚未解锁，请在控制箱中解锁"), true);
            return InteractionResult.CONSUME;
        }

        // 构建当前等级可见的仓库容器
        SimpleContainer storehouseContainer = HollowHouseStorehouseHelper.createVisibleStorehouseContainer(data);

        // 监听容器变化，实时同步回最大容量容器
        storehouseContainer.addListener(container -> HollowHouseStorehouseHelper.syncBackToStorehouse(data, (SimpleContainer) container));

        // 打开塔科夫背包界面，仓库作为右侧容器
        Component title = Component.literal("藏身处仓库 (Lv." + storehouseLevel + ")");
        NetworkHooks.openScreen(serverPlayer,
                new TarkovInventoryMenuProvider(storehouseContainer, title),
                TarkovInventoryMenuProvider.extraDataWriter(storehouseContainer.getContainerSize(), title));

        return InteractionResult.CONSUME;
    }
}
