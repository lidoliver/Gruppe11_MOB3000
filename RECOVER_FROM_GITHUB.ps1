# ADVANCED RECOVERY - Restore from GitHub
Write-Host "=== ADVANCED RECOVERY FROM GITHUB ===" -ForegroundColor Red

Write-Host "`nChecking what directories exist..." -ForegroundColor Yellow
Get-ChildItem -Path "app\src\main\java\" -Directory | Select-Object Name

Write-Host "`nFetching latest from GitHub..." -ForegroundColor Yellow
git fetch origin

Write-Host "`nShowing remote branches..." -ForegroundColor Yellow
git branch -r

Write-Host "`nResetting to origin/master (GitHub version)..." -ForegroundColor Yellow
git reset --hard origin/master

Write-Host "`nVerifying files after reset..." -ForegroundColor Yellow
if (Test-Path "app\src\main\java") {
    Get-ChildItem -Path "app\src\main\java\" -Recurse -Directory | Select-Object FullName | Select-Object -First 10
} else {
    Write-Host "ERROR: app\src\main\java doesn't exist!" -ForegroundColor Red
}

Write-Host "`nTrying alternative: Checkout files from Server-branch..." -ForegroundColor Yellow
git checkout origin/Server-branch -- app/src/main/java/

Write-Host "`nFinal verification..." -ForegroundColor Cyan
dir app\src\main\java\

Write-Host "`nGit Status:" -ForegroundColor Yellow
git status

Write-Host "`n=== RECOVERY ATTEMPT COMPLETE ===" -ForegroundColor Cyan

