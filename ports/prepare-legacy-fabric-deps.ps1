param([ValidateSet('fabric-1.19.2','fabric-1.18.2')][string]$Target = 'fabric-1.19.2')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$lock = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'upstream-artifacts.lock.json') -Raw | ConvertFrom-Json
$artifact = $lock | Where-Object target -eq $Target | Select-Object -First 1
if (!$artifact) { throw "Missing upstream lock: $Target" }
$source = Join-Path $PSScriptRoot ".downloads/$($artifact.filename)"
if ((Get-FileHash -LiteralPath $source -Algorithm SHA512).Hash.ToLowerInvariant() -ne $artifact.sha512) { throw 'Create hash mismatch' }
$destination = Join-Path $PSScriptRoot ".legacy-deps/$Target"
[IO.Directory]::CreateDirectory($destination) | Out-Null
$queue = [Collections.Generic.Queue[string]]::new()
$queue.Enqueue($source)
$visited = [Collections.Generic.HashSet[string]]::new()
$inventory = [Collections.Generic.List[object]]::new()
while ($queue.Count -gt 0) {
    $path = $queue.Dequeue()
    $zip = [IO.Compression.ZipFile]::OpenRead($path)
    try {
        $entry = $zip.GetEntry('fabric.mod.json')
        if (!$entry) { continue }
        $reader = [IO.StreamReader]::new($entry.Open())
        try { $metadata = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
        if ($path -ne $source) { $inventory.Add([pscustomobject]@{ file=[IO.Path]::GetFileName($path); id=$metadata.id; version=$metadata.version }) }
        foreach ($nested in $metadata.jars) {
            $name = [IO.Path]::GetFileName($nested.file)
            if (!$visited.Add($name)) { continue }
            $output = Join-Path $destination $name
            [IO.Compression.ZipFileExtensions]::ExtractToFile($zip.GetEntry($nested.file), $output, $true)
            $queue.Enqueue($output)
            Write-Output "Extracted unmodified embedded dependency: $name"
        }
    } finally { $zip.Dispose() }
}
# Match Fabric Loader's selection of the newest nested candidate per mod ID.
# Older duplicates are kept on disk for provenance, never added to classpath.
$selected = @($inventory | Group-Object id | ForEach-Object {
    $_.Group | Sort-Object @{Expression={
        $numeric = [regex]::Match($_.version, '\d+(\.\d+){1,3}').Value
        if ($numeric) { [version]$numeric } else { [version]'0.0' }
    }; Descending=$true} | Select-Object -First 1
})
[IO.File]::WriteAllText((Join-Path $destination 'mods.json'), ($selected | ConvertTo-Json -Depth 4), (New-Object Text.UTF8Encoding($false)))
