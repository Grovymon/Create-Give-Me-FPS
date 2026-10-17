# GMF 0.1.3.3 — experimental multi-version builds

Seven production-mapped JARs can be built, but these are experimental ports,
not seven fully in-world-validated releases. No publication is performed here.
The preserved 1.21.1 NeoForge recovery implementation remains in the parent project.
The authoritative target/build state is in `minecraft-versions.json`.

Create 6 uses Flywheel 1.x; Create 0.5.1 uses Flywheel 0.6.x. These require distinct
render adapters, not just different loader metadata. Shader integrations and GUI
also need real per-version ports. An unsupported feature must not silently become
a working-looking no-op.

1.20.1 Forge/NeoForge share the Forge-compatible ABI used by official Create.
Fabric and Quilt share a Fabric binary for these target versions; Quilt support
must still be tested with the corresponding Fabric API/QFAPI dependencies.

Build gates: Java 17/21 bytecode; pinned Create dependency and hook contracts;
Forge SRG reobfuscation / Fabric intermediary remapping; no unresolved placeholders;
RU/EN parity; packaged licence notices; isolated startup on each loader.
Do not label a target complete from compilation alone.

## Verification, 2026-10-17

| Target | Create | Automated tests | Development client startup |
| --- | --- | ---: | --- |
| 1.21.1 NeoForge | 6.0.10 | 22 | Previously passed recovery smoke |
| 1.20.1 Forge / NeoForge | 6.0.8 | 18 | Forge 47.1.33 passed; NeoForge NOT VERIFIED |
| 1.19.2 Forge | 0.5.1.i | 18 | Forge 43.5.2 passed |
| 1.18.2 Forge | 0.5.1.i | 18 | Forge 40.3.12 passed with dev-only LWJGL allocator workaround |
| 1.20.1 Fabric / Quilt | 6.0.8.1 | 18 | Fabric 0.17.3 passed; Quilt NOT VERIFIED |
| 1.19.2 Fabric / Quilt | 0.5.1-i | 18 | Fabric 0.17.3 passed; Quilt NOT VERIFIED |
| 1.18.2 Fabric / Quilt | 0.5.1-i | 18 | Fabric 0.17.3 passed; Quilt NOT VERIFIED |

“Startup” means GMF initialization plus resource/Flywheel shader loading, not
loading a factory world. Production JAR loading, mechanism restoration, GUI
interaction, huge-factory FPS, shader packs and addon combinations remain
**NOT VERIFIED** for the new ports. Test totals overlap shared tests; 130 total
executions do not mean 130 different bugs or behaviours were tested.

The 1.18.2 Fabric upstream Forge Config API Port 3.2.4 distribution logs a
non-fatal missing `net.minecraftforge.modmenu.ConfigFactoryHandler` Mod Menu
entrypoint. GMF's own entrypoints and resources loaded. This upstream warning
was not hidden or patched inside a third-party JAR.

## Legacy feature differences

1.18.2/1.19.2 use real Flywheel 0.6 instance adapters: rotation/belt shader data,
dynamic/tickable instance dispatch, block-entity culling and vanilla fallback
renderers. The owner binding spans constructors **and init()** and is released
in `finally`; restoring original speeds never changes Create's kinetic network.

The CreateBetterFPS accelerated buffer renderer is **not ported** to Create
0.5.1. Its toggle is absent, with an explicit localized explanation. Backend
choices use the actual legacy `OFF / BATCHING / INSTANCING` API; automatic mode
uses the legacy instancing capability fallback, not Flywheel 1.x indirect draw.
Create 6-only chain conveyors and package ports are hidden in the legacy UI.
Full moving-contraption/actor animation parity is **not implemented/verified**;
these JARs must not be advertised as complete replacements for 1.21.1 yet.

## Building and collecting

Use PowerShell 7, JDK 17 for Minecraft 1.18.2–1.20.1, Gradle 9.2.1 for Forge,
and Gradle 9.5.1 with a JDK 25 daemon for the pinned Loom 1.17.17 tooling.
The Fabric output still targets Java 17, not Java 25. Build 1.21.1 in the parent
project with its wrapper and Java 21.

```powershell
./inspect-upstream.ps1
./build-ports.ps1 -Java17 'C:/path/to/jdk17' -FabricDaemonJava 'C:/path/to/jdk25' `
  -ForgeGradle 'C:/path/to/gradle-9.2.1/bin/gradle.bat' `
  -FabricGradle 'C:/path/to/gradle-9.5.1/bin/gradle.bat'
./verify-artifacts.ps1
```

The dependency preparation scripts extract **unaltered** nested dependencies
from SHA-512-verified upstream Create archives. They are build inputs only;
GMF does not redistribute Create/Flywheel/Porting Lib classes. MixinExtras is
embedded using each loader's supported nested-JAR format.

`production-selectors.gradle` maps annotation selectors with the exact loader
mapping file after reobfuscation/remapping. This is necessary for `remap=false`
Create-owned methods whose descriptors contain Minecraft classes, and for the
Entity Culling dispatcher invocation. Named-descriptor leakage fails the Fabric
build. It changes only generated GMF mixin annotations, not upstream binaries.

`verify-artifacts.ps1` checks bytecode, metadata, refmaps, RU/EN keys, licences,
embedded MixinExtras and test results, then copies the seven exact production
artifacts to `outputs/releases/0.1.3.3/multiversion` alongside SHA-256 hashes in
`artifacts.json`. No launcher installation, Git commit/push or publication occurs.
