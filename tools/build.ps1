param([switch]$WithLint)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$studioJdk = Join-Path ${env:ProgramFiles} 'Android\Android Studio\jbr'
if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath $studioJdk)) { $env:JAVA_HOME = $studioJdk }
Push-Location -LiteralPath $taskRoot
try {
    $tasks = @(':app:assembleDebug', ':storybook:assembleDebug', ':domain:test', ':network:test', ':app:testDebugUnitTest')
    if ($WithLint) { $tasks += ':app:lintDebug' }
    & .\gradlew.bat @tasks -I tools/windows-test-paths.gradle --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
    New-Item -ItemType Directory -Path 'artifacts' -Force | Out-Null
    Copy-Item -LiteralPath 'app/build/outputs/apk/debug/app-debug.apk' -Destination 'artifacts/puzzle-combats-debug.apk'
    Copy-Item -LiteralPath 'storybook/build/outputs/apk/debug/storybook-debug.apk' -Destination 'artifacts/combats-storybook-debug.apk'
} finally { Pop-Location }
