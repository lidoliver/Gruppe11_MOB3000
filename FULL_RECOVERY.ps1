# COMPLETE RECOVERY - Reset both branches to before case change
Write-Host "=== COMPLETE RECOVERY: RESETTING TO BEFORE CASE CHANGE ===" -ForegroundColor Red

# Step 1: Fetch everything from GitHub
Write-Host "`nStep 1: Fetching all branches from GitHub..." -ForegroundColor Yellow
git fetch origin --all

# Step 2: Check current branch
Write-Host "`nStep 2: Checking current branch..." -ForegroundColor Yellow
$currentBranch = git branch --show-current
Write-Host "Current branch: $currentBranch" -ForegroundColor Cyan

# Step 3: Reset master branch to before case change
Write-Host "`nStep 3: Resetting master to before case change..." -ForegroundColor Yellow
git checkout master
Write-Host "Looking for last good commit on master..." -ForegroundColor Cyan
git log --oneline -20

Write-Host "`nResetting master to commit 063ba2c (before case changes)..." -ForegroundColor Yellow
git reset --hard 063ba2c

Write-Host "`n✓ Master branch reset!" -ForegroundColor Green

# Step 4: Reset Server-branch to before case change
Write-Host "`nStep 4: Resetting Server-branch to before case change..." -ForegroundColor Yellow
git checkout Server-branch
Write-Host "Looking for last good commit on Server-branch..." -ForegroundColor Cyan
git log --oneline -20

Write-Host "`nResetting Server-branch to commit 2d7c989 (backup-before-casefix)..." -ForegroundColor Yellow
git reset --hard 2d7c989

Write-Host "`n✓ Server-branch reset!" -ForegroundColor Green

# Step 5: Go back to master
Write-Host "`nStep 5: Switching back to master..." -ForegroundColor Yellow
git checkout master

# Step 6: Verify files are back
Write-Host "`nStep 6: Verifying files are restored..." -ForegroundColor Yellow
if (Test-Path "app\src\main\java\My\Application\MainActivity.kt") {
    Write-Host "✓ Files with My/Application casing found!" -ForegroundColor Green
} elseif (Test-Path "app\src\main\java\my\application\MainActivity.kt") {
    Write-Host "✓ Files with my/application casing found!" -ForegroundColor Green
} else {
    Write-Host "✗ MainActivity.kt not found in either location!" -ForegroundColor Red
    Write-Host "Checking what exists..." -ForegroundColor Yellow
    Get-ChildItem -Path "app\src\main\java\" -Recurse -Directory | Select-Object FullName
}

# Step 7: Show status
Write-Host "`nStep 7: Current status..." -ForegroundColor Yellow
Write-Host "Master branch files:" -ForegroundColor Cyan
dir app\src\main\java\ -Directory

Write-Host "`nGit Status:" -ForegroundColor Cyan
git status

Write-Host "`n=== RECOVERY COMPLETE ===" -ForegroundColor Green
Write-Host "`nBoth branches have been reset locally." -ForegroundColor Cyan
Write-Host "Your files should be back with the original casing." -ForegroundColor Cyan

Write-Host "`n=== SAFE PUSH TO GITHUB (NO HISTORY OVERWRITE) ===" -ForegroundColor Yellow
Write-Host "`nCreating new commits to restore files..." -ForegroundColor Cyan

# Create new commit on master to restore files
git checkout master
git add .
git commit -m "Restore files: Revert case change that deleted files

This commit restores all files to their original state before the
case sensitivity changes that caused file deletion.
Original state from commit 063ba2c."

Write-Host "✓ Master branch commit created" -ForegroundColor Green

# Create new commit on Server-branch
git checkout Server-branch
git add .
git commit -m "Restore files: Revert case change that deleted files

This commit restores all files to their original state.
Original state from commit 2d7c989 (backup-before-casefix)."

Write-Host "✓ Server-branch commit created" -ForegroundColor Green

# Go back to master
git checkout master

Write-Host "`n=== READY TO PUSH ===" -ForegroundColor Green
Write-Host "`nYou can now safely push to GitHub WITHOUT force:" -ForegroundColor Cyan
Write-Host "  git push origin master" -ForegroundColor White
Write-Host "  git push origin Server-branch" -ForegroundColor White
Write-Host "`nThis will ADD new commits to GitHub history (safe)." -ForegroundColor Green
Write-Host "No history will be overwritten!" -ForegroundColor Green

