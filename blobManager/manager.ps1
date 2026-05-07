$Command = if ($args.Count -gt 0) { $args[0] } else { $null }
$Rest = @()
if ($args.Count -gt 1) {
    $Rest += $args[1..($args.Count - 1)]
}
$Version = "1.0.0"
$PackageName = "blobManager"

$errorApiModule = Import-Module (Join-Path $PSScriptRoot "packages\util\api\errorAPI.psm1") -Force -DisableNameChecking -PassThru
$configApiModule = Import-Module (Join-Path $PSScriptRoot "packages\util\api\configAPI.psm1") -Force -DisableNameChecking -PassThru
$versionModule = Import-Module (Join-Path $PSScriptRoot "packages\util\api\commands\version.psm1") -Force -DisableNameChecking -PassThru
$helpModule = Import-Module (Join-Path $PSScriptRoot "packages\util\api\commands\help.psm1") -Force -DisableNameChecking -PassThru

$Cmd = @{
    NewMessageState    = $errorApiModule.ExportedCommands["New-MessageState"]
    WriteWarnings      = $errorApiModule.ExportedCommands["Write-Warnings"]
    GetJsonConfig      = $configApiModule.ExportedCommands["Get-JsonConfig"]
    TestConfigVersion  = $configApiModule.ExportedCommands["Test-ConfigVersion"]
    CommandVersion     = $versionModule.ExportedCommands["Command-Version"]
    CommandHelp        = $helpModule.ExportedCommands["Command-Help"]
}

foreach ($entry in $Cmd.GetEnumerator()) {
    if ($null -eq $entry.Value) {
        throw "Required command handle missing: $($entry.Key)"
    }
}

$configPath = Join-Path $PSScriptRoot "packages\config\global.json"
$config = & $Cmd.GetJsonConfig -Path $configPath -Fallback @{}
$state = & $Cmd.NewMessageState
$versionStatus = & $Cmd.TestConfigVersion -PackageName $PackageName -ScriptVersion $Version -Config $config -State $state

switch ($Command) {
    "mc" {
        $isMcVersionCommand = $Rest.Count -gt 0 -and $Rest[0] -eq "-v"
        if (-not $isMcVersionCommand) {
            & $Cmd.WriteWarnings -State $state
        }

        $scriptPath = Join-Path $PSScriptRoot "packages/scripts/mc.ps1"
        & $scriptPath @Rest

        if ($LASTEXITCODE -ne $null) {
            exit $LASTEXITCODE
        }

        exit 0
    }
    "modrinth" {
        $isModrinthVersionCommand = $Rest.Count -gt 0 -and $Rest[0] -eq "-v"
        if (-not $isModrinthVersionCommand) {
            & $Cmd.WriteWarnings -State $state
        }

        $scriptPath = Join-Path $PSScriptRoot "packages/scripts/modrinth.ps1"
        & $scriptPath @Rest

        if ($LASTEXITCODE -ne $null) {
            exit $LASTEXITCODE
        }

        exit 0
    }
    "anytype" {
        $isAnytypeVersionCommand = $Rest.Count -gt 0 -and $Rest[0] -eq "-v"
        if (-not $isAnytypeVersionCommand) {
            & $Cmd.WriteWarnings -State $state
        }

        $scriptPath = Join-Path $PSScriptRoot "packages/scripts/anytype.ps1"
        & $scriptPath @Rest

        if ($LASTEXITCODE -ne $null) {
            exit $LASTEXITCODE
        }

        exit 0
    }
    "dc" {
        $isDcVersionCommand = $Rest.Count -gt 0 -and $Rest[0] -eq "-v"
        if (-not $isDcVersionCommand) {
            & $Cmd.WriteWarnings -State $state
        }

        $scriptPath = Join-Path $PSScriptRoot "packages/scripts/dc.ps1"
        & $scriptPath @Rest

        if ($LASTEXITCODE -ne $null) {
            exit $LASTEXITCODE
        }

        exit 0
    }
    "-v" {
        & $Cmd.CommandVersion -PackageName $PackageName -Version $Version -ConfigVersion $versionStatus.ConfigVersion -WarningMessage $versionStatus.WarningMessage
        exit 0
    }

    "-?" {
        & $Cmd.WriteWarnings -State $state
        & $Cmd.CommandHelp -PackageName $PackageName -Commands @("mc -?", "modrinth -?", "anytype -?", "dc -?", "-v", "-?")
        exit 0
    }

    default {
        & $Cmd.WriteWarnings -State $state
        Write-Host "Unknown command: $Command"
        Write-Host "Run: .\blob.ps1 -?"
        exit 1
    }
}
