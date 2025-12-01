# Quick Build Script for PlayerSync Traveler's Backpack Compatibility
# This script helps you quickly build the mod

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "PlayerSync Traveler's Backpack Compat" -ForegroundColor Cyan
Write-Host "Quick Build Script" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Check for PlayerSync JAR
$libsPath = "libs"
$playerSyncJar = Get-ChildItem -Path $libsPath -Filter "playersync-*.jar" -ErrorAction SilentlyContinue

if (-not $playerSyncJar) {
    Write-Host "WARNING: PlayerSync JAR not found in libs/ folder!" -ForegroundColor Yellow
    Write-Host "Please download PlayerSync from:" -ForegroundColor Yellow
    Write-Host "https://github.com/mlus-asuka/PlayerSync/releases" -ForegroundColor Cyan
    Write-Host ""
    $continue = Read-Host "Continue anyway? (y/n)"
    if ($continue -ne "y") {
        exit
    }
} else {
    Write-Host "Found PlayerSync: $($playerSyncJar.Name)" -ForegroundColor Green
}

Write-Host ""
Write-Host "Starting build..." -ForegroundColor Cyan

# Clean previous builds
Write-Host "Cleaning previous builds..." -ForegroundColor Yellow
.\gradlew.bat clean

# Build the mod
Write-Host ""
Write-Host "Building mod..." -ForegroundColor Yellow
.\gradlew.bat build

# Check if build was successful
if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host "Build Successful!" -ForegroundColor Green
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host ""
    
    $outputJar = Get-ChildItem -Path "build\libs" -Filter "*.jar" | Where-Object { $_.Name -notlike "*-sources.jar" } | Select-Object -First 1
    
    if ($outputJar) {
        Write-Host "Output JAR: $($outputJar.FullName)" -ForegroundColor Cyan
        Write-Host "Size: $([math]::Round($outputJar.Length / 1KB, 2)) KB" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "You can now copy this JAR to your server's mods folder." -ForegroundColor Green
    }
} else {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Red
    Write-Host "Build Failed!" -ForegroundColor Red
    Write-Host "=====================================" -ForegroundColor Red
    Write-Host "Check the output above for errors." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
