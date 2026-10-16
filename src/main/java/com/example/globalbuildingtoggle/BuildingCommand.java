package com.example.globalbuildingtoggle;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class BuildingCommand {
    private static final int OP_LEVEL = BuildingEvents.OP_LEVEL;

    private BuildingCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("building")
                .executes(ctx -> status(ctx.getSource()))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("off")
                        .requires(s -> s.hasPermission(OP_LEVEL))
                        .executes(ctx -> set(ctx.getSource(), true)))
                .then(Commands.literal("on")
                        .requires(s -> s.hasPermission(OP_LEVEL))
                        .executes(ctx -> set(ctx.getSource(), false))));
    }

    private static int status(CommandSourceStack src) {
        boolean restricted = BuildingState.isRestricted();
        Component msg = restricted
                ? Component.literal("Building is currently DISABLED for regular players (OPs can still build).")
                        .withStyle(ChatFormatting.RED)
                : Component.literal("Building is currently ENABLED for everyone.")
                        .withStyle(ChatFormatting.GREEN);
        src.sendSuccess(() -> msg, false);
        return restricted ? 0 : 1;
    }

    private static int set(CommandSourceStack src, boolean restrict) {
        if (BuildingState.isRestricted() == restrict) {
            src.sendSuccess(() -> Component.literal(restrict
                    ? "Building is already disabled."
                    : "Building is already enabled.").withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }
        BuildingState.setRestricted(restrict);
        Component broadcast = restrict
                ? Component.literal("[Building] Block placement has been DISABLED by "
                        + src.getTextName() + ". Only OPs can place blocks.").withStyle(ChatFormatting.RED)
                : Component.literal("[Building] Block placement has been ENABLED by "
                        + src.getTextName() + ". Everyone can build again.").withStyle(ChatFormatting.GREEN);
        src.getServer().getPlayerList().broadcastSystemMessage(broadcast, false);
        return 1;
    }
}
