package com.agrieconomy.command;

import com.agrieconomy.disaster.DisasterManager;
import com.agrieconomy.disaster.DisasterType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * /disaster 커맨드.
 *
 * <pre>
 * /disaster auto on|off
 * /disaster trigger &lt;종류&gt;                      — 발동자 현재 청크
 * /disaster trigger &lt;종류&gt; &lt;chunkX&gt; &lt;chunkZ&gt;   — 특정 청크
 * /disaster clear &lt;chunkX&gt; &lt;chunkZ&gt;
 * </pre>
 *
 * 명세서의 오타(/diaster)는 /disaster 로 정정.
 */
public class DisasterCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("disaster")
                .requires(src -> src.hasPermission(2))

                // /disaster auto on|off
                .then(Commands.literal("auto")
                        .then(Commands.literal("on").executes(ctx -> {
                            DisasterManager.get(ctx.getSource().getLevel()).setAutoMode(true);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§a[재해] 자동 발생 활성화."), true);
                            return 1;
                        }))
                        .then(Commands.literal("off").executes(ctx -> {
                            DisasterManager.get(ctx.getSource().getLevel()).setAutoMode(false);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§e[재해] 자동 발생 비활성화."), true);
                            return 1;
                        })))

                // /disaster trigger <종류> [chunkX chunkZ]
                .then(Commands.literal("trigger")
                        .then(Commands.argument("type", StringArgumentType.word())
                                // 발동자 현재 청크
                                .executes(ctx -> {
                                    String typeName = StringArgumentType.getString(ctx, "type");
                                    DisasterType type = DisasterType.fromString(typeName);
                                    if (type == null) {
                                        ctx.getSource().sendFailure(
                                                Component.literal("§c알 수 없는 재해 종류: " + typeName));
                                        return 0;
                                    }
                                    ServerLevel level = ctx.getSource().getLevel();
                                    var pos = ctx.getSource().getPosition();
                                    int cx = ((int) pos.x) >> 4;
                                    int cz = ((int) pos.z) >> 4;
                                    DisasterManager.get(level).trigger(level, cx, cz, type);
                                    return 1;
                                })
                                // 특정 청크
                                .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                                        .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                                                .executes(ctx -> {
                                                    String typeName = StringArgumentType.getString(ctx, "type");
                                                    DisasterType type = DisasterType.fromString(typeName);
                                                    if (type == null) {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§c알 수 없는 재해 종류: " + typeName));
                                                        return 0;
                                                    }
                                                    int cx = IntegerArgumentType.getInteger(ctx, "chunkX");
                                                    int cz = IntegerArgumentType.getInteger(ctx, "chunkZ");
                                                    ServerLevel level = ctx.getSource().getLevel();
                                                    DisasterManager.get(level).trigger(level, cx, cz, type);
                                                    return 1;
                                                })))))

                // /disaster clear <chunkX> <chunkZ>
                .then(Commands.literal("clear")
                        .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                                .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            int cx = IntegerArgumentType.getInteger(ctx, "chunkX");
                                            int cz = IntegerArgumentType.getInteger(ctx, "chunkZ");
                                            DisasterManager.get(ctx.getSource().getLevel()).clear(cx, cz);
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("§a[재해] 청크 [" + cx + ", " + cz + "] 재해 해제."), true);
                                            return 1;
                                        }))))
        );
    }
}
