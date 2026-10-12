package ru.hivetech.telemetry.compat;

import com.google.gson.JsonObject;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ru.hivetech.telemetry.TelemetryCollector;

public final class ImmersiveEngineeringAdapter
    extends AbstractNamespaceAdapter {

    public ImmersiveEngineeringAdapter() {
        super(
            "immersiveengineering",
            new String[] {"immersiveengineering"},
            new String[] {"immersiveengineering"}
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
            "hammer",
            "wirecutter",
            "voltmeter"
        )) {
            emitCounter(
                collector,
                player,
                "immersiveengineering.tool." + action,
                objectId,
                Math.max(1, stack.getCount()),
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "wirecoil",
            "wire_coil"
        )) {
            emitCounter(
                collector,
                player,
                "immersiveengineering.wire_coil." + action,
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
            "capacitor"
        )) {
            emitCounter(
                collector,
                player,
                "immersiveengineering.capacitor." + action,
                objectId,
                1L,
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "connector",
            "relay"
        )) {
            emitCounter(
                collector,
                player,
                "immersiveengineering.connector." + action,
                objectId,
                1L,
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "crusher",
            "dieselgenerator",
            "diesel_generator",
            "excavator",
            "arc_furnace",
            "arcfurnace",
            "assembler",
            "bottling",
            "fermenter",
            "squeezer",
            "refinery",
            "metalpress",
            "metal_press"
        )) {
            emitCounter(
                collector,
                player,
                "immersiveengineering.machine." + action,
                objectId,
                1L,
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "blastbrick",
            "cokebrick",
            "sheetmetal",
            "lightengineering",
            "heavyengineering",
            "redstoneengineering"
        )) {
            emitCounter(
                collector,
                player,
                "immersiveengineering.multiblock_component." + action,
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
            "hammer"
        )) {
            JsonObject extra =
                CompatUtil.copyMetadata(metadata);

            extra.addProperty(
                "multiblock_formation_confirmed",
                false
            );

            emitCounter(
                collector,
                player,
                "immersiveengineering.hammer.use",
                objectId,
                1L,
                extra
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "wirecoil",
            "wire_coil"
        )) {
            emitCounter(
                collector,
                player,
                "immersiveengineering.wire_coil.use",
                objectId,
                1L,
                metadata
            );
        }
    }
}
