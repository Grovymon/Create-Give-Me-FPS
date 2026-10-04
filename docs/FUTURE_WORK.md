# Deferred work: designs, not advertised features

## GPU timing

Implement only after render pass ownership is verified with Flywheel and Iris.
Use render-thread-owned ring of at least four query pairs; timestamp start/end at known
sections, read an older pair only if both GL_QUERY_RESULT_AVAILABLE are true.
Never glFinish, never wait for a query, never reuse an outstanding query.
Skip measurement if the ring is full; disable if unsupported or if another timer owns a conflicting
GL_TIME_ELAPSED scope. Prefer timestamp pairs for nested passes.
Clean query objects on renderer/context disposal, abort partial frames, tag results with frame ID/pass
and shader state. Report whole measured section, not per-machine attribution.
Validate first with timer disabled/enabled frame distributions and no FPS stalls.
No GL timer is shipped by this audit.

## Dimension overrides

Global visual default plus sparse map keyed by ResourceKey<Level>/dimension registry ID.
Compose immutable effective policy; no copy of the full config.
Apply on client dimension transition, restore global fallback for unknown dimensions.
Exclude launch-only backend/mixin settings; define precedence against temporary Potato/A-B.
Add migration/snapshot/transition tests before UI or automatic application.

## Transported-item interpolation and batching

Current safe hook skips only the renderItems overlay or item shadows.
First profile transform/model rendering CPU work, draw-call distribution and item-model diversity.
A cached interpolation design must invalidate on transported object identity, belt movement, processing,
stack/model/NBT changes, resource reload and camera-dependent transforms; never delay transport ticks.
Batching needs compatible material/model/light/transparency passes and shader integration.
No 'one draw call for all stacks' claim without actual GPU evidence. Keep opt-in reference renderer
and validation for processing particles, enchanted/custom items and translucent models.

## Further coverage

Custom addon adapters, CPU fallback frustum coverage, large actor bounds and moving-contraption
wall culling need proven lifecycle/transforms. Explicit Create client drill/saw particle origins
must be traced before suppressing vanilla break events. Broad vanilla filtering by location is forbidden.
