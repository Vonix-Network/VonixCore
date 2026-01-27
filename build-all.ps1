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
$Version = "1.2"
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

$BuildCount = 0
$SuccessCount = 0

# Build NeoForge
Write-ColorOutput Yellow "[1/6] Building NeoForge version..."
$NeoForgeDir = Join-Path $PSScriptRoot "VonixCore-NeoForge-Universal"
if (Test-Path $NeoForgeDir) {
    $BuildCount++
    Push-Location $NeoForgeDir
    try {
        & ./gradlew build --no-daemon
        if ($LASTEXITCODE -eq 0) {
            $JarFiles = Get-ChildItem -Path "build\libs" -Filter "*.jar" | Where-Object { $_.Name -notmatch "sources|javadoc" }
            foreach ($jar in $JarFiles) {
                $DestName = "VonixCore-NeoForge-$Version.jar"
                Copy-Item $jar.FullName (Join-Path $OutputDir $DestName)
                Write-ColorOutput Green "  OK Built: $DestName"
                $SuccessCount++
            }
        } else {
            Write-ColorOutput Red "  FAIL NeoForge build failed!"
        }
    } finally {
        Pop-Location
    }
} else {
    Write-ColorOutput Red "  SKIP NeoForge directory not found"
}

Write-Host ""

# Build Forge 1.20.1
Write-ColorOutput Yellow "[2/6] Building Forge 1.20.1 version..."
$Forge1201Dir = Join-Path $PSScriptRoot "VonixCore-Forge-1.20.1"
if (Test-Path $Forge1201Dir) {
    $BuildCount++
    Push-Location $Forge1201Dir
    try {
        & ./gradlew build --no-daemon
        if ($LASTEXITCODE -eq 0) {
            $JarFiles = Get-ChildItem -Path "build\libs" -Filter "*.jar" | Where-Object { $_.Name -notmatch "sources|javadoc" }
            foreach ($jar in $JarFiles) {
                $DestName = "VonixCore-Forge-1.20.1-$Version.jar"
                Copy-Item $jar.FullName (Join-Path $OutputDir $DestName)
                Write-ColorOutput Green "  OK Built: $DestName"
                $SuccessCount++
            }
        } else {
            Write-ColorOutput Red "  FAIL Forge 1.20.1 build failed!"
        }
    } finally {
        Pop-Location
    }
} else {
    Write-ColorOutput Red "  SKIP Forge 1.20.1 directory not found"
}

Write-Host ""

# Build Forge 1.18.2
Write-ColorOutput Yellow "[3/6] Building Forge 1.18.2 version..."
$Forge1182Dir = Join-Path $PSScriptRoot "VonixCore-Template-Forge-1.18.2"
if (Test-Path $Forge1182Dir) {
    $BuildCount++
    Push-Location $Forge1182Dir
    try {
        & ./gradlew build --no-daemon
        if ($LASTEXITCODE -eq 0) {
            $JarFiles = Get-ChildItem -Path "build\libs" -Filter "*.jar" | Where-Object { $_.Name -notmatch "sources|javadoc" }
            foreach ($jar in $JarFiles) {
                $DestName = "VonixCore-Forge-1.18.2-$Version.jar"
                Copy-Item $jar.FullName (Join-Path $OutputDir $DestName)
                Write-ColorOutput Green "  OK Built: $DestName"
                $SuccessCount++
            }
        } else {
            Write-ColorOutput Red "  FAIL Forge 1.18.2 build failed!"
        }
    } finally {
        Pop-Location
    }
} else {
    Write-ColorOutput Red "  SKIP Forge 1.18.2 directory not found"
}

Write-Host ""

# Build Fabric 1.20.1
Write-ColorOutput Yellow "[4/6] Building Fabric 1.20.1 version..."
$Fabric1201Dir = Join-Path $PSScriptRoot "vonixcore-template-fabric-1.20.1"
if (Test-Path $Fabric1201Dir) {
    $BuildCount++
    Push-Location $Fabric1201Dir
    try {
        & ./gradlew build --no-daemon
        if ($LASTEXITCODE -eq 0) {
            $JarFiles = Get-ChildItem -Path "build\libs" -Filter "*.jar" | Where-Object { $_.Name -notmatch "sources|javadoc" }
            foreach ($jar in $JarFiles) {
                $DestName = "VonixCore-Fabric-1.20.1-$Version.jar"
                Copy-Item $jar.FullName (Join-Path $OutputDir $DestName)
                Write-ColorOutput Green "  OK Built: $DestName"
                $SuccessCount++
            }
        } else {
            Write-ColorOutput Red "  FAIL Fabric 1.20.1 build failed!"
        }
    } finally {
        Pop-Location
    }
} else {
    Write-ColorOutput Red "  SKIP Fabric 1.20.1 directory not found"
}

Write-Host ""

# Build Fabric 1.21.1
Write-ColorOutput Yellow "[5/6] Building Fabric 1.21.1 version..."
$Fabric1211Dir = Join-Path $PSScriptRoot "vonixcore-template-fabric-1.21.1"
if (Test-Path $Fabric1211Dir) {
    $BuildCount++
    Push-Location $Fabric1211Dir
    try {
        & ./gradlew build --no-daemon
        if ($LASTEXITCODE -eq 0) {
            $JarFiles = Get-ChildItem -Path "build\libs" -Filter "*.jar" | Where-Object { $_.Name -notmatch "sources|javadoc" }
            foreach ($jar in $JarFiles) {
                $DestName = "VonixCore-Fabric-1.21.1-$Version.jar"
                Copy-Item $jar.FullName (Join-Path $OutputDir $DestName)
                Write-ColorOutput Green "  OK Built: $DestName"
                $SuccessCount++
            }
        } else {
            Write-ColorOutput Red "  FAIL Fabric 1.21.1 build failed!"
        }
    } finally {
        Pop-Location
    }
} else {
    Write-ColorOutput Red "  SKIP Fabric 1.21.1 directory not found"
}

Write-Host ""

# Build Bukkit
Write-ColorOutput Yellow "[6/6] Building Bukkit version..."
$BukkitDir = Join-Path $PSScriptRoot "VonixCore-Bukkit-Universal"
if (Test-Path $BukkitDir) {
    $BuildCount++
    Push-Location $BukkitDir
    try {
        & ./gradlew shadowJar --no-daemon
        if ($LASTEXITCODE -eq 0) {
            $JarFiles = Get-ChildItem -Path "build\libs" -Filter "*.jar" | Where-Object { $_.Name -notmatch "sources|javadoc" }
            foreach ($jar in $JarFiles) {
                $DestName = "VonixCore-Bukkit-$Version.jar"
                Copy-Item $jar.FullName (Join-Path $OutputDir $DestName)
                Write-ColorOutput Green "  OK Built: $DestName"
                $SuccessCount++
            }
        } else {
            Write-ColorOutput Red "  FAIL Bukkit build failed!"
        }
    } finally {
        Pop-Location
    }
} else {
    Write-ColorOutput Red "  SKIP Bukkit directory not found"
}

Write-Host ""
Write-ColorOutput Cyan "=========================================="
Write-ColorOutput Cyan "             Build Complete!"
Write-ColorOutput Cyan "=========================================="
Write-Host ""
Write-Host "Builds attempted: $BuildCount, Successful: $SuccessCount"
Write-Host ""
Write-Host "Output files in: $OutputDir"
Write-Host ""
Get-ChildItem $OutputDir | ForEach-Object {
    $SizeMB = [math]::Round($_.Length / 1MB, 2)
    Write-Host ("  {0} ({1} MB)" -f $_.Name, $SizeMB)
}
