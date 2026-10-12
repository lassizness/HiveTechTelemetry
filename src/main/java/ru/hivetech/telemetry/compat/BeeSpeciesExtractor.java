package ru.hivetech.telemetry.compat;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.util.Locale;
import java.util.Set;

public final class BeeSpeciesExtractor {
    private BeeSpeciesExtractor() {
    }

    public static String extract(ItemStack stack) {
        if (
            stack == null
            || stack.isEmpty()
            || !stack.hasTagCompound()
        ) {
            return null;
        }

        return find(
            stack.getTagCompound(),
            0
        );
    }

    private static String find(
        NBTBase tag,
        int depth
    ) {
        if (tag == null || depth > 6) {
            return null;
        }

        if (tag instanceof NBTTagString) {
            String value =
                ((NBTTagString) tag).getString();

            return looksLikeSpecies(value)
                ? value
                : null;
        }

        if (tag instanceof NBTTagCompound) {
            NBTTagCompound compound =
                (NBTTagCompound) tag;

            Set<String> keys =
                compound.getKeySet();

            for (String preferred : new String[] {
                "Species",
                "species",
                "UID0",
                "uid0",
                "Primary",
                "primary"
            }) {
                if (!keys.contains(preferred)) {
                    continue;
                }

                String direct =
                    compound.getString(preferred);

                if (looksLikeSpecies(direct)) {
                    return direct;
                }
            }

            for (String key : keys) {
                NBTBase child =
                    compound.getTag(key);

                String value =
                    find(child, depth + 1);

                if (value != null) {
                    return value;
                }
            }

            return null;
        }

        if (tag instanceof NBTTagList) {
            NBTTagList list =
                (NBTTagList) tag;

            for (int i = 0; i < list.tagCount(); i++) {
                String value =
                    find(
                        list.get(i),
                        depth + 1
                    );

                if (value != null) {
                    return value;
                }
            }
        }

        return null;
    }

    private static boolean looksLikeSpecies(
        String value
    ) {
        if (value == null) {
            return false;
        }

        String normalized =
            value.trim()
                .toLowerCase(Locale.ROOT);

        if (
            normalized.length() < 4
            || normalized.length() > 128
        ) {
            return false;
        }

        return normalized.contains("species")
            || normalized.startsWith("forestry.")
            || normalized.startsWith("extrabees.")
            || normalized.startsWith("gendustry.")
            || normalized.startsWith("binnie.");
    }
}
