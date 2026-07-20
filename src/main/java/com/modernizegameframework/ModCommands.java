package com.modernizegameframework;

import com.modernizegameframework.bodypart.BodyPartEvents;
import com.modernizegameframework.bodypart.BodyPartHelper;
import com.modernizegameframework.bodypart.BodyPartNetwork;
import com.modernizegameframework.bodypart.BodyPartType;
import com.modernizegameframework.movement.MovementConfig;
import com.modernizegameframework.securecontainer.SecureContainerConfig;
import com.modernizegameframework.stamina.StaminaConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * 模组指令注册
 * 提供 /mgf 指令用于热开关各功能模块
 *
 * 用法：
 *   /mgf sc on|off      - 开关安全箱
 *   /mgf bhop on|off    - 开关连跳/移动
 *   /mgf stamina on|off - 开关体力
 *   /mgf status         - 查看当前状态
 */
@Mod.EventBusSubscriber(modid = ModernizeGameFramework.MODID)
public class ModCommands {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("mgf")
                        .requires(source -> source.hasPermission(2)) // 需要 OP 权限
                        .then(Commands.literal("sc")
                                .then(Commands.literal("on")
                                        .executes(ctx -> toggle(ctx, "安全箱", true,
                                                SecureContainerConfig.ENABLED)))
                                .then(Commands.literal("off")
                                        .executes(ctx -> toggle(ctx, "安全箱", false,
                                                SecureContainerConfig.ENABLED))))
                        .then(Commands.literal("bhop")
                                .then(Commands.literal("on")
                                        .executes(ctx -> toggle(ctx, "连跳/移动", true,
                                                MovementConfig.ENABLED)))
                                .then(Commands.literal("off")
                                        .executes(ctx -> toggle(ctx, "连跳/移动", false,
                                                MovementConfig.ENABLED))))
                        .then(Commands.literal("stamina")
                                .then(Commands.literal("on")
                                        .executes(ctx -> toggle(ctx, "体力", true,
                                                StaminaConfig.ENABLED)))
                                .then(Commands.literal("off")
                                        .executes(ctx -> toggle(ctx, "体力", false,
                                                StaminaConfig.ENABLED))))
                        .then(Commands.literal("bodypart")
                                .then(Commands.literal("on")
                                        .executes(ctx -> toggleBodyPart(ctx, true)))
                                .then(Commands.literal("off")
                                        .executes(ctx -> toggleBodyPart(ctx, false))))
                        .then(Commands.literal("status")
                                .executes(ModCommands::showStatus))
        );

        // 回满所有在线玩家肢节血量
        dispatcher.register(
                Commands.literal("mgfh")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("all")
                                .executes(ModCommands::healAllBodyParts))
        );

        // 对所有在线玩家指定部位造成伤害（测试用）
        dispatcher.register(
                Commands.literal("cutHP")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("part", StringArgumentType.word())
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0.0f))
                                        .executes(ModCommands::cutBodyPartHealth)))
        );
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx, String name,
                              boolean enable, net.minecraftforge.common.ForgeConfigSpec.BooleanValue config) {
        config.set(enable);
        ctx.getSource().sendSuccess(
                () -> Component.literal("§a" + name + " 已" + (enable ? "§e开启" : "§c关闭")),
                true);
        return 1;
    }

    private static int toggleBodyPart(CommandContext<CommandSourceStack> ctx, boolean enable) {
        Config.BODYPART_ENABLED.set(enable);
        BodyPartEvents.updateAllPlayers(enable);
        ctx.getSource().sendSuccess(
                () -> Component.literal("§a肢节血量系统 已" + (enable ? "§e开启" : "§c关闭")),
                true);
        return 1;
    }

    private static int healAllBodyParts(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            ctx.getSource().sendFailure(Component.literal("§c服务器未启动"));
            return 0;
        }

        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BodyPartHelper.healAll(player);
            BodyPartNetwork.syncToClient(player);
            count++;
        }

        int finalCount = count;
        ctx.getSource().sendSuccess(
                () -> Component.literal("§a已回满 §e" + finalCount + " §a名玩家的肢节血量"),
                true);
        return count;
    }

    private static int cutBodyPartHealth(CommandContext<CommandSourceStack> ctx) {
        String partId = StringArgumentType.getString(ctx, "part");
        float amount = FloatArgumentType.getFloat(ctx, "amount");
        BodyPartType type = BodyPartType.fromId(partId);

        if (type == null) {
            ctx.getSource().sendFailure(Component.literal("§c未知部位: §f" + partId));
            return 0;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            ctx.getSource().sendFailure(Component.literal("§c服务器未启动"));
            return 0;
        }

        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BodyPartHelper.applyDamage(player, type, amount);
            BodyPartNetwork.syncToClient(player);
            count++;
        }

        int finalCount = count;
        BodyPartType finalType = type;
        ctx.getSource().sendSuccess(
                () -> Component.literal("§a已对所有 §e" + finalCount + " §a名玩家 §f" + finalType.getId() + " §a部位造成 §c" + amount + " §a点伤害"),
                true);
        return count;
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        boolean sc = SecureContainerConfig.ENABLED.get();
        boolean bhop = MovementConfig.ENABLED.get();
        boolean stamina = StaminaConfig.ENABLED.get();
        boolean bodypart = Config.BODYPART_ENABLED.get();

        ctx.getSource().sendSuccess(() -> Component.literal(
                "§6===== MGF 功能状态 =====\n" +
                "§f安全箱: " + (sc ? "§a开启" : "§c关闭") + "\n" +
                "§f连跳/移动: " + (bhop ? "§a开启" : "§c关闭") + "\n" +
                "§f体力: " + (stamina ? "§a开启" : "§c关闭") + "\n" +
                "§f肢节血量: " + (bodypart ? "§a开启" : "§c关闭")), false);
        return 1;
    }
}