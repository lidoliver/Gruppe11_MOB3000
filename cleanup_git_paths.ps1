# Complete Git cleanup script to remove all case variations
# Run this in PowerShell from the project root

Write-Host "=== Git Repository Cleanup: Removing case variations ===" -ForegroundColor Cyan

# Step 1: Remove ALL variations from Git index (but keep working directory files)
Write-Host "`nStep 1: Removing all variations from Git index..." -ForegroundColor Yellow

# Remove My/Application (capital M, capital A)
git rm -r --cached "app/src/main/java/My/Application" 2>$null
git rm -r --cached "app/src/androidTest/java/My/Application" 2>$null
git rm -r --cached "app/src/test/java/My/Application" 2>$null

# Remove My/application (capital M, lowercase a)
git rm -r --cached "app/src/main/java/My/application" 2>$null
git rm -r --cached "app/src/androidTest/java/My/application" 2>$null
git rm -r --cached "app/src/test/java/My/application" 2>$null

Write-Host "Removed old paths from Git index" -ForegroundColor Green

# Step 2: Delete the physical directories with wrong casing
Write-Host "`nStep 2: Cleaning up physical directories..." -ForegroundColor Yellow

$oldPaths = @(
    "app\src\main\java\My\Application",
    "app\src\main\java\My\application",
    "app\src\androidTest\java\My\Application",
    "app\src\androidTest\java\My\application",
    "app\src\test\java\My\Application",
    "app\src\test\java\My\application"
)

foreach ($path in $oldPaths) {
    if (Test-Path $path) {
        Remove-Item -Recurse -Force $path
        Write-Host "Deleted: $path" -ForegroundColor Green
    }
}

# Step 3: Add only the correct lowercase paths
Write-Host "`nStep 3: Adding correct lowercase paths to Git..." -ForegroundColor Yellow

git add app/src/main/java/my/application/
git add app/src/androidTest/java/my/application/ 2>$null
git add app/src/test/java/my/application/ 2>$null

Write-Host "Added correct paths" -ForegroundColor Green

# Step 4: Show status
Write-Host "`nStep 4: Current Git status..." -ForegroundColor Yellow
git status

Write-Host "`n=== Cleanup Complete ===" -ForegroundColor Cyan
Write-Host "`nNext steps:" -ForegroundColor Yellow
Write-Host "1. Review the changes with: git status"
Write-Host "2. Commit the changes: git commit -m 'Fix: Remove all case variations, use only my/application'"
Write-Host "3. Push to GitHub: git push origin master"
Write-Host "`nThis will ensure only 'my/application' (all lowercase) exists on GitHub." -ForegroundColor Green

