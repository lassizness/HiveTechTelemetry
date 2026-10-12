package ru.hivetech.telemetry;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TelemetryCompat {
    private TelemetryCompat() {
    }

    public static String sourceFor(ResourceLocation id) {
        if (id == null) {
            return "forge";
        }

        String value = id.toString();
        int separator = value.indexOf(':');
        String namespace =
            separator > 0
                ? value.substring(0, separator)
                : "minecraft";

        if ("ic2".equals(namespace)) {
            return "ic2";
        }

        if ("appliedenergistics2".equals(namespace)) {
            return "ae2";
        }

        if ("forestry".equals(namespace)) {
            return "forestry";
        }

        if (namespace != null && namespace.startsWith("thermal")) {
            return "thermal";
        }

        return "forge";
    }

    public static List<String> detectedIntegrations() {
        List<String> result = new ArrayList<>();

        if (Loader.isModLoaded("ic2")) {
            result.add("ic2");
        }
        if (Loader.isModLoaded("appliedenergistics2")) {
            result.add("ae2");
        }
        if (
            Loader.isModLoaded("thermalexpansion")
            || Loader.isModLoaded("thermalfoundation")
            || Loader.isModLoaded("thermaldynamics")
        ) {
            result.add("thermal");
        }
        if (Loader.isModLoaded("forestry")) {
            result.add("forestry");
        }

        return Collections.unmodifiableList(result);
    }
}
