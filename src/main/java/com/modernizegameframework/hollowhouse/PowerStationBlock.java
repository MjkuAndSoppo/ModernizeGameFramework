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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * 藏身处供电站方块
 * 右键打开供电站界面，仅能在藏身处维度内触发
 */
public class PowerStationBlock extends Block {

    /**
     * 发光状态属性
     */
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public PowerStationBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
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

        int powerLevel = data.getWorkBlockLevel(HollowHouseWorkBlockType.POWER.getId());
        if (powerLevel <= 0) {
            player.displayClientMessage(Component.literal("§c供电站尚未解锁，请在控制箱中解锁"), true);
            return InteractionResult.CONSUME;
        }

        // 确保燃油槽位数量与等级一致
        data.getPowerStationData().setFuelSlotCount(powerLevel);

        // 记录供电站方块位置，用于发光与音效
        data.setPowerStationPos(pos);

        // 打开供电站界面
        PowerStationNetwork.openScreen(serverPlayer, powerLevel);

        return InteractionResult.CONSUME;
    }
}
