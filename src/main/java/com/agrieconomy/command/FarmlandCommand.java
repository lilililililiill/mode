package com.agrieconomy.command;

import com.agrieconomy.farmland.FarmlandManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * /farmland 커맨드 (관리자 전용).
 *
 * <pre>
 * /farmland remove &lt;chunkX&gt; &lt;chunkZ&gt;
 * /farmland register &lt;chunkX&gt; &lt;chunkZ&gt; &lt;owner&gt;
 * /farmland setowner &lt;chunkX&gt; &lt;chunkZ&gt; &lt;newOwner&gt;
 * /farmland info &lt;chunkX&gt; &lt;chunkZ&gt;
 * </pre>
 */
public class FarmlandCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("farmland")
                .requires(src -> src.hasPermission(2))

                // /farmland remove
                .then(Commands.literal("remove")
                        .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                                .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            int cx = IntegerArgumentType.getInteger(ctx, "chunkX");
                                            int cz = IntegerArgumentType.getInteger(ctx, "chunkZ");
                                            boolean ok = FarmlandManager.get(ctx.getSource().getLevel()).remove(cx, cz);
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal(ok
                                                            ? "§a[농지] 청크 [" + cx + ", " + cz + "] 농지 제거 완료."
                                                            : "§c[농지] 해당 청크는 농지가 아닙니다."), true);
                                            return ok ? 1 : 0;
                                        }))))

                // /farmland register
                .then(Commands.literal("register")
                        .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                                .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                                        .then(Commands.argument("owner", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    int cx     = IntegerArgumentType.getInteger(ctx, "chunkX");
                                                    int cz     = IntegerArgumentType.getInteger(ctx, "chunkZ");
                                                    String own = StringArgumentType.getString(ctx, "owner");
                                                    FarmlandManager.get(ctx.getSource().getLevel()).register(cx, cz, own);
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal("§a[농지] 청크 [" + cx + ", " + cz + "] 등록 (소유자: " + own + ")."), true);
                                                    return 1;
                                                })))))

                // /farmland setowner
                .then(Commands.literal("setowner")
                        .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                                .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                                        .then(Commands.argument("newOwner", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    int cx        = IntegerArgumentType.getInteger(ctx, "chunkX");
                                                    int cz        = IntegerArgumentType.getInteger(ctx, "chunkZ");
                                                    String newOwn = StringArgumentType.getString(ctx, "newOwner");
                                                    ServerLevel level = ctx.getSource().getLevel();
                                                    boolean ok = FarmlandManager.get(level).changeOwner(cx, cz, newOwn);
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal(ok
                                                                    ? "§a[농지] 소유자 변경 완료: " + newOwn
                                                                    : "§c[농지] 해당 청크는 농지가 아닙니다."), true);
                                                    return ok ? 1 : 0;
                                                })))))

                // /farmland info
                .then(Commands.literal("info")
                        .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                                .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            int cx = IntegerArgumentType.getInteger(ctx, "chunkX");
                                            int cz = IntegerArgumentType.getInteger(ctx, "chunkZ");
                                            var data = FarmlandManager.get(ctx.getSource().getLevel()).get(cx, cz);
                                            if (data == null) {
                                                ctx.getSource().sendSuccess(
                                                        () -> Component.literal("§e[농지] 등록된 농지가 아닙니다."), false);
                                            } else {
                                                ctx.getSource().sendSuccess(
                                                        () -> Component.literal("§6[농지 정보] 청크: [" + cx + ", " + cz + "] | 소유자: " + data.getOwnerName()), false);
                                            }
                                            return 1;
                                        }))))
        );
    }
}
