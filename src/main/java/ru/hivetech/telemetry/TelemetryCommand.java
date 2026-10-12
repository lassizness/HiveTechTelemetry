package ru.hivetech.telemetry;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class TelemetryCommand extends CommandBase {
    @Override
    public String getName() {
        return "httelemetry";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/httelemetry <status|flush>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(
        MinecraftServer server,
        ICommandSender sender,
        String[] args
    ) throws CommandException {
        TelemetryBridge bridge =
            HiveTechTelemetry.getBridge();

        if (bridge == null) {
            sender.sendMessage(
                new TextComponentString(
                    "[HiveTechTelemetry] Bridge is not initialized."
                )
            );
            return;
        }

        String action =
            args.length == 0
                ? "status"
                : args[0].toLowerCase();

        if ("flush".equals(action)) {
            bridge.requestFlush();
            sender.sendMessage(
                new TextComponentString(
                    "[HiveTechTelemetry] Flush requested."
                )
            );
            return;
        }

        if (!"status".equals(action)) {
            throw new CommandException(getUsage(sender));
        }

        sender.sendMessage(
            new TextComponentString(
                "[HiveTechTelemetry] "
                + (bridge.isStarted() ? "RUNNING" : "STOPPED")
            )
        );
        sender.sendMessage(
            new TextComponentString(
                "Queue: "
                + bridge.getQueueSize()
                + ", buffered: "
                + bridge.getBufferedCounters()
                + ", players: "
                + bridge.getOnlinePlayers()
            )
        );
        sender.sendMessage(
            new TextComponentString(
                "Sent: "
                + bridge.getSentEvents()
                + ", duplicates: "
                + bridge.getDuplicateEvents()
                + ", unresolved: "
                + bridge.getUnresolvedPlayers()
            )
        );
        sender.sendMessage(
            new TextComponentString(
                "HTTP: "
                + (
                    bridge.getLastHttpStatus() == 0
                        ? "—"
                        : bridge.getLastHttpStatus()
                )
                + ", failures: "
                + bridge.getConsecutiveFailures()
            )
        );
        sender.sendMessage(
            new TextComponentString(
                "Last success: "
                + formatTime(bridge.getLastSuccessAt())
                + ", heartbeat: "
                + formatTime(bridge.getLastHeartbeatAt())
            )
        );
        sender.sendMessage(
            new TextComponentString(
                "Last error: "
                + (
                    bridge.getLastError().isEmpty()
                        ? "—"
                        : bridge.getLastError()
                )
            )
        );
    }

    private static String formatTime(long millis) {
        if (millis <= 0L) {
            return "—";
        }

        SimpleDateFormat format =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        format.setTimeZone(
            TimeZone.getDefault()
        );

        return format.format(new Date(millis));
    }
}
