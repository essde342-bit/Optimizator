# Initial performance audit — Minecraft 1.21.4 client

## Scope

The audit targets client-side workloads that can be reduced without changing the server simulation.

### 1. World/chunk rendering

Large render distance is a compound cost: more visible chunk sections, more geometry submitted to the renderer, more chunk rebuild pressure, and more memory traffic.

Action taken: adaptive render-distance reduction. Instead of forcing one low value, Optimizator starts from the player's current setting and reduces it only after sustained low FPS.

### 2. Entity rendering

Entity rendering is especially expensive when many mobs, item entities, armor stands, and other renderable objects are simultaneously inside the view.

Minecraft already exposes an entity-distance scale. Reusing that native control is lower-risk than replacing entity selection logic.

Action taken: adaptive entityDistanceScaling reduction.

### 3. Clouds

Cloud rendering is optional and can add repeated geometry work in scenes where the GPU is already under pressure.

Action taken: clouds are temporarily switched off at higher reduction levels and restored after recovery.

### 4. Particles

Particle storms can create a large number of short-lived client objects and render calls. This is a good place for a small allocation-free guard because it does not need to modify world simulation.

Action taken: a per-second budget is applied only to non-forced particle creation. Forced particles are preserved.

### 5. CPU/GPU feedback

A single instantaneous FPS sample is noisy. A controller that reacts every frame can oscillate and make performance worse.

Action taken: a 40-tick control interval plus consecutive-sample hysteresis.

## What is deliberately not changed yet

### Chunk meshing internals

The chunk builder and mesh upload path are among the most important deep optimization targets, but they are tightly coupled to Minecraft's rendering internals. A safe implementation needs profiling plus exact 1.21.4 mapping coverage before changing scheduling, buffer reuse, or rebuild queues.

### Culling replacement

Minecraft already has frustum/entity visibility checks. Replacing them blindly can break nameplates, mounts, shadows, and special renderer behavior. The current version therefore uses the vanilla entity-distance control instead of a second competing culler.

### Server simulation

No server tick rate, mob AI, redstone, random tick, or network packet logic is modified. A client optimization mod should not silently change multiplayer gameplay semantics.

## Validation checklist

The project is considered build-ready only when all of these pass:

1. GitHub Actions completes the Gradle build.
2. The remapped jar exists in build/libs.
3. Mixins apply without startup errors.
4. The client loads with the default config.
5. The config file survives restart and malformed values fall back safely.
6. Adaptive changes restore the user's original render/entity/cloud settings.
7. Particle limiting never cancels forced particles.
8. No server-side entrypoint is loaded.

## Next profiling targets

The next audit phase should measure, on representative PojavLauncher hardware:

- chunk rebuild time and queue pressure;
- frame time split between world, entities, particles, and UI;
- Java allocation rate during chunk rebuilds;
- memory pressure caused by visible chunk count;
- GPU fill/bandwidth pressure at 4–12 chunk distances;
- behavior differences across Pojav renderer backends.

Changes to low-level renderer code should be accepted only after a before/after measurement and a crash/visual-regression check.
