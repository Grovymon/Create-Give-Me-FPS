# Create: Give Me FPS — performance analysis

## Scope and evidence boundary

This analysis targets Minecraft 1.21.1, NeoForge 21.1.219, Create 6.0.10
(`ac0c444d9828da3453ae8cc65338e8de063286fb`) and Flywheel 1.0.6. Class and
method names below were inspected in the matching Create release source. They
identify plausible costs; they are not runtime timings. GMF labels runtime data
as measured, estimated, inferred, or unavailable and never converts object
counts into milliseconds.

## Existing rendering architecture

Create delegates kinetic and belt geometry to Flywheel whenever
`VisualizationManager.supportsVisualization(level)` is true. `BeltVisual`
creates scrolling belt instances and an optional rotating pulley. Kinetic
visuals store speed and axis in instances, leaving continuous animation to the
rendering backend. `ContraptionVisual` builds one transformed structure model,
embeds child visuals, and rebuilds only when structure or child versions change.
GMF must preserve these paths.

## Candidate bottlenecks

### Transported item models and shadows

```text
Subsystem: Belts / transported items
Class: com.simibubi.create.content.kinetics.belt.BeltRenderer
Method: renderItems, renderItem
Side: Client
Thread: Render thread
Frequency: Every rendered controller belt, every rendered frame
CPU/GPU: Both
Measured/Assumed: Source-observed, runtime cost not yet measured
Current behavior: Iterates transported stacks; computes transforms and lighting; renders a planar shadow and one or more item-model copies.
Existing Create/Flywheel optimization: Belt geometry is instanced, item rendering is distance-culled by Create, and extra stack copies are limited by distance.
Possible GMF optimization: Skip only the planar transported-item shadow beyond a configured distance.
Expected benefit: Lower CPU vertex submission and fragment work in item-heavy belt scenes.
Compatibility risk: Low to medium because the hook targets one Create method invocation.
Gameplay risk: None; rendering only.
```

This is the first GMF optimization. It is disabled safely if its mixin cannot
apply. It exposes attempted, rendered, and skipped shadow counters for a real
before/after session.

### Vanilla fallback belt geometry

```text
Subsystem: Belts
Class: com.simibubi.create.content.kinetics.belt.BeltRenderer
Method: renderSafe
Side: Client
Thread: Render thread
Frequency: Every visible belt segment and frame when Flywheel visualization is unavailable
CPU/GPU: Both
Measured/Assumed: Source-observed
Current behavior: Builds transformed cached buffers and UV scrolling state for belt surfaces and pulley geometry.
Existing Create/Flywheel optimization: The normal Flywheel path uses BeltVisual instances; cached buffers reduce fallback rebuild cost.
Possible GMF optimization: None selected. Restoring or preserving Flywheel is preferable.
Expected benefit: N/A
Compatibility risk: High for replacement rendering.
Gameplay risk: None, but visual regression risk is high.
```

### Moving contraption structure and children

```text
Subsystem: Contraptions
Class: com.simibubi.create.content.contraptions.render.ContraptionVisual
Method: setupStructure, setupChildren, beginFrame, checkAndUpdateLightSections
Side: Client
Thread: Flywheel render task plans
Frequency: Transform each frame; rebuild on version changes; light sections on bounding-section changes
CPU/GPU: Both
Measured/Assumed: Source-observed
Current behavior: One transformed structure instance plus child and actor visual plans. Light sections are collected for the current bounding box.
Existing Create/Flywheel optimization: Structure and children use independent version counters; the structure is not rebuilt every frame.
Possible GMF optimization: Profile version churn and section changes before considering any hook.
Expected benefit: Unknown until measured.
Compatibility risk: High.
Gameplay risk: Rendering-only candidates exist, but incorrect bounds can hide visible blocks.
```

### Vanilla fallback contraption rendering

```text
Subsystem: Contraptions
Class: com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer
Method: buildStructureBuffer, render, shouldRender
Side: Client
Thread: Render thread
Frequency: Cached structure build per contraption/render type; render per visible entity/frame
CPU/GPU: Both
Measured/Assumed: Source-observed
Current behavior: Tesselates rendered blocks into cached buffers when Flywheel visualization is unavailable; performs entity frustum checks.
Existing Create/Flywheel optimization: SuperByteBufferCache, ModelBlockRenderer caching, entity frustum culling.
Possible GMF optimization: No patch selected; preserve cache and Flywheel paths.
Expected benefit: N/A
Compatibility risk: High.
Gameplay risk: Visual correctness risk.
```

### Transparent pipe visuals

```text
Subsystem: Fluids
Class: com.simibubi.create.content.fluids.pipes.GlassPipeVisual
Method: beginFrame
Side: Client
Thread: Flywheel dynamic visual plan
Frequency: Every frame for every active visible glass-pipe visual
CPU/GPU: Both
Measured/Assumed: Source-observed
Current behavior: Visits six directions, resolves flow textures/tint/light, and recycles stream and surface instances.
Existing Create/Flywheel optimization: SmartRecycler reuses instances and discards only extras.
Possible GMF optimization: Distance-based visual update or material reuse only after runtime profiling.
Expected benefit: Unknown.
Compatibility risk: Medium to high.
Gameplay risk: Rendering only, but stale flow visuals are possible.
```

### Processing particles

```text
Subsystem: Particles
Class: PressingBehaviour, SawBlockEntity, CrushingWheelControllerBlockEntity, AirFlowParticle
Method: Create-specific particle spawn methods
Side: Client
Thread: Client tick/render particle engine
Frequency: Activity dependent
CPU/GPU: Both
Measured/Assumed: Source-observed call sites
Current behavior: Emits vanilla and Create particle types during processing.
Existing Create/Flywheel optimization: Vanilla particle engine culling/settings plus localized spawn amounts.
Possible GMF optimization: Distance- and budget-based admission at verified Create call sites.
Expected benefit: Potentially useful in particle-heavy factories.
Compatibility risk: Medium; many call sites and vanilla particle types are involved.
Gameplay risk: Visual only.
```

### Kinetic and machine simulation

```text
Subsystem: CPU simulation
Class: KineticBlockEntity and concrete machine block entities
Method: tick and network update paths
Side: Server/common
Thread: Server thread
Frequency: Tick and dirty-state dependent
CPU/GPU: CPU
Measured/Assumed: Not attributed by GMF 0.1
Current behavior: Gameplay-authoritative speed, stress, inventory, recipes, fluids, and contraption logic.
Existing Create/Flywheel optimization: Subsystem-specific caching and dirty-state handling in Create.
Possible GMF optimization: None in 0.1. Profile first.
Expected benefit: Unknown.
Compatibility risk: High.
Gameplay risk: High; tick skipping is prohibited.
```

## First optimization decision

The transported-item shadow hook is narrow, client-only, reversible, visible in
counters, and does not replace Flywheel or modify item transport. It therefore
has the best initial risk profile. It remains subject to an in-game A/B run in a
repeatable belt scene; a successful build alone is not performance validation.
