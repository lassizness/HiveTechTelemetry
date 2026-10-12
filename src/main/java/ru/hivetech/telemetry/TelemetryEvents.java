package ru.hivetech.telemetry;

import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class TelemetryEvents {
    private final TelemetryCollector collector;

    public TelemetryEvents(TelemetryCollector collector) {
        this.collector = collector;
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

        collector.emitCounter(
            player,
            "block.break",
            TelemetryCompat.sourceFor(id),
            id == null ? null : id.toString(),
            1L,
            collector.playerMetadata(player)
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

        collector.emitCounter(
            player,
            "block.place",
            TelemetryCompat.sourceFor(id),
            id == null ? null : id.toString(),
            1L,
            collector.playerMetadata(player)
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
    public void onPickup(EntityItemPickupEvent event) {
        EntityPlayerMP player =
            serverPlayer(event.getEntityPlayer());

        if (player == null) {
            return;
        }

        EntityItem entityItem = event.getItem();
        if (entityItem == null) {
            return;
        }

        emitItemCounter(
            player,
            "item.pickup",
            entityItem.getItem()
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

        collector.emitCounter(
            player,
            type,
            TelemetryCompat.sourceFor(id),
            id == null ? null : id.toString(),
            Math.max(1, stack.getCount()),
            collector.playerMetadata(player)
        );
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
