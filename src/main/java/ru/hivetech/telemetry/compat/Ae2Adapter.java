package ru.hivetech.telemetry.compat;

import com.google.gson.JsonObject;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ru.hivetech.telemetry.TelemetryCollector;

public final class Ae2Adapter
    extends AbstractNamespaceAdapter {

    public Ae2Adapter() {
        super(
            "ae2",
            new String[] {"appliedenergistics2"},
            new String[] {"appliedenergistics2"}
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
            "controller"
        )) {
            emitCounter(
                collector,
                player,
                "ae2.controller." + action,
                objectId,
                Math.max(1, stack.getCount()),
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "storagecell",
            "storage_cell",
            "cell1k",
            "cell4k",
            "cell16k",
            "cell64k",
            "cell256k"
        )) {
            emitCounter(
                collector,
                player,
                "ae2.storage_cell." + action,
                objectId,
                Math.max(1, stack.getCount()),
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "craftingstorage",
            "crafting_storage"
        )) {
            emitCounter(
                collector,
                player,
                "ae2.crafting_storage." + action,
                objectId,
                Math.max(1, stack.getCount()),
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "interface"
        )) {
            emitCounter(
                collector,
                player,
                "ae2.interface." + action,
                objectId,
                Math.max(1, stack.getCount()),
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "molecularassembler",
            "molecular_assembler"
        )) {
            emitCounter(
                collector,
                player,
                "ae2.molecular_assembler." + action,
                objectId,
                Math.max(1, stack.getCount()),
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "drive"
        )) {
            emitCounter(
                collector,
                player,
                "ae2.drive." + action,
                objectId,
                Math.max(1, stack.getCount()),
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "terminal",
            "craftingterminal",
            "patternterminal"
        )) {
            emitCounter(
                collector,
                player,
                "ae2.terminal." + action,
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
            "controller"
        )) {
            emitCounter(
                collector,
                player,
                "ae2.controller." + action,
                objectId,
                1L,
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "drive",
            "interface",
            "molecularassembler",
            "craftingstorage",
            "craftingmonitor",
            "securitystation",
            "energycell",
            "denseenergycell",
            "charger"
        )) {
            emitCounter(
                collector,
                player,
                "ae2.machine." + action,
                objectId,
                1L,
                metadata
            );
        }

        if (CompatUtil.containsAny(
            fingerprint,
            "cable",
            "bus",
            "quartzfiber",
            "mech",
            "part"
        )) {
            emitCounter(
                collector,
                player,
                "ae2.network_component." + action,
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
            "networktool",
            "network_tool",
            "memorycard",
            "memory_card",
            "certusquartzwrench",
            "netherquartzwrench"
        )) {
            emitCounter(
                collector,
                player,
                "ae2.network_tool.use",
                objectId,
                1L,
                metadata
            );
        }
    }
}
