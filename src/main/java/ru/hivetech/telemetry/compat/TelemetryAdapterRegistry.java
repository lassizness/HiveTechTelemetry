package ru.hivetech.telemetry.compat;

import com.google.gson.JsonObject;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ru.hivetech.telemetry.TelemetryCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TelemetryAdapterRegistry {
    private final List<TelemetryModAdapter> active =
        new ArrayList<>();

    public TelemetryAdapterRegistry() {
        add(new Ae2Adapter());
        add(new Ic2Adapter());
        add(new ForestryAdapter());
        add(new ThermalAdapter());
        add(new ImmersiveEngineeringAdapter());
        add(new IndustrialForegoingAdapter());
    }

    private void add(TelemetryModAdapter adapter) {
        if (adapter.isLoaded()) {
            active.add(adapter);
        }
    }

    public List<String> descriptions() {
        List<String> result = new ArrayList<>();

        for (TelemetryModAdapter adapter : active) {
            result.add(adapter.describe());
        }

        return Collections.unmodifiableList(result);
    }

    public void item(
        EntityPlayerMP player,
        String action,
        ItemStack stack,
        String objectId,
        JsonObject metadata,
        TelemetryCollector collector
    ) {
        ResourceLocation id =
            stack == null || stack.isEmpty()
                ? null
                : stack.getItem().getRegistryName();

        String namespace =
            CompatUtil.namespace(id);

        for (TelemetryModAdapter adapter : active) {
            if (adapter.handlesNamespace(namespace)) {
                adapter.onItem(
                    player,
                    action,
                    stack,
                    objectId,
                    metadata,
                    collector
                );
            }
        }
    }

    public void block(
        EntityPlayerMP player,
        String action,
        IBlockState state,
        World world,
        BlockPos pos,
        String objectId,
        JsonObject metadata,
        TelemetryCollector collector
    ) {
        ResourceLocation id =
            state == null
                ? null
                : state.getBlock().getRegistryName();

        String namespace =
            CompatUtil.namespace(id);

        for (TelemetryModAdapter adapter : active) {
            if (adapter.handlesNamespace(namespace)) {
                adapter.onBlock(
                    player,
                    action,
                    state,
                    world,
                    pos,
                    objectId,
                    metadata,
                    collector
                );
            }
        }
    }

    public void useOnBlock(
        EntityPlayerMP player,
        ItemStack held,
        IBlockState targetState,
        World world,
        BlockPos pos,
        JsonObject metadata,
        TelemetryCollector collector
    ) {
        ResourceLocation id =
            held == null || held.isEmpty()
                ? null
                : held.getItem().getRegistryName();

        String namespace =
            CompatUtil.namespace(id);

        for (TelemetryModAdapter adapter : active) {
            if (adapter.handlesNamespace(namespace)) {
                adapter.onUseOnBlock(
                    player,
                    held,
                    targetState,
                    world,
                    pos,
                    metadata,
                    collector
                );
            }
        }
    }
}
