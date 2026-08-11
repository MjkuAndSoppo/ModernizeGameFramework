package com.modernizegameframework.hollowhouse;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * 藏身处维度管理器
 * 负责创建藏身处维度、生成平台、玩家传送等核心逻辑
 */
public class HollowHouseDimensionManager {

    // 藏身处维度资源键
    public static final ResourceKey<Level> HOLLOW_HOUSE_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "hollow_house"));

    // 每位玩家藏身处的区块间隔，避免相互干扰
    private static final int CHUNK_SPACING = 16;
    // 平台中心 Y 坐标
    private static final int PLATFORM_Y = 64;
    // 屏障保护盒半边长（总空间 64×64×64）
    private static final int BARRIER_BOX_HALF_SIZE = 32;

    /**
     * 获取藏身处维度，若不存在则创建
     */
    public static ServerLevel getOrCreateHollowHouseDimension(MinecraftServer server) {
        ServerLevel existing = server.getLevel(HOLLOW_HOUSE_DIMENSION);
        if (existing != null) {
            return existing;
        }

        Holder<DimensionType> dimensionType = server.registryAccess()
                .registryOrThrow(Registries.DIMENSION_TYPE)
                .getHolderOrThrow(ResourceKey.create(Registries.DIMENSION_TYPE,
                        ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "hollow_house")));

        Registry<Biome> biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);
        Holder<Biome> plains = biomeRegistry.getHolderOrThrow(Biomes.PLAINS);

        // 创建虚空超平坦生成器（不添加任何层）
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.empty(), plains, Collections.emptyList());
        ChunkGenerator chunkGenerator = new FlatLevelSource(settings);

        LevelStem levelStem = new LevelStem(dimensionType, chunkGenerator);

        // 通过反射获取 MinecraftServer 的 storageSource 与 levels 字段
        LevelStorageSource.LevelStorageAccess storageSource;
        Map<ResourceKey<Level>, ServerLevel> levels;
        try {
            Field storageField = MinecraftServer.class.getDeclaredField("storageSource");
            storageField.setAccessible(true);
            storageSource = (LevelStorageSource.LevelStorageAccess) storageField.get(server);

            Field levelsField = MinecraftServer.class.getDeclaredField("levels");
            levelsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<ResourceKey<Level>, ServerLevel> levelsMap = (Map<ResourceKey<Level>, ServerLevel>) levelsField.get(server);
            levels = levelsMap;
        } catch (Exception e) {
            ModernizeGameFramework.LOGGER.error("无法访问 MinecraftServer 内部字段", e);
            throw new RuntimeException("无法访问 MinecraftServer 内部字段", e);
        }

        // 使用 LevelStem 创建 ServerLevel
        ServerLevel level = new ServerLevel(
                server,
                Util.backgroundExecutor(),
                storageSource,
                server.getWorldData().overworldData(),
                HOLLOW_HOUSE_DIMENSION,
                levelStem,
                null,
                false,
                server.getWorldData().worldGenOptions().seed(),
                Collections.emptyList(),
                false,
                null
        );

        levels.put(HOLLOW_HOUSE_DIMENSION, level);
        return level;
    }

    /**
     * 获取指定玩家的藏身处中心坐标
     */
    public static BlockPos getPlayerHollowHouseCenter(UUID playerId, HollowHouseData data) {
        if (!data.isCreated()) {
            // 根据玩家 UUID 分配唯一的区块坐标
            long most = playerId.getMostSignificantBits();
            long least = playerId.getLeastSignificantBits();
            int chunkX = (int) (Mth.abs((int) (most ^ least)) % 10000) * CHUNK_SPACING;
            int chunkZ = (int) (Mth.abs((int) ((most >>> 32) ^ (least >>> 32))) % 10000) * CHUNK_SPACING;
            data.setChunkPos(chunkX, chunkZ);
            data.markCreated();
        }
        int centerX = data.getChunkX() * 16 + 8;
        int centerZ = data.getChunkZ() * 16 + 8;
        return new BlockPos(centerX, PLATFORM_Y, centerZ);
    }

    /**
     * 生成或修复玩家藏身处的基础平台
     */
    public static void ensurePlatform(ServerLevel level, BlockPos center) {
        int platformSize = HollowHouseConfig.PLATFORM_SIZE.get();
        int half = platformSize / 2;
        Block platformBlock = Blocks.SMOOTH_STONE;

        for (int dx = -half; dx < half; dx++) {
            for (int dz = -half; dz < half; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, platformBlock.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    /**
     * 判断指定坐标是否已脱离藏身处中心 ±32 格保护范围
     */
    public static boolean isOutsideHollowHouseArea(BlockPos center, double x, double y, double z) {
        int half = BARRIER_BOX_HALF_SIZE;
        return x < center.getX() - half || x > center.getX() + half
                || y < center.getY() - half || y > center.getY() + half
                || z < center.getZ() - half || z > center.getZ() + half;
    }

    /**
     * 获取玩家当前应归属的藏身处中心
     * 若玩家是被邀请进入房主藏身处，则使用房主的中心坐标
     */
    public static BlockPos getCurrentHollowHouseCenter(ServerPlayer player, HollowHouseData data) {
        UUID ownerId = data.getOwnerId();
        if (ownerId != null && !ownerId.equals(player.getUUID())) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
                if (owner != null) {
                    HollowHouseData ownerData = getData(owner);
                    if (ownerData != null && ownerData.isCreated()) {
                        return getPlayerHollowHouseCenter(ownerId, ownerData);
                    }
                }
            }
        }
        return getPlayerHollowHouseCenter(player.getUUID(), data);
    }

    /**
     * 将玩家传送回其藏身处入口方块上方 1.5 格处（当前所在维度）
     */
    public static void teleportToHollowHouseCenter(ServerPlayer player, BlockPos center) {
        teleportToHollowHouseCenter(player, (ServerLevel) player.level(), center);
    }

    /**
     * 将玩家传送至指定维度的藏身处入口方块上方 1.5 格处
     */
    public static void teleportToHollowHouseCenter(ServerPlayer player, ServerLevel targetLevel, BlockPos center) {
        Vec3 target = new Vec3(center.getX() + 0.5, center.getY() + 2.5, center.getZ() + 0.5);
        teleport(player, targetLevel, target);
    }

    /**
     * 将玩家传送至藏身处
     */
    public static void enterHollowHouse(ServerPlayer player) {
        if (!HollowHouseConfig.ENABLED.get()) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        ServerLevel hollowHouseLevel = getOrCreateHollowHouseDimension(server);
        HollowHouseData data = getData(player);
        if (data == null) {
            return;
        }

        // 记录返回位置
        Level fromLevel = player.level();
        data.setReturnPosition(
                player.getX(),
                player.getY(),
                player.getZ(),
                fromLevel.dimension().location().toString());

        BlockPos center = getPlayerHollowHouseCenter(player.getUUID(), data);

        // 初始平台只生成一次，防止玩家刷取方块并方便后续改造
        if (!data.isPlatformGenerated()) {
            ensurePlatform(hollowHouseLevel, center);
            data.markPlatformGenerated();
        }

        // 首次进入时生成控制箱掉落物
        if (!data.isControlBoxGiven()) {
            spawnControlBoxDrop(hollowHouseLevel, center.above(2));
            data.markControlBoxGiven();
        }

        // 生成或更新藏身处内的入口方块
        BlockPos portalPos = center.above(1);
        if (hollowHouseLevel.getBlockState(portalPos).isAir()) {
            hollowHouseLevel.setBlock(portalPos, HollowHouseRegistry.HOLLOW_HOUSE_PORTAL.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        data.setPortalPos(portalPos);

        // 标记当前藏身处房主为玩家自己
        data.setOwnerId(player.getUUID());

        // 传送玩家至入口方块上方 1.5 格处
        teleportToHollowHouseCenter(player, hollowHouseLevel, center);
        data.setInsideHollowHouse(true);
    }

    /**
     * 将玩家从藏身处传送回原来的位置
     */
    public static void leaveHollowHouse(ServerPlayer player) {
        HollowHouseData data = getData(player);
        if (data == null) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.tryParse(data.getReturnDimension()));
        ServerLevel targetLevel = server.getLevel(targetKey);
        if (targetLevel == null) {
            targetLevel = server.overworld();
        }

        Vec3 targetPos = new Vec3(data.getReturnX(), data.getReturnY(), data.getReturnZ());
        teleport(player, targetLevel, targetPos);
        data.setInsideHollowHouse(false);
        data.clearOwnerId();

        // 房主退出时清除所有邀请
        data.clearInvites();
    }

    /**
     * 生成控制箱方块掉落物
     */
    private static void spawnControlBoxDrop(ServerLevel level, BlockPos pos) {
        ItemEntity itemEntity = new ItemEntity(
                level,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                new ItemStack(HollowHouseRegistry.CONTROL_BOX_ITEM.get())
        );
        itemEntity.setNoPickUpDelay();
        level.addFreshEntity(itemEntity);
    }

    /**
     * 执行跨维度传送
     */
    private static void teleport(ServerPlayer player, ServerLevel targetLevel, Vec3 targetPos) {
        player.changeDimension(targetLevel, new SimpleTeleporter(targetPos));
    }

    /**
     * 运行时数据缓存，按玩家 UUID 索引
     * <p>
     * 死亡/跨维度克隆会产生新的玩家实体，但 UUID 不变，
     * 通过缓存可保证同一次游戏会话中始终访问到同一份数据对象。
     */
    private static final Map<UUID, HollowHouseDataImpl> HOLLOW_HOUSE_DATA_CACHE = new HashMap<>();

    /**
     * 获取玩家的藏身处数据
     * <p>
     * 优先从世界存档 {@link HollowHouseSavedData} 加载并缓存，
     * 不再依赖玩家实体的 Capability，避免死亡后数据丢失。
     */
    @Nullable
    public static HollowHouseData getData(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return null;
        }
        UUID playerId = player.getUUID();
        HollowHouseDataImpl cached = HOLLOW_HOUSE_DATA_CACHE.get(playerId);
        if (cached != null) {
            return cached;
        }
        HollowHouseSavedData savedData = HollowHouseSavedData.get(server);
        CompoundTag tag = savedData.getData(playerId);
        HollowHouseDataImpl data = new HollowHouseDataImpl();
        if (!tag.isEmpty()) {
            data.deserializeNBT(tag);
        }
        HOLLOW_HOUSE_DATA_CACHE.put(playerId, data);
        return data;
    }

    /**
     * 将指定玩家的缓存数据同步回世界存档并标记脏数据
     */
    public static void syncDataToSavedData(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UUID playerId = player.getUUID();
        HollowHouseDataImpl cached = HOLLOW_HOUSE_DATA_CACHE.get(playerId);
        if (cached == null) {
            return;
        }
        HollowHouseSavedData savedData = HollowHouseSavedData.get(server);
        CompoundTag oldTag = savedData.getData(playerId);
        CompoundTag newTag = cached.serializeNBT();
        if (!newTag.equals(oldTag)) {
            savedData.setData(playerId, newTag);
        }
    }

    /**
     * 玩家登出时清理运行时缓存
     */
    public static void clearDataCache(UUID playerId) {
        HOLLOW_HOUSE_DATA_CACHE.remove(playerId);
    }

    /**
     * 简单传送器实现
     */
    private static class SimpleTeleporter implements ITeleporter {

        private final Vec3 targetPos;

        public SimpleTeleporter(Vec3 targetPos) {
            this.targetPos = targetPos;
        }

        @Override
        public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
            Entity repositioned = repositionEntity.apply(false);
            repositioned.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            return repositioned;
        }

        @Nullable
        @Override
        public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
            return new PortalInfo(targetPos, Vec3.ZERO, entity.getYRot(), entity.getXRot());
        }

        @Override
        public boolean isVanilla() {
            return false;
        }
    }
}
