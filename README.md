# Optimizator

Client-side optimization mod for Minecraft 1.21.4 + Fabric.

Optimizator is built around a different goal from a simple video-settings tweak: reduce renderer work while preserving the requested visual result whenever technically possible.

## Default safety profile

On a fresh install, **all optimization features are OFF**. The master switch is also OFF. Every implemented optimization can be enabled or disabled by the user, and supported numeric parameters are exposed in the settings screens. Older pre-v2 configs are migrated to this neutral profile.

## Current optimization layer

- Adaptive performance controller with hysteresis (opt-in; visual-parity safe by default).
- Adaptive entity-distance scaling.
- Automatic cloud disabling under sustained load, with restoration.
- Chunk GPU-upload budgeting to reduce render-thread spikes.
- Conservative far culling for item entities and XP orbs.
- Particle filtering before Particle objects are allocated (opt-in by default).
- Particle quality modes: ALL, DECREASED, MINIMAL.
- Particle budget per second.
- Optional particle distance culling.
- Per-particle-type disable and reduced lists, including trailing * wildcards.
- Optional profiler and live HUD overlay.
- Persistent config in config/optimizator.properties.

## User configuration

Open `Pause menu -> Optimizator`.

The main settings are split into pages for small screens:

- General / Profiler: master switch, adaptive controller, profiler and overlay.
- Entities / Adaptive: entity culling, shadow control, distances, FPS floor and cloud behaviour.
- Chunks: upload budget and upload timing limits.
- Particle settings: quality, limiter, culling, budget and per-type rules.
- Performance profiler: live metrics and bottleneck information.

All optimization switches are OFF after a fresh install or a full reset.

## Detailed particle controls

Open:

Pause menu -> Optimizator -> Particle settings

Available controls:

- Particle quality: ALL / DECREASED / MINIMAL.
- Global particle limiter.
- Distance culling.
- Maximum particles per second.
- Particle culling distance.
- Disabled particle IDs.
- Reduced particle IDs.

Examples:

```text
minecraft:smoke,minecraft:campfire_cosy_smoke
minecraft:ash,minecraft:ambient_entity_effect
minecraft:*
```

Filtered particles are rejected from ClientWorld.addParticle(...) before Minecraft creates the Particle instance. This is deeper than only changing the vanilla particle option because rejected particles never enter the particle queues.

## Deep renderer direction

The long-term target is not to replace one vanilla setting with another.

The next renderer work is focused on:

1. Section visibility and occlusion.
2. Asynchronous visibility work.
3. Cancellation and prioritization of obsolete chunk rebuilds.
4. Reusable mesh/GPU buffers.
5. Block-face culling during mesh generation.
6. Fog-aware section visibility.
7. Chunk-visibility-based entity culling.

Minecraft 1.21.4 already contains the renderer-side primitives needed for this direction: WorldRenderer exposes chunk/entity collection stages, built chunks and a ChunkBuilder, while chunk data contains occlusion information. Sodium uses similar concepts with asynchronous culling, block-face culling, fog occlusion, entity culling and buffer reuse.

## Visual-preservation rule

By default, Optimizator should not intentionally lower texture/detail quality.

Settings that can visibly change the scene are explicit and configurable:

- particle quality/filtering;
- adaptive render distance;
- adaptive entity distance;
- cloud disabling;
- optional entity-shadow removal.

The preferred optimizations are those that remove invisible or redundant renderer work, reduce allocations, improve scheduling and reuse existing buffers.

## Adaptive controller

The controller reacts every 40 client ticks and requires consecutive low/high samples before changing its reduction level.

Dynamic render-distance changes can themselves trigger expensive chunk refreshes, so adaptive settings are not considered the final deep-optimization path.

## Build

GitHub Actions uses Java 21 and Gradle 8.12 and runs:

```text
gradle build --no-daemon
```

Production jars are uploaded as workflow artifacts.

## Compatibility

Target: Minecraft 1.21.4, Fabric Loader 0.16.14+, Fabric API 0.119.2+, Java 21+.

The mod is client-only and does not change server simulation, networking, mob AI, redstone or random ticks.

## Performance methodology

Future renderer changes should be accepted only after comparing frame time and checking:

- chunk rebuild pressure;
- upload queue pressure;
- Java allocations;
- visibility/culling cost;
- entity rendering cost;
- particle creation rate;
- GPU upload time;
- visual correctness after resource reload, chunk rebuilds and fast camera movement.
