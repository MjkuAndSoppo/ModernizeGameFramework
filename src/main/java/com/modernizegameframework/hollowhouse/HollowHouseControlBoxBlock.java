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

import java.util.UUID;

/**
 * 藏身处控制箱方块
 * 多功能面板：右键可查看已邀请玩家并退出藏身处
 */
public class HollowHouseControlBoxBlock extends Block {

    public HollowHouseControlBoxBlock(Properties properties) {
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

        // 显示当前已邀请玩家列表
        StringBuilder inviteList = new StringBuilder("§6当前已邀请玩家：");
        if (data.getInvitedPlayers().isEmpty()) {
            inviteList.append("§7无");
        } else {
            for (UUID id : data.getInvitedPlayers()) {
                inviteList.append("\n§f- §7").append(id.toString());
            }
        }
        serverPlayer.sendSystemMessage(Component.literal(inviteList.toString()));

        // 若玩家在藏身处内，则右键控制箱退出藏身处
        if (level.dimension() == HollowHouseDimensionManager.HOLLOW_HOUSE_DIMENSION) {
            HollowHouseDimensionManager.leaveHollowHouse(serverPlayer);
        }

        return InteractionResult.CONSUME;
    }
}
