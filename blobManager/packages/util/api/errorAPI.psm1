function New-MessageState {
    return @{
        Errors = [System.Collections.ArrayList]::new()
        Warnings = [System.Collections.ArrayList]::new()
    }
}

function Add-ErrorMessage {
    param(
        [hashtable]$State,
        [string]$Message
    )

    [void]$State.Errors.Add($Message)
}

function Add-WarningMessage {
    param(
        [hashtable]$State,
        [string]$Message
    )

    [void]$State.Warnings.Add($Message)
}

function Write-Warnings {
    param(
        [hashtable]$State,
        [bool]$Clear = $true,
        [string]$Header = "There are some warnings, see below for details."
    )

    if ($State.Warnings.Count -gt 0) {
        Write-Host $Header -ForegroundColor Yellow
        foreach ($warning in $State.Warnings) {
            Write-Host $warning -ForegroundColor Yellow
        }
    }

    if ($Clear) {
        $State.Warnings.Clear()
    }
}

function Throw-IfErrors {
    param(
        [hashtable]$State,
        [string]$Header = "Command failed, see below for details:",
        [int]$ExitCode = 1
    )

    if ($State.Errors.Count -gt 0) {
        Write-Host $Header -ForegroundColor Red
        foreach ($errMsg in $State.Errors) {
            Write-Host $errMsg -ForegroundColor Red
        }

        Write-Warnings -State $State -Clear $false
        exit $ExitCode
    }
}

function Exit-BadUsage {
    param([string]$Usage, [string]$ArgsLabel)

    Write-Host "Incorrect arguments: $ArgsLabel" -ForegroundColor Red
    Write-Host "Usage: $Usage" -ForegroundColor Red
    exit 1
}

Export-ModuleMember -Function New-MessageState, Add-ErrorMessage, Add-WarningMessage, Write-Warnings, Throw-IfErrors, Exit-BadUsage
