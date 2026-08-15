# Create: Give Me FPS

[Русская версия](README_RU.md) | English

<p align="center">
  <img src="docs/icon.png" alt="Create: Give Me FPS icon" width="360">
</p>

**Release: 0.1.2** · Client-side controls and diagnostics for **Minecraft Java
Edition 1.21.1**, **NeoForge**, **Create 6.0.10**, and **Flywheel 1.0.6**.

Create: Give Me FPS reduces selected *client-side visual work* in large Create
factories. It does not change recipes, inventories, stress networks, machine
logic, or server simulation.

## 0.1.2 — changes since 0.1.1

- Added Developer Mode with a bounded local frame-history recorder.
- Added **Start recording** and **Stop and save**. Starting creates a
  timestamped session immediately; stopping always writes a final snapshot.
- **Save report** now writes a snapshot immediately. A missing `events`
  directory is recreated before every write, so it cannot discard a report.
- Added nearby Create-scene census, automatic frame-spike captures, and local
  reports in `logs/GMF/`.
- Strengthened direct suppression of supported Create fluid effects, including
  hose-related fluid-rendering paths.
- Added an optional CreateBetterFPS-derived accelerated renderer toggle with
  preserved MIT attribution.

## Controls

### Factory visuals

- six presets: Potato, Low, Medium, Above Average, High, and Ultra;
- searchable per-group switches for supported Create mechanism animations;
- Full, Reduced, or Static distant animations;
- full-animation distance from **0 to 32 blocks** (`0` means no full-distance
  animations);
- reduced-animation update rate and visual update divisor;
- belt-item rendering, belt-item shadows, and loose crushing-wheel output
  rendering.

### Particles

- Create particle mode: Full, Reduced, or Off;
- separate limits for steam/smoke, sparks, item and block break effects, and
  water/lava splash effects;
- direct filtering for supported Create fluid-effect paths.

Particle controls affect visuals only. Effects created by other mods or an
unhooked Create rendering path may remain visible; reports identify such cases
instead of claiming that every source is covered.

### Flywheel and shaders

- select Flywheel backend: Default, Indirect, Instancing, or Off;
- optional **Accelerated Rendering**, adapted from CreateBetterFPS, when both
  Sodium and Iris are present.

Changing the Flywheel backend or Accelerated Rendering requires a full game
restart. The selected value is saved immediately, but the renderer is chosen
during startup.

## Developer Mode and reports

Press **G** in a loaded world to open the menu. Developer Mode provides:

- **Start recording** — immediately creates `logs/GMF/<timestamp>/` with
  session metadata;
- **Save report** — writes a snapshot now;
- **Stop and save** — writes a final snapshot, then disables recording;
- optional automatic reports when a frame spike is detected;
- nearby-scene counts for Create block entities, kinetic blocks, belts, chain
  conveyors, contraptions, and common mechanism families.

Reports are local files, not telemetry. Automatic spike detection can create
many reports in a dense factory; turn it off when collecting one manual test.
Scene categories are estimates for prioritisation, not a replacement for a CPU
or GPU profiler.

## Requirements

| Component | Required version |
| --- | --- |
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.x |
| Create | 6.0.10 |
| Flywheel | 1.0.6 |
| Java | 21 |

The mod is client-side, but every installed mod must remain compatible with the
versions used by the modpack.

## Installation

1. Install NeoForge, Create, and Flywheel for Minecraft 1.21.1.
2. Download `Create-Give-Me-FPS-0.1.2-NeoForge-1.21.1.jar` from
   [Releases](../../releases).
3. Place the JAR in the instance's `mods` directory.
4. Start the game and press **G** in a world.

## Important limitations

- The mod does not ship low-poly replacements, LOD terrain, or lower-resolution
  textures for Create.
- Not every Create visual uses the same hook. Disabling a supported group
  reduces the covered path, but cannot guarantee that a third-party addon or
  renderer-specific effect disappears.
- A saved report does not itself prove an FPS gain. Compare from the same
  position, camera direction, resolution, render distance, and shader setup.

## Build from source

```powershell
.\gradlew.bat clean test build
```

The JAR is created in `build/libs/`.

## Credits and licence

The optional accelerated renderer is adapted from
[CreateBetterFPS](https://github.com/MoePus/CreateBetterFPS) by MoePus under
the MIT License. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and the
preserved [original notice](THIRD_PARTY_LICENSES/CreateBetterFPS-MIT.txt).

This project is released under the [MIT License](LICENSE).
