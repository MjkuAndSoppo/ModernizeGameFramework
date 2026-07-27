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
import java.util.List;
import java.util.function.Supplier;

/**
 * 医疗站网络通道
 * 负责打开医疗站界面、同步生产任务、接收制作请求
 */
public class MedicalStationNetwork {

    /**
     * 网络协议版本号
     */
    private static final String PROTOCOL_VERSION = "1";

    /**
     * 网络通道实例
     */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ModernizeGameFramework.MODID, "medical_station"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    /**
     * 消息 ID 计数器
     */
    private static int packetId = 0;

    /**
     * 注册网络消息
     */
    public static void register() {
        CHANNEL.registerMessage(packetId++, OpenMedicalStationScreenPacket.class,
                OpenMedicalStationScreenPacket::encode, OpenMedicalStationScreenPacket::decode,
                OpenMedicalStationScreenPacket::handle);
        CHANNEL.registerMessage(packetId++, SyncMedicalTasksPacket.class,
                SyncMedicalTasksPacket::encode, SyncMedicalTasksPacket::decode,
                SyncMedicalTasksPacket::handle);
        CHANNEL.registerMessage(packetId++, StartMedicalProductionPacket.class,
                StartMedicalProductionPacket::encode, StartMedicalProductionPacket::decode,
                StartMedicalProductionPacket::handle);
    }

    /**
     * 向客户端发送打开医疗站界面的数据包
     */
    public static void openScreen(ServerPlayer player, int medicalLevel) {
        HollowHouseData data = HollowHouseDimensionManager.getData(player);
        List<MedicalTask> tasks = data != null ? data.getMedicalTasks() : new ArrayList<>();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenMedicalStationScreenPacket(medicalLevel, tasks));
    }

    /**
     * 向客户端同步医疗站任务列表
     */
    public static void syncTasks(ServerPlayer player) {
        HollowHouseData data = HollowHouseDimensionManager.getData(player);
        if (data == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncMedicalTasksPacket(data.getMedicalTasks()));
    }

    /**
     * 编码任务列表到缓冲区
     */
    private static void writeTasks(FriendlyByteBuf buffer, List<MedicalTask> tasks) {
        buffer.writeInt(tasks.size());
        for (MedicalTask task : tasks) {
            buffer.writeUtf(task.getRecipe().name());
            buffer.writeInt(task.getAmount());
            buffer.writeInt(task.getRemainingSeconds());
            buffer.writeInt(task.getCompletedAmount());
        }
    }

    /**
     * 从缓冲区解码任务列表
     */
    private static List<MedicalTask> readTasks(FriendlyByteBuf buffer) {
        List<MedicalTask> tasks = new ArrayList<>();
        int size = buffer.readInt();
        for (int i = 0; i < size; i++) {
            MedicalRecipe recipe = MedicalRecipe.fromName(buffer.readUtf());
            int amount = buffer.readInt();
            int remainingSeconds = buffer.readInt();
            int completedAmount = buffer.readInt();
            if (recipe != null) {
                tasks.add(new MedicalTask(recipe, amount, remainingSeconds, completedAmount));
            }
        }
        return tasks;
    }

    /**
     * 打开医疗站界面数据包
     */
    public static class OpenMedicalStationScreenPacket {

        private final int medicalLevel;
        private final List<MedicalTask> tasks;

        public OpenMedicalStationScreenPacket(int medicalLevel, List<MedicalTask> tasks) {
            this.medicalLevel = medicalLevel;
            this.tasks = new ArrayList<>(tasks);
        }

        public static void encode(OpenMedicalStationScreenPacket packet, FriendlyByteBuf buffer) {
            buffer.writeInt(packet.medicalLevel);
            writeTasks(buffer, packet.tasks);
        }

        public static OpenMedicalStationScreenPacket decode(FriendlyByteBuf buffer) {
            int level = buffer.readInt();
            List<MedicalTask> tasks = readTasks(buffer);
            return new OpenMedicalStationScreenPacket(level, tasks);
        }

        public static void handle(OpenMedicalStationScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> openClientScreen(packet.medicalLevel, packet.tasks));
            context.setPacketHandled(true);
        }

        @OnlyIn(Dist.CLIENT)
        private static void openClientScreen(int medicalLevel, List<MedicalTask> tasks) {
            Minecraft.getInstance().setScreen(new MedicalStationScreen(medicalLevel, tasks));
        }
    }

    /**
     * 同步医疗站任务列表数据包
     */
    public static class SyncMedicalTasksPacket {

        private final List<MedicalTask> tasks;

        public SyncMedicalTasksPacket(List<MedicalTask> tasks) {
            this.tasks = new ArrayList<>(tasks);
        }

        public static void encode(SyncMedicalTasksPacket packet, FriendlyByteBuf buffer) {
            writeTasks(buffer, packet.tasks);
        }

        public static SyncMedicalTasksPacket decode(FriendlyByteBuf buffer) {
            return new SyncMedicalTasksPacket(readTasks(buffer));
        }

        public static void handle(SyncMedicalTasksPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                if (Minecraft.getInstance().screen instanceof MedicalStationScreen screen) {
                    screen.updateTasks(packet.tasks);
                }
            });
            context.setPacketHandled(true);
        }
    }

    /**
     * 开始制作医疗物品请求数据包
     */
    public static class StartMedicalProductionPacket {

        private final String recipeName;
        private final int amount;

        public StartMedicalProductionPacket(String recipeName, int amount) {
            this.recipeName = recipeName;
            this.amount = Math.max(1, amount);
        }

        public static void encode(StartMedicalProductionPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.recipeName);
            buffer.writeInt(packet.amount);
        }

        public static StartMedicalProductionPacket decode(FriendlyByteBuf buffer) {
            return new StartMedicalProductionPacket(buffer.readUtf(), buffer.readInt());
        }

        public static void handle(StartMedicalProductionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                startProduction(player, packet.recipeName, packet.amount);
            });
            context.setPacketHandled(true);
        }
    }

    /**
     * 服务端处理制作请求
     */
    private static void startProduction(ServerPlayer player, String recipeName, int amount) {
        if (!HollowHouseConfig.ENABLED.get()) {
            return;
        }
        if (player.level().dimension() != HollowHouseDimensionManager.HOLLOW_HOUSE_DIMENSION) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c只能在藏身处内使用医疗站"), true);
            return;
        }

        HollowHouseData data = HollowHouseDimensionManager.getData(player);
        if (data == null) {
            return;
        }

        MedicalRecipe recipe = MedicalRecipe.fromName(recipeName);
        if (recipe == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c未知配方"), true);
            return;
        }

        int medicalLevel = data.getWorkBlockLevel(HollowHouseWorkBlockType.MEDICAL.getId());
        if (!recipe.isAvailable(medicalLevel)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c医疗站等级不足"), true);
            return;
        }

        // 检查经验点数（使用经验等级作为经验点）
        int totalExperienceCost = recipe.getExperienceCost() * amount;
        if (player.totalExperience < totalExperienceCost) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c经验点数不足"), true);
            return;
        }

        // 检查仓库物品是否足够
        for (ItemStack ingredient : recipe.getIngredients()) {
            int required = ingredient.getCount() * amount;
            if (!HollowHouseStorehouseHelper.hasItem(data, ingredient, required)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§c仓库中 " + ingredient.getHoverName().getString() + " 不足，需要 " + required + " 个"), true);
                return;
            }
        }

        // 消耗仓库物品
        for (ItemStack ingredient : recipe.getIngredients()) {
            int required = ingredient.getCount() * amount;
            HollowHouseStorehouseHelper.consumeItem(data, ingredient, required);
        }

        // 消耗经验
        if (totalExperienceCost > 0) {
            player.giveExperiencePoints(-totalExperienceCost);
        }

        // 添加生产任务
        data.addMedicalTask(recipe, amount);

        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§a开始制作 §e" + recipe.getDisplayName() + "*" + amount), true);

        // 同步任务列表
        syncTasks(player);
    }
}
