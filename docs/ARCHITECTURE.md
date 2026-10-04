# Architecture

## Safety boundary

Common entrypoint/config/server tick observation do not link client classes.
All optimization mixins are in the client list. Server observation measures ticks;
it never skips or changes them. No entity/inventory/fluid/network/save mutations.

## Ownership and policies

CreateVisualClassifier caches registry namespace/id and structural Create inheritance per
BlockEntityType. Original Create mechanisms retain their group classification.
Compatible addon types use the same state/registry layer without linking addon classes.
Custom unrelated visuals fail open until an explicit adapter exists.

AbstractBlockEntityVisual owns VisualAnimationState. CachedRenderBBBlockEntity provides
world bounds: cached identity invalidation, padded once, unknown/virtual-level owners visible.
LevelRendererFrustumMixin uses Minecraft's prepared world frustum, not raycasts.
FrustumGrace requires three distinct off-screen frames, resumes immediately on visibility.
This feature is opt-in. It freezes/skips visual updates, not Flywheel visibility queues.

Rotating/scrolling instances register once and capture canonical source values in setup/setters.
Rendering can upload zero speed without overwriting source RPM/UV speed. Actual source zero,
negative speeds, offsets and later updates are respected. Concurrent registries use explicit
instance deletion/world cleanup; no synchronized per-frame snapshots or movement-triggered chunk rebuilds.
Known per-instance policies are reapplied after camera frustum preparation every frame.
CPU fallback renderer hooks implement distance/group policies; off-screen coverage is not universal.

Contraption beginFrame always updates embedding transforms, light, structure changes and child ownership.
Only actor/child animation plans are gated. Virtual local coordinates never masquerade as world bounds.
Contraption wall culling is deliberately deferred.

## Occlusion ownership

Entity Culling worker hooks only publish observations to concurrent sets.
The client thread owns HiddenVisualState and queueRemove/queueAdd transitions. Unknown is visible.
Only GMF-owned hiding/rejected creation owes restoration; visible observations do not repeatedly queueAdd.
Pending work is budgeted (1024/tick), with tracked reconciliation at 5 Hz for forced-visible timeouts.
World/manager changes discard disposed manager ownership; removed blocks are cleaned up.
Nowheel disables GMF ownership. Config-off restores GMF-owned visual debt.

## Measurements and UI

Fixed primitive rings collect frames/ticks. Scene scans occur at diagnostic boundaries/manual refresh,
not every frame. RenderEvidence snapshots populations/counters on demand.
MEASURED counts are not subsystem milliseconds. Estimated census and inferred attribution stay separate.
Recommendations require explicit confirmation. Runtime A/B temporarily changes one selected target,
restores VisualConfigSnapshot on completion/cancellation/world change, and never saves experiment settings.
PerformanceModeToggle restores the exact visual snapshot and guards config saves while Potato is temporary.
Adaptive proposals use sustained thresholds and a cooldown; no automatic setting writes.

## Configuration and resources

Schema migration is monotonic and radius bounded. ProfileRenderingValues is the shared preset matrix.
Disabled addon registry IDs are a list, not addon hard dependencies. Renderer choices latch at launch;
runtime changes only save the next-launch preference and update the restart warning.

Tests validate localization parity, literal translation keys, mixin source references, ownership policy,
state transitions, source preservation, snapshots, profiles, recommendations and the resolved Create
belt upload bytecode target. A graphical client launch stays outside CI.

## Module map

`client` event/key/restart wiring; `config` policies/snapshots; `integration` owner classifier;
`optimization` animation/occlusion/belt/particle paths; `diagnostics` evidence/report sessions;
`profiler` primitive rings/statistics; `recommendation` proposals; `benchmark` selected experiments;
`gui` localized controls.
