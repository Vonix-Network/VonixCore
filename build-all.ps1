
# Define Project Directories
$projects = @(
    "VonixCore-Bukkit-Universal",
    "vonixcore-template-fabric-1.20.1",
    "vonixcore-template-fabric-1.21.1",
    "VonixCore-Forge-1.20.1",
    "VonixCore-NeoForge-Universal",
    "VonixCore-Template-Forge-1.18.2"
)

# Loop Through Each Project and Build
foreach ($project in $projects) {
    Write-Host "Building $project..."
    Push-Location $project
    ./gradlew build
    Pop-Location
}
