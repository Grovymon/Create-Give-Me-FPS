param([string]$OutputDirectory = (Join-Path $PSScriptRoot '../../releases/0.1.3.3/multiversion'))
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$matrix = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'minecraft-versions.json') -Raw | ConvertFrom-Json
[IO.Directory]::CreateDirectory([IO.Path]::GetFullPath($OutputDirectory)) | Out-Null
$report = @()
foreach ($target in $matrix.targets) {
    $project = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot $target.project))
    $name = if ($target.id -eq 'neoforge-1.21.1') { 'create_gmf-0.1.3.3.jar' }
        elseif ($target.id -eq 'forge-1.20.1') { 'create_gmf-0.1.3.3-mc1.20.1-forge-neoforge.jar' }
        else { "create_gmf-0.1.3.3-mc$($target.minecraft)-$(if ($target.id.StartsWith('fabric-')) {'fabric-quilt'} else {'forge'}).jar" }
    $path = Join-Path $project "build/libs/$name"
    if (!(Test-Path -LiteralPath $path)) { throw "Missing production artifact: $path" }
    $zip = [IO.Compression.ZipFile]::OpenRead($path)
    try {
        foreach ($required in @('dev/creategmf/CreateGmf.class','create_gmf.mixins.json','assets/create_gmf/lang/en_us.json','assets/create_gmf/lang/ru_ru.json')) {
            if (!$zip.GetEntry($required)) { throw "$name missing $required" }
        }
        if (!($zip.Entries.FullName -match '(^|/)LICENSE$')) { throw "$name missing licence" }
        if (!($zip.Entries.FullName -match 'THIRD_PARTY_NOTICES')) { throw "$name missing notices" }
        foreach ($entry in $zip.Entries | Where-Object FullName -like 'dev/creategmf/*.class') {
            $stream = $entry.Open()
            try {
                $header = New-Object byte[] 8
                if ($stream.Read($header, 0, 8) -ne 8) { throw 'Truncated class' }
                $major = $header[6] * 256 + $header[7]
                if ($major -gt ($target.java + 44)) { throw "$name has incompatible bytecode $major" }
            } finally { $stream.Dispose() }
        }
        $languages = @{}
        foreach ($locale in @('en_us','ru_ru')) {
            $reader = [IO.StreamReader]::new($zip.GetEntry("assets/create_gmf/lang/$locale.json").Open())
            try { $languages[$locale] = ($reader.ReadToEnd() | ConvertFrom-Json).PSObject.Properties.Name | Sort-Object } finally { $reader.Dispose() }
        }
        if (Compare-Object $languages.en_us $languages.ru_ru) { throw "$name language key mismatch" }
        $metadata = if ($target.id.StartsWith('fabric-')) { 'fabric.mod.json' }
            elseif ($target.id.StartsWith('neoforge-')) { 'META-INF/neoforge.mods.toml' } else { 'META-INF/mods.toml' }
        $reader = [IO.StreamReader]::new($zip.GetEntry($metadata).Open())
        try { $text = $reader.ReadToEnd() } finally { $reader.Dispose() }
        if (!$text.Contains('0.1.3.3') -or !$text.Contains($target.minecraft) -or !$text.Contains('entityculling')) { throw "$name wrong metadata" }
        if ($target.id -ne 'neoforge-1.21.1' -and !$zip.GetEntry('create_gmf.refmap.json')) { throw "$name missing production refmap" }
        if ($target.id.StartsWith('forge-') -and !$zip.GetEntry('META-INF/jarjar/mixinextras-forge-0.4.1.jar')) { throw "$name missing embedded MixinExtras" }
        if ($target.id.StartsWith('fabric-') -and !($zip.Entries.FullName -match 'META-INF/jars/mixinextras-fabric-0.4.1.jar')) { throw "$name missing embedded MixinExtras" }
    } finally { $zip.Dispose() }
    $tests = 0
    foreach ($xml in Get-ChildItem -LiteralPath "$project/build/test-results/test" -Filter 'TEST-*.xml') {
        $suite = [xml](Get-Content -LiteralPath $xml.FullName -Raw)
        if ([int]$suite.testsuite.failures -gt 0 -or [int]$suite.testsuite.errors -gt 0) { throw "$name has failing tests" }
        $tests += [int]$suite.testsuite.tests
    }
    if ($tests -lt 18) { throw "$name missing test evidence" }
    $output = Join-Path $OutputDirectory $name
    if ([IO.Path]::GetFullPath($path) -ne [IO.Path]::GetFullPath($output)) { Copy-Item -LiteralPath $path -Destination $output }
    $report += [pscustomobject]@{ target=$target.id; file=$name; bytes=(Get-Item -LiteralPath $path).Length; sha256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant(); tests=$tests; validation='BUILD_TESTS_PACKAGE_ONLY'; worldTest='NOT_VERIFIED' }
}
$json = $report | ConvertTo-Json -Depth 6
[IO.File]::WriteAllText((Join-Path $OutputDirectory 'artifacts.json'), $json, (New-Object Text.UTF8Encoding($false)))
$report | Format-Table target,bytes,tests,file
