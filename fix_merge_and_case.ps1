# Complete solution to merge conflict and case sensitivity issues
# Run this in PowerShell from project root

Write-Host "=== Resolving Merge Conflicts and Fixing Case Sensitivity ===" -ForegroundColor Cyan

# First, abort the current merge to start fresh
Write-Host "`nAborting current merge to start clean..." -ForegroundColor Yellow
git merge --abort 2>$null

# Remove ALL case variations from Git's index completely
Write-Host "`nRemoving all path variations from Git index..." -ForegroundColor Yellow
git rm -rf --cached app/src/main/java/My/ 2>$null
git rm -rf --cached app/src/androidTest/java/My/ 2>$null
git rm -rf --cached app/src/test/java/My/ 2>$null

# Delete physical directories with wrong casing
Write-Host "`nDeleting physical directories with incorrect casing..." -ForegroundColor Yellow
if (Test-Path "app\src\main\java\My") {
    Remove-Item -Recurse -Force "app\src\main\java\My"
    Write-Host "Deleted My/ directory" -ForegroundColor Green
}
if (Test-Path "app\src\androidTest\java\My") {
    Remove-Item -Recurse -Force "app\src\androidTest\java\My"
    Write-Host "Deleted androidTest My/ directory" -ForegroundColor Green
}
if (Test-Path "app\src\test\java\My") {
    Remove-Item -Recurse -Force "app\src\test\java\My"
    Write-Host "Deleted test My/ directory" -ForegroundColor Green
}

# Add only the correct lowercase directory
Write-Host "`nAdding correct lowercase paths..." -ForegroundColor Yellow
git add app/src/main/java/my/
git add app/src/androidTest/java/my/ 2>$null
git add app/src/test/java/my/ 2>$null
git add app/build.gradle.kts

# Commit the changes
Write-Host "`nCommitting changes..." -ForegroundColor Yellow
git commit -m "Fix: Normalize all paths to lowercase my/application

- Removed My/Application (capital M, capital A)
- Removed My/application (capital M, lowercase a)
- Using only my/application (all lowercase)
- Updated build.gradle.kts namespace and applicationId"

Write-Host "`n=== Complete! ===" -ForegroundColor Green
Write-Host "`nNow you can:" -ForegroundColor Yellow
Write-Host "1. Pull from remote: git pull origin master"
Write-Host "2. If there are conflicts, they should be minimal"
Write-Host "3. Push changes: git push origin master"

