# Client rendering mixins

All rendering hooks are installed from the client array; no server simulation mixins.
Targets: Create 6.0.10-280 and Flywheel 1.0.6. Optional hooks can fail open; required injection
counts on critical current targets make matching failures visible. This is not universal version compatibility.

- BeltRenderer: renderItems skip is explicit/GMF-enabled only; renderItem shadow wrapper leaves transport intact.
- AbstractBlockEntityVisual/AbstractVisualLifecycle: state ownership, observed population and deletion cleanup.
- SimpleDynamic/SimpleTickable/CreateBlockEntityDynamic: animation-plan policy, not machine tick cancellation.
- RotatingInstance: setup ownership before canonical setter captures; rendered zero never becomes source RPM.
- ScrollInstanceSource/BeltVisual: canonical speed/offset captures, exact ColoredLitInstance.setChanged upload point.
- AbstractInstance: owner-scoped registry application/deletion. Unknown scrolling instances stay untouched.
- LevelRendererFrustum: after prepared world frustum, re-evaluate registered shader policy each frame.
- ContraptionVisual: inherits AbstractEntityVisual for owner access (no invalid subclass shadow).
  Always run beginFrame; only gate actor/child animation plans.
- BlockEntityCullableTracking: optional EC setCulled observation and removed-owner cleanup.
- BlockEntityStorageOcclusion: reject known-hidden creation while retaining client restoration ownership.
- EntityCullingCullTask/EntityCullingPrefetch: legacy worker/new snapshot renderer probes;
  both disabled with Nowheel, no copied EC implementation.
- ParticleEngine: Create-namespace factory only; FluidFX/FluidSplashPacket/BlazeBurner: explicit client Create effects.
- Mechanism-specific fallback renderers retain distance/group controls; frustum coverage is limited to known Visual owners.
- fast_render hooks are selected only at client launch with Sodium+Iris, retaining upstream MIT notices.

Removed: ItemEntityRenderer/proximity-crusher optimizer, FluidManipulationBehaviour common/server effect
cancellation, shared SpriteContents fluid texture hooks and unclassified scroll-position guessing.
No blanket vanilla particle or atlas changes.

Static resource/bytecode tests check references and selected invocation/field/API contracts.
Dev startup exercises loaded class transformations but does not certify every lazy-loaded renderer path.
Use the manual matrix in [AUDIT_REPORT](AUDIT_REPORT.md) before a release.
