package com.agrieconomy.farmland;

import net.minecraft.world.item.Item;

/**
 * 농지 등록 전용 아이템.
 * 우클릭 처리는 {@link FarmlandProtectionHandler#onRightClickBlock}에서 담당한다.
 */
public class FarmlandRegisterItem extends Item {

    public FarmlandRegisterItem() {
        super(new Item.Properties().stacksTo(1));
    }
}