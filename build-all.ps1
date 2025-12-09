# VonixCore Build Script (PowerShell)
# Builds all platform versions and outputs to BuildOutput folder

$ErrorActionPreference = "Stop"

# Colors for output
function Write-ColorOutput($ForegroundColor, $Message) {
    Write-Host $Message -ForegroundColor $ForegroundColor
}

# Create output directory
$OutputDir = Join-Path $PSScriptRoot "BuildOutput"
if (Test-Path $OutputDir) {
    Remove-Item -Recurse -Force $OutputDir
}
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

Write-ColorOutput Cyan "=========================================="
Write-ColorOutput Cyan "     VonixCore Multi-Platform Build"
Write-ColorOutput Cyan "=========================================="
Write-Host ""

# Get version from gradle.properties (NeoForge as source of truth)
$Version = "1.0.0"
$PropsFile = Join-Path $PSScriptRoot "VonixCore-NeoForge-Universal\gradle.properties"
if (Test-Path $PropsFile) {
    $Content = Get-Content $PropsFile
    foreach ($line in $Content) {
        if ($line -match "^mod_version=(.+)$") {
            $Version = $Matches[1].Trim()
            break
        }
    }
}
Write-Host "Building version: $Version"
Write-Host ""

# Build NeoForge
Write-ColorOutput Yellow "[1/3] Building NeoForge version..."
$NeoForgeDir = Join-Path $PSScriptRoot "VonixCore-NeoForge-Universal"
if (Test-Path $NeoForgeDir) {
    Push-Location $NeoForgeDir
    try {
        & ./gradlew build --no-daemon
        if ($LASTEXITCODE -eq 0) {
            $JarFiles = Get-ChildItem -Path "build\libs" -Filter "*.jar" | Where-Object { $_.Name -notmatch "sources|javadoc" }
            foreach ($jar in $JarFiles) {
                $DestName = "VonixCore-NeoForge-$Version.jar"
                Copy-Item $jar.FullName (Join-Path $OutputDir $DestName)
                Write-ColorOutput Green "  OK Built: $DestName"
            }
        } else {
            Write-ColorOutput Red "  FAIL NeoForge build failed!"
        }
    } finally {
        Pop-Location
    }
} else {
    Write-ColorOutput Red "  FAIL NeoForge directory not found"
}

Write-Host ""

# Build Paper
Write-ColorOutput Yellow "[2/3] Building Paper version..."
$PaperDir = Join-Path $PSScriptRoot "VonixCore-Paper-Universal"
if (Test-Path $PaperDir) {
    Push-Location $PaperDir
    try {
        & ./gradlew fatJar --no-daemon
        if ($LASTEXITCODE -eq 0) {
            $JarFiles = Get-ChildItem -Path "build\libs" -Filter "*-all.jar"
            foreach ($jar in $JarFiles) {
                $DestName = "VonixCore-Paper-$Version.jar"
                Copy-Item $jar.FullName (Join-Path $OutputDir $DestName)
                Write-ColorOutput Green "  OK Built: $DestName"
            }
        } else {
            Write-ColorOutput Red "  FAIL Paper build failed!"
        }
    } finally {
        Pop-Location
    }
} else {
    Write-ColorOutput Red "  FAIL Paper directory not found"
}

Write-Host ""

# Build Bukkit
Write-ColorOutput Yellow "[3/3] Building Bukkit version..."
$BukkitDir = Join-Path $PSScriptRoot "VonixCore-Bukkit-Universal"
if (Test-Path $BukkitDir) {
    Push-Location $BukkitDir
    try {
        & ./gradlew fatJar --no-daemon
        if ($LASTEXITCODE -eq 0) {
            $JarFiles = Get-ChildItem -Path "build\libs" -Filter "*-all.jar"
            foreach ($jar in $JarFiles) {
                $DestName = "VonixCore-Bukkit-$Version.jar"
                Copy-Item $jar.FullName (Join-Path $OutputDir $DestName)
                Write-ColorOutput Green "  OK Built: $DestName"
            }
        } else {
            Write-ColorOutput Red "  FAIL Bukkit build failed!"
        }
    } finally {
        Pop-Location
    }
} else {
    Write-ColorOutput Red "  FAIL Bukkit directory not found"
}

Write-Host ""
Write-ColorOutput Cyan "=========================================="
Write-ColorOutput Cyan "             Build Complete!"
Write-ColorOutput Cyan "=========================================="
Write-Host ""
Write-Host "Output files in: $OutputDir"
Write-Host ""
Get-ChildItem $OutputDir | ForEach-Object {
    $SizeMB = [math]::Round($_.Length / 1MB, 2)
    Write-Host ("  {0} ({1} MB)" -f $_.Name, $SizeMB)
}
