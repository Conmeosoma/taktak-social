param(
    [string]$ResourceRoot = (Join-Path $PSScriptRoot '../android-client/app/src/main/res')
)

$ErrorActionPreference = 'Stop'

function Read-StringResources([string]$Directory) {
    $strings = @{}
    foreach ($file in Get-ChildItem -LiteralPath $Directory -Filter '*.xml') {
        [xml]$document = Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8
        foreach ($node in $document.SelectNodes('/resources/string')) {
            $name = $node.GetAttribute('name')
            if ($strings.ContainsKey($name)) { throw "Duplicate string: $name in $Directory" }
            $strings[$name] = $node
        }
    }
    return ,$strings
}

function Get-FormatSignature([string]$Value) {
    # Exclude %% (a literal percent). Preserve argument positions and types.
    return (@([regex]::Matches($Value, '(?<!%)%(?:(\d+)\$)?([dsf])') |
        ForEach-Object { $_.Value } | Sort-Object) -join ',')
}

$base = Read-StringResources (Join-Path $ResourceRoot 'values')
$checked = 0
foreach ($locale in @('en', 'es', 'zh', 'ja', 'ko', 'hi')) {
    $localized = Read-StringResources (Join-Path $ResourceRoot "values-$locale")
    foreach ($name in $base.Keys) {
        $source = $base[$name]
        if ($source.GetAttribute('translatable') -eq 'false') { continue }
        if (-not $localized.ContainsKey($name)) { throw "Missing translation: $locale/$name" }
        if ([string]::IsNullOrWhiteSpace($localized[$name].InnerText)) { throw "Empty translation: $locale/$name" }
        if ((Get-FormatSignature $source.InnerText) -ne (Get-FormatSignature $localized[$name].InnerText)) {
            throw "Format arguments differ: $locale/$name"
        }
        $checked++
    }
}
Write-Output "PASS: $checked localized strings checked across six locales (XML, duplicates, coverage, format arguments)."
