param(
    [int]$Cycles = 1,
    [int]$StartupDelaySeconds = 3,
    [switch]$NoHotkey,
    [switch]$DryRun
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$runnerPath = Join-Path $scriptDir "special_enchantment_case_runner.ps1"
$configPath = Join-Path $scriptDir "predicate_enchantment_cases.json"

if (-not (Test-Path -LiteralPath $runnerPath)) {
    throw "Base runner not found: $runnerPath"
}

& $runnerPath -ConfigPath $configPath -GlobalHotkeyKey "8" -Cycles $Cycles -StartupDelaySeconds $StartupDelaySeconds -NoHotkey:$NoHotkey.IsPresent -DryRun:$DryRun.IsPresent
