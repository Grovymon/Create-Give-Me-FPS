param([ValidateSet('forge-1.19.2','forge-1.18.2')][string]$Target = 'forge-1.19.2')
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'generate-modern-forge.ps1') -Target $Target
$destination = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot "$Target/.generated"))
if ((Split-Path (Split-Path $destination)) -ne $PSScriptRoot) { throw 'Unsafe generated source path' }
if (Test-Path -LiteralPath $destination) { Remove-Item -LiteralPath $destination -Recurse -Force }
$common = Join-Path $PSScriptRoot 'legacy/src/main'
$own = Join-Path $PSScriptRoot "$Target/src/main"
$utf8 = New-Object Text.UTF8Encoding($false)
$mixins = @('BeltRendererMixin','ParticleEngineMixin','FluidFXMixin','FluidSplashPacketMixin',
    'FluidManipulationBehaviourMixin','ItemEntityRendererMixin','HandCrankRendererMixin','GearboxRendererMixin',
    'FlywheelRendererMixin','BeltTunnelRendererMixin','EjectorRendererMixin','EncasedFanRendererMixin',
    'LargeCogRendererMixin','KineticBlockEntityRendererMixin','BlazeBurnerBlockEntityMixin',
    'BlockEntityCullableTrackingMixin','EntityCullingCullTaskMixin','EntityCullingPrefetchMixin','FlapDisplaySectionMixin', 'HarvesterRendererMixin')
$legacyMixinNames = @(Get-ChildItem -LiteralPath "$common/java/dev/creategmf/mixin/client" -Filter '*.java' | ForEach-Object BaseName)
foreach ($root in @("$Target/.forge-shared",'forge-1.20.1/src/main','legacy/src/main')) {
    foreach ($kind in @('java','resources')) {
        $base = Join-Path $PSScriptRoot "$root/$kind"
        if (!(Test-Path -LiteralPath $base)) { continue }
        foreach ($file in Get-ChildItem -LiteralPath $base -Recurse -File) {
            $relative = $file.FullName.Substring($base.Length + 1)
            if (Test-Path -LiteralPath (Join-Path "$own/$kind" $relative)) { continue }
            if ($root -ne 'legacy/src/main' -and (Test-Path -LiteralPath (Join-Path "$common/$kind" $relative))) { continue }
            if ($kind -eq 'java') {
                if ($relative.Contains('fast_render') -or $file.BaseName -eq 'ScrollAnimationRegistry') { continue }
                if ($relative.Contains('mixin\client') -and $file.BaseName -notin ($mixins + $legacyMixinNames)) { continue }
            }
            $out = Join-Path "$destination/$kind" $relative
            [IO.Directory]::CreateDirectory((Split-Path $out)) | Out-Null
            if ($kind -eq 'resources') {
                if ($relative -eq 'create_gmf.mixins.json') {
                    $json = [IO.File]::ReadAllText($file.FullName) | ConvertFrom-Json
                    $json.client = @($mixins + $legacyMixinNames | ForEach-Object { "client.$_" })
                    [IO.File]::WriteAllText($out, ($json | ConvertTo-Json -Depth 8), $utf8)
                } else { [IO.File]::Copy($file.FullName, $out, $true) }
                continue
            }
            $code = [IO.File]::ReadAllText($file.FullName)
            $code = $code.Replace('import net.minecraft.client.gui.GuiGraphics;', 'import dev.creategmf.legacy.GmfGraphics;')
            $code = $code.Replace('GuiGraphics', 'GmfGraphics')
            $code = $code.Replace('Button.builder(', 'dev.creategmf.legacy.GmfButtons.builder(')
            $code = $code.Replace('import net.minecraft.core.registries.BuiltInRegistries;', 'import net.minecraft.core.Registry;')
            $code = $code.Replace('BuiltInRegistries.', 'Registry.')
            $code = $code.Replace('minecraft.getFps()', 'dev.creategmf.mixin.client.LegacyMinecraftAccessor.createGmf$getFps()')
            $code = $code.Replace('net.createmod.catnip.animation.AnimationTickHolder', 'com.simibubi.create.foundation.utility.AnimationTickHolder')
            $code = $code.Replace('net/createmod/catnip/animation/AnimationTickHolder', 'com/simibubi/create/foundation/utility/AnimationTickHolder')
            $code = $code.Replace('net/createmod/catnip/animation/LerpedFloat', 'com/simibubi/create/foundation/utility/animation/LerpedFloat')
            $code = $code.Replace('import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;', 'import dev.creategmf.legacy.LegacyVisuals;')
            $code = $code.Replace('VisualizationHelper.', 'LegacyVisuals.')
            $code = $code.Replace('ScrollAnimationRegistry.onFrame();', '').Replace('ScrollAnimationRegistry.onClientTick();', '')
            if ($file.BaseName -eq 'BeltRendererMixin') {
                # Create 0.5 renders the shadow inside renderItems, not renderItem.
                $code = $code.Replace('method = "renderItem",', 'method = "renderItems",').Replace('require = 0', 'require = 1')
            }
            if ($file.BaseName -eq 'KineticBlockEntityRendererMixin') {
                $code = $code.Replace('method = "getAngleForBe"', 'method = "getAngleForTe"').Replace('require = 0', 'require = 1')
            }
            if ($Target -eq 'forge-1.18.2') {
                if ($file.BaseName -eq 'ClientKeyMappings') {
                    $code = $code.Replace('import net.minecraftforge.client.event.RegisterKeyMappingsEvent;', 'import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;')
                    $code = $code.Replace('RegisterKeyMappingsEvent event', 'FMLClientSetupEvent event').Replace('event.register(OPEN_MENU);', 'event.enqueueWork(() -> net.minecraftforge.client.ClientRegistry.registerKeyBinding(OPEN_MENU));')
                }
                if ($file.BaseName -eq 'PerformanceOverlay') {
                    $code = $code.Replace('import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;', 'import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;')
                    $code = $code.Replace('RegisterGuiOverlaysEvent event', 'FMLClientSetupEvent event')
                    $code = $code.Replace('event.registerAboveAll("gmf_statistics", (gui, graphics, partialTick, width, height) -> render(graphics, partialTick));', 'event.enqueueWork(() -> net.minecraftforge.client.gui.OverlayRegistry.registerOverlayTop("gmf_statistics", (gui, graphics, partialTick, width, height) -> render(graphics, partialTick)));')
                }
                if ($file.BaseName -eq 'CreateGmfClient') { $code = $code.Replace('ConfigScreenHandler', 'ConfigGuiHandler').Replace('ConfigScreenFactory', 'ConfigGuiFactory') }
            }
            $code = $code.Replace('super.render(graphics,', 'super.render(graphics,')
            if ($file.BaseName -eq 'PerformanceOverlay') {
                $code = $code.Replace('-> render(graphics, partialTick)', '-> render(new GmfGraphics(graphics), partialTick)')
            }
            if ($file.BaseName -eq 'CreateOcclusionController') {
                $code = $code.Replace('import dev.engine_room.flywheel.api.visualization.VisualManager;', 'import dev.creategmf.legacy.LegacyVisualManager;')
                $code = $code.Replace('import dev.engine_room.flywheel.api.visualization.VisualizationManager;', '')
                $code = $code.Replace('VisualManager<BlockEntity>', 'LegacyVisualManager')
                $code = [regex]::Replace($code, '(?s)private static LegacyVisualManager visualManager\(Level level\) \{.*?\n    \}', 'private static LegacyVisualManager visualManager(Level level) { return LegacyVisuals.manager(level); }')
            }
            if ($file.BaseName -eq 'HardwareDetector') {
                $code = $code.Replace('import dev.engine_room.flywheel.api.backend.Backend;', 'import com.jozufozu.flywheel.backend.Backend;')
                $code = $code.Replace('import dev.engine_room.flywheel.api.backend.BackendManager;', '')
                $code = $code.Replace('Backend.REGISTRY.getIdOrThrow(BackendManager.currentBackend()).toString()', 'Backend.getBackendDescriptor()')
            }
            if ($file.BaseName -eq 'GmfSettingsScreen') {
                foreach ($widget in @('searchBox','animationDistanceInput')) {
                    $code = [regex]::Replace($code, "$widget\.setX\(([^;]+)\);", "$widget.x = `$1;")
                    $code = [regex]::Replace($code, "$widget\.setY\(([^;]+)\);", "$widget.y = `$1;")
                    $code = $code.Replace("$widget.getY()", "$widget.y")
                }
                $code = [regex]::Replace($code, '(?s)y = toggleRow\(graphics, contentLeft, y, w, Component.translatable\("config.create_gmf.accelerated_renderer"\),.*?\}, mouseX, mouseY\);', '')
                $code = $code.Replace('"gui.create_gmf.accelerated_renderer_hint"', '"gui.create_gmf.legacy.accelerated_unavailable"')
                $code = $code.Replace('animationDistanceInput.setFocused(true)', 'animationDistanceInput.setFocus(true)')
                $code = $code.Replace('searchBox.setHint(Component.translatable("gui.create_gmf.settings.search_hint"));', 'searchBox.setSuggestion(search.isEmpty() ? Component.translatable("gui.create_gmf.settings.search_hint").getString() : null);')
                $code = $code.Replace('searchBox.setResponder(value -> search = value);', 'searchBox.setResponder(value -> { search = value; searchBox.setSuggestion(value.isEmpty() ? Component.translatable("gui.create_gmf.settings.search_hint").getString() : null); });')
                $code = $code.Replace('for (MechanismAnimationGroup group : MechanismAnimationGroup.values()) {', 'for (MechanismAnimationGroup group : MechanismAnimationGroup.values()) { if (group == MechanismAnimationGroup.CHAINS_AND_CONVEYORS || group == MechanismAnimationGroup.PACKAGE_PORTS) continue;')
            }
            if ($file.BaseName -eq 'GmfConfig') {
                $code = $code.Replace('CLIENT.acceleratedRenderer.set(true)', 'CLIENT.acceleratedRenderer.set(false)')
                $code = $code.Replace('.define("acceleratedRenderer", true)', '.define("acceleratedRenderer", false)')
            }
            if ($file.BaseName -eq 'CreateSceneScanner') {
                $code = [regex]::Replace($code, '(?s)if \(belt.getInventory\(\).getLazyClientItem\(\) != null\) \{.*?\}', '')
            }
            if ($file.BaseName -eq 'CreateGmf') { $code = $code.Replace('Minecraft 1.20.1', ('Minecraft ' + $Target.Replace('forge-', ''))) }
            if ($Target -eq 'forge-1.18.2') {
                $code = $code.Replace('Component.literal(', 'new net.minecraft.network.chat.TextComponent(')
                $code = $code.Replace('Component.translatable(', 'new net.minecraft.network.chat.TranslatableComponent(')
                $code = $code.Replace('Component.empty()', 'new net.minecraft.network.chat.TextComponent("")')
                foreach ($option in @('renderDistance','simulationDistance','enableVsync','framerateLimit')) {
                    $code = $code.Replace("options.$option().get()", "options.$option")
                }
            }
            [IO.File]::WriteAllText($out, $code, $utf8)
        }
    }
}
# Explicit feature difference, never a working-looking accelerated-render toggle.
foreach ($language in @('en_us','ru_ru')) {
    $path = Join-Path $destination "resources/assets/create_gmf/lang/$language.json"
    $json = [IO.File]::ReadAllText($path) | ConvertFrom-Json
    $message = if ($language -eq 'ru_ru') { 'Ускоренный рендер недоступен для Create 0.5.1: здесь используется старый Flywheel 0.6.' } else { 'Accelerated rendering is not available for Create 0.5.1, which uses legacy Flywheel 0.6.' }
    $json | Add-Member -NotePropertyName 'gui.create_gmf.legacy.accelerated_unavailable' -NotePropertyValue $message -Force
    $json.'enum.create_gmf.flywheel_backend.indirect' = if ($language -eq 'ru_ru') { 'Пакетный' } else { 'Batching' }
    [IO.File]::WriteAllText($path, ($json | ConvertTo-Json -Depth 8), $utf8)
}
Write-Output "Generated legacy $Target source adapter (not a runtime verification)"
