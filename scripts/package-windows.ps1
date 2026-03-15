param(
    [ValidateSet('app-image', 'exe', 'both')]
    [string]$Type = 'both',

    [string]$DestDir = '',

    [switch]$SkipJPackage
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
$versionFile = Join-Path $rootDir 'build\version.txt'
$manifestBaseFile = Join-Path $rootDir 'build\manifest-base.txt'
$srcDir = Join-Path $rootDir 'src\main\java'
$resourcesDir = Join-Path $rootDir 'src\main\resources'
$outDir = Join-Path $rootDir 'out'
$distDir = Join-Path $rootDir 'dist'
$inputDir = Join-Path $rootDir 'build\package-input\windows'
$defaultDestDir = Join-Path $rootDir 'build\package\windows'
$packagingDir = Join-Path $rootDir 'packaging'
$appName = 'CircuitSim'
$mainClass = 'circuitsim.CircuitSim'
$vendor = 'BlazingHotCode'
$copyright = 'Copyright (c) 2026 BlazingHotCode'
$upgradeUuid = '5c0f7a4f-8b9c-4d22-94b8-5d6c4c7e7c11'
$menuGroup = 'BlazingHotCode'

if ([string]::IsNullOrWhiteSpace($DestDir)) {
    $DestDir = $defaultDestDir
}

function Require-Command {
    param([string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found: $Name"
    }
}

function Get-Version {
    if (-not (Test-Path $versionFile)) {
        throw "Missing version file: $versionFile"
    }

    return (Get-Content $versionFile -Raw).Trim()
}

function Get-JavaToolPath {
    param([string]$ToolName)

    $candidates = @()

    $commonJdkRoots = @(
        'C:\Program Files\Eclipse Adoptium',
        'C:\Program Files\Microsoft',
        'C:\Program Files\Java',
        'C:\Program Files\OpenJDK'
    )

    foreach ($root in $commonJdkRoots) {
        if (-not (Test-Path $root)) {
            continue
        }

        $jdkDirs = Get-ChildItem $root -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '^(jdk|temurin|openjdk|microsoft)' }
        foreach ($jdkDir in $jdkDirs) {
            $candidates += (Join-Path $jdkDir.FullName "bin\$ToolName.exe")
        }
    }

    if ($env:JAVA_HOME) {
        $candidates += (Join-Path $env:JAVA_HOME "bin\$ToolName.exe")
    }

    $javacCommand = Get-Command javac -ErrorAction SilentlyContinue
    if ($javacCommand) {
        $jdkBinDir = Split-Path -Parent $javacCommand.Source
        $jdkHome = Split-Path -Parent $jdkBinDir
        $candidates += (Join-Path $jdkHome "bin\$ToolName.exe")
    }

    $resolvedCommand = Get-Command $ToolName -ErrorAction SilentlyContinue
    if ($resolvedCommand) {
        return $resolvedCommand.Source
    }

    foreach ($candidate in $candidates | Select-Object -Unique) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    return $null
}

function Require-JavaToolPath {
    param([string]$ToolName)

    $toolPath = Get-JavaToolPath $ToolName
    if ($toolPath) {
        return $toolPath
    }

    $javacCommand = Get-Command javac -ErrorAction SilentlyContinue
    $javacSource = if ($javacCommand) { $javacCommand.Source } else { 'not found' }
    $javaHomeText = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'not set' }

    throw @"
Required tool not found: $ToolName

JAVA_HOME: $javaHomeText
javac resolved to: $javacSource

Your current JDK appears to be the Android Studio runtime, which includes javac/jlink/jdeps but not jpackage.
Install a full JDK 21 that includes jpackage (Temurin/Microsoft/Oracle/OpenJDK) and either:
  1. set JAVA_HOME to that JDK, or
  2. add its bin folder to PATH
"@
}

function Test-WixToolsAvailable {
    return ($null -ne (Get-WixToolPath 'light') -and $null -ne (Get-WixToolPath 'candle'))
}

function Get-WixToolPath {
    param([string]$ToolName)

    $resolvedCommand = Get-Command $ToolName -ErrorAction SilentlyContinue
    if ($resolvedCommand) {
        return $resolvedCommand.Source
    }

    $candidateRoots = @(
        'C:\Program Files (x86)\WiX Toolset v3.14\bin',
        'C:\Program Files (x86)\WiX Toolset v3.11\bin',
        'C:\Program Files\WiX Toolset v3.14\bin',
        'C:\Program Files\WiX Toolset v3.11\bin'
    )

    foreach ($root in $candidateRoots) {
        $candidate = Join-Path $root "$ToolName.exe"
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    return $null
}

function Add-WixToolsToPathIfNeeded {
    $lightPath = Get-WixToolPath 'light'
    $candlePath = Get-WixToolPath 'candle'
    if (-not $lightPath -or -not $candlePath) {
        return $false
    }

    $wixBin = Split-Path -Parent $lightPath
    if (-not ($env:PATH -split ';' | Where-Object { $_ -eq $wixBin })) {
        $env:PATH = "$wixBin;$env:PATH"
    }
    return $true
}

Require-Command javac
Require-Command jar
Require-Command jdeps

$javacPath = Require-JavaToolPath 'javac'
$jarPath = Require-JavaToolPath 'jar'
$jdepsPath = Require-JavaToolPath 'jdeps'

$version = Get-Version
$versionedJar = Join-Path $distDir "$appName-$version.jar"
$mainJar = Join-Path $distDir "$appName.jar"
$manifestFile = Join-Path $distDir 'manifest.generated.txt'

if (-not (Test-Path $manifestBaseFile)) {
    throw "Missing manifest template: $manifestBaseFile"
}

if (Test-Path $outDir) {
    Remove-Item $outDir -Recurse -Force
}

New-Item -ItemType Directory -Path $outDir -Force | Out-Null
New-Item -ItemType Directory -Path $distDir -Force | Out-Null

$sources = @(Get-ChildItem -Path $srcDir -Recurse -Filter '*.java' | Sort-Object FullName | ForEach-Object { $_.FullName })
if ($sources.Count -eq 0) {
    throw "No Java sources found under $srcDir"
}

& $javacPath --release 21 -d $outDir $sources
if ($LASTEXITCODE -ne 0) {
    throw 'javac failed'
}

if (Test-Path $resourcesDir) {
    Copy-Item (Join-Path $resourcesDir '*') $outDir -Recurse -Force
}

$manifestBase = (Get-Content $manifestBaseFile -Raw).TrimEnd([char[]]"`r`n")
Set-Content -Path $manifestFile -Value ($manifestBase + "`r`nImplementation-Version: $version`r`n") -Encoding ascii

& $jarPath cfm $versionedJar $manifestFile -C $outDir .
if ($LASTEXITCODE -ne 0) {
    throw 'jar failed'
}

Copy-Item $versionedJar $mainJar -Force

$modules = (& $jdepsPath --multi-release 21 --ignore-missing-deps --print-module-deps $mainJar).Trim()
if ([string]::IsNullOrWhiteSpace($modules)) {
    $modules = 'java.base,java.desktop'
}

if (Test-Path $inputDir) {
    Remove-Item $inputDir -Recurse -Force
}

New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
New-Item -ItemType Directory -Path $DestDir -Force | Out-Null
Copy-Item $mainJar (Join-Path $inputDir "$appName.jar") -Force

$appImageDir = Join-Path $DestDir $appName
if (Test-Path $appImageDir) {
    Remove-Item $appImageDir -Recurse -Force
}

$installerExe = Join-Path $DestDir "$appName-$version.exe"
if (($Type -eq 'exe' -or $Type -eq 'both') -and (Test-Path $installerExe)) {
    Remove-Item $installerExe -Force
}

if ($SkipJPackage) {
    Write-Host "Built jar only. Skipped jpackage. Required modules: $modules"
    exit 0
}

$jpackagePath = Require-JavaToolPath 'jpackage'

$commonArgs = @(
    '--name', $appName,
    '--app-version', $version,
    '--input', $inputDir,
    '--main-jar', "$appName.jar",
    '--main-class', $mainClass,
    '--dest', $DestDir,
    '--vendor', $vendor,
    '--description', 'Interactive, real-time circuit simulator',
    '--copyright', $copyright,
    '--add-modules', $modules,
    '--java-options', '-Dfile.encoding=UTF-8',
    '--java-options', "-Dcircuitsim.appVersion=$version"
)

$iconPath = Join-Path $packagingDir 'circuitsim.ico'
if (Test-Path $iconPath) {
    $commonArgs += @('--icon', $iconPath)
}

& $jpackagePath --type app-image @commonArgs
if ($LASTEXITCODE -ne 0) {
    throw 'jpackage app-image failed'
}

if ($Type -eq 'exe' -or $Type -eq 'both') {
    if (-not (Add-WixToolsToPathIfNeeded)) {
        throw @"
WiX Toolset v3 is required to build the Windows installer .exe.

What succeeded:
  - Portable app image created at: $appImageDir

How to fix:
  1. Install WiX Toolset 3.x from https://wixtoolset.org/
  2. Add the WiX install folder containing candle.exe and light.exe to PATH
  3. Re-run this script

If you only want the portable build, run:
  powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1 -Type app-image
"@
    }

    & $jpackagePath --type exe --win-shortcut --win-menu --win-menu-group $menuGroup --win-upgrade-uuid $upgradeUuid @commonArgs
    if ($LASTEXITCODE -ne 0) {
        throw 'jpackage exe failed'
    }
}

Write-Host "Windows packages created in $DestDir"
