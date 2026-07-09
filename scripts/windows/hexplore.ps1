[CmdletBinding()]
param(
  [ValidateSet('setup', 'build', 'install', 'devices')]
  [string]$Command = 'setup',
  [string]$DeviceSerial
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptRoot '..\..')).Path
$GradleWrapper = Join-Path $RepoRoot 'gradlew.bat'
$LocalPropertiesPath = Join-Path $RepoRoot 'local.properties'
$EnvExamplePath = Join-Path $RepoRoot '.env.example'
$EnvPath = Join-Path $RepoRoot '.env'

function Write-Section {
  param([string]$Title)
  Write-Host ""
  Write-Host "== $Title =="
}

function Get-LocalSdkDir {
  if (-not (Test-Path $LocalPropertiesPath)) {
    return $null
  }

  $match = Select-String -Path $LocalPropertiesPath -Pattern '^\s*sdk\.dir\s*=\s*(.+)\s*$' | Select-Object -First 1
  if ($null -eq $match) {
    return $null
  }

  return $match.Matches[0].Groups[1].Value.Trim()
}

function Resolve-SdkDir {
  $defaultSdkDir = $null
  if (-not [string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
    $defaultSdkDir = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
  }

  $candidates = @(
    $env:ANDROID_SDK_ROOT,
    $env:ANDROID_HOME,
    $defaultSdkDir,
    (Get-LocalSdkDir)
  )

  foreach ($candidate in $candidates) {
    if ([string]::IsNullOrWhiteSpace($candidate)) {
      continue
    }

    if (Test-Path $candidate) {
      return (Resolve-Path $candidate).Path
    }
  }

  throw 'Android SDK not found. Set ANDROID_SDK_ROOT to your Windows SDK path and install Android SDK Platform-Tools and platform 36 in Android Studio.'
}

function Write-LocalProperties {
  param([string]$SdkDir)

  $normalizedSdkDir = ([System.IO.Path]::GetFullPath($SdkDir)).Replace('\', '/')
  Set-Content -Path $LocalPropertiesPath -Value "sdk.dir=$normalizedSdkDir" -Encoding ASCII
}

function Ensure-EnvFile {
  if ((Test-Path $EnvPath) -or -not (Test-Path $EnvExamplePath)) {
    return
  }

  Copy-Item -Path $EnvExamplePath -Destination $EnvPath
}

function Invoke-Gradle {
  param([string[]]$Args)

  & $GradleWrapper @Args
  if ($LASTEXITCODE -ne 0) {
    throw "Gradle command failed: $($Args -join ' ')"
  }
}

function Invoke-Adb {
  param(
    [string]$SdkDir,
    [string[]]$Args
  )

  $AdbPath = Join-Path $SdkDir 'platform-tools\adb.exe'
  if (-not (Test-Path $AdbPath)) {
    throw "ADB not found at $AdbPath. Install Android SDK Platform-Tools."
  }

  & $AdbPath @Args
  if ($LASTEXITCODE -ne 0) {
    throw "ADB command failed: $($Args -join ' ')"
  }
}

function Invoke-Setup {
  Write-Section 'Setup'
  Ensure-EnvFile

  $sdkDir = Resolve-SdkDir
  Write-LocalProperties -SdkDir $sdkDir

  Write-Host "SDK: $sdkDir"
  Write-Host 'Java version:'
  & java -version
  if ($LASTEXITCODE -ne 0) {
    throw 'Java is not available. Install JDK 17 and try again.'
  }

  Write-Host 'ADB version:'
  Invoke-Adb -SdkDir $sdkDir -Args @('version')

  Write-Host ''
  Write-Host 'Setup complete. Next commands:'
  Write-Host '  build   -> .\scripts\windows\build.bat'
  Write-Host '  install -> .\scripts\windows\install.bat'
  Write-Host '  devices -> .\scripts\windows\devices.bat'
}

function Invoke-Build {
  Write-Section 'Build debug APK'
  Invoke-Gradle -Args @('assembleDebug')
}

function Invoke-Install {
  Write-Section 'Install over USB'

  if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $env:ANDROID_SERIAL = $DeviceSerial
    Write-Host "Using device serial: $DeviceSerial"
  }

  Invoke-Gradle -Args @('installDebug')
}

function Invoke-Devices {
  Write-Section 'Connected devices'
  $sdkDir = Resolve-SdkDir
  Invoke-Adb -SdkDir $sdkDir -Args @('devices')
}

switch ($Command) {
  'setup' { Invoke-Setup }
  'build' { Invoke-Build }
  'install' { Invoke-Install }
  'devices' { Invoke-Devices }
}
