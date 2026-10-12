package ru.hivetech.telemetry;

import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import ru.hivetech.telemetry.compat.TelemetryAdapterRegistry;

public final class TelemetryEvents {
    private final TelemetryCollector collector;
    private final TelemetryAdapterRegistry adapters;

    public TelemetryEvents(
        TelemetryCollector collector,
        TelemetryAdapterRegistry adapters
    ) {
        this.collector = collector;
        this.adapters = adapters;
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayerMP player = serverPlayer(event.player);
        if (player != null) {
            collector.playerLogin(player);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EntityPlayerMP player = serverPlayer(event.player);
        if (player != null) {
            collector.playerLogout(player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        EntityPlayerMP player = serverPlayer(event.player);
        if (player != null) {
            collector.playerTick(player);
        }
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        EntityPlayerMP player = serverPlayer(event.getPlayer());
        if (player == null) {
            return;
        }

        ResourceLocation id =
            event.getState().getBlock().getRegistryName();

        String objectId =
            blockObject(event.getState());

        JsonObject metadata =
            collector.playerMetadata(player);

        collector.emitCounter(
            player,
            "block.break",
            TelemetryCompat.sourceFor(id),
            objectId,
            1L,
            metadata
        );

        adapters.block(
            player,
            "break",
            event.getState(),
            event.getWorld(),
            event.getPos(),
            objectId,
            metadata,
            collector
        );
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.PlaceEvent event) {
        EntityPlayerMP player = serverPlayer(event.getPlayer());
        if (player == null) {
            return;
        }

        ResourceLocation id =
            event.getPlacedBlock().getBlock().getRegistryName();

        String objectId =
            blockObject(event.getPlacedBlock());

        JsonObject metadata =
            collector.playerMetadata(player);

        collector.emitCounter(
            player,
            "block.place",
            TelemetryCompat.sourceFor(id),
            objectId,
            1L,
            metadata
        );

        adapters.block(
            player,
            "place",
            event.getPlacedBlock(),
            event.getWorld(),
            event.getPos(),
            objectId,
            metadata,
            collector
        );
    }

    @SubscribeEvent
    public void onCraft(PlayerEvent.ItemCraftedEvent event) {
        EntityPlayerMP player = serverPlayer(event.player);
        if (player == null) {
            return;
        }

        emitItemCounter(
            player,
            "item.craft",
            event.crafting
        );
    }

    @SubscribeEvent
    public void onSmelt(PlayerEvent.ItemSmeltedEvent event) {
        EntityPlayerMP player = serverPlayer(event.player);
        if (player == null) {
            return;
        }

        emitItemCounter(
            player,
            "item.smelt",
            event.smelting
        );
    }

    @SubscribeEvent
    public void onPickup(PlayerEvent.ItemPickupEvent event) {
        EntityPlayerMP player =
            serverPlayer(event.player);

        if (player == null) {
            return;
        }

        emitItemCounter(
            player,
            "item.pickup",
            event.getStack()
        );
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        Entity victim = event.getEntity();

        if (
            victim instanceof EntityPlayerMP
            && !(victim instanceof FakePlayer)
        ) {
            EntityPlayerMP dead = (EntityPlayerMP) victim;
            JsonObject metadata =
                collector.playerMetadata(dead);

            if (event.getSource() != null) {
                metadata.addProperty(
                    "damage_type",
                    event.getSource().damageType
                );
            }

            collector.emitImmediate(
                dead,
                "player.death",
                "forge",
                null,
                1L,
                metadata
            );
        }

        Entity trueSource =
            event.getSource() == null
                ? null
                : event.getSource().getTrueSource();

        EntityPlayerMP killer =
            trueSource instanceof EntityPlayerMP
                && !(trueSource instanceof FakePlayer)
                ? (EntityPlayerMP) trueSource
                : null;

        if (killer == null || killer == victim) {
            return;
        }

        if (victim instanceof EntityPlayer) {
            JsonObject metadata =
                collector.playerMetadata(killer);
            metadata.addProperty(
                "target_username",
                victim.getName()
            );

            collector.emitCounter(
                killer,
                "player.kill",
                "forge",
                "player",
                1L,
                metadata
            );
            return;
        }

        ResourceLocation id = EntityList.getKey(victim);

        collector.emitCounter(
            killer,
            "entity.kill",
            TelemetryCompat.sourceFor(id),
            id == null
                ? victim.getClass().getName()
                : id.toString(),
            1L,
            collector.playerMetadata(killer)
        );
    }

    @SubscribeEvent
    public void onDimension(
        PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        EntityPlayerMP player = serverPlayer(event.player);
        if (player == null) {
            return;
        }

        JsonObject metadata = new JsonObject();
        metadata.addProperty("from_dimension", event.fromDim);
        metadata.addProperty("to_dimension", event.toDim);

        collector.emitImmediate(
            player,
            "dimension.enter",
            "forge",
            "dimension:" + event.toDim,
            1L,
            metadata
        );
    }

    @SubscribeEvent
    public void onAdvancement(AdvancementEvent event) {
        EntityPlayerMP player =
            serverPlayer(event.getEntityPlayer());

        if (
            player == null
            || event.getAdvancement() == null
            || event.getAdvancement().getId() == null
        ) {
            return;
        }

        collector.emitImmediate(
            player,
            "advancement.complete",
            "forge",
            event.getAdvancement().getId().toString(),
            1L,
            collector.playerMetadata(player)
        );
    }

    @SubscribeEvent(
        priority = EventPriority.LOWEST,
        receiveCanceled = false
    )
    public void onRightClickBlock(
        PlayerInteractEvent.RightClickBlock event
    ) {
        EntityPlayerMP player =
            serverPlayer(event.getEntityPlayer());

        if (player == null) {
            return;
        }

        ItemStack held =
            player.getHeldItem(event.getHand());

        if (held == null || held.isEmpty()) {
            return;
        }

        JsonObject metadata =
            collector.playerMetadata(player);

        metadata.addProperty(
            "target_x",
            event.getPos().getX()
        );
        metadata.addProperty(
            "target_y",
            event.getPos().getY()
        );
        metadata.addProperty(
            "target_z",
            event.getPos().getZ()
        );

        adapters.useOnBlock(
            player,
            held,
            event.getWorld().getBlockState(
                event.getPos()
            ),
            event.getWorld(),
            event.getPos(),
            metadata,
            collector
        );
    }

    private void emitItemCounter(
        EntityPlayerMP player,
        String type,
        ItemStack stack
    ) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        ResourceLocation id =
            stack.getItem().getRegistryName();

        String objectId = itemObject(stack);

        JsonObject metadata =
            collector.playerMetadata(player);

        collector.emitCounter(
            player,
            type,
            TelemetryCompat.sourceFor(id),
            objectId,
            Math.max(1, stack.getCount()),
            metadata
        );

        String action =
            type.startsWith("item.")
                ? type.substring("item.".length())
                : type;

        adapters.item(
            player,
            action,
            stack,
            objectId,
            metadata,
            collector
        );
    }

    private static String itemObject(ItemStack stack) {
        ResourceLocation id =
            stack.getItem().getRegistryName();

        if (id == null) {
            return null;
        }

        String value = id.toString();
        int meta = stack.getMetadata();

        return meta == 0
            ? value
            : value + "@" + meta;
    }

    private static String blockObject(
        net.minecraft.block.state.IBlockState state
    ) {
        ResourceLocation id =
            state.getBlock().getRegistryName();

        if (id == null) {
            return null;
        }

        String value = id.toString();
        int meta;

        try {
            meta = state.getBlock().getMetaFromState(state);
        } catch (RuntimeException ignored) {
            meta = 0;
        }

        return meta == 0
            ? value
            : value + "@" + meta;
    }

    private static EntityPlayerMP serverPlayer(
        EntityPlayer player
    ) {
        if (
            !(player instanceof EntityPlayerMP)
            || player instanceof FakePlayer
            || player.world == null
            || player.world.isRemote
        ) {
            return null;
        }

        return (EntityPlayerMP) player;
    }
}
