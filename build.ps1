[CmdletBinding()]
param(
    [string]$OutputDirectory = (Join-Path $PSScriptRoot 'dist')
)

$ErrorActionPreference = 'Stop'
$archiveTimestamp = [DateTimeOffset]::new(1980, 1, 1, 0, 0, 0, [TimeSpan]::Zero)

$requiredFiles = @(
    'module.prop',
    'customize.sh',
    'service.sh',
    'action.sh',
    'uninstall.sh',
    'config.conf.example',
    'services.list.example',
    'system/bin/a11yctl',
    'META-INF/com/google/android/update-binary',
    'META-INF/com/google/android/updater-script'
)

foreach ($relativePath in $requiredFiles) {
    $path = Join-Path $PSScriptRoot $relativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Missing module file: $relativePath"
    }
}

$moduleProperties = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'module.prop') -Encoding UTF8
$version = ($moduleProperties | Where-Object { $_ -like 'version=*' } | Select-Object -First 1) -replace '^version=', ''
if ([string]::IsNullOrWhiteSpace($version)) {
    throw 'module.prop does not define version'
}

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$outputPath = Join-Path $OutputDirectory "magisk-accessibility-manager-$version.zip"
if (Test-Path -LiteralPath $outputPath) {
    Remove-Item -LiteralPath $outputPath -Force
}

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::Open(
    $outputPath,
    [System.IO.Compression.ZipArchiveMode]::Create
)

try {
    foreach ($relativePath in $requiredFiles) {
        $sourcePath = Join-Path $PSScriptRoot $relativePath
        $entryName = $relativePath.Replace('\', '/')
        $entry = $archive.CreateEntry(
            $entryName,
            [System.IO.Compression.CompressionLevel]::Optimal
        )
        $entry.LastWriteTime = $archiveTimestamp
        $sourceStream = [System.IO.File]::OpenRead($sourcePath)
        $entryStream = $entry.Open()
        try {
            $sourceStream.CopyTo($entryStream)
        }
        finally {
            $entryStream.Dispose()
            $sourceStream.Dispose()
        }
    }
}
finally {
    $archive.Dispose()
}

Write-Output $outputPath
