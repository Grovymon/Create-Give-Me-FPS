$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$lang = Join-Path $root 'src\main\resources\assets\create_gmf\lang'
$enObject = Get-Content -LiteralPath (Join-Path $lang 'en_us.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$ruObject = Get-Content -LiteralPath (Join-Path $lang 'ru_ru.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$en = @{}
$ru = @{}
$enObject.PSObject.Properties | ForEach-Object { $en[$_.Name] = $_.Value }
$ruObject.PSObject.Properties | ForEach-Object { $ru[$_.Name] = $_.Value }
$missingRu = @($en.Keys | Where-Object { -not $ru.ContainsKey($_) })
$missingEn = @($ru.Keys | Where-Object { -not $en.ContainsKey($_) })
if ($missingRu.Count -or $missingEn.Count) {
    throw "Localization mismatch. Missing ru: $($missingRu -join ', '); missing en: $($missingEn -join ', ')"
}
$java = Get-ChildItem -LiteralPath (Join-Path $root 'src\main\java') -Recurse -Filter '*.java'
$hardcoded = @($java | Select-String -Pattern 'Component\.literal\("\p{L}')
if ($hardcoded.Count) {
    throw "Potential hardcoded UI strings:`n$($hardcoded -join "`n")"
}
Write-Output "Localization OK: $($en.Count) matching keys; no literal UI labels found."
