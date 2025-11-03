# EMERGENCY RECOVERY SCRIPT
# This will restore all your files

Write-Host "=== EMERGENCY FILE RECOVERY ===" -ForegroundColor Red

# Step 1: Check reflog to find the last good commit
Write-Host "`nFinding your last good commit..." -ForegroundColor Yellow
git reflog | Select-Object -First 20

Write-Host "`n=== INSTRUCTIONS ===" -ForegroundColor Cyan
Write-Host "Look at the reflog output above."
Write-Host "Find the line BEFORE 'Fix: Normalize all paths' commit"
Write-Host "It will look like: 'HEAD@{1}: commit: your previous commit message'"
Write-Host ""
Write-Host "Then run ONE of these commands:" -ForegroundColor Yellow
Write-Host ""
Write-Host "Option 1 - Reset to previous commit (recommended):" -ForegroundColor Green
Write-Host "  git reset --hard HEAD@{1}"
Write-Host ""
Write-Host "Option 2 - If HEAD@{1} isn't right, use the commit hash:" -ForegroundColor Green
Write-Host "  git reset --hard <commit-hash-from-reflog>"
Write-Host ""
Write-Host "Option 3 - Restore files without changing history:" -ForegroundColor Green
Write-Host "  git checkout HEAD@{1} -- ."
Write-Host ""

# Show current status
Write-Host "`nCurrent Git Status:" -ForegroundColor Yellow
git status

Write-Host "`n=== NEXT STEPS ===" -ForegroundColor Cyan
Write-Host "1. Choose one of the recovery commands above"
Write-Host "2. Run it in this terminal"
Write-Host "3. Your files will be restored immediately"
Write-Host ""
Write-Host "ALL YOUR WORK IS SAFE IN GIT HISTORY!" -ForegroundColor Green

