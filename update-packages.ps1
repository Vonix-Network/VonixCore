$basePath = "VonixCore-Bukkit-Universal\src\main\java\network\vonix\vonixcore\graves"
$files = Get-ChildItem -Path $basePath -Filter "*.java" -Recurse

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $content = $content -replace 'package com\.ranull\.graves', 'package network.vonix.vonixcore.graves'
    $content = $content -replace 'import com\.ranull\.graves', 'import network.vonix.vonixcore.graves'
    Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
    Write-Host "Updated: $($file.Name)"
}
Write-Host "Done! Updated $($files.Count) files."
