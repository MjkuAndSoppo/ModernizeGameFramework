package com.modernizegameframework.hollowhouse;

import com.modernizegameframework.ModernizeGameFramework;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * 供电站网络通道
 * 负责打开界面、同步数据、处理客户端操作
 */
public class PowerStationNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "power_station"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(packetId++, OpenPowerStationScreenPacket.class,
                OpenPowerStationScreenPacket::encode, OpenPowerStationScreenPacket::decode,
                OpenPowerStationScreenPacket::handle);
        CHANNEL.registerMessage(packetId++, SyncPowerStationPacket.class,
                SyncPowerStationPacket::encode, SyncPowerStationPacket::decode,
                SyncPowerStationPacket::handle);
        CHANNEL.registerMessage(packetId++, TogglePowerPacket.class,
                TogglePowerPacket::encode, TogglePowerPacket::decode,
                TogglePowerPacket::handle);
        CHANNEL.registerMessage(packetId++, SetFuelSlotPacket.class,
                SetFuelSlotPacket::encode, SetFuelSlotPacket::decode,
                SetFuelSlotPacket::handle);
    }

    public static void openScreen(ServerPlayer player, int powerLevel) {
        HollowHouseData data = HollowHouseDimensionManager.getData(player);
        PowerStationData psData = data != null ? data.getPowerStationData() : new PowerStationData();
        List<ItemStack> fuelItems = data != null ? collectStorehouseFuelItems(data) : Collections.emptyList();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenPowerStationScreenPacket(powerLevel, psData, fuelItems));
    }

    public static void syncData(ServerPlayer player) {
        HollowHouseData data = HollowHouseDimensionManager.getData(player);
        if (data == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncPowerStationPacket(data.getPowerStationData()));
    }

    /**
     * 收集仓库中可作为燃料的物品列表
     */
    private static List<ItemStack> collectStorehouseFuelItems(HollowHouseData data) {
        List<ItemStack> list = new ArrayList<>();
        net.minecraft.world.SimpleContainer inventory = data.getStorehouseInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && PowerStationData.computeFuelValue(stack) > 0) {
                list.add(stack.copy());
            }
        }
        return list;
    }

    private static void writePowerData(FriendlyByteBuf buffer, PowerStationData data) {
        List<ItemStack> slots = data.getFuelSlots();
        buffer.writeInt(slots.size());
        for (ItemStack stack : slots) {
            buffer.writeItem(stack);
        }
        buffer.writeBoolean(data.isGenerating());
        buffer.writeInt(data.getRemainingSeconds());
        buffer.writeInt(data.getPowerValue());
        buffer.writeLong(data.getLastUpdateTime());
    }

    private static PowerStationData readPowerData(FriendlyByteBuf buffer) {
        PowerStationData data = new PowerStationData();
        int slotCount = buffer.readInt();
        for (int i = 0; i < slotCount; i++) {
            data.setFuelSlotCount(i + 1);
            data.setFuelSlot(i, buffer.readItem());
        }
        data.setGenerating(buffer.readBoolean());
        data.setRemainingSeconds(buffer.readInt());
        data.setPowerValue(buffer.readInt());
        data.setLastUpdateTime(buffer.readLong());
        return data;
    }

    private static void writeFuelItems(FriendlyByteBuf buffer, List<ItemStack> items) {
        buffer.writeInt(items.size());
        for (ItemStack stack : items) {
            buffer.writeItem(stack);
        }
    }

    private static List<ItemStack> readFuelItems(FriendlyByteBuf buffer) {
        List<ItemStack> list = new ArrayList<>();
        int size = buffer.readInt();
        for (int i = 0; i < size; i++) {
            list.add(buffer.readItem());
        }
        return list;
    }

    /**
     * 打开供电站界面数据包
     */
    public static class OpenPowerStationScreenPacket {

        private final int powerLevel;
        private final PowerStationData data;
        private final List<ItemStack> storehouseFuelItems;

        public OpenPowerStationScreenPacket(int powerLevel, PowerStationData data, List<ItemStack> storehouseFuelItems) {
            this.powerLevel = powerLevel;
            this.data = data;
            this.storehouseFuelItems = new ArrayList<>(storehouseFuelItems != null ? storehouseFuelItems : Collections.emptyList());
        }

        public static void encode(OpenPowerStationScreenPacket packet, FriendlyByteBuf buffer) {
            buffer.writeInt(packet.powerLevel);
            writePowerData(buffer, packet.data);
            writeFuelItems(buffer, packet.storehouseFuelItems);
        }

        public static OpenPowerStationScreenPacket decode(FriendlyByteBuf buffer) {
            int level = buffer.readInt();
            PowerStationData data = readPowerData(buffer);
            List<ItemStack> fuelItems = readFuelItems(buffer);
            return new OpenPowerStationScreenPacket(level, data, fuelItems);
        }

        public static void handle(OpenPowerStationScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> openClientScreen(packet.powerLevel, packet.data, packet.storehouseFuelItems));
            context.setPacketHandled(true);
        }

        @OnlyIn(Dist.CLIENT)
        private static void openClientScreen(int powerLevel, PowerStationData data, List<ItemStack> fuelItems) {
            Minecraft.getInstance().setScreen(new PowerStationScreen(powerLevel, data, fuelItems));
        }
    }

    /**
     * 同步供电站数据包
     */
    public static class SyncPowerStationPacket {

        private final PowerStationData data;

        public SyncPowerStationPacket(PowerStationData data) {
            this.data = data;
        }

        public static void encode(SyncPowerStationPacket packet, FriendlyByteBuf buffer) {
            writePowerData(buffer, packet.data);
        }

        public static SyncPowerStationPacket decode(FriendlyByteBuf buffer) {
            return new SyncPowerStationPacket(readPowerData(buffer));
        }

        public static void handle(SyncPowerStationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                if (Minecraft.getInstance().screen instanceof PowerStationScreen screen) {
                    screen.updateData(packet.data);
                }
            });
            context.setPacketHandled(true);
        }
    }

    /**
     * 切换发电开关请求
     */
    public static class TogglePowerPacket {

        public TogglePowerPacket() {
        }

        public static void encode(TogglePowerPacket packet, FriendlyByteBuf buffer) {
        }

        public static TogglePowerPacket decode(FriendlyByteBuf buffer) {
            return new TogglePowerPacket();
        }

        public static void handle(TogglePowerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                togglePower(player);
            });
            context.setPacketHandled(true);
        }
    }

    /**
     * 设置燃油槽位请求
     */
    public static class SetFuelSlotPacket {

        private final int slotIndex;
        private final ItemStack stack;

        public SetFuelSlotPacket(int slotIndex, ItemStack stack) {
            this.slotIndex = slotIndex;
            this.stack = stack.copy();
        }

        public static void encode(SetFuelSlotPacket packet, FriendlyByteBuf buffer) {
            buffer.writeInt(packet.slotIndex);
            buffer.writeItem(packet.stack);
        }

        public static SetFuelSlotPacket decode(FriendlyByteBuf buffer) {
            return new SetFuelSlotPacket(buffer.readInt(), buffer.readItem());
        }

        public static void handle(SetFuelSlotPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                setFuelSlot(player, packet.slotIndex, packet.stack);
            });
            context.setPacketHandled(true);
        }
    }

    /**
     * 服务端处理发电开关切换
     */
    private static void togglePower(ServerPlayer player) {
        if (!HollowHouseConfig.ENABLED.get()) {
            return;
        }
        if (player.level().dimension() != HollowHouseDimensionManager.HOLLOW_HOUSE_DIMENSION) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c只能在藏身处内操作供电站"), true);
            return;
        }

        HollowHouseData data = HollowHouseDimensionManager.getData(player);
        if (data == null) {
            return;
        }

        int powerLevel = data.getWorkBlockLevel(HollowHouseWorkBlockType.POWER.getId());
        if (powerLevel <= 0) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c供电站尚未解锁"), true);
            return;
        }

        PowerStationData psData = data.getPowerStationData();
        psData.setFuelSlotCount(powerLevel);
        psData.calibrateTime();

        if (psData.isGenerating()) {
            // 关闭发电
            psData.setGenerating(false);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c已停止发电"), true);
        } else {
            // 开启发电
            if (psData.getRemainingSeconds() <= 0 && psData.getPowerValue() <= 0 && psData.computeFuelUnits() <= 0) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c没有可用燃料，无法发电"), true);
                syncData(player);
                return;
            }
            psData.setGenerating(true);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a已开始发电"), true);
        }

        syncData(player);
    }

    /**
     * 服务端处理设置燃油槽位
     * 会将新燃料从仓库中扣除，并将槽位中旧燃料返还仓库
     */
    private static void setFuelSlot(ServerPlayer player, int slotIndex, ItemStack requestedStack) {
        if (!HollowHouseConfig.ENABLED.get()) {
            return;
        }
        if (player.level().dimension() != HollowHouseDimensionManager.HOLLOW_HOUSE_DIMENSION) {
            return;
        }

        HollowHouseData data = HollowHouseDimensionManager.getData(player);
        if (data == null) {
            return;
        }

        int powerLevel = data.getWorkBlockLevel(HollowHouseWorkBlockType.POWER.getId());
        if (powerLevel <= 0 || slotIndex < 0 || slotIndex >= powerLevel) {
            return;
        }

        PowerStationData psData = data.getPowerStationData();
        psData.setFuelSlotCount(powerLevel);

        net.minecraft.world.SimpleContainer storehouse = data.getStorehouseInventory();

        // 先取出槽位中已有物品并返还仓库
        ItemStack currentSlot = psData.getFuelSlot(slotIndex);
        if (!currentSlot.isEmpty()) {
            int remaining = HollowHouseStorehouseHelper.addItem(data, currentSlot);
            if (remaining > 0) {
                ItemStack drop = currentSlot.copy();
                drop.setCount(remaining);
                if (!player.addItem(drop)) {
                    player.drop(drop, false);
                }
            }
            psData.setFuelSlot(slotIndex, ItemStack.EMPTY);
        }

        // 如果请求为空，则仅完成清空操作
        if (requestedStack.isEmpty()) {
            syncData(player);
            return;
        }

        // 只能放入有效燃料
        if (PowerStationData.computeFuelValue(requestedStack) <= 0) {
            syncData(player);
            return;
        }

        // 从仓库中查找并扣除可用数量
        ItemStack actual = consumeFuelFromStorehouse(storehouse, requestedStack);
        if (!actual.isEmpty()) {
            psData.setFuelSlot(slotIndex, actual);
        }

        syncData(player);
    }

    /**
     * 从仓库中扣除指定燃料，返回实际扣除到的物品栈
     */
    private static ItemStack consumeFuelFromStorehouse(net.minecraft.world.SimpleContainer storehouse, ItemStack requested) {
        if (requested.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // 耐久型燃料（燃油罐/桶）：寻找相同损伤值的物品并整个移除
        if (requested.isDamageableItem()) {
            for (int i = 0; i < storehouse.getContainerSize(); i++) {
                ItemStack slot = storehouse.getItem(i);
                if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, requested)) {
                    storehouse.setItem(i, ItemStack.EMPTY);
                    return slot.copy();
                }
            }
            return ItemStack.EMPTY;
        }

        // 原版可燃物品：按请求数量扣除，优先堆叠
        int maxStack = requested.getMaxStackSize();
        int remaining = Math.min(requested.getCount(), maxStack);
        ItemStack result = new ItemStack(requested.getItem(), 0);

        for (int i = 0; i < storehouse.getContainerSize() && remaining > 0; i++) {
            ItemStack slot = storehouse.getItem(i);
            if (!slot.isEmpty() && slot.is(requested.getItem())) {
                int take = Math.min(remaining, slot.getCount());
                result.grow(take);
                slot.shrink(take);
                if (slot.isEmpty()) {
                    storehouse.setItem(i, ItemStack.EMPTY);
                }
                remaining -= take;
            }
        }

        return result.isEmpty() ? ItemStack.EMPTY : result;
    }
}
