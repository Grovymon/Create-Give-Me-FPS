# Benchmark methodology

## Runtime A/B

Select exactly one target: belt shadows, occlusion, off-screen animation pause,
distant animation throttling, transported items, or particles.
Five seconds baseline → one second transition → five seconds optimized.
The screen closes for gameplay sampling. Opening any screen cancels capture.
Completion, cancellation and world change restore the exact original visual snapshot, including GMF-off.

Backend and accelerated renderer are restart-required experiments, so live Start is disabled.
Use two separately restarted runs with the same scene and launch settings; do not pretend
that a changed config value changes already-applied renderer mixins.

Results contain average FPS, 1% low only with sufficient samples, mean/p95/p99/max frame ms,
GC count/time deltas, process-available MSPT and hook evidence. The result screen wraps and scrolls.
Runtime A/B settings are not persisted.

A target may have **no opportunity**: no transported calls, no supported visuals, particles already
disabled, or Potato already making animation groups static. Other settings remain unchanged.
Compare counters before interpreting FPS noise; this is not a promise of performance gain.
Five seconds is a smoke comparison, not a statistically significant performance claim.
Repeat in reversed order in separately warmed runs to control thermal/JIT/cache/scene drift.

## Conditions and scenes

Fix camera position/direction, resolution, GUI/render distance, shaders, versions, factory state
and logging mode. Warm JVM, chunks and resources. Avoid world edits during captures.
Test 100/1000 rotating parts; dense factory; long empty/loaded belts; moving large contraption;
pipes/tanks; particle-heavy pulleys/burners; shaders; factory behind player; factory behind a wall.
Measure chunk/dimension transitions separately from steady-state frame cost.

PC synthetic benchmark runs time-budgeted CPU work slices and is not a Create GPU benchmark.
Its hardware recommendation is a starting point, not a measured factory gain.
No factory FPS results are claimed by compilation or startup.
