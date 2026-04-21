param(
    [ValidateSet("special", "predicate", "both")]
    [string]$Suite = "both",
    [int]$Cycles = 1,
    [int]$StartupDelaySeconds = 3,
    [switch]$NoHotkey,
    [switch]$DryRun
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$runnerPath = Join-Path $scriptDir "special_enchantment_case_runner.ps1"
$specialConfigPath = Join-Path $scriptDir "special_enchantment_cases.json"
$predicateConfigPath = Join-Path $scriptDir "predicate_enchantment_cases.json"

if (-not (Test-Path -LiteralPath $runnerPath)) {
    throw "Base runner not found: $runnerPath"
}

if ($NoHotkey) {
    if ($Suite -eq "special" -or $Suite -eq "both") {
        & $runnerPath -ConfigPath $specialConfigPath -Cycles $Cycles -StartupDelaySeconds $StartupDelaySeconds -NoHotkey -DryRun:$DryRun.IsPresent
    }
    if ($Suite -eq "predicate" -or $Suite -eq "both") {
        & $runnerPath -ConfigPath $predicateConfigPath -Cycles $Cycles -StartupDelaySeconds $StartupDelaySeconds -NoHotkey -DryRun:$DryRun.IsPresent
    }
    exit 0
}

if ($Suite -eq "special") {
    & $runnerPath -ConfigPath $specialConfigPath -GlobalHotkeyKey "9" -Cycles $Cycles -StartupDelaySeconds $StartupDelaySeconds -DryRun:$DryRun.IsPresent
    exit 0
}

if ($Suite -eq "predicate") {
    & $runnerPath -ConfigPath $predicateConfigPath -GlobalHotkeyKey "8" -Cycles $Cycles -StartupDelaySeconds $StartupDelaySeconds -DryRun:$DryRun.IsPresent
    exit 0
}

Write-Host "[INFO] Starting combined hotkey mode."
Write-Host "[INFO] Special suite hotkey: 9"
Write-Host "[INFO] Predicate suite hotkey: 8"
Write-Host "[INFO] Open a second terminal and run:"
Write-Host "       powershell -ExecutionPolicy Bypass -File .\\tools\\enchantment_case_runner.ps1 -Suite predicate"

& $runnerPath -ConfigPath $specialConfigPath -GlobalHotkeyKey "9" -Cycles $Cycles -StartupDelaySeconds $StartupDelaySeconds -DryRun:$DryRun.IsPresent
