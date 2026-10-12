package ru.hivetech.telemetry.compat;

import com.google.gson.JsonObject;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ru.hivetech.telemetry.TelemetryCollector;

public interface TelemetryModAdapter {
    String getId();

    boolean isLoaded();

    boolean handlesNamespace(String namespace);

    void onItem(
        EntityPlayerMP player,
        String action,
        ItemStack stack,
        String objectId,
        JsonObject metadata,
        TelemetryCollector collector
    );

    void onBlock(
        EntityPlayerMP player,
        String action,
        IBlockState state,
        World world,
        BlockPos pos,
        String objectId,
        JsonObject metadata,
        TelemetryCollector collector
    );

    void onUseOnBlock(
        EntityPlayerMP player,
        ItemStack held,
        IBlockState targetState,
        World world,
        BlockPos pos,
        JsonObject metadata,
        TelemetryCollector collector
    );

    String describe();
}
