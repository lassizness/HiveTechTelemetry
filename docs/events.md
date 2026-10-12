# Event catalog

HiveTechTelemetry 1.0 emits server-side events only.

## Core events

| Event | Aggregated | Object |
| --- | --- | --- |
| `player.login` | no | - |
| `player.logout` | no | - |
| `player.playtime` | yes | - |
| `player.death` | no | - |
| `player.kill` | yes | `player` |
| `block.break` | yes | block registry id |
| `block.place` | yes | block registry id |
| `item.craft` | yes | item registry id |
| `item.smelt` | yes | item registry id |
| `item.pickup` | yes | item registry id |
| `entity.kill` | yes | entity registry id |
| `dimension.enter` | no | `dimension:<id>` |
| `advancement.complete` | no | advancement id |

High-frequency events are aggregated in memory before they enter the durable
queue. The queued event gets one UUID and keeps it for every HTTP retry.

## Technology mod classification

The generic Forge layer already records registry IDs from the installed
technology mods. The `source` field is classified as:

- `ic2` for namespace `ic2`
- `ae2` for namespace `appliedenergistics2`
- `thermal` for namespaces beginning with `thermal`
- `forestry` for namespace `forestry`
- `forge` otherwise

This means achievements such as "craft an AE2 controller", "place an IC2
machine", or "pick up a Forestry item" work before any deep compatibility
adapter exists.

Deep adapter events such as AE2 autocrafting, Forestry mutations and IC2
energy milestones will be added on top of this transport after pinning the
exact installed mod API versions.
