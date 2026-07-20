package com.modernizegameframework.stamina;

import com.modernizegameframework.ModernizeGameFramework;
import com.modernizegameframework.movement.MovementNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 体力值系统 Forge 事件总线监听器
 * 处理玩家 tick、行为消耗、恢复、惩罚与同步
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID)
public class StaminaEvents {

    /**
     * 为玩家实体附加体力值能力
     */
    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!StaminaConfig.ENABLED.get()) return;
        if (event.getObject() instanceof Player player) {
            event.addCapability(ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "stamina"), new StaminaProvider(player));
        }
    }

    /**
     * 玩家进入世界时同步体力数据
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!StaminaConfig.ENABLED.get()) return;
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Player player) {
            StaminaNetwork.syncToClient(player);
        }
    }

    /**
     * 每 tick 处理体力消耗、恢复与疾跑控制
     * 消耗触发条件：玩家正在疾跑（按住 Ctrl 或双击 W）
     * 在 START 阶段清空跳跃标记，END 阶段处理消耗
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!StaminaConfig.ENABLED.get()) return;
        Player player = event.player;
        if (player.level().isClientSide) return;

        // START 阶段清空跳跃消耗标记（用于跨系统防重复扣费）
        if (event.phase == TickEvent.Phase.START) {
            MovementNetwork.JUMP_CONSUMED_THIS_TICK.remove(player.getUUID());
            return;
        }

        StaminaHelper.getStamina(player).ifPresent(stamina -> {
            // 玩家正在疾跑时扣除体力（手持基岩时豁免）
            if (player.isSprinting() && !hasBedrockInHand(player)) {
                stamina.onActionConsume(StaminaConfig.SPRINT_COST.get());
            }

            // 体力耗尽后强制停止疾跑
            if (stamina.isDepleted() && player.isSprinting()) {
                player.setSprinting(false);
            }

            // 自然恢复与溢出修正
            stamina.tick();

            // 同步到客户端
            if (stamina.pollDirty()) {
                StaminaNetwork.syncToClient(player);
            }
        });
    }

    /**
     * 跳跃时消耗体力，体力耗尽则撤销跳跃 Y 速度（手持基岩时豁免）
     * 注意：LivingJumpEvent 不可取消，只能通过撤销 Y 速度实现禁止跳跃
     * 若本 tick 已通过 MovementNetwork.BhopConsumePacket 扣过体力，跳过重复扣除
     */
    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!StaminaConfig.ENABLED.get()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        StaminaHelper.getStamina(player).ifPresent(stamina -> {
            if (stamina.isDepleted()) {
                // 体力耗尽时禁止跳跃：撤销 Y 速度（LivingJumpEvent 不可 cancel）
                Vec3 d = player.getDeltaMovement();
                player.setDeltaMovement(d.x, 0.0, d.z);
                return;
            }
            // 本 tick 已通过移动系统 packet 扣过体力，跳过重复扣除
            if (MovementNetwork.JUMP_CONSUMED_THIS_TICK.contains(player.getUUID())) {
                return;
            }
            // 普通跳跃消耗（手持基岩时豁免）
            if (!hasBedrockInHand(player)) {
                stamina.onActionConsume(StaminaConfig.JUMP_COST.get());
            }
        });
    }

    /**
     * 攻击时消耗体力（手持基岩时豁免）
     */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!StaminaConfig.ENABLED.get()) return;
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (hasBedrockInHand(player)) return;
        StaminaHelper.consume(player, StaminaConfig.ATTACK_COST.get());
    }

    /**
     * 低体力时减少 25% 伤害输出
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!StaminaConfig.ENABLED.get()) return;
        if (event.getSource().getEntity() instanceof Player player) {
            if (player.level().isClientSide) return;
            StaminaHelper.getStamina(player).ifPresent(stamina -> {
                if (stamina.isLow()) {
                    event.setAmount(event.getAmount() * 0.75f);
                }
            });
        }
    }

    /**
     * 玩家死亡重生后重置体力
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!StaminaConfig.ENABLED.get()) return;
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        StaminaHelper.getStamina(player).ifPresent(stamina -> {
            stamina.setCurrent(stamina.getMax());
        });
        StaminaNetwork.syncToClient(player);
    }

    /**
     * 检测玩家主手或副手是否持有基岩
     * 手持基岩时所有体力消耗行为均豁免
     *
     * @param player 玩家
     * @return 是否手持基岩
     */
    private static boolean hasBedrockInHand(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return mainHand.is(Blocks.BEDROCK.asItem()) || offHand.is(Blocks.BEDROCK.asItem());
    }
}
