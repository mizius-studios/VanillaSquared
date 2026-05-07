$fileApiModule = Import-Module (Join-Path $PSScriptRoot "..\api\fileAPI.psm1") -Force -DisableNameChecking -PassThru
$commonModule = Import-Module (Join-Path $PSScriptRoot "common.psm1") -Force -DisableNameChecking -PassThru

$script:ReadJsonFileCommand = $fileApiModule.ExportedCommands["Read-JsonFile"]
$script:WriteJsonFileCommand = $fileApiModule.ExportedCommands["Write-JsonFile"]
$script:EnsureParentDirectoryForFileCommand = $fileApiModule.ExportedCommands["Ensure-ParentDirectoryForFile"]
$script:RemoveFileIfExistsCommand = $fileApiModule.ExportedCommands["Remove-FileIfExists"]
$script:GetUtcTimestampCommand = $commonModule.ExportedCommands["Get-UtcTimestamp"]

function Get-DiscordRuntimeState {
    param(
        [string]$StatePath
    )

    if ($null -eq $script:ReadJsonFileCommand) {
        throw "Required command handle missing: Read-JsonFile"
    }

    return & $script:ReadJsonFileCommand -Path $StatePath -Fallback @{}
}

function Set-DiscordRuntimeState {
    param(
        [string]$StatePath,
        [hashtable]$State
    )

    if ($null -eq $script:WriteJsonFileCommand) {
        throw "Required command handle missing: Write-JsonFile"
    }

    & $script:WriteJsonFileCommand -Path $StatePath -Data $State -Depth 20
}

function Update-DiscordRuntimeState {
    param(
        [string]$StatePath,
        [hashtable]$Updates
    )

    $currentState = Get-DiscordRuntimeState -StatePath $StatePath
    if ($null -eq $currentState) {
        $currentState = @{}
    }

    foreach ($entry in $Updates.GetEnumerator()) {
        $currentState[$entry.Key] = $entry.Value
    }

    if ($null -eq $script:GetUtcTimestampCommand) {
        throw "Required command handle missing: Get-UtcTimestamp"
    }

    $currentState["UpdatedAt"] = & $script:GetUtcTimestampCommand
    Set-DiscordRuntimeState -StatePath $StatePath -State $currentState
    return $currentState
}

function Initialize-DiscordRuntimeFiles {
    param(
        [string]$StatePath,
        [string]$LogPath,
        [string]$StopSignalPath
    )

    if ($null -eq $script:EnsureParentDirectoryForFileCommand) {
        throw "Required command handle missing: Ensure-ParentDirectoryForFile"
    }

    foreach ($path in @($StatePath, $LogPath, $StopSignalPath)) {
        if (-not [string]::IsNullOrWhiteSpace([string]$path)) {
            [void](& $script:EnsureParentDirectoryForFileCommand -Path $path)
        }
    }
}

function Write-DiscordRuntimeLog {
    param(
        [string]$LogPath,
        [string]$Message
    )

    if ([string]::IsNullOrWhiteSpace([string]$LogPath) -or [string]::IsNullOrWhiteSpace([string]$Message)) {
        return
    }

    if ($null -eq $script:EnsureParentDirectoryForFileCommand -or $null -eq $script:GetUtcTimestampCommand) {
        throw "Required command handle missing for runtime logging."
    }

    [void](& $script:EnsureParentDirectoryForFileCommand -Path $LogPath)
    $timestamp = & $script:GetUtcTimestampCommand
    Add-Content -LiteralPath $LogPath -Value "[$timestamp] $Message" -Encoding UTF8
}

function Test-DiscordManagedProcess {
    param(
        $Pid
    )

    $parsedPid = 0
    if (-not [int]::TryParse([string]$Pid, [ref]$parsedPid) -or $parsedPid -le 0) {
        return $false
    }

    try {
        $process = Get-Process -Id $parsedPid -ErrorAction Stop
        return $null -ne $process -and -not $process.HasExited
    }
    catch {
        return $false
    }
}

function Get-DiscordManagedStatus {
    param(
        [string]$StatePath
    )

    $state = Get-DiscordRuntimeState -StatePath $StatePath
    $processExists = $false
    if ($null -ne $state -and $state.ContainsKey("Pid")) {
        $processExists = Test-DiscordManagedProcess -Pid $state["Pid"]
    }

    return @{
        State = $state
        ProcessExists = $processExists
    }
}

function Clear-DiscordStopSignal {
    param(
        [string]$StopSignalPath
    )

    if ($null -eq $script:RemoveFileIfExistsCommand) {
        throw "Required command handle missing: Remove-FileIfExists"
    }

    & $script:RemoveFileIfExistsCommand -Path $StopSignalPath
}

function Request-DiscordStopSignal {
    param(
        [string]$StopSignalPath
    )

    if ($null -eq $script:EnsureParentDirectoryForFileCommand -or $null -eq $script:GetUtcTimestampCommand) {
        throw "Required command handle missing for stop signal creation."
    }

    [void](& $script:EnsureParentDirectoryForFileCommand -Path $StopSignalPath)
    [System.IO.File]::WriteAllText($StopSignalPath, (& $script:GetUtcTimestampCommand), [System.Text.Encoding]::UTF8)
}

Export-ModuleMember -Function Get-DiscordRuntimeState, Set-DiscordRuntimeState, Update-DiscordRuntimeState, Initialize-DiscordRuntimeFiles, Write-DiscordRuntimeLog, Test-DiscordManagedProcess, Get-DiscordManagedStatus, Clear-DiscordStopSignal, Request-DiscordStopSignal
