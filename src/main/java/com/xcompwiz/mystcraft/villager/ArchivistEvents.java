package com.xcompwiz.mystcraft.villager;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.registry.MystVillagers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Migrate old stock, then let vanilla Villager.mobInteract own trading and its Merchant UI. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class ArchivistEvents {
    private ArchivistEvents() {}
    @SubscribeEvent
    public static void onVillagerInteraction(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide()) return;
        if (!(event.getTarget() instanceof Villager villager) || villager.isBaby() || !villager.isAlive() || villager.isTrading()) return;
        if (!villager.getVillagerData().profession().is(MystVillagers.ARCHIVIST.unwrapKey().orElseThrow())) return;
        ArchivistTrades.ensureVanillaOffers(villager);
        // Do not cancel, open a custom menu, or bypass vanilla's sleeping/secondary-use checks.
    }
}
