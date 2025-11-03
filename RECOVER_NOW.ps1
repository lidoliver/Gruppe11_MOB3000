# AUTOMATIC RECOVERY - Just run this!
Write-Host "=== RECOVERING YOUR FILES ===" -ForegroundColor Green

Write-Host "`nResetting to commit 27116fe (before the deletion)..." -ForegroundColor Yellow
git reset --hard 27116fe

Write-Host "`n✓ Files restored!" -ForegroundColor Green
Write-Host "`nChecking your files are back..." -ForegroundColor Yellow

# Check if files exist
if (Test-Path "app\src\main\java\my\application\MainActivity.kt") {
    Write-Host "✓ MainActivity.kt is back!" -ForegroundColor Green
} else {
    Write-Host "✗ MainActivity.kt not found - trying alternative recovery..." -ForegroundColor Red
    git reset --hard HEAD@{1}
}

Write-Host "`n=== RECOVERY COMPLETE ===" -ForegroundColor Cyan
Write-Host "Your files should be restored now."
Write-Host "`nVerifying..." -ForegroundColor Yellow
dir app\src\main\java\

Write-Host "`nGit Status:" -ForegroundColor Yellow
git status

