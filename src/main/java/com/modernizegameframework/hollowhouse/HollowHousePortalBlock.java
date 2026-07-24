package com.modernizegameframework.hollowhouse;

import net.minecraft.core.BlockPos;
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
 * 藏身处入口方块
 * 在主世界右键进入藏身处，在藏身处内右键退出藏身处
 */
public class HollowHousePortalBlock extends Block {

    public HollowHousePortalBlock(Properties properties) {
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

        HollowHouseData data = HollowHouseDimensionManager.getData(serverPlayer);
        if (data == null) {
            return InteractionResult.PASS;
        }

        if (level.dimension() == HollowHouseDimensionManager.HOLLOW_HOUSE_DIMENSION) {
            // 在藏身处维度内，右键入口方块返回原位置
            HollowHouseDimensionManager.leaveHollowHouse(serverPlayer);
        } else {
            // 在其他维度，右键入口方块进入藏身处
            HollowHouseDimensionManager.enterHollowHouse(serverPlayer);
        }

        return InteractionResult.CONSUME;
    }
}
