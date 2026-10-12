package ru.hivetech.telemetry.compat;

import com.google.gson.JsonObject;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ru.hivetech.telemetry.TelemetryCollector;

public final class IndustrialForegoingAdapter
    extends AbstractNamespaceAdapter {

    public IndustrialForegoingAdapter() {
        super(
            "industrialforegoing",
            new String[] {"industrialforegoing"},
            new String[] {"industrialforegoing"}
        );
    }

    @Override
    protected void classifyItem(
        EntityPlayerMP player,
        String action,
        ItemStack stack,
        String objectId,
        String fingerprint,
        JsonObject metadata,
        TelemetryCollector collector
    ) {
        if (CompatUtil.containsAny(
            fingerprint,
            "addon",
            "range",
            "speed",
            "efficiency"
        )) {
            emitCounter(
                collector,
                player,
                "industrialforegoing.addon." + action,
                objectId,
                Math.max(1, stack.getCount()),
                metadata
            );
        }
    }

    @Override
    protected void classifyBlock(
        EntityPlayerMP player,
        String action,
        IBlockState state,
        World world,
        BlockPos pos,
        String objectId,
        String fingerprint,
        JsonObject metadata,
        TelemetryCollector collector
    ) {
        if (CompatUtil.containsAny(
            fingerprint,
            "machine",
            "plant",
            "harvester",
            "sewer",
            "sludge",
            "mob",
            "animal",
            "laser",
            "drill",
            "crusher",
            "washer",
            "furnace",
            "generator",
            "reactor"
        )) {
            emitCounter(
                collector,
                player,
                "industrialforegoing.machine." + action,
                objectId,
                1L,
                metadata
            );
        }
    }

    @Override
    protected void classifyUse(
        EntityPlayerMP player,
        ItemStack held,
        IBlockState targetState,
        World world,
        BlockPos pos,
        String objectId,
        String fingerprint,
        JsonObject metadata,
        TelemetryCollector collector
    ) {
        if (CompatUtil.containsAny(
            fingerprint,
            "addon",
            "range",
            "speed",
            "efficiency"
        )) {
            emitCounter(
                collector,
                player,
                "industrialforegoing.addon.use",
                objectId,
                1L,
                metadata
            );
        }
    }
}
