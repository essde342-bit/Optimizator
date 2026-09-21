# Optimizator — deep performance audit
## Minecraft Java 1.21.4 Fabric

Audit scope: current `main` branch after the profiler, particle and chunk-upload changes.

## 1. Current status

### Build/runtime correctness

The current codebase had several concrete issues during this audit:

- missing `MinecraftClient` import in `ProfilerScreen`;
- missing `PerformanceProfiler` import in `ChunkBuilderMixin`;
- invalid `@Shadow` of inherited `Screen.addDrawableChild` from a `GameMenuScreen` mixin;
- entity culling modified the renderer's entity list in-place;
- profiler's so-called 1% low metric was actually the single worst frame;
- profiler was enabled by default, adding measurement overhead to the normal render loop;
- particle filtering performed registry identifier conversion and wall-clock reads for every particle spawn even when no particle filtering rule was configured.

These have been corrected in the current branch.

The last GitHub Actions run at the time of this audit is still being executed, so the final current-head compile/runtime result is intentionally not claimed until that run finishes.

## 2. Important architectural finding

Optimizator is currently **not yet a Sodium-class renderer replacement**.

The largest remaining performance gap is architectural:

`WorldRenderer -> vanilla BuiltChunkStorage -> vanilla ChunkBuilder -> vanilla SectionBuilder -> vanilla GPU upload`

is still fundamentally the vanilla pipeline.

The current mod mostly wraps parts of that pipeline with:

- upload budgeting;
- particle filtering;
- conservative entity filtering;
- adaptive options;
- profiling.

Those can help, but they do not yet replace the expensive mesh/culling/scheduling decisions.

## 3. Highest-value optimization targets

### P0 — Chunk rebuild coalescing

Implement one pending update state per render section.

Instead of allowing:

`update -> rebuild`
`update -> rebuild`
`update -> rebuild`

maintain:

`update + update + update -> one rebuild`

The pending state should merge update types and retain only the strongest required rebuild.

Sodium's current RenderSectionManager explicitly tracks pending updates and joins compatible update types instead of blindly submitting duplicate work. citeturn861066search2

### P0 — Obsolete rebuild cancellation

A rebuild result must carry a section generation/version.

Before applying a completed mesh:

`result.version == section.currentVersion`

Only then may the result become renderable.

Otherwise the result is destroyed/discarded.

This prevents wasted CPU and GPU work when the same section has changed again while an old build is still running.

### P0 — Section visibility graph

Do not independently test every chunk section from scratch every frame.

Create a compact render-section graph with:

- section coordinates;
- neighboring links;
- opaque/transparent presence flags;
- dirty state;
- current mesh state;
- last visible frame;
- rebuild generation.

The visibility graph should be traversed from the camera section.

Sodium's renderer uses a render-section tree plus cull results and asynchronous culling tasks. citeturn861066search2

### P0 — Occlusion traversal

Use existing chunk occlusion information before mesh submission.

Target pipeline:

`camera -> frustum -> section graph -> occlusion -> render list`

Do not rebuild the same visibility information during the draw phase.

Minecraft 1.21.4 already exposes Frustum, built chunks, ChunkRenderingDataPreparer, ChunkBuilder and captured-frustum data in WorldRenderer. citeturn618935search0

### P1 — Block-face culling during mesh generation

Remove faces before vertex generation.

Target:

`block -> neighbor visibility -> emit only required faces`

not:

`block -> build all faces -> discard later`

Sodium explicitly exposes block-face culling because eliminating faces early reduces rendering work substantially. citeturn861066search0

### P1 — Buffer reuse

The current mod throttles uploads but still relies on vanilla allocation/lifetime decisions.

Next phase should investigate:

- reusable per-section vertex storage;
- reuse of upload buffers;
- reuse of index buffers;
- minimizing temporary BuiltBuffer lifetime;
- reducing CPU/GPU synchronization.

### P1 — Rebuild prioritization

Priority should incorporate:

`visibility + camera distance + urgency + age`

Suggested order:

`VISIBLE_NEAR > VISIBLE_FAR > INVISIBLE_NEAR > INVISIBLE_FAR`

but an old invisible task must still be prevented from starving if it is repeatedly postponed.

### P1 — Translucent sorting

Translucent geometry should not be resorted when the camera movement is too small to affect ordering materially.

Use a camera-motion threshold and invalidate the sort only when the ordering can actually change.

This must be validated aggressively because translucent correctness is a common source of visual regressions.

### P1 — Fog occlusion

Skip sections that are guaranteed to be completely hidden by fog.

Sodium exposes fog occlusion as a renderer optimization, especially useful in heavy fog environments such as underwater scenes. citeturn861066search0

### P2 — Chunk-visibility entity culling

The current entity culling is intentionally conservative and is **not** the final design.

The correct deep implementation should reuse section visibility:

`entity -> containing section -> visible section?`

If the section is not visible, the entity renderer should be skipped.

Sodium documents this exact principle: entity culling can reuse chunk visibility data without adding a second expensive visibility system. citeturn861066search0

## 4. Current code-specific findings

### Particle filtering

The new fast path is substantially better than the initial implementation.

When the user has not configured per-type rules, the code no longer needs to convert the particle type to an Identifier string for every particle.

Camera position is cached once per client tick instead of querying it for every spawned particle.

The remaining optimization target is the **particle update/render loop itself**.

ParticleManager has:

- particle queues by texture sheet;
- a particle tick path;
- a render path;
- particle-group limits.

Minecraft 1.21.4 also exposes ParticleManager's per-texture-sheet queues and update/render methods. citeturn638663search0

The next particle phase should therefore optimize:

`spawn -> update -> visibility -> batch -> render`

instead of only:

`spawn -> reject`

### Chunk upload budget

The current upload limiter works at the upload-queue boundary, which is useful for preventing a burst of GPU uploads from monopolizing the render thread.

However, it is not yet a true chunk scheduling system.

Minecraft 1.21.4 ChunkBuilder already contains:

- scheduler;
- executor;
- consecutive executor;
- upload queue;
- buffer pools;
- SectionBuilder;
- queued task count.

citeturn684312view0

Therefore future work should move upstream:

`schedule task -> prioritize/cancel -> build -> upload`

rather than only:

`upload -> throttle`

### Profiler

Profiler is now opt-in by default.

This is important because a performance mod should not permanently add timing calls to the hot render path.

The profiler should remain a debug instrument, not part of the default frame loop.

The current profiler also distinguishes the 1% slow-frame sample from the worst single frame.

### Adaptive controller

The controller currently changes vanilla visual settings rather than optimizing renderer internals.

Because the project requirement is visual parity, adaptive mode is now opt-in by default.

Long term the controller should use:

- frame time;
- CPU stage time;
- chunk queue pressure;
- upload queue pressure;
- visible section count;
- entity count;
- particle workload;

to choose **which internal workload to schedule**, not simply which vanilla setting to lower.

## 5. Memory/GC priorities

The next profiling phase should measure allocations in:

- SectionBuilder;
- block model iteration;
- translucent sorting;
- entity state creation;
- block-entity render state;
- particle creation;
- upload preparation.

Preferred data model:

- primitive coordinates/flags where possible;
- reusable temporary arrays;
- compact bitsets for visibility;
- generation counters instead of object-heavy dirty-state objects;
- reuse of mesh metadata.

Avoid introducing global object pools without measurements. Pools can create retention and synchronization costs.

## 6. Multi-threading model

Do not create a generic "more threads = faster" system.

Use a pipeline:

`render-section update`
-> `immutable chunk snapshot`
-> `mesh build worker`
-> `completed build queue`
-> `render-thread upload`

Important constraints:

- never read mutable chunk state from worker threads after snapshot creation;
- never mutate render-section ownership from workers;
- every result needs a generation/version;
- cancelled builds must release buffers;
- resource reload must invalidate outstanding work.

Sodium's current architecture uses asynchronous culling and carefully manages safe read phases around render-section data. citeturn861066search2

## 7. Compatibility audit

Before adding deep renderer replacements, detect installed optimization mods.

Potential cooperation rules:

### Sodium present

Do not duplicate:

- chunk renderer;
- block-face culling;
- Sodium section culling;
- Sodium entity culling;
- Sodium buffer management.

Instead provide only Optimizator features that operate outside those paths.

### ImmediatelyFast present

Avoid duplicate GUI/buffer batching hooks.

### Entity Culling present

Do not install a second entity visibility system unless Optimizator can prove that it is complementary.

### FerriteCore/Lithium

These mostly target memory/server simulation respectively, so renderer optimizations remain separate.

## 8. Benchmark methodology

Never accept an optimization from average FPS alone.

Record:

- average FPS;
- 1% low;
- worst frame;
- frame-time mean;
- frame-time variance;
- chunk rebuild duration;
- chunk upload duration;
- visible section count;
- pending section count;
- particle count;
- entity count;
- Java allocation rate;
- GC pauses.

Required scenes:

1. empty plains;
2. dense forest;
3. village/city;
4. many entities;
5. heavy particles;
6. underwater/fog;
7. translucent glass/water;
8. fast flight/teleport;
9. rapid block updates;
10. resource-pack-heavy scene.

Comparison matrix:

`Vanilla`
`Vanilla + Optimizator`
`Sodium`
`Sodium + Optimizator`

Every comparison must use the same world, camera path, resolution, renderer backend and graphics configuration.

## 9. Recommended implementation order

### Phase A
Fix/build/runtime correctness.

### Phase B
Profiler and benchmark harness.

### Phase C
Section dirty-state merging.

### Phase D
Generation-based rebuild cancellation.

### Phase E
Render-section visibility graph.

### Phase F
Async culling.

### Phase G
Block-face culling.

### Phase H
Mesh/buffer reuse.

### Phase I
Translucent optimization.

### Phase J
Entity/block-entity culling from the same section visibility graph.

### Phase K
Compatibility arbitration with Sodium/ImmediatelyFast/Entity Culling.

### Phase L
Adaptive scheduler based on measured bottlenecks.

## 10. Principle

Do not optimize:

`lower quality -> less work`

Prefer:

`same image -> less work`

The highest-value Optimizator code will therefore live below the settings layer:

**section scheduling + visibility + mesh generation + buffer lifetime + submission.**
