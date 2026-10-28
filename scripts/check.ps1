# Runs build, lint and unit tests. Stops at the first failing step.
# No CI exists yet; if it is added, keep these steps identical to the CI steps.
$ErrorActionPreference = 'Stop'

function Invoke-Step([string]$Name, [scriptblock]$Command) {
    Write-Host "== $Name" -ForegroundColor Cyan
    & $Command
    # PowerShell 5.1 does not stop on a failing native command, so check the exit code.
    if ($LASTEXITCODE -ne 0) { Write-Host "FAILED: $Name" -ForegroundColor Red; exit $LASTEXITCODE }
}

Push-Location (Join-Path $PSScriptRoot '..')
try {
    Invoke-Step 'Build debug APK' { .\gradlew.bat assembleDebug }
    # "check" runs lint and the unit tests.
    Invoke-Step 'Lint and unit tests' { .\gradlew.bat check }
} finally { Pop-Location }

Write-Host 'All checks passed' -ForegroundColor Green
