package com.xcompwiz.mystcraft.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class MystPlayerMessages {
    private MystPlayerMessages() {}

    public static void send(Player player, Component message) { player.sendSystemMessage(message); }

    public static void send(Player player, Component message, boolean overlay) {
        if (player instanceof ServerPlayer serverPlayer) serverPlayer.sendSystemMessage(message, overlay);
        else player.sendSystemMessage(message);
    }
}
