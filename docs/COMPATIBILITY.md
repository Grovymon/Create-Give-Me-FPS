# Compatibility and validation boundaries

Target: Minecraft 1.21.1, Java 21, NeoForge 21.1.219, Create 6.0.10-280,
Flywheel 1.0.6, Ponder 1.0.82, Registrate MC1.21-1.3.0+67.
Metadata accepts Create 6.0.x, not 6.1. Wider patch compatibility is not established by that range.

Entity Culling is required on clients by loader metadata. Dev runtime pins the
NeoForge 1.21.1 artifact by Modrinth ID (not an ambiguous shared version string).
Servers do not require GMF or Entity Culling. Dedicated dev-server startup reached Done;
its isolated test process was stopped. This does not validate every client path.
The clean wrapper build and 33 tests pass. Bytecode contracts cover the selected Create belt upload,
Flywheel owner descriptor and Entity Culling 1.11.2 API/prefetch probe, not every renderer method.
Final dev-client startup with Entity Culling 1.11.2 completed resource/shader loading without GMF
mixin/API errors; the test process was stopped. No factory world or shader pack matrix was exercised.

Sodium/Embeddium, Iris, Distant Horizons and Nowheel are optional detections.
Accelerated rendering activates only with both Sodium and Iris and a launch-time preference.
Shader state uses guarded Iris reflection. Entity Culling unavailable/incompatible visibility
fails open; Nowheel owns culling when present. EC internal hook compatibility needs runtime validation.

## Addon coverage

The generic registry/inheritance layer has no addon dependencies and recognizes observed standard
KineticBlockEntity/SmartBlockEntity owners using Create/Flywheel base visuals.
This is architecture support, **not** a tested blanket claim for Crafts & Additions, New Age,
Diesel Generators, TFMG, Steam 'n' Rails, Big Cannons, Enchantment Industry, Slice & Dice,
Connected or Metallurgy. Test each installed version individually.
Own renderers/actors, unusual bounds and nonstandard lifecycle need optional explicit adapters.

## Remaining manual matrix

Flywheel Auto/Indirect/Instancing/Off; Iris shader on/off; Sodium/Embeddium; Nowheel present/absent;
EC supported versions; addon presence/absence; GUI resizing/scales.
Run distance and camera return, wall reveal, loaded/unloaded chunks, world/dimension change,
resource/renderer reload, config ON/OFF and real zero-RPM restart tests.
See [AUDIT_REPORT](AUDIT_REPORT.md). Pure tests and startup cannot certify those outcomes.
