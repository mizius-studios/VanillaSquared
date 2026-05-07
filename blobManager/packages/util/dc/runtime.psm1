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

function Stop-DiscordManagedProcess {
    param(
        [string]$StatePath,
        [string]$StopSignalPath,
        [int]$StopTimeoutSeconds = 15
    )

    if ($null -eq $script:GetUtcTimestampCommand) {
        throw "Required command handle missing: Get-UtcTimestamp"
    }

    $status = Get-DiscordManagedStatus -StatePath $StatePath
    $result = @{
        WasRunning = [bool]$status.ProcessExists
        StoppedGracefully = $false
        WasForceStopped = $false
        ProcessId = $null
    }

    if (-not $status.ProcessExists) {
        Update-DiscordRuntimeState -StatePath $StatePath -Updates @{
            State = "stopped"
            LastError = $null
            StoppedAt = & $script:GetUtcTimestampCommand
        } | Out-Null
        return $result
    }

    $managedProcessId = 0
    if (-not [int]::TryParse([string]$status.State["Pid"], [ref]$managedProcessId) -or $managedProcessId -le 0) {
        Update-DiscordRuntimeState -StatePath $StatePath -Updates @{
            State = "failed"
            LastError = "Managed process is running, but no valid PID was recorded."
        } | Out-Null

        $result["Error"] = "Managed process is running, but no valid PID was recorded."
        return $result
    }

    $result["ProcessId"] = $managedProcessId
    Request-DiscordStopSignal -StopSignalPath $StopSignalPath
    $deadline = (Get-Date).ToUniversalTime().AddSeconds($StopTimeoutSeconds)

    do {
        Start-Sleep -Milliseconds 500
        if (-not (Test-DiscordManagedProcess -Pid $managedProcessId)) {
            $result["StoppedGracefully"] = $true
            break
        }
    }
    while ((Get-Date).ToUniversalTime() -lt $deadline)

    if (-not $result["StoppedGracefully"]) {
        Stop-Process -Id $managedProcessId -Force -ErrorAction SilentlyContinue
        Clear-DiscordStopSignal -StopSignalPath $StopSignalPath
        Update-DiscordRuntimeState -StatePath $StatePath -Updates @{
            State = "stopped"
            LastError = "Process was force-stopped after timeout."
            StoppedAt = & $script:GetUtcTimestampCommand
        } | Out-Null
        $result["WasForceStopped"] = $true
        return $result
    }

    Update-DiscordRuntimeState -StatePath $StatePath -Updates @{
        State = "stopped"
        LastError = $null
        StoppedAt = & $script:GetUtcTimestampCommand
    } | Out-Null
    return $result
}

Export-ModuleMember -Function Get-DiscordRuntimeState, Set-DiscordRuntimeState, Update-DiscordRuntimeState, Initialize-DiscordRuntimeFiles, Write-DiscordRuntimeLog, Test-DiscordManagedProcess, Get-DiscordManagedStatus, Clear-DiscordStopSignal, Request-DiscordStopSignal, Stop-DiscordManagedProcess
