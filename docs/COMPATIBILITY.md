# Compatibility

Verified compile and development-launch matrix:

- Minecraft 1.21.1 and Java 21;
- NeoForge 21.1.219;
- Create 6.0.10 Maven build 280;
- Flywheel 1.0.6;
- Ponder 1.0.82;
- Registrate `MC1.21-1.3.0+67`.

Create and Flywheel are required. Sodium/Embeddium, Iris and Distant Horizons
remain optional. For Iris, GMF first checks the mod id and then calls
`IrisApi.isShaderPackInUse()` through guarded reflection so the Iris API is never
a hard class-loading dependency. If the API is missing or incompatible, shader
state is treated as unavailable instead of failing the game.

The rendering mixins are tied to Create 6.0.x and Flywheel 1.0.x visual
implementations. The declared Create range stops before 6.1 so an API-changing
release cannot be accepted silently.
