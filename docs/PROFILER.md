# Profiler and evidence

| Status | Meaning |
| --- | --- |
| MEASURED | Observed frame/tick interval or actual hook counter/population |
| ESTIMATED | Bounded nearby scene census; not every loaded/rendered object |
| INFERRED | Rule-based explanation or expected benefit |
| UNAVAILABLE | No reliable API/measurement, never substituted with an invented value |

Frame statistics: average FPS/frame ms, median, min/max, variance, p95/p99 nearest-rank,
approximate 1% low after 300 frames and 0.1% low after 1000.
MSPT comes from paired server tick events in the current process; remote-server MSPT is unavailable.
Memory/aggregate GC counters are sampled at low cadence, not per mechanism.

RenderEvidence distinguishes:
- live supported AbstractBlockEntityVisual population (known current-world owners);
- visible/off-screen bounds observations and addon count;
- EC occlusion observations plus GMF-owned hidden population;
- all Flywheel **block-entity** visuals (including other mods), or -1 if no manager;
- observed animation opportunities skipped/resumed, not unique mechanism counts;
- transported-item renderItems call attempts/skips, not stack count or draw calls;
- Create particle requests and suppressed requests; direct FluidFX batch suppression is counted as an effect,
  not a measured number of individual particles.

The nearby scene scan currently uses a bounded three-chunk radius at session boundaries.
It does not scan all chunks every frame. Live rings are primitive/preallocated; percentile calculation
copies/sorts only on snapshot. Detailed diagnostic/report work is opt-in and still has overhead:
do not compare a recorded run with an unrecorded run.

Developer report snapshots are captured on the client thread before background file writing.
Reports stay in logs/GMF locally. No telemetry. Bottleneck milliseconds for Create/individual machines,
CPU-vs-GPU certainty and GPU timer metrics remain UNAVAILABLE. Counts cannot establish causation.
