# SAFE RECOVERY - Using Git Revert (No History Overwrite)
Write-Host "=== SAFE RECOVERY USING GIT REVERT ===" -ForegroundColor Green

# Fetch everything
Write-Host "`nFetching from GitHub..." -ForegroundColor Yellow
git fetch origin --all

# Method 1: Revert the bad commits
Write-Host "`nMethod 1: Reverting the problematic commits..." -ForegroundColor Cyan
git checkout master

Write-Host "`nReverting commit 217cd6f (powershel git clean)..." -ForegroundColor Yellow
git revert 217cd6f --no-edit

Write-Host "`nReverting commit 27116fe (Fix: Normalize all paths)..." -ForegroundColor Yellow
git revert 27116fe --no-edit

Write-Host "`n✓ Master branch reverted!" -ForegroundColor Green

# Check if files are back
Write-Host "`nVerifying files..." -ForegroundColor Yellow
if (Test-Path "app\src\main\java\My\Application\MainActivity.kt") {
    Write-Host "✓ Files restored with My/Application casing!" -ForegroundColor Green
} elseif (Test-Path "app\src\main\java\my\application\MainActivity.kt") {
    Write-Host "✓ Files restored with my/application casing!" -ForegroundColor Green
} else {
    Write-Host "⚠ Files not found, trying alternative method..." -ForegroundColor Yellow

    # Alternative: Checkout files from old commit
    Write-Host "`nRestoring files from commit 063ba2c..." -ForegroundColor Yellow
    git checkout 063ba2c -- app/src/
    git add app/src/
    git commit -m "Restore all source files from original clone (063ba2c)"
}

# Show what we have
Write-Host "`nCurrent files:" -ForegroundColor Cyan
dir app\src\main\java\ -Directory

Write-Host "`nGit Status:" -ForegroundColor Cyan
git status

Write-Host "`n=== READY TO PUSH (SAFE) ===" -ForegroundColor Green
Write-Host "`nThis creates NEW commits that restore your files." -ForegroundColor Cyan
Write-Host "GitHub history is preserved - nothing is overwritten!" -ForegroundColor Green
Write-Host "`nTo push to GitHub, run:" -ForegroundColor Yellow
Write-Host "  git push origin master" -ForegroundColor White
Write-Host "  git push origin Server-branch" -ForegroundColor White

