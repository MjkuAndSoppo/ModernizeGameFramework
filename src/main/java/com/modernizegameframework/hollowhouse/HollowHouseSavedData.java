package com.modernizegameframework.hollowhouse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 藏身处玩家数据的世界级持久化存储
 * <p>
 * 将每位玩家的藏身处数据以 NBT 形式保存在主世界存档中，
 * 不再依赖玩家实体上的 Capability，从而避免死亡/跨维度克隆时数据丢失。
 */
public class HollowHouseSavedData extends SavedData {

    /**
     * 存储标识符
     */
    private static final String DATA_ID = "modernizegameframework_hollow_house";

    /**
     * 玩家 UUID 到数据 NBT 的映射
     */
    private final Map<UUID, CompoundTag> playerData = new HashMap<>();

    /**
     * 获取指定玩家的数据 NBT，不存在时返回空标签
     */
    public CompoundTag getData(UUID playerId) {
        return playerData.getOrDefault(playerId, new CompoundTag()).copy();
    }

    /**
     * 设置指定玩家的数据 NBT
     */
    public void setData(UUID playerId, CompoundTag tag) {
        playerData.put(playerId, tag.copy());
        setDirty();
    }

    /**
     * 序列化所有玩家数据到 NBT
     */
    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        CompoundTag root = new CompoundTag();
        CompoundTag playersTag = new CompoundTag();
        for (Map.Entry<UUID, CompoundTag> entry : playerData.entrySet()) {
            playersTag.put(entry.getKey().toString(), entry.getValue().copy());
        }
        root.put("players", playersTag);
        return root;
    }

    /**
     * 从 NBT 反序列化所有玩家数据
     */
    private static HollowHouseSavedData load(CompoundTag compoundTag) {
        HollowHouseSavedData data = new HollowHouseSavedData();
        if (compoundTag.contains("players", Tag.TAG_COMPOUND)) {
            CompoundTag playersTag = compoundTag.getCompound("players");
            for (String key : playersTag.getAllKeys()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    data.playerData.put(playerId, playersTag.getCompound(key).copy());
                } catch (IllegalArgumentException e) {
                    // 跳过非法 UUID 键，避免异常数据导致整个存档无法加载
                }
            }
        }
        return data;
    }

    /**
     * 获取服务端主世界的保存数据实例
     */
    public static HollowHouseSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(HollowHouseSavedData::load, HollowHouseSavedData::new, DATA_ID);
    }

    /**
     * 从 Capability 兼容层迁移旧数据到世界存储
     */
    public void migrateFromCapability(UUID playerId, @Nullable CompoundTag tag) {
        if (tag != null && !tag.isEmpty() && !playerData.containsKey(playerId)) {
            playerData.put(playerId, tag.copy());
            setDirty();
        }
    }
}
