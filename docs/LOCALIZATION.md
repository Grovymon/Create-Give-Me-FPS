# Localization

GMF uses Minecraft language resources and ships `en_us` and `ru_ru`. The mod
title `Create: Give Me FPS` is intentionally identical in both languages.
Minecraft selects the active file and provides the normal `en_us` fallback;
there is no separate GMF language setting.

Keys use these prefixes:

- `gui.create_gmf.*` for screens, buttons and dynamic labels;
- `config.create_gmf.*` for categories, settings and tooltips;
- `enum.create_gmf.*` for profile, profiler, evidence and confidence values;
- `diagnostic.create_gmf.*` for bottlenecks and reasons;
- `key.create_gmf.*` for key mappings.

Java enums expose translation keys, never display labels. Dynamic sentences use
translatable components with arguments. Numeric values and hardware names may
be literal arguments, but their labels and units are localized.

To add a language, copy `en_us.json`, preserve every key and translate only the
values. Run `scripts/check-localization.ps1`; it validates JSON, compares both
key sets and scans Java for literal UI components.
