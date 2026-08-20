package com.modernizegameframework.looting.command;

import com.modernizegameframework.looting.config.BetterLootingConfig;
import com.modernizegameframework.looting.network.NetworkHandler;
import com.modernizegameframework.looting.network.S2C.PacketSyncConfig;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 专门用于注册战利品模块自定义指令的类。
 */
public class ModCommands {

    public static void register() {
        // 注册 RegisterCommandsEvent 事件监听器（Forge）
        MinecraftForge.EVENT_BUS.register(ModCommands.class);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // 注册管理员指令 /bl range <xz> <y>
        event.getDispatcher().register(Commands.literal("bl")
                .requires(source -> source.hasPermission(2)) // 需要OP权限
                .then(Commands.literal("range")
                        .then(Commands.argument("xz", FloatArgumentType.floatArg(0.5f, 8.0f))
                                .then(Commands.argument("y", FloatArgumentType.floatArg(0.5f, 5.0f))
                                        .executes(context -> {
                                            float newXZ = FloatArgumentType.getFloat(context, "xz");
                                            float newY = FloatArgumentType.getFloat(context, "y");

                                            // 保存到服务端的配置中
                                            BetterLootingConfig config = BetterLootingConfig.get();
                                            config.scanRangeXZ = newXZ;
                                            config.scanRangeY = newY;
                                            BetterLootingConfig.save();

                                            // 组装数据包并全服广播
                                            PacketSyncConfig packet = new PacketSyncConfig(newXZ, newY);
                                            NetworkHandler.sendToPlayers(
                                                    context.getSource().getServer().getPlayerList().getPlayers(),
                                                    packet
                                            );

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("§a[BetterLooting] 已将拾取范围全局设置为 XZ: " + newXZ + ", Y: " + newY),
                                                    true
                                            );
                                            return 1;
                                        }))))
        );
    }
}