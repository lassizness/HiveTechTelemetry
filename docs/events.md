# Event catalog

HiveTechTelemetry 1.1 emits server-side events only.

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

Minecraft 1.12 frequently stores several variants under one registry ID. When
the item/block metadata is non-zero, the object is emitted as
`registry:name@meta`.

## Adapter model

Deep adapters are optional and isolated from mod APIs. They activate only when
the corresponding mod is loaded. No adapter class has a hard runtime
dependency on a third-party mod JAR, so removing one technology mod does not
prevent HiveTechTelemetry from starting.

Use:

```text
/httelemetry adapters
```

to see the actual loaded mod versions detected by Forge.

Adapters enrich generic events with registry IDs, item/tile classes and
normalized technology events. These normalized events are intended for
achievements and rankings.

## AE2 Extended Life

Mod id:

```text
appliedenergistics2
```

Source:

```text
ae2
```

Events include:

```text
ae2.item.craft
ae2.item.smelt
ae2.item.pickup
ae2.block.place
ae2.block.break
ae2.machine.place
ae2.machine.break
ae2.network_component.place
ae2.network_component.break
ae2.controller.craft
ae2.controller.place
ae2.storage_cell.craft
ae2.storage_cell.pickup
ae2.crafting_storage.craft
ae2.interface.craft
ae2.interface.place
ae2.molecular_assembler.craft
ae2.molecular_assembler.place
ae2.drive.craft
ae2.drive.place
ae2.terminal.craft
ae2.network_tool.use
```

The production server currently uses AE2 Extended Life rv6 stable 7
v0.54.20. The adapter remains mapping-independent and does not link against
its classes.

AE2 autocrafting completion is not guessed from ordinary Forge interaction
events. A future exact API/reflection hook can add `ae2.autocraft.*` after
its internal runtime path is verified.

## IC2

Mod id and source:

```text
ic2
```

Events include:

```text
ic2.item.craft
ic2.item.pickup
ic2.block.place
ic2.block.break
ic2.machine.place
ic2.machine.break
ic2.generator.place
ic2.energy_storage.place
ic2.energy.tier_reached
ic2.wrench.use
```

Machine classification uses the placed/broken tile entity class when
available. Energy storage milestones recognize BatBox, CESU, MFE and MFSU
families.

`ic2.energy.tier_reached` is a gameplay fact emitted when the player places
a recognized tiered storage block. Achievement unlocks remain idempotent even
if the player later places another block of the same tier.

## Forestry, Gendustry and Binnie

Loaded mod ids handled by one genetics adapter:

```text
forestry
gendustry
binniecore
extrabees
extratrees
genetics
botany
```

Generic event sources preserve the family where possible. Normalized genetics
events use `forestry` as the common source.

Events include:

```text
forestry.item.craft
forestry.item.pickup
forestry.block.place
forestry.block.break
forestry.bee.acquire
forestry.bee.discovered
forestry.bee_item.craft
forestry.genetics_item.pickup
forestry.apiary.place
forestry.alveary.place
forestry.machine.place
forestry.genetics_tool.use
```

Bee species are extracted from the stack NBT recursively. Common Forestry
chromosome keys such as `Species`, `UID0` and related nested genome data
are recognized without importing Forestry API classes.

`forestry.bee.discovered` ends in `.discovered`, therefore the backend
also writes it to `player_discoveries` and increments the player's unique
species count only once per species/server.

A confirmed biological mutation is not inferred merely because a bee item was
picked up. The adapter records species discovery now; a later exact Forestry
mutation hook can add a separate mutation event.

## Thermal Expansion / Foundation / Dynamics

Handled mod ids:

```text
thermalexpansion
thermalfoundation
thermaldynamics
```

Source:

```text
thermal
```

Events include:

```text
thermal.item.craft
thermal.item.pickup
thermal.block.place
thermal.block.break
thermal.machine.place
thermal.dynamo.place
thermal.storage.place
thermal.augment.craft
thermal.augment.pickup
thermal.augment.use
thermal.upgrade.craft
thermal.upgrade.use
```

Right-click usage is a confirmed interaction event, not proof that an augment
was installed successfully. `thermal.augment.use` therefore contains:

```json
{"confirmed_install":false}
```

and must not be used as a strict "installed augment" achievement until a
version-specific confirmation hook is added.

## Immersive Engineering

Mod id/source:

```text
immersiveengineering
```

Events include:

```text
immersiveengineering.item.craft
immersiveengineering.block.place
immersiveengineering.machine.place
immersiveengineering.capacitor.place
immersiveengineering.connector.place
immersiveengineering.multiblock_component.place
immersiveengineering.tool.craft
immersiveengineering.hammer.use
immersiveengineering.wire_coil.craft
immersiveengineering.wire_coil.use
```

Hammer right-clicks are recorded as tool usage. The event explicitly does not
claim that a multiblock was successfully formed.

## Industrial Foregoing

Mod id/source:

```text
industrialforegoing
```

Events include:

```text
industrialforegoing.item.craft
industrialforegoing.item.pickup
industrialforegoing.block.place
industrialforegoing.block.break
industrialforegoing.machine.place
industrialforegoing.addon.craft
industrialforegoing.addon.pickup
industrialforegoing.addon.use
```

## Metadata

Adapter events may add safe diagnostic metadata such as:

```json
{
  "dimension": 0,
  "adapter": "ic2",
  "tile_class": "ic2.core.block.machine.tileentity.TileEntityMacerator",
  "item_class": "..."
}
```

This makes the event catalog self-discovering from the admin interface: if a
modpack version uses an unexpected registry/meta mapping, the exact class and
unlocalized name remain visible without changing the backend schema.

## What is deliberately not guessed

HiveTechTelemetry does not currently fabricate these facts from ambiguous
player interactions:

- AE2 autocraft completion;
- exact AE2 network ownership/size;
- IC2 EU produced/consumed per player;
- confirmed Forestry mutation parentage;
- confirmed Thermal augment installation;
- confirmed Immersive Engineering multiblock formation;
- shared machine output attribution to a single player.

Those require exact internal hooks or an ownership model. Generic and
normalized events continue working regardless.
