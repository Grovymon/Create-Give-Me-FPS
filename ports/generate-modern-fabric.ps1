$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'generate-modern-forge.ps1') -ForFabric
$destination = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'fabric-1.20.1/.generated'))
if ((Split-Path (Split-Path $destination)) -ne $PSScriptRoot) { throw 'Unsafe generated source path' }
if (Test-Path -LiteralPath $destination) { Remove-Item -LiteralPath $destination -Recurse -Force }
$utf8 = New-Object Text.UTF8Encoding($false)
$own = Join-Path $PSScriptRoot 'fabric-1.20.1/src/main'
foreach ($root in @('fabric-1.20.1/.forge-shared', 'forge-1.20.1/src/main')) {
    foreach ($kind in @('java','resources')) {
        $base = Join-Path $PSScriptRoot "$root/$kind"
        foreach ($file in Get-ChildItem -LiteralPath $base -Recurse -File) {
            $relative = $file.FullName.Substring($base.Length + 1)
            if ($relative -eq 'META-INF\mods.toml') { continue }
            if (Test-Path -LiteralPath (Join-Path "$own/$kind" $relative)) { continue }
            $out = Join-Path "$destination/$kind" $relative
            [IO.Directory]::CreateDirectory((Split-Path $out)) | Out-Null
            if ($kind -eq 'resources') {
                if ($relative -eq 'create_gmf.mixins.json') {
                    $json = [IO.File]::ReadAllText($file.FullName).Replace('"client.StorageAnimationMixin",', '"client.StorageAnimationMixin", "client.MinecraftFrameMixin",')
                    [IO.File]::WriteAllText($out, $json, $utf8)
                } else { [IO.File]::Copy($file.FullName, $out, $true) }
                continue
            }
            $code = [IO.File]::ReadAllText($file.FullName)
            $code = $code.Replace('import net.minecraftforge.fml.ModList;', 'import dev.creategmf.platform.FabricModList;')
            $code = [regex]::Replace($code, '\bModList\b', 'FabricModList')
            $code = $code.Replace('import net.minecraftforge.fml.loading.FMLPaths;', 'import net.fabricmc.loader.api.FabricLoader;')
            $code = $code.Replace('FMLPaths.GAMEDIR.get()', 'FabricLoader.getInstance().getGameDir()')
            if ($relative.EndsWith('GmfMixinPlugin.java')) {
                $code = $code.Replace('import net.minecraftforge.fml.loading.LoadingModList;', 'import net.fabricmc.loader.api.FabricLoader;')
                $code = $code.Replace('LoadingModList.get().getModFileById(modId) != null', 'FabricLoader.getInstance().isModLoaded(modId)')
                $code = $code.Replace('isLoaded("embeddium") && isLoaded("oculus")', 'isLoaded("sodium") && isLoaded("iris")')
            }
            if ($relative.EndsWith('ShaderStatusDetector.java')) { $code = $code.Replace('isLoaded("oculus")', 'isLoaded("iris")') }
            if ($relative.EndsWith('ClientRuntime.java')) {
                $code = $code.Replace('import net.minecraftforge.event.TickEvent;', '')
                $code = $code.Replace('onRenderFrame(TickEvent.RenderTickEvent event)', 'onRenderFrame()')
                $code = $code.Replace('onClientTick(TickEvent.ClientTickEvent event)', 'onClientTick()')
                $code = [regex]::Replace($code, 'if \(event.phase != TickEvent.Phase.(START|END)\) return;', '')
            }
            if ($relative.EndsWith('ServerTickCollector.java')) {
                $code = [regex]::Replace($code, 'import net.minecraftforge.(eventbus.api.SubscribeEvent|fml.common.Mod.EventBusSubscriber|event.TickEvent);', '')
                $code = [regex]::Replace($code, '@EventBusSubscriber\(modid = CreateGmf.MOD_ID\)|@SubscribeEvent', '')
                $code = $code.Replace('(TickEvent.ServerTickEvent event)', '()')
                $code = [regex]::Replace($code, 'if \(event.phase != TickEvent.Phase.(START|END)\) return;', '')
            }
            if ($relative.EndsWith('PerformanceOverlay.java')) {
                $code = $code.Replace('import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;', 'import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;')
                $code = $code.Replace('register(RegisterGuiOverlaysEvent event)', 'register()')
                $code = $code.Replace('event.registerAboveAll("gmf_statistics", (gui, graphics, partialTick, width, height) -> render(graphics, partialTick));', 'HudRenderCallback.EVENT.register(PerformanceOverlay::render);')
            }
            if ($relative.EndsWith('ClientKeyMappings.java')) {
                $code = $code.Replace('import net.minecraftforge.client.event.RegisterKeyMappingsEvent;', 'import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;')
                $code = $code.Replace('register(RegisterKeyMappingsEvent event)', 'register()').Replace('event.register(', 'KeyBindingHelper.registerKeyBinding(')
            }
            if ($relative.EndsWith('GmfSettingsScreen.java')) {
                # Forge adds setHeight; vanilla/Fabric uses the constructor.
                $code = $code.Replace('searchBox.setHeight(18);', '')
                $code = $code.Replace('animationDistanceInput.setHeight(16);', '')
                $code = $code.Replace('animationDistanceInput = new EditBox(font, 0, 0, 1, 18,', 'animationDistanceInput = new EditBox(font, 0, 0, 1, 16,')
            }
            # Create Fabric uses Porting Lib's real FluidStack API.
            if ($relative.EndsWith('FluidFXMixin.java')) {
                $code = $code.Replace('import net.minecraftforge.fluids.FluidStack;', 'import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;')
            }
            [IO.File]::WriteAllText($out, $code, $utf8)
        }
    }
}
Write-Output 'Generated shared Minecraft 1.20.1 Fabric sources'
