package ru.hivetech.telemetry.compat;

import com.google.gson.JsonObject;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ru.hivetech.telemetry.TelemetryCollector;

public final class ForestryAdapter
    extends AbstractNamespaceAdapter {

    public ForestryAdapter() {
        super(
            "forestry",
            new String[] {
                "forestry",
                "gendustry",
                "binniecore",
                "extrabees",
                "extratrees",
                "genetics",
                "botany"
            },
            new String[] {
                "forestry",
                "gendustry",
                "binniecore",
                "extrabees",
                "extratrees",
                "genetics",
                "botany"
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
        String species =
            BeeSpeciesExtractor.extract(stack);

        if (species != null) {
            JsonObject extra =
                CompatUtil.copyMetadata(metadata);

            extra.addProperty(
                "species",
                species
            );

            emitCounter(
                collector,
                player,
                "forestry.bee.acquire",
                CompatUtil.limitedObject(species),
                Math.max(1, stack.getCount()),
                extra
            );

            if (
                "pickup".equals(action)
                || "craft".equals(action)
            ) {
                emitImmediate(
                    collector,
                    player,
                    "forestry.bee.discovered",
                    CompatUtil.limitedObject(species),
                    extra
                );
            }
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "bee",
            "drone",
            "princess",
            "queen",
            "larvae"
        )) {
            emitCounter(
                collector,
                player,
                "forestry.bee_item." + action,
                objectId,
                Math.max(1, stack.getCount()),
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "pollen",
            "sapling",
            "butterfly"
        )) {
            emitCounter(
                collector,
                player,
                "forestry.genetics_item." + action,
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
            "apiary"
        )) {
            emitCounter(
                collector,
                player,
                "forestry.apiary." + action,
                objectId,
                1L,
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "alveary"
        )) {
            emitCounter(
                collector,
                player,
                "forestry.alveary." + action,
                objectId,
                1L,
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "analyzer",
            "analyser",
            "carpenter",
            "centrifuge",
            "squeezer",
            "fermenter",
            "fabricator",
            "rainmaker",
            "moistener",
            "still",
            "sampler",
            "imprinter",
            "mutagen"
        )) {
            emitCounter(
                collector,
                player,
                "forestry.machine." + action,
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
            "analyzer",
            "analyser",
            "database",
            "spectacles"
        )) {
            emitCounter(
                collector,
                player,
                "forestry.genetics_tool.use",
                objectId,
                1L,
                metadata
            );
        }
    }
}
