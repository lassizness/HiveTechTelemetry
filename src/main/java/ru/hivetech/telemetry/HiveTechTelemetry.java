package ru.hivetech.telemetry;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.Logger;
import ru.hivetech.telemetry.compat.TelemetryAdapterRegistry;

import java.io.File;

@Mod(
    modid = HiveTechTelemetry.MODID,
    name = HiveTechTelemetry.NAME,
    version = HiveTechTelemetry.VERSION,
    acceptableRemoteVersions = "*",
    acceptedMinecraftVersions = "[1.12.2]",
    serverSideOnly = true
)
public final class HiveTechTelemetry {
    public static final String MODID = "hivetechtelemetry";
    public static final String NAME = "HiveTech Telemetry";
    public static final String VERSION = "1.1.0";

    public static Logger LOG;

    private static TelemetryConfig config;
    private static TelemetryQueue queue;
    private static TelemetryCollector collector;
    private static TelemetryAdapterRegistry adapters;
    private static TelemetryBridge bridge;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG = event.getModLog();

        File configDir =
            event.getModConfigurationDirectory();

        config = new TelemetryConfig(
            new File(
                configDir,
                "hivetech-telemetry.cfg"
            )
        );
        config.load();

        queue = new TelemetryQueue(
            new File(
                configDir,
                "hivetech-telemetry-queue.json"
            ),
            config.getMaxQueueEvents()
        );
        queue.load();

        collector =
            new TelemetryCollector(queue, config);

        adapters =
            new TelemetryAdapterRegistry();

        LOG.info(
            "HiveTech Telemetry initialized. serverCode={}, credentials={}, queuedEvents={}, integrations={}, adapters={}",
            config.getServerCode(),
            config.hasCredentials() ? "configured" : "missing",
            queue.size(),
            TelemetryCompat.detectedIntegrations(),
            adapters.descriptions()
        );
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        TelemetryEvents events =
            new TelemetryEvents(
                collector,
                adapters
            );

        MinecraftForge.EVENT_BUS.register(events);
        FMLCommonHandler.instance().bus().register(events);
    }

    @Mod.EventHandler
    public void serverStarting(
        FMLServerStartingEvent event
    ) {
        bridge = new TelemetryBridge(
            config,
            queue,
            collector
        );

        event.registerServerCommand(
            new TelemetryCommand()
        );
    }

    @Mod.EventHandler
    public void serverStarted(
        FMLServerStartedEvent event
    ) {
        if (bridge != null) {
            bridge.start();
        }
    }

    @Mod.EventHandler
    public void serverStopping(
        FMLServerStoppingEvent event
    ) {
        if (bridge != null) {
            bridge.stop();
            bridge = null;
        } else if (queue != null) {
            collector.flushCounters();
            queue.persist();
        }
    }

    public static TelemetryBridge getBridge() {
        return bridge;
    }

    public static TelemetryConfig getConfig() {
        return config;
    }

    public static TelemetryQueue getQueue() {
        return queue;
    }

    public static TelemetryAdapterRegistry getAdapters() {
        return adapters;
    }
}
