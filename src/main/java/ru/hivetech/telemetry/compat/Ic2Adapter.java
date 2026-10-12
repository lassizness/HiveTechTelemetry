package ru.hivetech.telemetry.compat;

import com.google.gson.JsonObject;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ru.hivetech.telemetry.TelemetryCollector;

public final class Ic2Adapter
    extends AbstractNamespaceAdapter {

    public Ic2Adapter() {
        super(
            "ic2",
            new String[] {"ic2"},
            new String[] {"ic2"}
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
            "wrench",
            "electricwrench"
        )) {
            emitCounter(
                collector,
                player,
                "ic2.tool." + action,
                objectId,
                Math.max(1, stack.getCount()),
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "generator",
            "solar",
            "geothermal",
            "nuclear",
            "reactor"
        )) {
            emitCounter(
                collector,
                player,
                "ic2.generator." + action,
                objectId,
                Math.max(1, stack.getCount()),
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "mfsu",
            "mfe",
            "cesu",
            "batbox"
        )) {
            emitCounter(
                collector,
                player,
                "ic2.energy_storage." + action,
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
            "tileentity",
            "machine",
            "macerator",
            "extractor",
            "compressor",
            "recycler",
            "canner",
            "induction",
            "centrifuge",
            "orewashing",
            "metalformer"
        )) {
            emitCounter(
                collector,
                player,
                "ic2.machine." + action,
                objectId,
                1L,
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "generator",
            "solar",
            "geothermal",
            "nuclear",
            "reactor"
        )) {
            emitCounter(
                collector,
                player,
                "ic2.generator." + action,
                objectId,
                1L,
                metadata
            );
        }

        int tier = energyTier(fingerprint);

        if (tier > 0 && "place".equals(action)) {
            JsonObject extra =
                CompatUtil.copyMetadata(metadata);

            extra.addProperty("tier", tier);

            emitImmediate(
                collector,
                player,
                "ic2.energy.tier_reached",
                "tier:" + tier,
                extra
            );

            emitCounter(
                collector,
                player,
                "ic2.energy_storage.place",
                objectId,
                1L,
                extra
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
            "wrench",
            "electricwrench"
        )) {
            emitCounter(
                collector,
                player,
                "ic2.wrench.use",
                objectId,
                1L,
                metadata
            );
        }
    }

    private static int energyTier(String fingerprint) {
        if (CompatUtil.containsAny(
            fingerprint,
            "mfsu"
        )) {
            return 4;
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "mfe"
        )) {
            return 3;
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "cesu"
        )) {
            return 2;
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "batbox"
        )) {
            return 1;
        }

        return 0;
    }
}
