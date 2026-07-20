package com.modernizegameframework.bodypart;

import com.modernizegameframework.Config;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 止痛药物品
 * 右键使用可清除疼痛效果并获得止痛药屏蔽效果，受配置冷却限制
 */
public class PainkillerItem extends Item {

    /**
     * 止痛药冷却记录的 NBT 键名
     */
    private static final String COOLDOWN_TAG = "mgfPainkillerCooldown";

    public PainkillerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 客户端直接返回消耗，实际效果在服务端处理
        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }

        long gameTime = level.getGameTime();
        long lastUsed = player.getPersistentData().getLong(COOLDOWN_TAG);
        int cooldown = Config.BODYPART_PAINKILLER_COOLDOWN.get();

        if (gameTime - lastUsed < cooldown) {
            long remainingTicks = cooldown - (gameTime - lastUsed);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§c止痛药冷却中，还需 §e" + (remainingTicks / 20) + "§c 秒"),
                    true);
            return InteractionResultHolder.fail(stack);
        }

        // 清除疼痛并添加止痛药效果
        player.removeEffect(BodyPartEffects.PAIN.get());
        int duration = Config.BODYPART_PAINKILLER_DURATION.get();
        if (duration > 0) {
            player.addEffect(new MobEffectInstance(BodyPartEffects.PAINKILLER.get(), duration, 0, false, false, true));
        }

        player.getPersistentData().putLong(COOLDOWN_TAG, gameTime);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.consume(stack);
    }
}
