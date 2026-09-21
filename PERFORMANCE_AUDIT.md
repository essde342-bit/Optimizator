# Performance audit — Minecraft 1.21.4 client

## Goal

Optimizator is intended to increase FPS by reducing work inside the client renderer, not by simply forcing low vanilla video settings.

Preferred order:

1. remove work that cannot affect the final image;
2. reuse data instead of allocating it repeatedly;
3. move independent work away from the render thread;
4. batch GPU uploads and draw work;
5. only then use adaptive visual reductions when the user enables them.

This is the same general direction used by modern renderer optimizers such as Sodium: chunk building, culling, buffer management, and render submission are first-class optimization targets rather than only video-option changes. Sodium exposes settings for chunk update threading, deferred chunk updates, block-face culling, fog occlusion and entity culling. turn645209search0 turn645209search5

## Current implementation

### 1. Pre-allocation particle filtering

The particle filter runs from ClientWorld.addParticle(...) before Minecraft creates the Particle object.

Supported controls:

- ALL / DECREASED / MINIMAL particle quality;
- global particles on/off;
- distance culling before allocation;
- particles-per-second budget;
- exact particle type disable list;
- exact particle type reduced list;
- trailing * wildcards such as minecraft:smoke*.

This goes deeper than changing the vanilla particle option because unwanted particles are rejected before they enter the ParticleManager queues.

Sodium exposes the same three conceptual quality levels, while dedicated particle optimization mods also demonstrate the value of culling invisible particles and filtering by type. turn645209search0 turn645209search5

### 2. Chunk GPU-upload budgeting

The current ChunkBuilderMixin limits how many queued GPU upload tasks can execute in one frame and can additionally stop after a configured time budget.

The target is not to reduce final geometry quality, but to prevent a large batch of chunk uploads from creating a render-thread spike.

Vanilla 1.21.4 ChunkBuilder contains a dedicated uploadQueue and scheduler, so this is a genuine renderer-internal hook rather than a video-option change. turn495864search1

### 3. Entity pressure

Current code uses:

- adaptive entity-distance scaling;
- optional far culling for item entities and XP orbs.

This is intentionally conservative. Sodium's entity culling uses chunk visibility information to skip entities hidden inside non-visible chunks, which is deeper than a distance check. turn645209search5

## Deep renderer targets

### A. Section visibility and occlusion

Vanilla 1.21.4 already has:

- a Frustum;
- built chunk storage;
- chunk occlusion data;
- an Octree;
- a chunk-rendering data preparer.

WorldRenderer also exposes an updateChunks stage and an entity collection stage. turn442879search0 turn965233search3 turn965233search5

The next serious optimization target is therefore not another distance slider. It is a renderer-side visibility pipeline that avoids traversing or rendering sections known to be invisible.

Sodium's current renderer schedules asynchronous culling work and consumes the resulting render lists instead of rebuilding visibility from scratch on the render thread. turn645209search8

### B. Chunk mesh generation

ChunkBuilder.BuiltChunk owns rebuild/sort tasks and the 1.21.4 pipeline contains a dedicated SectionBuilder. turn442879search1 turn965233search1

Long-term implementation should investigate:

- duplicate rebuild requests;
- rebuild prioritization by camera distance;
- cancellation of obsolete rebuilds;
- allocation reuse for mesh buffers;
- minimizing temporary Java objects while emitting quads;
- separating opaque and translucent work earlier.

This area has high performance leverage, but it is also where incorrect mixins can corrupt buffers or race with resource reloads.

### C. Buffer allocation and upload reuse

Sodium's development history explicitly contains work on buffer reuse because repeated buffer allocations can worsen frame-time stability during chunk loading. turn645209search1

Optimizator should therefore avoid an implementation that merely throttles uploads while still producing excessive short-lived buffers. The deeper goal is to reduce allocation volume itself.

### D. Block-face submission

Sodium exposes a block-face-culling option because eliminating faces which cannot be seen can remove geometry very early in the pipeline. turn645209search5

For Optimizator, this is a high-value target because it can preserve visual output while reducing vertex generation and GPU bandwidth. It should be implemented inside chunk mesh generation, not by changing texture or graphics settings.

### E. Fog-aware visibility

Vanilla has a dedicated fog/render path, and Sodium exposes fog occlusion to skip chunks fully hidden by fog. turn645209search0 turn645209search5

This can be added as a visibility decision without reducing requested graphics quality, but the implementation must account for underwater, lava, blindness/darkness and unusual camera angles.

### F. Entity visibility

The deeper end goal is chunk-visibility-based entity culling rather than simply reducing entity distance.

That work belongs close to WorldRenderer.getEntitiesToRender(...) and the chunk visibility data so the renderer can reject entities behind fully occluding terrain. The method exists directly in vanilla 1.21.4. turn442879search0

## Adaptive controller

The adaptive controller currently has hysteresis and reacts every 40 client ticks.

This is intentionally not the main optimization mechanism. Dynamic render distance can cause expensive chunk refreshes/rebuilds; Sodium has an issue documenting that dynamic render-distance changes on Minecraft 1.21.4 can trigger full rendering refresh behavior. turn645209search2

Future revisions should prefer:

1. deep culling;
2. scheduling and batching;
3. buffer reuse;
4. only then adaptive visual changes.

## Visual-preservation rule

Defaults should preserve vanilla visual output wherever possible.

A setting that changes the image should be explicit:

- particle quality;
- particle filters;
- adaptive render distance;
- adaptive entity distance;
- cloud disabling;
- optional shadow removal.

Deep culling, mesh pruning, buffer reuse and render-thread scheduling are preferred because they can reduce work without intentionally lowering texture/detail quality.

## Java vs native/GPU code

Java is sufficient for the current phase.

A native library is not automatically faster here because the main opportunities are inside Minecraft's renderer data structures and OpenGL submission path. Native code becomes interesting later only for a measured hotspot such as:

- highly specialized frustum/occlusion math;
- SIMD batch processing;
- a reusable native allocator.

GPU shader changes are a separate path and should not be used to hide CPU-side renderer inefficiencies.

## Validation

Every deep renderer change must pass:

1. GitHub Actions Gradle build;
2. client startup with default config;
3. resource reload;
4. world join/leave;
5. F3+A style chunk rebuild;
6. teleport and fast camera movement;
7. particle storm;
8. translucent blocks;
9. entity-heavy scene;
10. visual comparison against vanilla.

Performance measurements should compare frame time, not only average FPS.

## Current priority

The highest-value implementation sequence for Optimizator is:

particle pre-allocation filter -> chunk upload scheduling -> chunk visibility/occlusion -> rebuild cancellation/prioritization -> buffer reuse -> block-face culling -> deep entity culling -> optional native/GPU experiments.
