# Create: Give Me FPS

[Русская версия](README_RU.md) | English

Client-side performance controls and in-world diagnostics for **Minecraft Java
Edition 1.21.1**, **NeoForge**, **Create 6.0.10**, and **Flywheel 1.0.6**.

Create: Give Me FPS reduces avoidable rendering work in large Create factories
without stopping machines, inventories, recipes, stress networks, or server
simulation.

## Features

- six graphics presets: Potato, Low, Medium, Above Average, High, and Ultra;
- searchable per-mechanism animation controls for 20 Create mechanism groups;
- full, reduced, or static distant animation modes;
- full-animation-distance slider from 0 to 256 blocks (`0` means disabled);
- reduced animation frame-rate control;
- Create particle reduction or suppression;
- belt item-shadow distance controls;
- in-world lag diagnostics and before/after measurements;
- shader detection through the Iris public API;
- complete English and Russian interface localization.

Press **G** in a world to open the compact menu. Diagnostics run over the live
world and ask the player to keep the same position and camera direction.

## Compatibility

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.x |
| Create | 6.0.10 |
| Flywheel | 1.0.6 |
| Java | 21 |

The mod is client-side. Both the mod and its dependencies must still match the
versions used by the modpack.

## Build

On Windows:

```powershell
.\gradlew.bat clean test build
```

On Linux or macOS:

```bash
./gradlew clean test build
```

The resulting JAR is written to `build/libs/`.

## Important limitations

The animation controls reduce or stop supported animation updates. They do not
replace Create blocks with dedicated low-poly models or lower-resolution
textures. A successful build also does not guarantee an FPS gain in every
factory or shader pack; compare performance from the same position, camera,
resolution, render distance, and shader configuration.

## License

MIT. See [LICENSE](LICENSE).

