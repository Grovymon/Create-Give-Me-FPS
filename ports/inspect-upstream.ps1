param([string[]]$Targets = @())
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$matrix = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'minecraft-versions.json') -Raw | ConvertFrom-Json
$cacheDir = Join-Path $PSScriptRoot '.downloads'
[System.IO.Directory]::CreateDirectory($cacheDir) | Out-Null
$headers = @{'User-Agent' = 'Grovymon-Create-GMF-port-audit/0.1.3.3'}
$result = @()
foreach ($target in $matrix.targets) {
    if ($Targets.Count -gt 0 -and $target.id -notin $Targets) { continue }
    $version = Invoke-RestMethod -Headers $headers -Uri "https://api.modrinth.com/v2/version/$($target.createVersionId)"
    if ($target.minecraft -notin $version.game_versions) { throw "Minecraft mismatch: $($target.id)" }
    foreach ($loader in $target.loaders) {
        if ($loader -notin $version.loaders) { throw "Upstream does not declare $loader for $($target.id)" }
    }
    $primary = @($version.files | Where-Object primary)[0]
    if (!$primary) { throw "No primary artifact: $($target.id)" }
    $file = Join-Path $cacheDir $primary.filename
    if (!(Test-Path -LiteralPath $file)) {
        Invoke-WebRequest -Headers $headers -Uri $primary.url -OutFile $file
    }
    $hash = (Get-FileHash -LiteralPath $file -Algorithm SHA512).Hash.ToLowerInvariant()
    if ($hash -ne $primary.hashes.sha512) { throw "SHA512 mismatch: $file" }
    $zip = [System.IO.Compression.ZipFile]::OpenRead($file)
    try {
        $classes = @($zip.Entries | Where-Object { $_.FullName.EndsWith('.class') } | ForEach-Object FullName)
        $nested = @($zip.Entries | Where-Object { $_.FullName.EndsWith('.jar') } | ForEach-Object FullName)
        $row = [ordered]@{
            target=$target.id; minecraft=$target.minecraft; loaders=@($version.loaders)
            createVersion=$version.version_number; modrinthVersionId=$version.id
            url=$primary.url; filename=$primary.filename; bytes=$primary.size; sha512=$hash
            classes=$classes.Count; embeddedJars=$nested
            kineticVisual=('com/simibubi/create/content/kinetics/base/KineticBlockEntityVisual.class' -in $classes)
            kineticInstance=('com/simibubi/create/content/kinetics/base/KineticBlockEntityInstance.class' -in $classes)
            beltRenderer=('com/simibubi/create/content/kinetics/belt/BeltRenderer.class' -in $classes)
            validation='UPSTREAM_BINARY_ONLY_NOT_A_GMF_BUILD'
        }
        $result += [pscustomobject]$row
        Write-Output "$($target.id): $($version.version_number), checksum verified, $($classes.Count) classes"
    } finally { $zip.Dispose() }
}
$output = Join-Path $PSScriptRoot 'upstream-artifacts.lock.json'
if ($Targets.Count -gt 0 -and (Test-Path -LiteralPath $output)) {
    $previous = @(Get-Content -LiteralPath $output -Raw | ConvertFrom-Json)
    $result = @($previous | Where-Object { $_.target -notin $Targets }) + $result
}
[System.IO.File]::WriteAllText($output, (ConvertTo-Json -InputObject @($result) -Depth 8), (New-Object System.Text.UTF8Encoding($false)))
