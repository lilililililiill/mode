package com.agrieconomy.command;

import com.agrieconomy.market.MarketManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * /market 커맨드.
 */
public class MarketCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("market")
                .requires(src -> src.hasPermission(2))

                .then(Commands.literal("add")
                        .then(Commands.argument("itemId", StringArgumentType.word())
                                .then(Commands.argument("basePrice", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("minPrice", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("maxPrice", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> {
                                                            String itemId = StringArgumentType.getString(ctx, "itemId");
                                                            int base = IntegerArgumentType.getInteger(ctx, "basePrice");
                                                            int min = IntegerArgumentType.getInteger(ctx, "minPrice");
                                                            int max = IntegerArgumentType.getInteger(ctx, "maxPrice");
                                                            ServerLevel level = ctx.getSource().getLevel();
                                                            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(itemId));
                                                            if (item == null || item == Items.AIR) {
                                                                ctx.getSource().sendFailure(Component.literal("§c[거래소] 아이템을 찾을 수 없습니다."));
                                                                return 0;
                                                            }
                                                            MarketManager.get(level).addEntry(new ItemStack(item), base, min, max);
                                                            ctx.getSource().sendSuccess(
                                                                    () -> Component.literal("§a[거래소] " + itemId + " 등록 완료."), true);
                                                            return 1;
                                                        })))))

                .then(Commands.literal("remove")
                        .then(Commands.argument("itemId", StringArgumentType.word())
                                .executes(ctx -> {
                                    String itemId = StringArgumentType.getString(ctx, "itemId");
                                    Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(itemId));
                                    if (item == null || item == Items.AIR) {
                                        ctx.getSource().sendFailure(Component.literal("§c[거래소] 아이템을 찾을 수 없습니다."));
                                        return 0;
                                    }
                                    ServerLevel level = ctx.getSource().getLevel();
                                    boolean ok = MarketManager.get(level).removeEntry(new ItemStack(item));
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal(ok
                                                    ? "§a[거래소] " + itemId + " 삭제 완료."
                                                    : "§c[거래소] 등록된 품목이 없습니다: " + itemId), true);
                                    return ok ? 1 : 0;
                                })))

                .then(Commands.literal("list")
                        .executes(ctx -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            var entries = MarketManager.get(level).getAllEntries();
                            if (entries.isEmpty()) {
                                ctx.getSource().sendSuccess(
                                        () -> Component.literal("§e등록된 품목이 없습니다."), false);
                            } else {
                                StringBuilder sb = new StringBuilder("§6[거래소 품목 목록]\n");
                                entries.forEach(e -> sb.append(String.format(
                                        "§f- %s | 기준: %d | 현재: %d | 변동: %+d%%\n",
                                        e.getItemId(), e.getBasePrice(),
                                        e.getCurrentPrice(), e.getChangePercent())));
                                ctx.getSource().sendSuccess(
                                        () -> Component.literal(sb.toString()), false);
                            }
                            return 1;
                        }))
        );
    }
}
