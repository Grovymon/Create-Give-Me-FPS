# Mixins

## BeltRendererMixin

```text
Mixin: BeltRendererMixin
Target Class: com.simibubi.create.content.kinetics.belt.BeltRenderer
Target Method: renderItem
Purpose: Avoid the planar transported-item shadow draw beyond the configured distance.
Why Mixin is necessary: Create exposes no event around its private transported-item render method.
Available API: NeoForge/Flywheel rendering APIs do not control this Create-owned draw call.
Why API cannot be used: The shadow is submitted directly by BeltRenderer.
Compatibility Risk: Low to medium; target is one stable Create 6.0.10 invocation.
Performance Impact: One distance comparison and counter update per attempted belt-item shadow.
Fallback: The optional injection is skipped if its target no longer matches; the
optimization can also be disabled in config. Items and transport are never modified.
```

## ParticleEngineMixin

Filters only particle types registered in the `create` namespace. Reduced mode
admits one out of three such particles; off mode admits none. Vanilla and other
mods' particle namespaces are untouched. This is visual-only and reversible.

`BlazeBurnerBlockEntityMixin` separately filters the vanilla flame, soul-flame
and smoke particles emitted by Create's Blaze Burner. This owner-scoped hook is
needed because those particles have the `minecraft` namespace and therefore
must not be removed globally.

## Flywheel animation mixins

`SimpleDynamicVisualMixin` rate-limits distant Create dynamic visual plans in
reduced mode and freezes them in static mode. `SimpleTickableVisualMixin`
applies the same policy to visual-only tick plans. `RotatingInstanceMixin` and
`BeltVisualMixin` stop distant GPU rotation and belt texture scrolling outside
the configured full-animation distance. `AbstractBlockEntityVisualMixin`
exposes the visual's existing world position to the distance controller.

`AbstractInstanceMixin` is a safety net for Create `ScrollInstance` animations
that keep running on the GPU without a new visual plan. `BlazeBurnerVisualMixin`
handles the burner's replaceable flame instance explicitly.

`FlapDisplaySectionMixin` turns display-board transitions into immediate text
updates when the Other group or the strict zero setting disables animations.
`SpriteContentsTickerMixin` freezes the shared water/lava atlas frames only at
strict zero. The latter necessarily affects water and lava everywhere because
Minecraft does not provide a separate ticker for fluids shown inside Create.

Every mechanism family can also be disabled independently. The controller then
skips only that family's client visual plans and sets its GPU animation speed to
zero when applicable. Gameplay block entities, inventories and kinetic networks
keep ticking.

The targets are Create 6.0.10 and Flywheel 1.0.6 implementation classes. Every
hook is optional (`require = 0`) and the declared Create dependency range stops
before 6.1.

These hooks are animation LOD, not replacement-mesh LOD. GMF 0.4 does not claim
to synthesize Distant Horizons-style low-detail Create models or textures.
