param([ValidateSet('fabric-1.19.2','fabric-1.18.2')][string]$Target = 'fabric-1.19.2')
$ErrorActionPreference = 'Stop'
$forge = $Target.Replace('fabric-', 'forge-')
& (Join-Path $PSScriptRoot 'generate-legacy-forge.ps1') -Target $forge
$destination = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot "$Target/.generated"))
if ((Split-Path (Split-Path $destination)) -ne $PSScriptRoot) { throw 'Unsafe generated source path' }
if (Test-Path -LiteralPath $destination) { Remove-Item -LiteralPath $destination -Recurse -Force }
$own = Join-Path $PSScriptRoot "$Target/src/main"
$platform = Join-Path $PSScriptRoot 'fabric-1.20.1/src/main'
$utf8 = New-Object Text.UTF8Encoding($false)
foreach ($root in @("$forge/.generated", 'fabric-1.20.1/src/main')) {
    foreach ($kind in @('java','resources')) {
        $base = Join-Path $PSScriptRoot "$root/$kind"
        foreach ($file in Get-ChildItem -LiteralPath $base -Recurse -File) {
            $relative = $file.FullName.Substring($base.Length + 1)
            if ($relative -eq 'META-INF\mods.toml' -or $relative -eq 'fabric.mod.json') { continue }
            if (Test-Path -LiteralPath (Join-Path "$own/$kind" $relative)) { continue }
            if ($root -ne 'fabric-1.20.1/src/main' -and $file.BaseName -ne 'FlywheelBackendController' -and (Test-Path -LiteralPath (Join-Path "$platform/$kind" $relative))) { continue }
            if ($root -eq 'fabric-1.20.1/src/main' -and $file.BaseName -eq 'FlywheelBackendController') { continue }
            $out = Join-Path "$destination/$kind" $relative
            [IO.Directory]::CreateDirectory((Split-Path $out)) | Out-Null
            $code = [IO.File]::ReadAllText($file.FullName)
            if ($kind -eq 'resources') {
                if ($relative -eq 'create_gmf.mixins.json') {
                    $json = $code | ConvertFrom-Json
                    $json.client += 'client.MinecraftFrameMixin'
                    $code = $json | ConvertTo-Json -Depth 8
                }
                [IO.File]::WriteAllText($out, $code, $utf8)
                continue
            }
            $code = $code.Replace('import net.minecraftforge.fml.ModList;', 'import dev.creategmf.platform.FabricModList;')
            $code = [regex]::Replace($code, '\bModList\b', 'FabricModList')
            $code = $code.Replace('import net.minecraftforge.fml.loading.FMLPaths;', 'import net.fabricmc.loader.api.FabricLoader;')
            $code = $code.Replace('FMLPaths.GAMEDIR.get()', 'FabricLoader.getInstance().getGameDir()')
            if ($file.BaseName -eq 'GmfMixinPlugin') {
                $code = $code.Replace('import net.minecraftforge.fml.loading.LoadingModList;', 'import net.fabricmc.loader.api.FabricLoader;')
                $code = $code.Replace('LoadingModList.get().getModFileById(modId) != null', 'FabricLoader.getInstance().isModLoaded(modId)')
            }
            if ($file.BaseName -eq 'ShaderStatusDetector') { $code = $code.Replace('isLoaded("oculus")', 'isLoaded("iris")') }
            if ($file.BaseName -eq 'ClientRuntime') {
                $code = $code.Replace('import net.minecraftforge.event.TickEvent;', '')
                $code = $code.Replace('onRenderFrame(TickEvent.RenderTickEvent event)', 'onRenderFrame()')
                $code = $code.Replace('onClientTick(TickEvent.ClientTickEvent event)', 'onClientTick()')
                $code = [regex]::Replace($code, 'if \(event.phase != TickEvent.Phase.(START|END)\) return;', '')
            }
            if ($file.BaseName -eq 'ServerTickCollector') {
                $code = [regex]::Replace($code, 'import net.minecraftforge.(eventbus.api.SubscribeEvent|fml.common.Mod.EventBusSubscriber|event.TickEvent);', '')
                $code = [regex]::Replace($code, '@EventBusSubscriber\(modid = CreateGmf.MOD_ID\)|@SubscribeEvent', '')
                $code = $code.Replace('(TickEvent.ServerTickEvent event)', '()')
                $code = [regex]::Replace($code, 'if \(event.phase != TickEvent.Phase.(START|END)\) return;', '')
            }
            if ($file.BaseName -eq 'PerformanceOverlay') {
                $code = $code.Replace('import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;', 'import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;')
                $code = $code.Replace('import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;', 'import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;')
                $code = $code.Replace('register(RegisterGuiOverlaysEvent event)', 'register()')
                $code = $code.Replace('event.registerAboveAll("gmf_statistics", (gui, graphics, partialTick, width, height) -> render(new GmfGraphics(graphics), partialTick));', 'HudRenderCallback.EVENT.register((pose, partialTick) -> render(new GmfGraphics(pose), partialTick));')
                $code = [regex]::Replace($code, '(?s)public static void register\([^)]*event\) \{.*?\n    \}', 'public static void register() { HudRenderCallback.EVENT.register((pose, partialTick) -> render(new GmfGraphics(pose), partialTick)); }')
            }
            if ($file.BaseName -eq 'ClientKeyMappings') {
                $code = $code.Replace('import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;', 'import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;')
                $code = $code.Replace('import net.minecraftforge.client.event.RegisterKeyMappingsEvent;', 'import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;')
                $code = $code.Replace('register(RegisterKeyMappingsEvent event)', 'register()').Replace('event.register(', 'KeyBindingHelper.registerKeyBinding(')
                $code = [regex]::Replace($code, '(?s)public static void register\([^)]*event\) \{.*?\n    \}', 'public static void register() { KeyBindingHelper.registerKeyBinding(OPEN_MENU); }')
            }
            if ($file.BaseName -eq 'GmfSettingsScreen') {
                $code = $code.Replace('searchBox.setHeight(18);', '').Replace('animationDistanceInput.setHeight(16);', '')
                $code = $code.Replace('animationDistanceInput = new EditBox(font, 0, 0, 1, 18,', 'animationDistanceInput = new EditBox(font, 0, 0, 1, 16,')
            }
            if ($file.BaseName -eq 'FluidFXMixin') {
                $code = $code.Replace('net.minecraftforge.fluids.FluidStack', 'io.github.fabricators_of_create.porting_lib.util.FluidStack')
            }
            if ($file.BaseName -eq 'FlywheelBackendController') {
                $code = $code.Replace('FlwConfig.get().client.backend.set(backend);', 'FlwConfig.get().backend.set(backend);')
                $code = $code.Replace('FlwConfig.get().client.backend.save();', 'FlwConfig.get().save();')
            }
            if ($file.BaseName -eq 'CreateGmf') {
                $code = $code.Replace('import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;', 'import net.minecraftforge.api.ModLoadingContext;')
                $code = $code.Replace('ForgeConfigRegistry.INSTANCE.register(', 'ModLoadingContext.registerConfig(')
                $code = $code.Replace('Minecraft 1.20.1', ('Minecraft ' + $Target.Replace('fabric-', '')))
            }
            if ($Target -eq 'fabric-1.18.2') {
                $code = $code.Replace('Component.literal(', 'new net.minecraft.network.chat.TextComponent(')
                $code = $code.Replace('Component.translatable(', 'new net.minecraft.network.chat.TranslatableComponent(')
                $code = $code.Replace('Component.empty()', 'new net.minecraft.network.chat.TextComponent("")')
            }
            [IO.File]::WriteAllText($out, $code, $utf8)
        }
    }
}
Write-Output "Generated $Target adapter; runtime not yet verified"
