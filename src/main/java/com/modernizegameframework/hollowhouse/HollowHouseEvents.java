package com.modernizegameframework.hollowhouse;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 藏身处系统游戏事件监听器
 * 处理怪物生成禁用、PVP、掉落保护、死亡重生、天气等规则
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID)
public class HollowHouseEvents {

    private static final ResourceLocation CAPABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "hollow_house_data");

    /**
     * 为玩家实体附加藏身处数据能力
     * 无论开关是否开启都附加，避免开关切换时已在线玩家丢失能力
     */
    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof Player) {
            event.addCapability(CAPABILITY_ID, new HollowHouseDataProvider());
        }
    }

    /**
     * 禁止怪物在藏身处维度自然生成
     */
    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!HollowHouseConfig.ENABLED.get()) {
            return;
        }
        if (event.getLevel() instanceof Level level
                && level.dimension() == HollowHouseDimensionManager.HOLLOW_HOUSE_DIMENSION
                && event.getSpawnType() != MobSpawnType.COMMAND
                && event.getSpawnType() != MobSpawnType.SPAWN_EGG) {
            event.setSpawnCancelled(true);
        }
    }

    /**
     * 玩家掉入虚空时传送回平台中心
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!HollowHouseConfig.ENABLED.get()) {
            return;
        }
        Entity entity = event.getEntity();
        Level level = event.getLevel();
        if (level.dimension() != HollowHouseDimensionManager.HOLLOW_HOUSE_DIMENSION) {
            return;
        }
        if (entity instanceof ServerPlayer player && player.getY() < 0) {
            HollowHouseData data = HollowHouseDimensionManager.getData(player);
            if (data != null && data.isCreated()) {
                BlockPos center = HollowHouseDimensionManager.getCurrentHollowHouseCenter(player, data);
                HollowHouseDimensionManager.teleportToHollowHouseCenter(player, center);
            }
        }
    }

    /**
     * 实时监测藏身处内玩家坐标
     * 当玩家 Y 坐标小于 0 或脱离藏身处中心 ±32 格范围时，立即传送回中心
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }
        if (!HollowHouseConfig.ENABLED.get()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().dimension() != HollowHouseDimensionManager.HOLLOW_HOUSE_DIMENSION) {
            return;
        }

        HollowHouseData data = HollowHouseDimensionManager.getData(player);
        if (data == null || !data.isCreated()) {
            return;
        }

        BlockPos center = HollowHouseDimensionManager.getCurrentHollowHouseCenter(player, data);
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        if (y < 0 || HollowHouseDimensionManager.isOutsideHollowHouseArea(center, x, y, z)) {
            HollowHouseDimensionManager.teleportToHollowHouseCenter(player, center);
        }

        // 医疗站生产任务每秒推进一次（每 20 tick）
        tickMedicalTasks(player, data);
    }

    /**
     * 医疗站任务 tick 计数器，按玩家维度索引
     */
    private static final java.util.Map<java.util.UUID, Integer> medicalTaskTickCounters = new java.util.HashMap<>();

    /**
     * 推进医疗站生产任务进度
     */
    private static void tickMedicalTasks(ServerPlayer player, HollowHouseData data) {
        int counter = medicalTaskTickCounters.getOrDefault(player.getUUID(), 0) + 1;
        medicalTaskTickCounters.put(player.getUUID(), counter);
        if (counter < 20) {
            return;
        }
        medicalTaskTickCounters.put(player.getUUID(), 0);

        java.util.Iterator<MedicalTask> iterator = data.getMedicalTasks().iterator();
        boolean changed = false;
        while (iterator.hasNext()) {
            MedicalTask task = iterator.next();
            ItemStack output = task.tick();
            if (!output.isEmpty()) {
                // 将产出的医疗物品放入仓库
                HollowHouseStorehouseHelper.addItem(data, output);
                changed = true;
            }
            if (task.isFinished()) {
                iterator.remove();
                changed = true;
            }
        }

        if (changed) {
            MedicalStationNetwork.syncTasks(player);
        }
    }

    /**
     * 玩家在藏身处内死亡后在本维度重生
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!HollowHouseConfig.ENABLED.get()) {
            return;
        }
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        HollowHouseData data = HollowHouseDimensionManager.getData(serverPlayer);
        if (data == null) {
            return;
        }
        if (data.isInsideHollowHouse()) {
            ServerLevel hollowHouseLevel = HollowHouseDimensionManager.getOrCreateHollowHouseDimension(serverPlayer.getServer());
            BlockPos center = HollowHouseDimensionManager.getCurrentHollowHouseCenter(serverPlayer, data);
            serverPlayer.teleportTo(hollowHouseLevel, center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5, player.getYRot(), player.getXRot());
        }
    }

    /**
     * 玩家克隆时保留藏身处数据
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player current = event.getEntity();
        original.getCapability(HollowHouseDataRegistry.HOLLOW_HOUSE_DATA_CAPABILITY).ifPresent(oldData -> {
            current.getCapability(HollowHouseDataRegistry.HOLLOW_HOUSE_DATA_CAPABILITY).ifPresent(newData -> {
                newData.deserializeNBT(oldData.serializeNBT());
            });
        });
    }

    /**
     * 保持藏身处维度固定为晴天
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!HollowHouseConfig.ENABLED.get()) {
            return;
        }
        if (event.getLevel() instanceof ServerLevel serverLevel
                && serverLevel.dimension() == HollowHouseDimensionManager.HOLLOW_HOUSE_DIMENSION) {
            serverLevel.setWeatherParameters(Integer.MAX_VALUE, 0, false, false);
        }
    }

    /**
     * 取消藏身处内的摔落伤害
     */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!HollowHouseConfig.ENABLED.get()) {
            return;
        }
        if (event.getEntity().level().dimension() == HollowHouseDimensionManager.HOLLOW_HOUSE_DIMENSION) {
            event.setCanceled(true);
        }
    }
}
