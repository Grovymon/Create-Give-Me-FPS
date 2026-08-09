# Benchmark methodology

## PC benchmark

The benchmark warms up first, then runs small CPU transformation/iteration
slices while normal frame sampling continues. Each slice has a strict time
budget to avoid freezing the client. Results store measured operations per
millisecond, frame statistics, heap headroom and GC deltas. The hardware profile
is a rule-based starting point and remains user-editable.

## Optimization A/B benchmark

The user starts a bounded session in a chosen factory scene. GMF immediately
closes its screen, renders progress over normal gameplay, and records a
baseline with the belt-shadow optimization forced off, waits through a short
transition, records the same metrics with it forced on, then clears the runtime
override. It stores average FPS, 1% low, average/variance/max frame time, MSPT,
heap, GC deltas, and shadow counters. No FPS result is predicted.

Opening any screen cancels the capture so menu frames cannot contaminate the
result. G also cancels. The warning for a scene with no belt-item shadow calls
is wrapped to the result panel instead of overflowing the screen.

## Repeatable scenes

A: 100 rotating components. B: 1000 rotating components. C: dense kinetic
visuals. D: long belts. E: belts with many transported items. F: large static
factory. G: moving contraption. H: pipes/tanks. I: particle-heavy factory. J:
same with shaders. K: factory outside the camera. L: factory behind walls.

Each comparison must keep position, view direction, resolution, render distance,
shader pack and scene state fixed. JVM, chunks, resources and shaders must be
warmed before recording. No runtime benchmark has been performed merely by
building this project.
