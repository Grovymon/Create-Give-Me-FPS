# Технический аудит GMF: итог и границы проверки

Дата: 2026-10-04. Основа: main, commit `541c93b3867f4888a7963bbff593a92406bc8ec5`.
Изменения находятся в отдельном рабочем каталоге `outputs/create-gmf-audit-main`.
Старый checkout с незавершённой работой сохранён. На момент завершения аудита новых
коммитов/публикации/смены версии и копирования в Prism не было. По последующему запросу
пользователя изменения подготовлены к публикации как pre-release **0.1.3.2**.
Тестовые запуски используют только изолированные `run/client`
и `run/server`, не существующие миры пользователя.

## 1. Что было сломано

- ItemEntityRenderer + CrushingOutputOptimizer скрывали произвольный предмет по близости к дробилке.
- Регистр вращения мог принимать собственный обнулённый GPU-параметр за реальную скорость источника.
- Общие ванильные fluid/atlas hooks выходили за пределы Create-эффектов; common FluidManipulation hook мог
  вмешиваться в эффект из интегрированного сервера.
- Belt renderItems зависел от статичности анимации, а не только от явного выбора показа стаков/GMF-on.
- Occlusion worker менял общие очереди/состояние; повторные переходы и очистка нуждались в явном владении.
- Найдены при startup/bytecode проверке: invalid inherited contraption shadow, устаревший путь Cullable
  и перемещённый renderer-probe callsite Entity Culling.

## 2. Что было частичным

Дистанционные политики/частицы/тени/пресеты/локальный профайлер уже существовали.
Addon ownership был привязан к пакетам Create. Рекомендации ограничивались тенями, экран не был
подключён к результатам. A/B был belt-only. Contraption policy могла задерживать также трансформы/свет.
Поддержка собственного рендера любого аддона и реальная атрибуция GPU-времени отсутствовали.

## 3. Что исправлено/добавлено

Удалён небезопасный ItemEntity hook. Captured source speed/offset сохраняются независимо от отображаемого нуля;
реальное нулевое/отрицательное RPM обновляется через Create setter. Регистры очищаются при удалении/world change.
Камера пересчитывает политики каждый кадр после frustum preparation: возвращение не ждёт редкого bucket.

Новый opt-in frustum-параметр использует кешированные расширенные bounds и три distinct miss frames;
unknown и virtual owners не скрываются. Default false: игровые bounds/шейдерная матрица ещё не подтверждены.

Классификатор кеширует namespace/type/inheritance. GUI показывает установленные аддоны и обнаруженные
совместимые типы. Occlusion имеет чистую машину владения и bounded client handoff, без duplicate transition commands.
Nowheel уступается управление; EC API resolution guarded/atomic и имеет новый/старый package fallback.
Новая EC-prefetch интеграция и старая worker-интеграция включаются только без Nowheel.

Contraption beginFrame сохраняет matrices/light/rebuild/version updates; ограничиваются только child/actor
visual plans. Глобальный фильтр частиц пропускает все vanilla IDs; verified Create client effects фильтруются отдельно.
Общие shared-fluid texture hooks удалены.

Расширены реальные счётчики, p95/p99, рекомендации с подтверждением, шесть runtime A/B targets,
restart-only renderer experiments, точный Potato snapshot и opt-in adaptive proposals.
GUI длинных рекомендаций/A-B переносит и прокручивает текст. README и техническая документация приведены к коду.

## FEATURE | BEFORE | AFTER | STATUS

| FEATURE | BEFORE | AFTER | STATUS |
| --- | --- | --- | --- |
| ItemEntity near crusher | false hide by position | proximity hook removed | FIXED; manual world test pending |
| Belt item visibility | static policy could hide overlay | explicit GMF-enabled setting only | FIXED |
| Distant shader restoration | rendered zero could overwrite source | independent canonical source + frame re-evaluation | FIXED policy; in-game NOT VERIFIED |
| Off-screen animation | missing | cached bounds + grace + immediate visible policy | DONE opt-in; limited known Visual coverage |
| Addon architecture | Create package only | registry/type/inheritance cache + GUI | DONE generic; addon matrix NOT VERIFIED |
| Occlusion lifecycle | shared worker state | client-owned transition debt + cleanup | HARDENED; world matrix NOT VERIFIED |
| EC API/callsite | incompatible current EC | modern/legacy API + prefetch adapter | FIXED contracts; wall behavior pending |
| Contraption safety | entire plans could be gated | transform/light/rebuild unconditional | PARTIAL safe animation throttling |
| Create particles | broad vanilla classification | Create-owned factory/explicit client paths | FIXED scope; drill/saw provenance deferred |
| Diagnostics | frame stats + census | observed visual/work/effect counters | DONE counts; GPU attribution UNAVAILABLE |
| Recommendations | shadow only/unreachable | multiple evidence rules + connected confirmation UI | DONE |
| A/B | belt shadows only | six live targets + two restart experiments | DONE; measured benefit not claimed |
| Potato key | no exact restore | assignable unbound key + immutable visual snapshot | DONE |
| Adaptive mode | missing | sustained recommendations, no auto mutation | DONE opt-in |
| GPU timing | unavailable | asynchronous ring design documented | DEFERRED |
| Dimension overrides | unavailable | global + sparse override design | DEFERRED |
| Belt interpolation/batching | unmeasured idea | scoped investigation/test plan | DEFERRED |

## 4. Какие файлы изменены

Полный перечень — в приложении ниже. Основные области: config/client wiring, animation and occlusion
registries, Create/Flywheel mixins, diagnostic/report evidence, recommendations/benchmark GUI, resource translations,
unit/bytecode contracts, build/CI and docs. Не менялись world/kinetic gameplay классы и сохранения пользователя.

## 5. Новые production-классы

- `src/main/java/dev/creategmf/benchmark/OptimizationTarget.java`
- `src/main/java/dev/creategmf/client/PerformanceModeToggle.java`
- `src/main/java/dev/creategmf/config/ConfigMigration.java`
- `src/main/java/dev/creategmf/config/ProfileRenderingValues.java`
- `src/main/java/dev/creategmf/config/SettingsSnapshot.java`
- `src/main/java/dev/creategmf/config/VisualConfigSnapshot.java`
- `src/main/java/dev/creategmf/diagnostics/RenderEvidence.java`
- `src/main/java/dev/creategmf/gui/GmfAddonScreen.java`
- `src/main/java/dev/creategmf/integration/CreateVisualClassifier.java`
- `src/main/java/dev/creategmf/mixin/client/AbstractVisualLifecycleMixin.java`
- `src/main/java/dev/creategmf/mixin/client/EntityCullingPrefetchMixin.java`
- `src/main/java/dev/creategmf/mixin/client/LevelRendererFrustumMixin.java`
- `src/main/java/dev/creategmf/mixin/client/ScrollInstanceSourceMixin.java`
- `src/main/java/dev/creategmf/optimization/animations/AnimationRules.java`
- `src/main/java/dev/creategmf/optimization/animations/FrustumGrace.java`
- `src/main/java/dev/creategmf/optimization/animations/OffScreenAnimations.java`
- `src/main/java/dev/creategmf/optimization/animations/ShaderAnimationSource.java`
- `src/main/java/dev/creategmf/optimization/animations/VisualAnimationState.java`
- `src/main/java/dev/creategmf/optimization/animations/VisualPopulation.java`
- `src/main/java/dev/creategmf/optimization/animations/VisualWorkCounters.java`
- `src/main/java/dev/creategmf/optimization/belts/TransportedItemCounters.java`
- `src/main/java/dev/creategmf/optimization/occlusion/HiddenVisualState.java`
- `src/main/java/dev/creategmf/optimization/particles/ParticleRules.java`
- `src/main/java/dev/creategmf/recommendation/AdaptiveRecommendationGate.java`
- `src/main/java/dev/creategmf/recommendation/RecommendationRules.java`

Ключевые чистые политики: AnimationRules, FrustumGrace, HiddenVisualState, SettingsSnapshot,
ProfileRenderingValues, ConfigMigration, RecommendationRules, AdaptiveRecommendationGate.
Minecraft ownership/counters вынесены из них для независимого тестирования.

## 6. Mixins

Добавлены LevelRendererFrustum, AbstractVisualLifecycle, ScrollInstanceSource, EntityCullingPrefetch.
Изменены owner/base/dynamic/rotation/belt/contraption/particle/occlusion hooks и launch-latching accelerated renderer.
Удалены ItemEntityRenderer, FluidManipulationBehaviour, SpriteContents и SpriteContentsTicker.
Все в client array. Проверяется exact Belt upload owner `ColoredLitInstance`, inherited Entity field descriptor,
pinned EC API/prefetch invocation, наличие классов JSON. Это contracts, не полная runtime-проверка всех mixins.

## 7. Намеренно не реализовано

GPU timers, item interpolation/batching, per-dimension overrides и contraption wall culling отложены:
без доказанных pass ownership/bounds/invalidation нельзя безопасно выпускать replacement renderer.
Автоматическая смена профилей не реализована: только подтверждаемые рекомендации.
Ванильные drill/saw block-break эффекты не подавляются по координатам.
Собственные нестандартные addon renderers требуют explicit adapter.
Полные безопасные дизайны: [FUTURE_WORK](FUTURE_WORK.md).

## 8. Tests/build/runtime

Финальная команда `.\gradlew.bat clean test build --no-daemon`: **BUILD SUCCESSFUL**.
33 теста, 11 test suites; failures/errors/skipped = 0.
RU/EN parity, literal translation keys, mixin JSON references, Create/Flywheel/EC bytecode contracts прошли.
Проверен JAR: metadata, RU/EN, mixin JSON, MIT notices; addon hard dependencies не добавлены.
Первичная сборка аудита сохраняла версию исходного main: 0.1.3.
Её JAR: `build/libs/create_gmf-0.1.3.jar`, SHA256:
`02C4D979F3282970BA7DB882B4585FF0024B825F1578FA8206B9E30FF4371D78`.
Для публикации версия повышена до **0.1.3.2**; это отдельная повторная сборка.
CI сохраняет `./gradlew clean test build --no-daemon`, добавляет JAR/test-report artifact.
Unit tests: классификация, distance/frustum policy, источники speed, профиль/миграция/snapshot,
visible-hidden-visible/no duplicates, recommendations/hysteresis; resource/bytecode contracts.

Dev dedicated server дошёл до Done без client classloading crash; процесс завершён.
Dev client с EC 1.11.2 загрузил Create, ресурсы и Flywheel shaders без GMF mixin/API ошибок
после исправления contraption superclass. Финальный повторный startup с новым prefetch-hook также
загрузил ресурсы/Flywheel shaders без GMF mixin/API ошибок. Тестовый клиент остановлен.
Игра в существующем мире, factory performance, addon/shader/backend matrix: **NOT VERIFIED**.
Обычное предупреждение deprecated Catnip ShadedBlockSbbBuilder не является результатом FPS-теста.

## 9. Manual checklist (остаётся открытым для pre-release)

- [ ] Выбросить ItemEntity рядом с crusher; запустить ejector туда же; дождаться настоящего crusher output:
  все loose items видимы. Явно выключить/включить стеки ленты: только overlay меняется.
- [ ] Рядом → за радиус → 5 секунд → обратно: shafts, cogwheels, belts, fans, crushing wheels,
  water wheels, steam engines, Blaze Burner, Frogport, tunnels, ejector, contraption.
  Возобновление без ломания источника, F3+A или chunk reload. Повторить real source RPM zero/restart/reverse.
- [ ] Frustum: медленный поворот на краю, быстрое возвращение, большая машина/конструкция,
  разные FOV/render origin; unknown owner remains visible. Камера за стеной — отдельный occlusion test.
- [ ] Occlusion: стены/reveal; создание уже скрытого Visual; config/GMF ON-OFF;
  unload/reload chunk; dimension/world change; renderer/resource reload.
  Нет duplicate Visual, persistent hidden, growing tracked references.
- [ ] Nowheel present/absent; EC tested versions; Flywheel Auto/Indirect/Instancing/Off;
  Iris shader active/inactive; optional mods absent without CNFE.
- [ ] Contraption rotation/movement/version change/light crossing: структура движется/освещается,
  child animation throttled only. Поезда/инвентари/физика unchanged.
- [ ] Каждый перечисленный в ТЗ аддон отдельно: standard base visual versus custom renderer;
  GUI показывает только установленные обнаруженные compatible types.
- [ ] Particle Off: Create factory/FluidFX/burner; unrelated vanilla rain, lava, smoke, block breaking unaffected.
- [ ] Potato key: CUSTOM exact values → Potato → exact restore; сохранить отчёт во временном режиме,
  затем перезапуск — временные значения не должны попасть в TOML.
- [ ] A/B каждую runtime цель; отмена меню/выход/world change; GC and unavailable values;
  restart targets не создают live result. Keep scene/camera fixed, report no-op counts honestly.
- [ ] GUI RU/EN, small window/high scale, scroll recommendations/results/addons, tooltips/input.
- [ ] Соответствие gameplay: производительность/рецепты/инвентари/жидкости/RPM/stress до/после одинаковы.

## 10. Compatibility risks

Create/Flywheel internal implementations версионированы; metadata range шире фактически тестированной точки.
Bounds могут не покрывать нестандартные addon animation extensions; такие owners fail open, но generic base
ещё требует ручной матрицы. Frustum frame/task ordering и source field uploads с Flywheel worker tasks
нуждаются в игровой стресс-проверке. Регистры ConcurrentHashMap и volatile canonical values не доказывают
отсутствие всякого renderer scheduling race. Population covers known BE Visuals, not all actors/contraptions.
Occlusion forced-visible polling, manager disposal/creation и optional redirect conflicts требуют runtime test.
Direct FluidFX counters — batches, не точное число частиц. Hook attempt counts — не draw calls/CPU cost.
Длина A/B 5 секунд чувствительна к GC/миру; замеры не показывают гарантированного прироста.
Миграция не читает новее поддерживаемой схемы. Client classloading smoke не подтверждает addon compatibility.

## 11. Рекомендации для следующей версии

Сначала пройти checklist на тестовой фабрике с двумя renderer modes и одним shader pack,
повторить A/B с одинаковой камерой и долговременную проверку cleanup/world transitions.
После этого готовить prerelease; версию автоматически не менять.
Следующий инженерный этап: минимальный async GPU section timer с overhead-тестом,
затем measured belt CPU profile; addon adapters добавлять по наблюдаемым проблемам, не по названию мода.
Сохранять fail-open и отделение визуальных данных от gameplay.

## Appendix: complete changed/new/deleted paths

```text
.github/workflows/build.yml
README.md
README_RU.md
build.gradle
docs/ARCHITECTURE.md
docs/AUDIT_REPORT.md
docs/BENCHMARK.md
docs/COMPATIBILITY.md
docs/FUTURE_WORK.md
docs/MIXINS.md
docs/MVP_PLAN.md
docs/PERFORMANCE_ANALYSIS.md
docs/PROFILER.md
src/main/java/dev/creategmf/benchmark/OptimizationBenchmarkResult.java
src/main/java/dev/creategmf/benchmark/OptimizationBenchmarkSession.java
src/main/java/dev/creategmf/benchmark/OptimizationTarget.java
src/main/java/dev/creategmf/client/ClientKeyMappings.java
src/main/java/dev/creategmf/client/ClientRuntime.java
src/main/java/dev/creategmf/client/CreateGmfClient.java
src/main/java/dev/creategmf/client/PerformanceModeToggle.java
src/main/java/dev/creategmf/client/RendererRestartTracker.java
src/main/java/dev/creategmf/config/ConfigMigration.java
src/main/java/dev/creategmf/config/GmfConfig.java
src/main/java/dev/creategmf/config/ProfileRenderingValues.java
src/main/java/dev/creategmf/config/SettingsSnapshot.java
src/main/java/dev/creategmf/config/VisualConfigSnapshot.java
src/main/java/dev/creategmf/diagnostics/CreateSceneScanner.java
src/main/java/dev/creategmf/diagnostics/DeveloperDiagnostics.java
src/main/java/dev/creategmf/diagnostics/DiagnosticSession.java
src/main/java/dev/creategmf/diagnostics/DiagnosticsResult.java
src/main/java/dev/creategmf/diagnostics/RenderEvidence.java
src/main/java/dev/creategmf/fast_render/mixin/ShadedBlockSbbBuilderMixin.java
src/main/java/dev/creategmf/fast_render/mixin/SuperBufferFactoryMixin.java
src/main/java/dev/creategmf/fast_render/mixin/SuperByteBufferBuilderMixin.java
src/main/java/dev/creategmf/gui/GmfAddonScreen.java
src/main/java/dev/creategmf/gui/GmfDiagnosticsScreen.java
src/main/java/dev/creategmf/gui/GmfOptimizationBenchmarkScreen.java
src/main/java/dev/creategmf/gui/GmfRecommendationScreen.java
src/main/java/dev/creategmf/gui/GmfSettingsScreen.java
src/main/java/dev/creategmf/integration/CreateVisualClassifier.java
src/main/java/dev/creategmf/mixin/GmfMixinPlugin.java
src/main/java/dev/creategmf/mixin/client/AbstractBlockEntityVisualMixin.java
src/main/java/dev/creategmf/mixin/client/AbstractInstanceMixin.java
src/main/java/dev/creategmf/mixin/client/AbstractVisualLifecycleMixin.java
src/main/java/dev/creategmf/mixin/client/BeltRendererMixin.java
src/main/java/dev/creategmf/mixin/client/BeltVisualMixin.java
src/main/java/dev/creategmf/mixin/client/BlazeBurnerBlockEntityMixin.java
src/main/java/dev/creategmf/mixin/client/BlockEntityCullableTrackingMixin.java
src/main/java/dev/creategmf/mixin/client/BlockEntityStorageOcclusionMixin.java
src/main/java/dev/creategmf/mixin/client/ContraptionVisualMixin.java
src/main/java/dev/creategmf/mixin/client/CreateBlockEntityDynamicVisualMixin.java
src/main/java/dev/creategmf/mixin/client/EntityCullingPrefetchMixin.java
src/main/java/dev/creategmf/mixin/client/FluidManipulationBehaviourMixin.java
src/main/java/dev/creategmf/mixin/client/ItemEntityRendererMixin.java
src/main/java/dev/creategmf/mixin/client/LevelRendererFrustumMixin.java
src/main/java/dev/creategmf/mixin/client/RotatingInstanceMixin.java
src/main/java/dev/creategmf/mixin/client/ScrollInstanceSourceMixin.java
src/main/java/dev/creategmf/mixin/client/SpriteContentsMixin.java
src/main/java/dev/creategmf/mixin/client/SpriteContentsTickerMixin.java
src/main/java/dev/creategmf/optimization/animations/AnimationRules.java
src/main/java/dev/creategmf/optimization/animations/BlockEntityVisualAccess.java
src/main/java/dev/creategmf/optimization/animations/DistantAnimationController.java
src/main/java/dev/creategmf/optimization/animations/FrustumGrace.java
src/main/java/dev/creategmf/optimization/animations/OffScreenAnimations.java
src/main/java/dev/creategmf/optimization/animations/RotationAnimationRegistry.java
src/main/java/dev/creategmf/optimization/animations/ScrollAnimationRegistry.java
src/main/java/dev/creategmf/optimization/animations/ShaderAnimationSource.java
src/main/java/dev/creategmf/optimization/animations/SharedFluidAnimationRegistry.java
src/main/java/dev/creategmf/optimization/animations/VisualAnimationState.java
src/main/java/dev/creategmf/optimization/animations/VisualPopulation.java
src/main/java/dev/creategmf/optimization/animations/VisualWorkCounters.java
src/main/java/dev/creategmf/optimization/belts/TransportedItemCounters.java
src/main/java/dev/creategmf/optimization/crushing/CrushingOutputOptimizer.java
src/main/java/dev/creategmf/optimization/occlusion/CreateOcclusionController.java
src/main/java/dev/creategmf/optimization/occlusion/EntityCullingBridge.java
src/main/java/dev/creategmf/optimization/occlusion/HiddenVisualState.java
src/main/java/dev/creategmf/optimization/particles/CreateParticleOptimizer.java
src/main/java/dev/creategmf/optimization/particles/ParticleRules.java
src/main/java/dev/creategmf/profiler/FrameStatistics.java
src/main/java/dev/creategmf/recommendation/AdaptiveRecommendationGate.java
src/main/java/dev/creategmf/recommendation/Recommendation.java
src/main/java/dev/creategmf/recommendation/RecommendationEngine.java
src/main/java/dev/creategmf/recommendation/RecommendationRules.java
src/main/resources/assets/create_gmf/lang/en_us.json
src/main/resources/assets/create_gmf/lang/ru_ru.json
src/main/resources/create_gmf.mixins.json
src/test/java/dev/creategmf/ResourceContractTest.java
src/test/java/dev/creategmf/benchmark/OptimizationTargetTest.java
src/test/java/dev/creategmf/config/ProfileSafetyTest.java
src/test/java/dev/creategmf/integration/ClassificationSafetyTest.java
src/test/java/dev/creategmf/integration/RenderHookBytecodeTest.java
src/test/java/dev/creategmf/optimization/AnimationSafetyTest.java
src/test/java/dev/creategmf/optimization/OcclusionStateTest.java
src/test/java/dev/creategmf/recommendation/RecommendationRulesTest.java
```

MIT attribution CreateBetterFPS/Nowheel сохранены в исходниках и JAR notices.
Entity Culling implementation не копировался: API/reflection и narrow renderer-probe adapter.
