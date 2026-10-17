param([ValidateSet('forge-1.19.2','forge-1.18.2')][string]$Target = 'forge-1.19.2')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$lock = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'upstream-artifacts.lock.json') -Raw | ConvertFrom-Json
$artifact = $lock | Where-Object target -eq $Target | Select-Object -First 1
if (!$artifact) { throw "Missing upstream lock: $Target" }
$jar = Join-Path $PSScriptRoot ".downloads/$($artifact.filename)"
if (!(Test-Path -LiteralPath $jar)) { throw 'Run inspect-upstream.ps1 first to fetch and verify Create.' }
if ((Get-FileHash -LiteralPath $jar -Algorithm SHA512).Hash.ToLowerInvariant() -ne $artifact.sha512) { throw 'Create hash mismatch' }
$zip = [IO.Compression.ZipFile]::OpenRead($jar)
try {
    $reader = [IO.StreamReader]::new($zip.GetEntry('META-INF/jarjar/metadata.json').Open())
    try { $metadata = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
    foreach ($nested in $metadata.jars) {
        $group = $nested.identifier.group
        $name = $nested.identifier.artifact
        $version = $nested.version.artifactVersion
        if ($group -notin @('com.jozufozu.flywheel','com.tterrag.registrate')) { continue }
        $directory = Join-Path $PSScriptRoot ('.maven/' + $group.Replace('.','/') + "/$name/$version")
        [IO.Directory]::CreateDirectory($directory) | Out-Null
        $output = Join-Path $directory "$name-$version.jar"
        [IO.Compression.ZipFileExtensions]::ExtractToFile($zip.GetEntry($nested.path), $output, $true)
        $pom = '<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>' + $group + '</groupId><artifactId>' + $name + '</artifactId><version>' + $version + '</version></project>'
        [IO.File]::WriteAllText((Join-Path $directory "$name-$version.pom"), $pom, (New-Object Text.UTF8Encoding($false)))
        Write-Output "Extracted unmodified $group`:$name`:$version from verified Create distribution"
    }
} finally { $zip.Dispose() }
