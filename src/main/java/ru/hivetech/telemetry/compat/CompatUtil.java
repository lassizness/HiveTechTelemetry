package ru.hivetech.telemetry.compat;

import com.google.gson.JsonObject;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Locale;

public final class CompatUtil {
    private CompatUtil() {
    }

    public static String namespace(ResourceLocation id) {
        if (id == null) {
            return "minecraft";
        }

        String value = id.toString();
        int separator = value.indexOf(':');

        return separator > 0
            ? value.substring(0, separator)
            : "minecraft";
    }

    public static String itemFingerprint(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }

        StringBuilder value = new StringBuilder();

        ResourceLocation id =
            stack.getItem().getRegistryName();

        append(value, id == null ? null : id.toString());
        append(value, stack.getItem().getClass().getName());

        try {
            append(value, stack.getUnlocalizedName());
        } catch (RuntimeException ignored) {
        }

        return value
            .toString()
            .toLowerCase(Locale.ROOT);
    }

    public static String blockFingerprint(
        IBlockState state,
        World world,
        BlockPos pos
    ) {
        StringBuilder value = new StringBuilder();

        if (state != null) {
            ResourceLocation id =
                state.getBlock().getRegistryName();

            append(
                value,
                id == null ? null : id.toString()
            );
            append(
                value,
                state.getBlock().getClass().getName()
            );
        }

        TileEntity tile = tile(world, pos);

        if (tile != null) {
            append(value, tile.getClass().getName());
        }

        return value
            .toString()
            .toLowerCase(Locale.ROOT);
    }

    public static TileEntity tile(
        World world,
        BlockPos pos
    ) {
        if (world == null || pos == null) {
            return null;
        }

        try {
            return world.getTileEntity(pos);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static JsonObject copyMetadata(
        JsonObject metadata
    ) {
        if (metadata == null) {
            return new JsonObject();
        }

        return metadata.deepCopy();
    }

    public static void addMachineMetadata(
        JsonObject metadata,
        World world,
        BlockPos pos
    ) {
        TileEntity tile = tile(world, pos);

        if (tile != null) {
            metadata.addProperty(
                "tile_class",
                tile.getClass().getName()
            );
        }
    }

    public static boolean containsAny(
        String value,
        String... needles
    ) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        String normalized =
            value.toLowerCase(Locale.ROOT);

        for (String needle : needles) {
            if (
                needle != null
                && !needle.isEmpty()
                && normalized.contains(
                    needle.toLowerCase(Locale.ROOT)
                )
            ) {
                return true;
            }
        }

        return false;
    }

    public static String limitedObject(
        String value
    ) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.length() <= 128) {
            return trimmed;
        }

        return trimmed.substring(0, 128);
    }

    public static String registryObject(
        ItemStack stack
    ) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        ResourceLocation id =
            stack.getItem().getRegistryName();

        if (id == null) {
            return null;
        }

        String value = id.toString();
        int meta = stack.getMetadata();

        return limitedObject(
            meta == 0
                ? value
                : value + "@" + meta
        );
    }

    private static void append(
        StringBuilder target,
        String value
    ) {
        if (value == null || value.isEmpty()) {
            return;
        }

        if (target.length() > 0) {
            target.append(' ');
        }

        target.append(value);
    }
}
