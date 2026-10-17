param(
    [string[]]$Targets = @('forge-1.20.1','forge-1.19.2','forge-1.18.2','fabric-1.20.1','fabric-1.19.2','fabric-1.18.2'),
    [string]$ForgeGradle,
    [string]$FabricGradle,
    [string]$FabricDaemonJava,
    [string]$Java17
)
$ErrorActionPreference = 'Stop'
function Find-Gradle([string]$Version) {
    $cache = Join-Path $env:USERPROFILE ".gradle/wrapper/dists/gradle-$Version-bin"
    $candidate = Get-ChildItem -LiteralPath $cache -Recurse -Filter gradle.bat -ErrorAction SilentlyContinue | Select-Object -First 1
    if (!$candidate) { throw "Gradle $Version not found; pass its absolute .bat path explicitly." }
    return $candidate.FullName
}
if (!$ForgeGradle) { $ForgeGradle = Find-Gradle '9.2.1' }
if (!$FabricGradle) { $FabricGradle = Find-Gradle '9.5.1' }
if (!$Java17) {
    $candidate = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot '.tools/jdk17') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($candidate) { $Java17 = $candidate.FullName }
}
if (!$Java17 -or !(Test-Path -LiteralPath (Join-Path $Java17 'bin/java.exe'))) { throw 'Pass -Java17 with a JDK 17 directory.' }
if (!$FabricDaemonJava -and ($Targets | Where-Object { $_ -like 'fabric-*' })) {
    throw 'Pass -FabricDaemonJava with a JDK 25 directory for Loom 1.17.17 / Gradle 9.5.1. Published code still targets Java 17.'
}
$matrix = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'minecraft-versions.json') -Raw | ConvertFrom-Json
foreach ($target in $Targets) {
    $entry = $matrix.targets | Where-Object id -eq $target | Select-Object -First 1
    if (!$entry -or $target -eq 'neoforge-1.21.1') { throw "Unknown port $target; build 1.21.1 using the parent wrapper." }
    if ($target -in @('forge-1.19.2','forge-1.18.2')) { & "$PSScriptRoot/prepare-legacy-deps.ps1" -Target $target }
    if ($target -in @('fabric-1.19.2','fabric-1.18.2')) { & "$PSScriptRoot/prepare-legacy-fabric-deps.ps1" -Target $target }
    $gradle = if ($target.StartsWith('fabric-')) { $FabricGradle } else { $ForgeGradle }
    $arguments = @('build', '--console=plain', "-Porg.gradle.java.installations.paths=$Java17", '-Porg.gradle.java.installations.auto-download=false')
    if ($target.StartsWith('fabric-')) { $arguments += "-Dorg.gradle.java.home=$FabricDaemonJava" }
    Push-Location (Join-Path $PSScriptRoot $entry.project)
    try {
        & $gradle @arguments
        if ($LASTEXITCODE -ne 0) { throw "Build failed: $target" }
    } finally { Pop-Location }
}
