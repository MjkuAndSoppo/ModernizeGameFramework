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
 * 藏身处医疗站方块
 * 右键打开医疗站界面，仅能在藏身处维度内触发。
 */
public class MedicalBlock extends Block {

    public MedicalBlock(Properties properties) {
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

        int medicalLevel = data.getWorkBlockLevel(HollowHouseWorkBlockType.MEDICAL.getId());
        if (medicalLevel <= 0) {
            player.displayClientMessage(Component.literal("§c医疗站尚未解锁，请在控制箱中解锁"), true);
            return InteractionResult.CONSUME;
        }

        // 打开医疗站界面
        MedicalStationNetwork.openScreen(serverPlayer, medicalLevel);

        return InteractionResult.CONSUME;
    }
}
