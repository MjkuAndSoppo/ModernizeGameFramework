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
        CHANNEL.registerMessage(packetId++, CancelMedicalTaskPacket.class,
                CancelMedicalTaskPacket::encode, CancelMedicalTaskPacket::decode,
                CancelMedicalTaskPacket::handle);
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
     * 客户端与服务端系统时间可能不一致，因此直接发送剩余秒数与暂停状态，
     * 避免客户端基于时间戳计算时出现漂移或断电后仍读秒的问题。
     */
    private static void writeTasks(FriendlyByteBuf buffer, List<MedicalTask> tasks) {
        buffer.writeInt(tasks.size());
        for (MedicalTask task : tasks) {
            buffer.writeUtf(task.getRecipe().name());
            buffer.writeInt(task.getAmount());
            buffer.writeInt(task.getCompletedAmount());
            buffer.writeInt(task.getRemainingSeconds());
            buffer.writeBoolean(task.isPaused());
        }
    }

    /**
     * 从缓冲区解码任务列表
     * 根据剩余秒数与暂停状态反推出本地时间戳，使客户端显示与服务端保持一致。
     */
    private static List<MedicalTask> readTasks(FriendlyByteBuf buffer) {
        List<MedicalTask> tasks = new ArrayList<>();
        int size = buffer.readInt();
        for (int i = 0; i < size; i++) {
            MedicalRecipe recipe = MedicalRecipe.fromName(buffer.readUtf());
            int amount = buffer.readInt();
            int completedAmount = buffer.readInt();
            int remainingSeconds = buffer.readInt();
            boolean paused = buffer.readBoolean();
            if (recipe != null) {
                long now = System.currentTimeMillis();
                int totalSeconds = recipe.getProductionSeconds() * amount;
                int elapsedSeconds = Math.max(0, totalSeconds - remainingSeconds);
                long startTime = now - (long) elapsedSeconds * 1000L;
                long pauseStartTime = paused ? now : 0L;
                MedicalTask task = new MedicalTask(recipe, amount, startTime, completedAmount, 0L, pauseStartTime);
                tasks.add(task);
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

        // 检查供电状态：需要电力的工作方块只能在供电站发电时开始任务
        if (HollowHouseWorkBlockType.MEDICAL.isRequiresPower() && !data.getPowerStationData().isGenerating()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c供电站未发电，无法开始制作"), true);
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

    /**
     * 取消医疗站任务请求数据包
     */
    public static class CancelMedicalTaskPacket {

        private final int taskIndex;

        public CancelMedicalTaskPacket(int taskIndex) {
            this.taskIndex = taskIndex;
        }

        public static void encode(CancelMedicalTaskPacket packet, FriendlyByteBuf buffer) {
            buffer.writeInt(packet.taskIndex);
        }

        public static CancelMedicalTaskPacket decode(FriendlyByteBuf buffer) {
            return new CancelMedicalTaskPacket(buffer.readInt());
        }

        public static void handle(CancelMedicalTaskPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                cancelTask(player, packet.taskIndex);
            });
            context.setPacketHandled(true);
        }
    }

    /**
     * 服务端处理取消任务请求
     */
    private static void cancelTask(ServerPlayer player, int taskIndex) {
        if (!HollowHouseConfig.ENABLED.get()) {
            return;
        }
        if (player.level().dimension() != HollowHouseDimensionManager.HOLLOW_HOUSE_DIMENSION) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c只能在藏身处内操作医疗站"), true);
            return;
        }

        HollowHouseData data = HollowHouseDimensionManager.getData(player);
        if (data == null) {
            return;
        }

        List<MedicalTask> tasks = data.getMedicalTasks();
        if (taskIndex < 0 || taskIndex >= tasks.size()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c任务不存在"), true);
            return;
        }

        MedicalTask task = tasks.get(taskIndex);

        // 先校准一次任务进度，确保已完成产出已发放
        ItemStack output = task.tick();
        if (!output.isEmpty()) {
            HollowHouseStorehouseHelper.addItem(data, output);
        }

        // 返还剩余材料到仓库
        for (ItemStack ingredient : task.getRemainingIngredients()) {
            int remaining = HollowHouseStorehouseHelper.addItem(data, ingredient);
            if (remaining > 0) {
                ItemStack drop = ingredient.copy();
                drop.setCount(remaining);
                // 仓库放不下则尝试给予玩家
                if (!player.addItem(drop)) {
                    player.drop(drop, false);
                }
            }
        }

        // 返还剩余经验
        int remainingExp = task.getRemainingExperienceCost();
        if (remainingExp > 0) {
            player.giveExperiencePoints(remainingExp);
        }

        // 移除任务（必须调用 data 的方法，getMedicalTasks 返回的是副本）
        data.removeMedicalTask(taskIndex);

        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§a已取消制作 §e" + task.getRecipe().getDisplayName() + " §a并返还剩余材料"), true);

        // 同步任务列表
        syncTasks(player);
    }
}
