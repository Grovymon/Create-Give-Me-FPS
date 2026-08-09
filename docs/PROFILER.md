# Profiler

## Measured

- frame intervals captured from NeoForge `RenderFrameEvent.Pre`;
- average, median, minimum, maximum and variance of frame time;
- approximate 1% low after at least 300 frame samples;
- approximate 0.1% low after at least 1000 samples;
- server tick duration and MSPT from paired server pre/post events;
- heap used/max, aggregate GC count and reported collection time;
- transported-item shadow calls rendered or skipped by GMF.

## Estimated or inferred

Nearby Create block-entity, belt, kinetic and contraption counts are an
estimated pressure census. Shader, render and main-thread bottlenecks are
inferences when GPU timers or subsystem timers are unavailable. The UI always
shows the evidence label.

## Sampling and overhead

Frame and tick collectors use preallocated primitive ring buffers. The live
display uses Minecraft's current FPS value and a two-second rolling frame-time
window, so old non-shader frames cannot dominate the shader result. Memory is
sampled once per second. Scene census is bounded by a configurable nearby chunk
radius and runs only at diagnostic boundaries. Detailed profiling is a bounded
session and is not enabled permanently.

## Limitations

GMF 0.4 has no per-subsystem GPU timer and no instrumentation that attributes a
specific number of milliseconds to each Create machine. It does not infer such
numbers from object counts. Dedicated-server MSPT is measured on that process
but is not yet synchronized to remote clients.
