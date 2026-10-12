package ru.hivetech.telemetry.compat;

import com.google.gson.JsonObject;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import ru.hivetech.telemetry.TelemetryCollector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractNamespaceAdapter
    implements TelemetryModAdapter {

    private final String id;
    private final Set<String> modIds;
    private final Set<String> namespaces;

    protected AbstractNamespaceAdapter(
        String id,
        String[] modIds,
        String[] namespaces
    ) {
        this.id = id;
        this.modIds =
            new HashSet<>(Arrays.asList(modIds));
        this.namespaces =
            new HashSet<>(Arrays.asList(namespaces));
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public boolean isLoaded() {
        for (String modId : modIds) {
            if (Loader.isModLoaded(modId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean handlesNamespace(String namespace) {
        return namespaces.contains(namespace);
    }

    @Override
    public void onItem(
        EntityPlayerMP player,
        String action,
        ItemStack stack,
        String objectId,
        JsonObject metadata,
        TelemetryCollector collector
    ) {
        String fingerprint =
            CompatUtil.itemFingerprint(stack);

        JsonObject extra =
            CompatUtil.copyMetadata(metadata);

        extra.addProperty(
            "adapter",
            getId()
        );

        extra.addProperty(
            "item_class",
            stack.getItem().getClass().getName()
        );

        emitCounter(
            collector,
            player,
            getId() + ".item." + action,
            objectId,
            Math.max(1, stack.getCount()),
            extra
        );

        classifyItem(
            player,
            action,
            stack,
            objectId,
            fingerprint,
            extra,
            collector
        );
    }

    @Override
    public void onBlock(
        EntityPlayerMP player,
        String action,
        IBlockState state,
        World world,
        BlockPos pos,
        String objectId,
        JsonObject metadata,
        TelemetryCollector collector
    ) {
        String fingerprint =
            CompatUtil.blockFingerprint(
                state,
                world,
                pos
            );

        JsonObject extra =
            CompatUtil.copyMetadata(metadata);

        extra.addProperty(
            "adapter",
            getId()
        );

        CompatUtil.addMachineMetadata(
            extra,
            world,
            pos
        );

        emitCounter(
            collector,
            player,
            getId() + ".block." + action,
            objectId,
            1L,
            extra
        );

        classifyBlock(
            player,
            action,
            state,
            world,
            pos,
            objectId,
            fingerprint,
            extra,
            collector
        );
    }

    @Override
    public void onUseOnBlock(
        EntityPlayerMP player,
        ItemStack held,
        IBlockState targetState,
        World world,
        BlockPos pos,
        JsonObject metadata,
        TelemetryCollector collector
    ) {
        if (held == null || held.isEmpty()) {
            return;
        }

        JsonObject extra =
            CompatUtil.copyMetadata(metadata);

        extra.addProperty("adapter", getId());

        CompatUtil.addMachineMetadata(
            extra,
            world,
            pos
        );

        String objectId =
            CompatUtil.registryObject(held);

        emitCounter(
            collector,
            player,
            getId() + ".tool.use",
            objectId,
            1L,
            extra
        );

        classifyUse(
            player,
            held,
            targetState,
            world,
            pos,
            objectId,
            CompatUtil.itemFingerprint(held),
            extra,
            collector
        );
    }

    protected void classifyItem(
        EntityPlayerMP player,
        String action,
        ItemStack stack,
        String objectId,
        String fingerprint,
        JsonObject metadata,
        TelemetryCollector collector
    ) {
    }

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
    }

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
    }

    protected final void emitCounter(
        TelemetryCollector collector,
        EntityPlayerMP player,
        String event,
        String object,
        long amount,
        JsonObject metadata
    ) {
        collector.emitCounter(
            player,
            event,
            getId(),
            CompatUtil.limitedObject(object),
            amount,
            metadata
        );
    }

    protected final void emitImmediate(
        TelemetryCollector collector,
        EntityPlayerMP player,
        String event,
        String object,
        JsonObject metadata
    ) {
        collector.emitImmediate(
            player,
            event,
            getId(),
            CompatUtil.limitedObject(object),
            1L,
            metadata
        );
    }

    @Override
    public String describe() {
        StringBuilder value =
            new StringBuilder(getId());

        value.append('[');

        boolean first = true;

        for (String modId : modIds) {
            if (!Loader.isModLoaded(modId)) {
                continue;
            }

            if (!first) {
                value.append(',');
            }

            first = false;
            value.append(modId);

            ModContainer container =
                Loader.instance()
                    .getIndexedModList()
                    .get(modId);

            if (
                container != null
                && container.getVersion() != null
            ) {
                value.append('=');
                value.append(
                    container.getVersion()
                );
            }
        }

        value.append(']');

        return value.toString();
    }
}
