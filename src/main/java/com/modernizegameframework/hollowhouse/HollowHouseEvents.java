package com.modernizegameframework.hollowhouse;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.particles.ParticleTypes;
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

    // #region debug-point death-clears-levels
    /**
     * 调试日志上报工具，用于追踪玩家死亡后数据丢失问题
     */
    private static final java.util.concurrent.ExecutorService DEBUG_EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "hollowhouse-debug");
        t.setDaemon(true);
        return t;
    });

    private static String debugUrlCache = null;

    private static String loadDebugUrl() {
        if (debugUrlCache != null) {
            return debugUrlCache;
        }
        try {
            // 依次尝试多个可能的工作目录：项目根目录、run 子目录
            java.nio.file.Path[] candidates = new java.nio.file.Path[]{
                    java.nio.file.Paths.get(".dbg", "death-clears-levels.env"),
                    java.nio.file.Paths.get("..", ".dbg", "death-clears-levels.env"),
                    java.nio.file.Paths.get("run", ".dbg", "death-clears-levels.env")
            };
            for (java.nio.file.Path env : candidates) {
                if (java.nio.file.Files.exists(env)) {
                    for (String line : java.nio.file.Files.readAllLines(env, java.nio.charset.StandardCharsets.UTF_8)) {
                        if (line.startsWith("DEBUG_SERVER_URL=")) {
                            debugUrlCache = line.substring("DEBUG_SERVER_URL=".length()).trim();
                            return debugUrlCache;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 调试加载失败时静默忽略，避免影响正常逻辑
        }
        // 兜底使用已知端口，确保调试事件不丢失
        return "http://127.0.0.1:7777/event";
    }

    private static java.util.Map<String, Object> debugMap(Object... pairs) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }

    /**
     * 供其他类使用的调试日志入口
     */
    public static void debugLogExternal(String event, java.util.Map<String, Object> payload) {
        debugLog(event, payload);
    }

    private static void debugLog(String event, java.util.Map<String, Object> payload) {
        String url = loadDebugUrl();
        if (url == null || url.isEmpty()) {
            return;
        }
        DEBUG_EXECUTOR.submit(() -> {
            try {
                java.net.URL u = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                StringBuilder sb = new StringBuilder();
                sb.append("{");
                sb.append("\"session\":\"").append("death-clears-levels").append("\",");
                sb.append("\"runId\":\"").append("post-fix").append("\",");
                sb.append("\"event\":\"").append(event).append("\",");
                sb.append("\"timestamp\":").append(System.currentTimeMillis()).append(",");
                sb.append("\"payload\":{");
                boolean first = true;
                for (java.util.Map.Entry<String, Object> e : payload.entrySet()) {
                    if (!first) {
                        sb.append(",");
                    }
                    first = false;
                    sb.append("\"").append(e.getKey()).append("\":");
                    Object v = e.getValue();
                    if (v == null) {
                        sb.append("null");
                    } else if (v instanceof Number) {
                        sb.append(v);
                    } else if (v instanceof Boolean) {
                        sb.append(v);
                    } else {
                        String s = String.valueOf(v).replace("\\", "\\\\").replace("\"", "\\\"");
                        sb.append("\"").append(s).append("\"");
                    }
                }
                sb.append("}}");
                byte[] body = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                conn.getOutputStream().write(body);
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                // 调试上报失败时静默忽略
            }
        });
    }
    // #endregion debug-point death-clears-levels

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

        // 将当前藏身处数据同步到世界存档，确保死亡/登出/崩溃后数据不丢失
        HollowHouseDimensionManager.syncDataToSavedData(player);

        BlockPos center = HollowHouseDimensionManager.getCurrentHollowHouseCenter(player, data);
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        if (y < 0 || HollowHouseDimensionManager.isOutsideHollowHouseArea(center, x, y, z)) {
            HollowHouseDimensionManager.teleportToHollowHouseCenter(player, center);
        }

        // 供电站与医疗站每秒推进一次（每 20 tick）
        tickPowerAndMedicalTasks(player, data);

        // 照明工作方块每 tick 推进扩散
        tickLighting(player, data);
    }

    /**
     * 照明扩散管理器，按玩家 UUID 索引
     */
    private static final java.util.Map<java.util.UUID, LightingSpreadManager> lightingManagers = new java.util.HashMap<>();

    /**
     * 任务 tick 计数器，按玩家 UUID 索引
     */
    private static final java.util.Map<java.util.UUID, Integer> taskTickCounters = new java.util.HashMap<>();

    /**
     * 记录每位玩家上一次的供电状态，用于检测断电/通电切换并同步医疗站任务
     */
    private static final java.util.Map<java.util.UUID, Boolean> lastPowerStates = new java.util.HashMap<>();

    /**
     * 推进供电站发电进度与医疗站生产任务
     */
    private static void tickPowerAndMedicalTasks(ServerPlayer player, HollowHouseData data) {
        int counter = taskTickCounters.getOrDefault(player.getUUID(), 0) + 1;
        taskTickCounters.put(player.getUUID(), counter);
        if (counter < 20) {
            return;
        }
        taskTickCounters.put(player.getUUID(), 0);

        // 先处理供电站
        boolean powerGenerating = tickPowerStation(player, data);

        // 根据供电状态暂停/恢复需要电力的医疗站任务
        boolean medicalRequiresPower = HollowHouseWorkBlockType.MEDICAL.isRequiresPower();
        if (medicalRequiresPower) {
            if (powerGenerating) {
                resumeMedicalTasks(data);
            } else {
                pauseMedicalTasks(data);
            }
        }

        // 当供电状态发生切换时，立即同步医疗站任务到客户端，
        // 使客户端能即时显示暂停/恢复，避免断电期间仍读秒
        boolean lastPowerState = lastPowerStates.getOrDefault(player.getUUID(), true);
        if (medicalRequiresPower && lastPowerState != powerGenerating) {
            MedicalStationNetwork.syncTasks(player);
            lastPowerStates.put(player.getUUID(), powerGenerating);
        }

        // 推进医疗站任务（已暂停的任务不会增加有效经过时间）
        data.tickMedicalTasks(player);
    }

    /**
     * 推进照明工作方块扩散效果
     * 检测红石信号与供电状态，决定点亮、熄灭或切换等级
     */
    private static void tickLighting(ServerPlayer player, HollowHouseData data) {
        BlockPos lightingPos = data.getLightingPos();
        ServerLevel level = (ServerLevel) player.level();
        BlockPos areaCenter = HollowHouseDimensionManager.getCurrentHollowHouseCenter(player, data);

        LightingSpreadManager manager = lightingManagers.computeIfAbsent(player.getUUID(), k -> new LightingSpreadManager());

        // 若照明方块已被破坏或不再存在，立即熄灭并清理记录
        if (lightingPos == null || !(level.getBlockState(lightingPos).getBlock() instanceof LightingBlock)) {
            if (manager.isProcessing() || data.getLightingPos() != null) {
                manager.update(data.getLightingPos(), 0, areaCenter);
                manager.tick(level);
            }
            if (data.getLightingPos() != null) {
                data.setLightingPos(null);
            }
            return;
        }

        int unlockedLevel = data.getWorkBlockLevel(HollowHouseWorkBlockType.LIGHTING.getId());
        int selectedLevel = data.getLightingData().getSelectedLevel();

        // 未解锁或选中等级为 0 时保持关闭
        if (unlockedLevel <= 0 || selectedLevel <= 0) {
            manager.update(lightingPos, 0, areaCenter);
            manager.tick(level);
            return;
        }

        // 照明需要供电站发电
        boolean hasPower = data.getPowerStationData().isGenerating();
        // 检测红石信号
        boolean hasRedstone = level.getBestNeighborSignal(lightingPos) > 0;

        int desiredLevel = (hasPower && hasRedstone) ? selectedLevel : 0;

        manager.update(lightingPos, desiredLevel, areaCenter);
        manager.tick(level);
    }

    /**
     * 推进供电站发电进度
     *
     * @return 当前是否处于发电状态
     */
    private static boolean tickPowerStation(ServerPlayer player, HollowHouseData data) {
        int powerLevel = data.getWorkBlockLevel(HollowHouseWorkBlockType.POWER.getId());
        PowerStationData psData = data.getPowerStationData();
        psData.setFuelSlotCount(powerLevel);
        psData.calibrateTime();

        if (!psData.isGenerating()) {
            updatePowerStationBlockState(player, data, false);
            return false;
        }

        int secondsPerUnit = PowerStationData.getSecondsPerPowerUnit(powerLevel);

        // 剩余时间消耗完毕时，尝试从燃油槽位补充发电值
        while (psData.getRemainingSeconds() <= 0) {
            if (psData.getPowerValue() > 0) {
                psData.setPowerValue(psData.getPowerValue() - 1);
                psData.setRemainingSeconds(psData.getRemainingSeconds() + secondsPerUnit);
            } else if (psData.consumeOneFuelUnit()) {
                psData.setRemainingSeconds(psData.getRemainingSeconds() + secondsPerUnit);
            } else {
                // 燃料耗尽，停止发电
                psData.setGenerating(false);
                psData.setRemainingSeconds(0);
                updatePowerStationBlockState(player, data, false);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c供电站燃料耗尽，已停止发电"), true);
                PowerStationNetwork.syncData(player);
                return false;
            }
        }

        // 播放熔炉燃烧音效与粒子效果
        playPowerStationEffects(player, data);

        updatePowerStationBlockState(player, data, true);
        PowerStationNetwork.syncData(player);
        return true;
    }

    /**
     * 更新供电站方块发光状态
     */
    private static void updatePowerStationBlockState(ServerPlayer player, HollowHouseData data, boolean lit) {
        BlockPos pos = data.getPowerStationPos();
        if (pos == null) {
            return;
        }
        Level level = player.level();
        if (level.dimension() != HollowHouseDimensionManager.HOLLOW_HOUSE_DIMENSION) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PowerStationBlock)) {
            return;
        }
        if (state.getValue(PowerStationBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(PowerStationBlock.LIT, lit), 3);
        }
    }

    /**
     * 播放供电站燃烧音效与粒子
     */
    private static void playPowerStationEffects(ServerPlayer player, HollowHouseData data) {
        BlockPos pos = data.getPowerStationPos();
        if (pos == null) {
            return;
        }
        Level level = player.level();
        if (level.dimension() != HollowHouseDimensionManager.HOLLOW_HOUSE_DIMENSION) {
            return;
        }
        if (level.getRandom().nextInt(20) == 0) {
            level.playSound(null, pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS,
                    1.0F, 1.0F);
        }
        if (level instanceof ServerLevel serverLevel && level.getRandom().nextInt(5) == 0) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                    1, 0.0, 0.05, 0.0, 0.0);
        }
    }

    /**
     * 暂停需要电力的医疗站任务
     */
    private static void pauseMedicalTasks(HollowHouseData data) {
        for (MedicalTask task : data.getMedicalTasks()) {
            if (!task.isFinished() && !task.isPaused()) {
                task.pause();
            }
        }
    }

    /**
     * 恢复需要电力的医疗站任务
     */
    private static void resumeMedicalTasks(HollowHouseData data) {
        for (MedicalTask task : data.getMedicalTasks()) {
            if (task.isPaused()) {
                task.resume();
            }
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
        // #region debug-point death-clears-levels
        debugLog("player-respawn", debugMap(
                "uuid", serverPlayer.getUUID().toString(),
                "inside", data != null ? String.valueOf(data.isInsideHollowHouse()) : "null",
                "levels", data != null ? data.getWorkBlockLevels().toString() : "null"
        ));
        // #endregion debug-point death-clears-levels
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
    /**
     * 玩家克隆时无需手动复制藏身处数据：
     * 数据已迁移到世界存档并按 UUID 缓存，新实体使用相同 UUID 即可访问同一份数据。
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // #region debug-point death-clears-levels
        Player original = event.getOriginal();
        Player current = event.getEntity();
        debugLog("player-clone", debugMap(
                "originalUuid", original.getUUID().toString(),
                "currentUuid", current.getUUID().toString(),
                "isWasDeath", event.isWasDeath()
        ));
        // #endregion debug-point death-clears-levels
    }

    /**
     * 玩家登出时将缓存数据同步到世界存档并清理缓存，防止内存泄漏与数据不一致
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!HollowHouseConfig.ENABLED.get()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            HollowHouseDimensionManager.syncDataToSavedData(player);
            HollowHouseDimensionManager.clearDataCache(player.getUUID());
        }
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
