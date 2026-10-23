package com.xcompwiz.mystcraft.world.dimension;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.TicketStorage;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;

/** Restores vanilla and NeoForge tickets for dynamically loaded Ages. */
public final class AgePersistentForcedChunkRuntime {
    private AgePersistentForcedChunkRuntime() {}
    public static int reinstate(ServerLevel level) {
        TicketStorage tickets = level.getDataStorage().get(TicketStorage.TYPE);
        if (tickets == null) return 0;
        tickets.activateAllDeactivatedTickets();
        ForcedChunkManager.activateAllDeactivatedTickets(level, tickets);
        return level.getForceLoadedChunks().size();
    }
}
