function Add-RequiredConfigError {
    param(
        [string]$Value,
        [string]$ConfigKey,
        [string]$PackageName,
        [string]$ConfigPath,
        $AddErrorMessage,
        [hashtable]$State
    )

    if ([string]::IsNullOrWhiteSpace([string]$Value)) {
        & $AddErrorMessage -State $State -Message "$PackageName $ConfigKey is missing in $ConfigPath"
    }
}

function Get-UtcTimestamp {
    return (Get-Date).ToUniversalTime().ToString("o")
}

function ConvertTo-DiscordDisplayName {
    param(
        $User
    )

    if ($null -eq $User) {
        return $null
    }

    $username = if ($User.PSObject.Properties.Name -contains "username") { [string]$User.username } else { $null }
    $globalName = if ($User.PSObject.Properties.Name -contains "global_name") { [string]$User.global_name } else { $null }
    $discriminator = if ($User.PSObject.Properties.Name -contains "discriminator") { [string]$User.discriminator } else { $null }

    if (-not [string]::IsNullOrWhiteSpace($globalName)) {
        return $globalName
    }

    if (-not [string]::IsNullOrWhiteSpace($username)) {
        if (-not [string]::IsNullOrWhiteSpace($discriminator) -and $discriminator -ne "0") {
            return "$username#$discriminator"
        }

        return $username
    }

    if ($User.PSObject.Properties.Name -contains "id" -and -not [string]::IsNullOrWhiteSpace([string]$User.id)) {
        return [string]$User.id
    }

    return $null
}

function Test-DiscordIntentsValue {
    param(
        $Value
    )

    $parsedValue = 0L
    return [long]::TryParse([string]$Value, [ref]$parsedValue)
}

Export-ModuleMember -Function Add-RequiredConfigError, Get-UtcTimestamp, ConvertTo-DiscordDisplayName, Test-DiscordIntentsValue
