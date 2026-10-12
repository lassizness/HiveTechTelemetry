# HiveTechTelemetry

Server-only telemetry agent for HiveTech Minecraft 1.12.2.

## Runtime

- Minecraft 1.12.2
- Forge 14.23.5.2860 / Cleanroom-compatible runtime
- Java 8 bytecode
- no client mod required
- HTTPS-only outbound connection to the HiveTech backend
- per-server `htb_...` Bridge token
- durable JSON queue on disk
- event UUID idempotency across retries
- exponential retry/backoff
- heartbeat every 60 seconds
- no direct database connection

## Collected events

The first version records:

- player login/logout
- playtime
- player death and PvP kills
- block break/place
- item craft/smelt/pickup
- entity kills
- dimension visits
- advancement completion

High-frequency events are aggregated before they enter the durable queue.

IC2, Applied Energistics 2 Extended Life, Forestry/Gendustry/Binnie,
Thermal Expansion/Foundation, Immersive Engineering and Industrial Foregoing
are handled by optional deep adapters on top of the generic Forge layer.

The adapters expose normalized machine/component/tool/genetics events without
hard-linking the server to third-party mod classes. See
[docs/events.md](docs/events.md).

## Backend

Default API base:

```text
https://hivegrid.online/internal/game
```

Endpoints:

```text
POST /telemetry/events
POST /telemetry/heartbeat
```

Authentication:

```http
Authorization: Bearer htb_...
X-HiveTech-Server: HiveTech
```

The token must never be committed or printed to logs.

## Configuration

Forge creates:

```text
config/hivetech-telemetry.cfg
```

and the durable queue:

```text
config/hivetech-telemetry-queue.json
```

A safe example without a token is in
[docs/hivetech-telemetry.cfg.example](docs/hivetech-telemetry.cfg.example).

## Commands

Operator-only:

```text
/httelemetry status
/httelemetry flush
```

`status` shows queue size, buffered counters, online players, sent/duplicate
counts, HTTP status, failures, last successful batch and the latest error.

`flush` asks the worker to flush buffered counters and retry the queue as
soon as possible.

`adapters` prints the active technology adapters together with Forge-reported
mod versions.

## Build

Pull requests are compiled by GitHub Actions with Java 8.


The project intentionally follows the same build chain as HiveTechModeration:

```bash
bash build.sh
```

On the HiveTech build host it automatically uses:

```text
/opt/hivetech/runtime/java8
```

and Gradle 4.10.3.

Output:

```text
build/libs/HiveTechTelemetry-1.1.0.jar
```

## Safety

The mod never performs HTTP on the Minecraft server thread. Forge event
handlers only update small in-memory counters or append an event to the local
queue. A single daemon worker performs persistence, batching, heartbeat and
network I/O.

If the backend is unavailable, queued events remain on disk and are retried
with backoff. The server can continue running without the telemetry backend.

## License

Apache License 2.0.
