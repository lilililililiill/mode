package com.agrieconomy.crop;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.agrieconomy.AgriEconomy;

/**
 * 작물 아이템 툴팁에 등급 정보를 표시한다.
 * 클라이언트 전용.
 */
@Mod.EventBusSubscriber(modid = AgriEconomy.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CropGradeTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.hasTag()) return;

        String gradeStr = stack.getTag().getString(CropGrade.NBT_KEY);
        if (gradeStr.isEmpty()) return;

        CropGrade grade = CropGrade.fromString(gradeStr);

        // 등급별 색상 및 표시 이름
        Component gradeText = switch (grade) {
            case SMALL   -> Component.literal("◆ 작음")   .withStyle(ChatFormatting.GRAY);
            case NORMAL  -> Component.literal("◆ 보통")   .withStyle(ChatFormatting.WHITE);
            case LARGE   -> Component.literal("◆ 큼")     .withStyle(ChatFormatting.GREEN);
            case PREMIUM -> Component.literal("◆ 특급")   .withStyle(ChatFormatting.GOLD);
        };

        // 가격 배율 표시
        Component multiplierText = Component.literal(
                String.format(" (x%.1f)", grade.getPriceMultiplier()))
                .withStyle(ChatFormatting.DARK_GRAY);

        event.getToolTip().add(1,
                Component.literal("등급: ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(gradeText)
                        .append(multiplierText));
    }
}
