# Optimizator

Client-side optimization mod for Minecraft 1.21.4 + Fabric.

The project uses a conservative rule: do not alter world simulation, networking, or server behavior just to gain FPS. The first release focuses on client rendering pressure that can be reduced dynamically on low-end devices, including PojavLauncher setups.

## Current optimization layer

- Adaptive render-distance reduction when sustained FPS drops below the configured floor.
- Adaptive entity render-distance scaling using Minecraft's existing client option.
- Automatic cloud disabling during heavier load, with restoration when performance recovers.
- A lightweight per-second particle budget for non-forced particles.
- Hysteresis so settings do not bounce every frame.
- Persistent configuration in config/optimizator.properties.
- Internal chunk GPU-upload budgeting to reduce render-thread upload spikes.

## Default profile

adaptive=true
particle_limiter=true
disable_clouds_under_load=true
min_render_distance=5
min_entity_distance=0.35
fps_floor=35
particle_budget=1200

The adaptive controller reacts every 40 client ticks. It needs multiple consecutive low/high samples before changing the reduction level, which prevents rapid oscillation.

## Build

The GitHub Actions workflow uses Java 21 and Gradle 8.12 and runs:

gradle build --no-daemon

The remapped production jars are uploaded as workflow artifacts.

## Compatibility

Target: Minecraft 1.21.4, Fabric Loader 0.16.14+, Fabric API 0.119.2+, Java 21+.

The mod is client-only.

## Important limitation

This is an optimization layer, not a replacement renderer. Deep renderer changes similar to Sodium require careful handling of Minecraft's chunk mesh pipeline, buffer lifetime, transparency ordering, and driver behavior. Those changes are intentionally not copied into this first base because incorrect low-level changes can trade FPS for crashes, visual corruption, or Pojav-specific driver problems.
