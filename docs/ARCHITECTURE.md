# Architecture

## Modules

```text
dev.creategmf
├─ benchmark       incremental PC and A/B sessions
├─ client          client entrypoint, event wiring, overlay and key mapping
├─ config          versioned NeoForge client configuration
├─ diagnostics     bounded scene sampling and bottleneck classification
├─ gui             localized screens
├─ hardware        safe hardware/render-stack snapshot
├─ integration     Create and Flywheel observations
├─ optimization    isolated, reversible rendering hooks and counters
├─ profiler        frame, server-tick and memory collectors
├─ recommendation  explicit rule-based proposals
└─ util            small data structures and formatting helpers
```

Client-only classes live under `client`, `gui`, `hardware` and the client
optimization package. The common mod entrypoint never links them. Dedicated
servers load the common config declaration and server tick collector only.

## Data flow

Frame events write nanoseconds into a fixed ring buffer. Server tick events
write tick durations into a separate synchronized ring buffer. Memory is
sampled at a low fixed cadence. Screens request immutable snapshots; snapshot
creation may sort a copied array, while hot-path writes allocate nothing.

PC benchmarking runs small time-budgeted work slices from client ticks. It does
not execute one long blocking loop. Lag diagnostics records a bounded window of
measured frame/tick/memory data and performs a nearby Create scene census at
session boundaries, not every tick.

The classifier produces a bottleneck type, evidence status and confidence.
Diagnostics opens the explicit mechanism-rendering controls after a result;
it does not invent a generic safe change from an inferred bottleneck. The
remaining recommendation engine accepts only a measured Create-rendering or
transported-item scene and never proposes an unchanged value.

## Rendering integration

Create/Flywheel APIs are compile-time dependencies matching Create 6.0.10.
Backend identification uses `BackendManager.currentBackend()` and Flywheel's
backend registry. The first rendering optimization wraps the existing
`BeltRenderer.renderItem` shadow call and leaves item rendering and simulation
untouched.

## Configuration

NeoForge persists client visual/performance settings in its standard TOML
config. `configVersion` is currently 4. Runtime benchmark overrides are kept in
memory and never silently rewrite user settings.

## Failure behavior

The mixin is narrowly scoped and documented. If it cannot apply, Mixin reports
the incompatibility during startup instead of silently claiming an active
optimization. Optional renderer and shader detection returns unavailable rather
than failing the game.
