param(
    [string]$SdkRoot = "$env:LOCALAPPDATA\Android\Sdk"
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$toolsRoot = Join-Path $projectRoot '.tools'
# Some emulator file readers cannot open Cyrillic Windows paths. The short alias
# refers to the same workspace directory and does not move or duplicate user data.
$fileSystem = New-Object -ComObject Scripting.FileSystemObject
$toolsRoot = $fileSystem.GetFolder($toolsRoot).ShortPath
$avdHome = Join-Path $toolsRoot 'avd'
$avdPath = Join-Path $avdHome 'Contest_API34.avd'
$systemImagePath = Join-Path $toolsRoot 'android-sdk\system-images\android-34\default\x86_64'
$emulatorPath = Join-Path $SdkRoot 'emulator\emulator.exe'
if (-not (Test-Path -LiteralPath $emulatorPath)) { throw "Android Emulator not found at $emulatorPath" }
if (-not (Test-Path -LiteralPath (Join-Path $systemImagePath 'system.img'))) {
    throw 'Download/extract the verified API 34 image first. See docs/emulator.md.'
}
New-Item -ItemType Directory -Force -Path $avdPath,(Join-Path $toolsRoot 'android-user') | Out-Null
$settings = @(
    'AvdId=Contest_API34','avd.ini.displayname=Contest API 34','avd.ini.encoding=UTF-8',
    'abi.type=x86_64','tag.id=default','tag.display=Default Android System Image',
    'hw.cpu.arch=x86_64','hw.cpu.ncore=2','hw.ramSize=2048',
    'hw.lcd.width=750','hw.lcd.height=1624','hw.lcd.density=320',
    'hw.gpu.enabled=yes','hw.gpu.mode=swiftshader','hw.keyboard=yes',
    'hw.audioInput=no','hw.audioOutput=no','hw.camera.back=none','hw.camera.front=none',
    'disk.dataPartition.size=2G','vm.heapSize=256','showDeviceFrame=no',
    'fastboot.forceColdBoot=yes','fastboot.forceFastBoot=no','PlayStore.enabled=false',
    ('image.sysdir.1='+$systemImagePath)
)
$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllLines((Join-Path $avdPath 'config.ini'), $settings, $utf8)
[System.IO.File]::WriteAllLines((Join-Path $avdHome 'Contest_API34.ini'),
    @('avd.ini.encoding=UTF-8', ('path='+$avdPath), 'target=android-34'), $utf8)
$env:ANDROID_USER_HOME = Join-Path $toolsRoot 'android-user'
$env:ANDROID_EMULATOR_HOME = $env:ANDROID_USER_HOME
$env:ANDROID_AVD_HOME = $avdHome
$env:ANDROID_HOME = $SdkRoot
$env:ANDROID_SDK_ROOT = $SdkRoot
& $emulatorPath -accel-check
if ($LASTEXITCODE -ne 0) { throw 'Android hardware acceleration is unavailable.' }
$emulatorProcess = Start-Process -FilePath $emulatorPath -ArgumentList @(
    '-avd','Contest_API34','-no-window','-no-audio','-no-boot-anim',
    '-gpu','swiftshader','-accel','on','-no-snapshot-save','-no-snapshot-load',
    '-memory','2048','-cores','2','-port','5556','-feature','-Vulkan','-show-kernel'
) -WindowStyle Hidden -RedirectStandardOutput (Join-Path $toolsRoot 'emulator-stdout.log') -RedirectStandardError (Join-Path $toolsRoot 'emulator-stderr.log') -PassThru
Write-Output "Started emulator process $($emulatorProcess.Id); target device emulator-5556."
Write-Output 'Confirm sys.boot_completed=1 through adb before installing the app.'
