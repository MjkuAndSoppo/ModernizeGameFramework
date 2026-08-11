package com.modernizegameframework.hollowhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 藏身处玩家数据默认实现
 */
public class HollowHouseDataImpl implements HollowHouseData {

    private int chunkX = 0;
    private int chunkZ = 0;
    private boolean created = false;
    private double returnX = 0;
    private double returnY = 64;
    private double returnZ = 0;
    private String returnDimension = "minecraft:overworld";
    private boolean insideHollowHouse = false;
    private final Set<UUID> invitedPlayers = new HashSet<>();
    private boolean controlBoxGiven = false;
    private boolean platformGenerated = false;
    private final Map<String, Integer> workBlockLevels = new HashMap<>();
    @Nullable
    private BlockPos portalPos = null;
    @Nullable
    private UUID ownerId = null;

    /**
     * 仓库容器：最大容量为 4 级时的 55 行 × 8 列 = 440 格
     */
    private final SimpleContainer storehouseInventory = new SimpleContainer(
            HollowHouseStorehouseHelper.getMaxStorehouseSlots());

    /**
     * 医疗站生产任务列表
     */
    private final List<MedicalTask> medicalTasks = new ArrayList<>();

    /**
     * 供电站数据
     */
    private final PowerStationData powerStationData = new PowerStationData();

    @Nullable
    private BlockPos powerStationPos = null;

    /**
     * 照明工作方块数据
     */
    private final LightingData lightingData = new LightingData();

    @Nullable
    private BlockPos lightingPos = null;

    @Override
    public int getChunkX() {
        return chunkX;
    }

    @Override
    public int getChunkZ() {
        return chunkZ;
    }

    @Override
    public void setChunkPos(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    @Override
    public boolean isCreated() {
        return created;
    }

    @Override
    public void markCreated() {
        this.created = true;
    }

    @Override
    public double getReturnX() {
        return returnX;
    }

    @Override
    public double getReturnY() {
        return returnY;
    }

    @Override
    public double getReturnZ() {
        return returnZ;
    }

    @Override
    public String getReturnDimension() {
        return returnDimension;
    }

    @Override
    public void setReturnPosition(double x, double y, double z, String dimension) {
        this.returnX = x;
        this.returnY = y;
        this.returnZ = z;
        this.returnDimension = dimension;
    }

    @Override
    public boolean isInsideHollowHouse() {
        return insideHollowHouse;
    }

    @Override
    public void setInsideHollowHouse(boolean inside) {
        this.insideHollowHouse = inside;
    }

    @Override
    public Set<UUID> getInvitedPlayers() {
        return invitedPlayers;
    }

    @Override
    public void invitePlayer(UUID playerId) {
        invitedPlayers.add(playerId);
    }

    @Override
    public void revokeInvite(UUID playerId) {
        invitedPlayers.remove(playerId);
    }

    @Override
    public void clearInvites() {
        invitedPlayers.clear();
    }

    @Override
    public boolean isInvited(UUID playerId) {
        return invitedPlayers.contains(playerId);
    }

    @Override
    public boolean isControlBoxGiven() {
        return controlBoxGiven;
    }

    @Override
    public void markControlBoxGiven() {
        this.controlBoxGiven = true;
    }

    @Override
    public boolean isPlatformGenerated() {
        return platformGenerated;
    }

    @Override
    public void markPlatformGenerated() {
        this.platformGenerated = true;
    }

    @Nullable
    @Override
    public BlockPos getPortalPos() {
        return portalPos;
    }

    @Override
    public void setPortalPos(@Nullable BlockPos pos) {
        this.portalPos = pos;
    }

    @Override
    public Map<String, Integer> getWorkBlockLevels() {
        return workBlockLevels;
    }

    @Override
    public int getWorkBlockLevel(String workBlockId) {
        return workBlockLevels.getOrDefault(workBlockId, 0);
    }

    @Override
    public void setWorkBlockLevel(String workBlockId, int level) {
        if (level <= 0) {
            workBlockLevels.remove(workBlockId);
        } else {
            workBlockLevels.put(workBlockId, level);
        }
    }

    @Nullable
    @Override
    public UUID getOwnerId() {
        return ownerId;
    }

    @Override
    public void setOwnerId(@Nullable UUID ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("chunkX", chunkX);
        tag.putInt("chunkZ", chunkZ);
        tag.putBoolean("created", created);
        tag.putDouble("returnX", returnX);
        tag.putDouble("returnY", returnY);
        tag.putDouble("returnZ", returnZ);
        tag.putString("returnDimension", returnDimension);
        tag.putBoolean("insideHollowHouse", insideHollowHouse);

        ListTag inviteList = new ListTag();
        for (UUID id : invitedPlayers) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", id);
            inviteList.add(entry);
        }
        tag.put("invitedPlayers", inviteList);

        tag.putBoolean("controlBoxGiven", controlBoxGiven);
        tag.putBoolean("platformGenerated", platformGenerated);

        CompoundTag workBlocksTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : workBlockLevels.entrySet()) {
            workBlocksTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("workBlockLevels", workBlocksTag);

        if (portalPos != null) {
            tag.putInt("portalX", portalPos.getX());
            tag.putInt("portalY", portalPos.getY());
            tag.putInt("portalZ", portalPos.getZ());
        }

        if (ownerId != null) {
            tag.putUUID("ownerId", ownerId);
        }

        tag.put("storehouseInventory", saveStorehouseInventory());

        CompoundTag medicalTasksTag = new CompoundTag();
        ListTag taskList = new ListTag();
        for (int i = 0; i < medicalTasks.size(); i++) {
            taskList.add(medicalTasks.get(i).serializeNBT());
        }
        medicalTasksTag.put("Tasks", taskList);
        tag.put("medicalTasks", medicalTasksTag);

        tag.put("powerStationData", powerStationData.serializeNBT());

        if (powerStationPos != null) {
            tag.putInt("powerStationX", powerStationPos.getX());
            tag.putInt("powerStationY", powerStationPos.getY());
            tag.putInt("powerStationZ", powerStationPos.getZ());
        }

        tag.put("lightingData", lightingData.serializeNBT());

        if (lightingPos != null) {
            tag.putInt("lightingX", lightingPos.getX());
            tag.putInt("lightingY", lightingPos.getY());
            tag.putInt("lightingZ", lightingPos.getZ());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        chunkX = tag.getInt("chunkX");
        chunkZ = tag.getInt("chunkZ");
        created = tag.getBoolean("created");
        returnX = tag.getDouble("returnX");
        returnY = tag.getDouble("returnY");
        returnZ = tag.getDouble("returnZ");
        returnDimension = tag.getString("returnDimension");
        insideHollowHouse = tag.getBoolean("insideHollowHouse");

        invitedPlayers.clear();
        if (tag.contains("invitedPlayers", Tag.TAG_LIST)) {
            ListTag inviteList = tag.getList("invitedPlayers", Tag.TAG_COMPOUND);
            for (int i = 0; i < inviteList.size(); i++) {
                CompoundTag entry = inviteList.getCompound(i);
                invitedPlayers.add(entry.getUUID("uuid"));
            }
        }

        controlBoxGiven = tag.getBoolean("controlBoxGiven");
        platformGenerated = tag.getBoolean("platformGenerated");

        workBlockLevels.clear();
        if (tag.contains("workBlockLevels", Tag.TAG_COMPOUND)) {
            CompoundTag workBlocksTag = tag.getCompound("workBlockLevels");
            for (String key : workBlocksTag.getAllKeys()) {
                workBlockLevels.put(key, workBlocksTag.getInt(key));
            }
        }

        if (tag.contains("portalX")) {
            portalPos = new BlockPos(
                    tag.getInt("portalX"),
                    tag.getInt("portalY"),
                    tag.getInt("portalZ"));
        } else {
            portalPos = null;
        }

        if (tag.hasUUID("ownerId")) {
            ownerId = tag.getUUID("ownerId");
        } else {
            ownerId = null;
        }

        if (tag.contains("storehouseInventory", Tag.TAG_COMPOUND)) {
            loadStorehouseInventory(tag.getCompound("storehouseInventory"));
        }

        medicalTasks.clear();
        if (tag.contains("medicalTasks", Tag.TAG_COMPOUND)) {
            CompoundTag medicalTasksTag = tag.getCompound("medicalTasks");
            if (medicalTasksTag.contains("Tasks", Tag.TAG_LIST)) {
                ListTag taskList = medicalTasksTag.getList("Tasks", Tag.TAG_COMPOUND);
                for (int i = 0; i < taskList.size(); i++) {
                    MedicalTask task = MedicalTask.deserializeNBT(taskList.getCompound(i));
                    if (task != null) {
                        medicalTasks.add(task);
                    }
                }
            }
        }

        if (tag.contains("powerStationData", Tag.TAG_COMPOUND)) {
            powerStationData.deserializeNBT(tag.getCompound("powerStationData"));
        }

        if (tag.contains("powerStationX")) {
            powerStationPos = new BlockPos(
                    tag.getInt("powerStationX"),
                    tag.getInt("powerStationY"),
                    tag.getInt("powerStationZ"));
        } else {
            powerStationPos = null;
        }

        if (tag.contains("lightingData", Tag.TAG_COMPOUND)) {
            lightingData.deserializeNBT(tag.getCompound("lightingData"));
        }

        if (tag.contains("lightingX")) {
            lightingPos = new BlockPos(
                    tag.getInt("lightingX"),
                    tag.getInt("lightingY"),
                    tag.getInt("lightingZ"));
        } else {
            lightingPos = null;
        }
    }

    @Override
    public SimpleContainer getStorehouseInventory() {
        return storehouseInventory;
    }

    @Override
    public void loadStorehouseInventory(CompoundTag tag) {
        storehouseInventory.clearContent();
        if (!tag.contains("Items", Tag.TAG_LIST)) {
            return;
        }
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int slot = itemTag.getByte("Slot") & 0xFF;
            if (slot >= 0 && slot < storehouseInventory.getContainerSize()) {
                storehouseInventory.setItem(slot, ItemStack.of(itemTag));
            }
        }
    }

    @Override
    public CompoundTag saveStorehouseInventory() {
        CompoundTag tag = new CompoundTag();
        ListTag items = new ListTag();
        for (int i = 0; i < storehouseInventory.getContainerSize(); i++) {
            ItemStack stack = storehouseInventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                items.add(itemTag);
            }
        }
        tag.put("Items", items);
        return tag;
    }

    @Override
    public List<MedicalTask> getMedicalTasks() {
        return new ArrayList<>(medicalTasks);
    }

    @Override
    public void addMedicalTask(MedicalRecipe recipe, int amount) {
        medicalTasks.add(new MedicalTask(recipe, amount));
    }

    @Override
    public void setMedicalTasks(List<MedicalTask> tasks) {
        medicalTasks.clear();
        if (tasks != null) {
            medicalTasks.addAll(tasks);
        }
    }

    @Override
    public void tickMedicalTasks(ServerPlayer player) {
        java.util.Iterator<MedicalTask> iterator = medicalTasks.iterator();
        boolean changed = false;
        while (iterator.hasNext()) {
            MedicalTask task = iterator.next();
            ItemStack output = task.tick();
            if (!output.isEmpty()) {
                HollowHouseStorehouseHelper.addItem(this, output);
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

    @Override
    public void removeMedicalTask(int index) {
        if (index >= 0 && index < medicalTasks.size()) {
            medicalTasks.remove(index);
        }
    }

    @Override
    public PowerStationData getPowerStationData() {
        return powerStationData;
    }

    @Override
    public void setPowerStationData(PowerStationData data) {
        if (data != null) {
            powerStationData.deserializeNBT(data.serializeNBT());
        }
    }

    @Override
    public BlockPos getPowerStationPos() {
        return powerStationPos;
    }

    @Override
    public void setPowerStationPos(@Nullable BlockPos pos) {
        this.powerStationPos = pos;
    }

    @Override
    public LightingData getLightingData() {
        return lightingData;
    }

    @Override
    public void setLightingData(LightingData data) {
        if (data != null) {
            lightingData.deserializeNBT(data.serializeNBT());
        }
    }

    @Override
    @Nullable
    public BlockPos getLightingPos() {
        return lightingPos;
    }

    @Override
    public void setLightingPos(@Nullable BlockPos pos) {
        this.lightingPos = pos;
    }
}
