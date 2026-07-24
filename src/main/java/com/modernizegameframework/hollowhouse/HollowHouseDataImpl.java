package com.modernizegameframework.hollowhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.HashSet;
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
    @Nullable
    private BlockPos portalPos = null;
    @Nullable
    private UUID ownerId = null;

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

        if (portalPos != null) {
            tag.putInt("portalX", portalPos.getX());
            tag.putInt("portalY", portalPos.getY());
            tag.putInt("portalZ", portalPos.getZ());
        }

        if (ownerId != null) {
            tag.putUUID("ownerId", ownerId);
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
    }
}
