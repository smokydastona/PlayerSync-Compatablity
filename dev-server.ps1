# Development Server Setup Script
# This script sets up a test server with the compatibility mod

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Development Server Setup" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Create run directory if it doesn't exist
if (-not (Test-Path "run")) {
    New-Item -ItemType Directory -Path "run" | Out-Null
    Write-Host "Created run directory" -ForegroundColor Green
}

# Build the mod first
Write-Host "Building mod..." -ForegroundColor Yellow
.\gradlew.bat build

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed! Cannot continue." -ForegroundColor Red
    exit
}

Write-Host ""
Write-Host "Starting development server..." -ForegroundColor Cyan
Write-Host "This will download Minecraft and Forge if needed." -ForegroundColor Yellow
Write-Host ""

# Run the server
.\gradlew.bat runServer

Write-Host ""
Write-Host "Server stopped." -ForegroundColor Yellow
