param([ValidateSet('forge-1.20.1','forge-1.19.2','forge-1.18.2')][string]$Target = 'forge-1.20.1', [switch]$ForFabric)
$ErrorActionPreference = 'Stop'
$portRoot = $PSScriptRoot
$sourceRoot = Join-Path (Split-Path $portRoot) 'src/main'
$relativeDestination = if ($ForFabric) { 'fabric-1.20.1/.forge-shared' } elseif ($Target -ne 'forge-1.20.1') { "$Target/.forge-shared" } else { 'forge-1.20.1/.generated' }
$destination = Join-Path $portRoot $relativeDestination
$expectedDestination = [System.IO.Path]::GetFullPath((Join-Path $portRoot $relativeDestination))
if ([System.IO.Path]::GetFullPath($destination) -ne $expectedDestination) { throw 'Unsafe generated-source destination' }
if (Test-Path -LiteralPath $destination) { Remove-Item -LiteralPath $destination -Recurse -Force }
$utf8 = New-Object System.Text.UTF8Encoding($false)
# Mechanical namespace/API conversion only. Platform-specific files are versioned
# under the port's src/ directory and take precedence over the shared baseline.
$overrides = Join-Path $portRoot 'forge-1.20.1/src/main'
foreach ($kind in @('java', 'resources')) {
    $base = Join-Path $sourceRoot $kind
    foreach ($file in Get-ChildItem -LiteralPath $base -Recurse -File) {
        $relative = $file.FullName.Substring($base.Length + 1)
        # Flywheel 1.0.5 dispatches simple visuals directly from Storage. The
        # concrete StorageAnimationMixin supplies these hooks without requiring
        # unsupported interface injectors in Forge's Mixin 0.8.5.
        if ($relative -match 'Simple(Dynamic|Tickable)VisualMixin.java$') { continue }
        if (Test-Path -LiteralPath (Join-Path "$overrides/$kind" $relative)) { continue }
        if ($relative -eq 'META-INF\neoforge.mods.toml') { continue }
        $out = Join-Path "$destination/$kind" $relative
        [System.IO.Directory]::CreateDirectory((Split-Path $out)) | Out-Null
        if ($kind -eq 'resources') {
            if ($relative -eq 'create_gmf.mixins.json') {
                $json = [System.IO.File]::ReadAllText($file.FullName).Replace('JAVA_21', 'JAVA_17')
                $json = $json.Replace('"plugin":', '"refmap": "create_gmf.refmap.json", "plugin":')
                $json = $json.Replace('"client.SimpleDynamicVisualMixin",', '"client.StorageAnimationMixin",')
                $json = $json.Replace('"client.SimpleTickableVisualMixin",', '')
                [System.IO.File]::WriteAllText($out, $json, $utf8)
                continue
            }
            [System.IO.File]::Copy($file.FullName, $out, $true)
            continue
        }
        $code = [System.IO.File]::ReadAllText($file.FullName)
        $code = $code.Replace('net.neoforged.neoforge.common.ModConfigSpec', 'net.minecraftforge.common.ForgeConfigSpec')
        $code = $code.Replace('ModConfigSpec', 'ForgeConfigSpec')
        $code = $code.Replace('net.neoforged.neoforge.', 'net.minecraftforge.')
        $code = $code.Replace('net.neoforged.fml.', 'net.minecraftforge.fml.')
        $code = $code.Replace('net.neoforged.bus.api.', 'net.minecraftforge.eventbus.api.')
        $code = $code.Replace('net.neoforged.api.distmarker.', 'net.minecraftforge.api.distmarker.')
        $code = $code.Replace('ResourceLocation.fromNamespaceAndPath(', 'new ResourceLocation(')
        $code = $code.Replace('.getFirst()', '.get(0)')
        $code = $code.Replace('Math.clamp(', 'net.minecraft.util.Mth.clamp(')
        $code = $code.Replace('NeoForgeFlwConfig', 'ForgeFlwConfig')
        if ($relative.EndsWith('BeltRendererMixin.java')) {
            $code = $code.Replace('@Mixin(BeltRenderer.class)', '@Mixin(value = BeltRenderer.class, remap = false)')
        }
        if ($relative.EndsWith('CreateBlockEntityDynamicVisualMixin.java')) {
            # 1.20.1 SignalVisual is SimpleTickableVisual only. It is covered by
            # StorageAnimationMixin.tickPlan, not by a nonexistent beginFrame.
            $code = $code.Replace(', "com.simibubi.create.content.trains.signal.SignalVisual"', '')
        }
        if ($relative -match 'EntityCulling(CullTask|Prefetch)Mixin.java$') {
            $code = $code.Replace('@Mixin(targets', '@org.spongepowered.asm.mixin.Pseudo' + "`n" + '@Mixin(targets')
        }
        if ($relative.EndsWith('GmfMixinPlugin.java')) {
            $code = $code.Replace('isLoaded("sodium") && isLoaded("iris")', 'isLoaded("embeddium") && isLoaded("oculus")')
        }
        if ($relative.EndsWith('ShaderStatusDetector.java')) {
            $code = $code.Replace('isLoaded("iris")', 'isLoaded("oculus")')
        }
        $code = $code.Replace('renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)', 'renderBackground(GuiGraphics graphics)')
        $code = $code.Replace('mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)', 'mouseScrolled(double mouseX, double mouseY, double scrollY)')
        $code = $code.Replace('super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)', 'super.mouseScrolled(mouseX, mouseY, scrollY)')
        if ($relative.Contains('fast_render')) {
            $code = $code.Replace('import com.mojang.blaze3d.vertex.MeshData;', 'import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;')
            $code = $code.Replace('MeshData data', 'RenderedBuffer data').Replace('bufferBuilder.build()', 'bufferBuilder.end()')
            $code = $code.Replace('data.close()', 'data.release()')
            $code = $code.Replace('builder.addVertex(pos0.x, pos0.y, pos0.z).setColor(color).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(float3.x, float3.y, float3.z);', 'builder.vertex(pos0.x, pos0.y, pos0.z).color(color).uv(u, v).overlayCoords(overlay).uv2(light).normal(float3.x, float3.y, float3.z).endVertex();')
            $code = $code.Replace('writer.push(STACK, SCRATCH_BUFFER, BUFFED_VERTEX, format);', 'writer.push(STACK, SCRATCH_BUFFER, BUFFED_VERTEX, VertexFormatRegistry.instance().get(format));')
            $code = [regex]::Replace($code, 'FORMAT.getOffset\((IrisVertexFormats\.\w+)\)', 'FORMAT.getOffset(FORMAT.getElements().indexOf($1))')
            if ($relative.EndsWith('ShadedBlockSbbBuilderMixin.java')) {
                $code = [regex]::Replace($code, '@Shadow\s+protected boolean invertFakeNormal;', '')
                $code = $code.Replace('shadeSwapVertices.toIntArray(), invertFakeNormal', 'shadeSwapVertices.toIntArray(), false')
            }
        }
        if ($relative.EndsWith('ClientRuntime.java')) {
            $code = $code.Replace('import net.minecraftforge.client.event.ClientTickEvent;', 'import net.minecraftforge.event.TickEvent;')
            $code = $code.Replace('import net.minecraftforge.client.event.RenderFrameEvent;', '')
            $code = $code.Replace('onRenderFrame(RenderFrameEvent.Pre event) {', "onRenderFrame(TickEvent.RenderTickEvent event) {`n        if (event.phase != TickEvent.Phase.START) return;")
            $code = $code.Replace('onClientTick(ClientTickEvent.Post event) {', "onClientTick(TickEvent.ClientTickEvent event) {`n        if (event.phase != TickEvent.Phase.END) return;")
        }
        if ($relative.EndsWith('ServerTickCollector.java')) {
            $code = $code.Replace('import net.minecraftforge.fml.common.EventBusSubscriber;', 'import net.minecraftforge.fml.common.Mod.EventBusSubscriber;')
            $code = $code.Replace('import net.minecraftforge.event.tick.ServerTickEvent;', 'import net.minecraftforge.event.TickEvent;')
            $code = $code.Replace('onServerTickPre(ServerTickEvent.Pre event) {', "onServerTickPre(TickEvent.ServerTickEvent event) {`n        if (event.phase != TickEvent.Phase.START) return;")
            $code = $code.Replace('onServerTickPost(ServerTickEvent.Post event) {', "onServerTickPost(TickEvent.ServerTickEvent event) {`n        if (event.phase != TickEvent.Phase.END) return;")
        }
        if ($relative.EndsWith('PerformanceOverlay.java')) {
            $code = $code.Replace('import net.minecraft.client.DeltaTracker;', '')
            $code = $code.Replace('import net.minecraft.client.gui.LayeredDraw.Layer;', '')
            $code = $code.Replace('import net.minecraftforge.client.event.RegisterGuiLayersEvent;', 'import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;')
            $code = $code.Replace('import net.minecraftforge.client.gui.VanillaGuiLayers;', '')
            $code = $code.Replace('private static final Layer LAYER = PerformanceOverlay::render;', '')
            $code = $code.Replace('register(RegisterGuiLayersEvent event)', 'register(RegisterGuiOverlaysEvent event)')
            $code = $code.Replace('event.registerAbove(VanillaGuiLayers.HOTBAR, ID, LAYER);', 'event.registerAboveAll("gmf_statistics", (gui, graphics, partialTick, width, height) -> render(graphics, partialTick));')
            $code = $code.Replace('GuiGraphics graphics, DeltaTracker deltaTracker', 'GuiGraphics graphics, float partialTick')
        }
        [System.IO.File]::WriteAllText($out, $code, $utf8)
    }
}
Write-Output "Generated shared 1.20.1 Forge sources from $sourceRoot"
