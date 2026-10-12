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

        if ("gendustry".equals(namespace)) {
            return "gendustry";
        }

        if (
            "binniecore".equals(namespace)
            || "extrabees".equals(namespace)
            || "extratrees".equals(namespace)
            || "genetics".equals(namespace)
            || "botany".equals(namespace)
        ) {
            return "binnie";
        }

        if (namespace != null && namespace.startsWith("thermal")) {
            return "thermal";
        }

        if ("immersiveengineering".equals(namespace)) {
            return "immersiveengineering";
        }

        if ("industrialforegoing".equals(namespace)) {
            return "industrialforegoing";
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
        if (Loader.isModLoaded("gendustry")) {
            result.add("gendustry");
        }
        if (
            Loader.isModLoaded("binniecore")
            || Loader.isModLoaded("extrabees")
            || Loader.isModLoaded("extratrees")
            || Loader.isModLoaded("genetics")
        ) {
            result.add("binnie");
        }
        if (Loader.isModLoaded("immersiveengineering")) {
            result.add("immersiveengineering");
        }
        if (Loader.isModLoaded("industrialforegoing")) {
            result.add("industrialforegoing");
        }

        return Collections.unmodifiableList(result);
    }
}
