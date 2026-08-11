package com.modernizegameframework.hollowhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

/**
 * 照明工作方块数据
 * 保存玩家选中的照明等级与照明方块位置
 */
public class LightingData {

    /**
     * 当前选中的照明等级，1~3
     */
    private int selectedLevel = 1;

    /**
     * 照明方块位置
     */
    @Nullable
    private BlockPos lightingPos = null;

    public int getSelectedLevel() {
        return selectedLevel;
    }

    public void setSelectedLevel(int level) {
        this.selectedLevel = Math.max(1, Math.min(3, level));
    }

    @Nullable
    public BlockPos getLightingPos() {
        return lightingPos;
    }

    public void setLightingPos(@Nullable BlockPos pos) {
        this.lightingPos = pos;
    }

    /**
     * 序列化为 NBT
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SelectedLevel", selectedLevel);
        if (lightingPos != null) {
            tag.putInt("LightingX", lightingPos.getX());
            tag.putInt("LightingY", lightingPos.getY());
            tag.putInt("LightingZ", lightingPos.getZ());
        }
        return tag;
    }

    /**
     * 从 NBT 反序列化
     */
    public void deserializeNBT(CompoundTag tag) {
        selectedLevel = tag.getInt("SelectedLevel");
        if (selectedLevel < 1 || selectedLevel > 3) {
            selectedLevel = 1;
        }
        if (tag.contains("LightingX")) {
            lightingPos = new BlockPos(
                    tag.getInt("LightingX"),
                    tag.getInt("LightingY"),
                    tag.getInt("LightingZ"));
        } else {
            lightingPos = null;
        }
    }
}
