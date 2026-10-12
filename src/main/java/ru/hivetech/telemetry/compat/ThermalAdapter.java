package ru.hivetech.telemetry.compat;

import com.google.gson.JsonObject;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ru.hivetech.telemetry.TelemetryCollector;

public final class ThermalAdapter
    extends AbstractNamespaceAdapter {

    public ThermalAdapter() {
        super(
            "thermal",
            new String[] {
                "thermalexpansion",
                "thermalfoundation",
                "thermaldynamics"
            },
            new String[] {
                "thermalexpansion",
                "thermalfoundation",
                "thermaldynamics"
            }
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
            "augment"
        )) {
            emitCounter(
                collector,
                player,
                "thermal.augment." + action,
                objectId,
                Math.max(1, stack.getCount()),
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "upgrade",
            "conversion",
            "kit"
        )) {
            emitCounter(
                collector,
                player,
                "thermal.upgrade." + action,
                objectId,
                Math.max(1, stack.getCount()),
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "dynamo"
        )) {
            emitCounter(
                collector,
                player,
                "thermal.dynamo." + action,
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
            "dynamo"
        )) {
            emitCounter(
                collector,
                player,
                "thermal.dynamo." + action,
                objectId,
                1L,
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "machine",
            "furnace",
            "pulverizer",
            "sawmill",
            "smelter",
            "crucible",
            "transposer",
            "insolator",
            "compactor",
            "centrifuge",
            "brewer",
            "enchanter",
            "refinery"
        )) {
            emitCounter(
                collector,
                player,
                "thermal.machine." + action,
                objectId,
                1L,
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "cell",
            "tank",
            "cache",
            "strongbox"
        )) {
            emitCounter(
                collector,
                player,
                "thermal.storage." + action,
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
            "augment"
        )) {
            JsonObject extra =
                CompatUtil.copyMetadata(metadata);

            extra.addProperty(
                "confirmed_install",
                false
            );

            emitCounter(
                collector,
                player,
                "thermal.augment.use",
                objectId,
                1L,
                extra
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "upgrade",
            "conversion",
            "kit"
        )) {
            emitCounter(
                collector,
                player,
                "thermal.upgrade.use",
                objectId,
                1L,
                metadata
            );
        }
    }
}
