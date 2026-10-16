package com.example.globalbuildingtoggle;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public class BuildingEvents {
    /** Permission level treated as "OP" (vanilla ops default to level 4; 2 is the usual gamemaster threshold). */
    public static final int OP_LEVEL = 2;

    /** Fake players (machines/deployers from other mods) are NOT treated as regular players. */
    private static final boolean RESTRICT_FAKE_PLAYERS = false;

    private final Map<UUID, Long> lastMessage = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        BuildingCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        BuildingState.setRestricted(false); // explicit reset on every start
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        lastMessage.clear();
    }

    private static boolean isRestrictedPlayer(Entity entity) {
        if (!BuildingState.isRestricted()) return false;
        if (!(entity instanceof ServerPlayer player)) return false;
        if (player instanceof FakePlayer && !RESTRICT_FAKE_PLAYERS) return false;
        return !player.hasPermissions(OP_LEVEL);
    }

    /** Items whose normal use is placing a block/fluid. Food block-items (berries) are excluded so eating still works. */
    private static boolean isPlacementItem(ItemStack stack) {
        if (stack.isEmpty() || stack.has(DataComponents.FOOD)) return false;
        Item item = stack.getItem();
        if (item instanceof BlockItem) return true;
        return item instanceof BucketItem && item != Items.BUCKET; // filled buckets place fluids
    }

    private void notifyDenied(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long last = lastMessage.get(player.getUUID());
        if (last != null && now - last < 20) return;
        lastMessage.put(player.getUUID(), now);
        player.displayClientMessage(
                Component.literal("Building is currently disabled.").withStyle(ChatFormatting.RED), true);
    }

    /**
     * Authoritative backstop. NeoForge fires this for every block placed through item use
     * (vanilla and modded) via ItemStack#useOn block-snapshotting; cancelling restores the blocks.
     */
    @SubscribeEvent
    public void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();
        if (!isRestrictedPlayer(entity)) return;
        event.setCanceled(true);
        notifyDenied((ServerPlayer) entity);
    }

    /**
     * Early denial of item use on a block. Only the ITEM half is denied, so right-clicking
     * chests, crafting tables, enchanting tables, doors, etc. keeps working.
     */
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isRestrictedPlayer(player)) return;
        if (!isPlacementItem(event.getItemStack())) return;
        event.setUseItem(TriState.FALSE);
        notifyDenied(player);
    }

    /** Covers placement that happens through Item#use rather than useOn (filled buckets, lily pads, frogspawn...). */
    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isRestrictedPlayer(player)) return;
        if (!isPlacementItem(event.getItemStack())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        notifyDenied(player);
    }
}
