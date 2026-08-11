package com.modernizegameframework.hollowhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * 藏身处照明工作方块
 * 右键打开照明等级面板，仅能在藏身处维度内触发
 * 被红石信号激活且供电站发电时，按选中等级点亮私人区域
 */
public class LightingBlock extends Block {

    public LightingBlock(Properties properties) {
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

        int lightingLevel = data.getWorkBlockLevel(HollowHouseWorkBlockType.LIGHTING.getId());
        if (lightingLevel <= 0) {
            player.displayClientMessage(Component.literal("§c照明尚未解锁，请在控制箱中解锁"), true);
            return InteractionResult.CONSUME;
        }

        // 记录照明方块位置，用于红石检测与扩散中心
        data.setLightingPos(pos);
        data.getLightingData().setSelectedLevel(Math.min(data.getLightingData().getSelectedLevel(), lightingLevel));

        // 打开照明面板
        LightingNetwork.openScreen(serverPlayer, lightingLevel);

        return InteractionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                @NotNull Block neighborBlock, @NotNull BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        // 红石信号变化时由 HollowHouseEvents 中的 tick 处理实际点亮逻辑，
        // 这里仅做最小触发，避免频繁操作世界方块
    }
}
