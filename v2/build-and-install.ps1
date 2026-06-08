# Haji Racing Build and Install Script
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Build and Install Haji Racing App" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Setup Java Environment
$javaPaths = @(
    "C:\Users\flycat666\.jdks\temurin-24.0.2\bin",
    "C:\Program Files\Android\Android Studio\jbr\bin",
    "C:\Program Files\Java\jdk*\bin"
)

$javaFound = $false
foreach ($path in $javaPaths) {
    if (Test-Path $path) {
        $env:JAVA_HOME = $path -replace '\\bin$', ''
        $env:PATH = "$path;$env:PATH"
        Write-Host "[OK] Java found: $path" -ForegroundColor Green
        $javaFound = $true
        break
    }
}

if (-not $javaFound) {
    Write-Host "[ERROR] Java not found in any known location" -ForegroundColor Red
    Write-Host "Please install JDK or set JAVA_HOME manually" -ForegroundColor Yellow
    pause
    exit 1
}

# Show Java Version
Write-Host ""
Write-Host "Java Version:" -ForegroundColor Yellow
java -version
Write-Host ""

# Build Project
Write-Host "Starting build..." -ForegroundColor Yellow
.\gradlew.bat assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[ERROR] Build failed! Please check error messages above." -ForegroundColor Red
    pause
    exit 1
}

Write-Host ""
Write-Host "[OK] Build successful!" -ForegroundColor Green

# Check Device Connection
$adbPath = "C:\Users\flycat666\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$deviceList = & $adbPath devices | Select-String "device"

if ($deviceList -eq $null) {
    Write-Host ""
    Write-Host "[ERROR] No device detected! Please connect your phone." -ForegroundColor Red
    pause
    exit 1
}

Write-Host ""
Write-Host "[OK] Device connected" -ForegroundColor Green

# Install to Device
Write-Host ""
Write-Host "Installing to device..." -ForegroundColor Yellow
& $adbPath -s 988cbf24 install -r app\build\outputs\apk\debug\app-debug.apk

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[ERROR] Installation failed!" -ForegroundColor Red
    pause
    exit 1
}

Write-Host ""
Write-Host "[OK] Installation successful!" -ForegroundColor Green

# Launch App
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Launching app..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
& $adbPath -s 988cbf24 shell am start -n com.haji.racing/com.haji.racing.MainActivity

Write-Host ""
Write-Host "[OK] App launched!" -ForegroundColor Green
Write-Host ""
Write-Host "Press any key to exit..." -ForegroundColor Gray
pause
