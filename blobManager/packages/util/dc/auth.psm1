$commonModule = Import-Module (Join-Path $PSScriptRoot "common.psm1") -Force -DisableNameChecking -PassThru
$script:ConvertToDiscordDisplayNameCommand = $commonModule.ExportedCommands["ConvertTo-DiscordDisplayName"]

function Get-DiscordAuthorizationHeaders {
    param(
        [hashtable]$Headers = @{},
        [string]$KeyFilePath,
        [string]$KeyName,
        [string]$EnvVarName,
        $ResolveKeyValue,
        $MergeAuthorizationHeader
    )

    $tokenResolution = & $ResolveKeyValue -KeyFilePath $KeyFilePath -KeyName $KeyName -EnvVarName $EnvVarName
    if (-not $tokenResolution.Success) {
        return @{
            Success = $false
            Error = "Missing Discord bot token. No local key or env var was found."
            Source = $null
            Headers = $null
            Token = $null
        }
    }

    return @{
        Success = $true
        Error = $null
        Source = $tokenResolution.Source
        Headers = & $MergeAuthorizationHeader -Headers $Headers -Token "Bot $($tokenResolution.Value)"
        Token = $tokenResolution.Value
    }
}

function Get-DiscordBotDisplayName {
    param(
        $Data
    )

    if ($null -eq $script:ConvertToDiscordDisplayNameCommand) {
        throw "Required command handle missing: ConvertTo-DiscordDisplayName"
    }

    return & $script:ConvertToDiscordDisplayNameCommand -User $Data
}

function Get-DiscordAuthErrorMessage {
    param(
        [hashtable]$Result
    )

    if ($null -eq $Result) {
        return "Discord authentication failed."
    }

    switch ([int]$Result.StatusCode) {
        401 { return "Discord rejected the bot token. Check the token source and verify it still matches the application bot token." }
        403 { return "Discord accepted the request path but denied access for this token." }
        429 { return "Discord rate limited the authentication request. Try again shortly." }
        default {
            if (-not [string]::IsNullOrWhiteSpace([string]$Result.ErrorMessage)) {
                return [string]$Result.ErrorMessage
            }

            return "Discord authentication failed."
        }
    }
}

Export-ModuleMember -Function Get-DiscordAuthorizationHeaders, Get-DiscordBotDisplayName, Get-DiscordAuthErrorMessage
