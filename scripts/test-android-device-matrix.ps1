param(
  [string]$Serial
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$gradle = Join-Path $projectRoot "gradlew.bat"
$artifactDirectory = Join-Path $projectRoot "build\device-matrix"
$qaApk = Join-Path $env:USERPROFILE ".gradle\studyreader-build\StudyReader\app\outputs\apk\qa\app-qa.apk"
$packageName = "com.example.studyreader.qa"
$activityName = "com.example.studyreader.MainActivity"

if (-not (Test-Path $adb)) {
  throw "Android Debug Bridge was not found at $adb"
}

if (-not $Serial) {
  $devices = @(& $adb devices | Select-String "\sdevice$" | ForEach-Object { ($_ -split "\s+")[0] })
  if ($devices.Count -ne 1) {
    throw "Connect exactly one Android test device or pass -Serial. Connected devices: $($devices -join ', ')"
  }
  $Serial = $devices[0]
}

$matrix = @(
  @{ Name = "phone-portrait"; Size = "1080x2400"; Density = "420"; Rotation = "0" },
  @{ Name = "tablet-portrait"; Size = "1600x2560"; Density = "320"; Rotation = "0" },
  @{ Name = "tablet-landscape"; Size = "1600x2560"; Density = "320"; Rotation = "1" }
)

$originalAccelerometerRotation = (& $adb -s $Serial shell settings get system accelerometer_rotation).Trim()
$originalUserRotation = (& $adb -s $Serial shell settings get system user_rotation).Trim()

New-Item -ItemType Directory -Force -Path $artifactDirectory | Out-Null

try {
  & $adb -s $Serial shell settings put system accelerometer_rotation 0

  foreach ($configuration in $matrix) {
    Write-Host "Testing $($configuration.Name)..."
    & $adb -s $Serial shell wm size $configuration.Size
    & $adb -s $Serial shell wm density $configuration.Density
    & $adb -s $Serial shell settings put system user_rotation $configuration.Rotation
    Start-Sleep -Seconds 2

    $env:ANDROID_SERIAL = $Serial
    & $gradle connectedQaAndroidTest
    if ($LASTEXITCODE -ne 0) {
      throw "Instrumentation tests failed for $($configuration.Name)."
    }

    if (-not (Test-Path $qaApk)) {
      throw "The QA APK was not found at $qaApk"
    }
    & $adb -s $Serial install -r $qaApk | Out-Null
    & $adb -s $Serial shell am force-stop $packageName
    & $adb -s $Serial shell am start -n "$packageName/$activityName" | Out-Null
    Start-Sleep -Milliseconds 1250
    & $adb -s $Serial shell screencap -p "/sdcard/$($configuration.Name)-launch.png"
    & $adb -s $Serial pull "/sdcard/$($configuration.Name)-launch.png" (Join-Path $artifactDirectory "$($configuration.Name)-launch.png") | Out-Null
    Start-Sleep -Seconds 2
    & $adb -s $Serial shell screencap -p "/sdcard/$($configuration.Name)-main.png"
    & $adb -s $Serial pull "/sdcard/$($configuration.Name)-main.png" (Join-Path $artifactDirectory "$($configuration.Name)-main.png") | Out-Null
  }
} finally {
  & $adb -s $Serial shell wm size reset
  & $adb -s $Serial shell wm density reset
  & $adb -s $Serial shell settings put system user_rotation $originalUserRotation
  & $adb -s $Serial shell settings put system accelerometer_rotation $originalAccelerometerRotation
  Remove-Item Env:ANDROID_SERIAL -ErrorAction SilentlyContinue
}

Write-Host "Device matrix passed. Screenshots: $artifactDirectory"
