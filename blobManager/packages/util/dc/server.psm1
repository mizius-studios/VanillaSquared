function ConvertTo-DiscordServerMap {
    param(
        $ServersConfig
    )

    $serverMap = @{}
    if ($null -eq $ServersConfig) {
        return $serverMap
    }

    if ($ServersConfig -is [hashtable]) {
        foreach ($entry in $ServersConfig.GetEnumerator()) {
            $serverMap[[string]$entry.Key] = [string]$entry.Value
        }
        return $serverMap
    }

    foreach ($property in $ServersConfig.PSObject.Properties) {
        $serverMap[[string]$property.Name] = [string]$property.Value
    }

    return $serverMap
}

function Test-DiscordServerId {
    param(
        [string]$ServerId
    )

    if ([string]::IsNullOrWhiteSpace([string]$ServerId)) {
        return $false
    }

    return $ServerId -match '^\d{17,20}$'
}

function Resolve-DiscordServerReference {
    param(
        [string]$Reference,
        [hashtable]$ServerMap = @{}
    )

    if ([string]::IsNullOrWhiteSpace([string]$Reference)) {
        return @{
            Success = $false
            Input = $Reference
            ServerId = $null
            Alias = $null
            Source = $null
            Error = "Server reference is required."
        }
    }

    $normalizedReference = [string]$Reference
    if ($ServerMap.ContainsKey($normalizedReference)) {
        $serverId = [string]$ServerMap[$normalizedReference]
        if (-not (Test-DiscordServerId -ServerId $serverId)) {
            return @{
                Success = $false
                Input = $Reference
                ServerId = $serverId
                Alias = $normalizedReference
                Source = "alias"
                Error = "Configured server alias '$normalizedReference' does not contain a valid Discord server id."
            }
        }

        return @{
            Success = $true
            Input = $Reference
            ServerId = $serverId
            Alias = $normalizedReference
            Source = "alias"
            Error = $null
        }
    }

    if (Test-DiscordServerId -ServerId $normalizedReference) {
        return @{
            Success = $true
            Input = $Reference
            ServerId = $normalizedReference
            Alias = $null
            Source = "raw"
            Error = $null
        }
    }

    return @{
        Success = $false
        Input = $Reference
        ServerId = $null
        Alias = $null
        Source = $null
        Error = "Unknown server reference '$normalizedReference'. Add it to dc.json servers or pass a raw Discord server id."
    }
}

function Get-DiscordServerAliasById {
    param(
        [string]$ServerId,
        [hashtable]$ServerMap = @{}
    )

    foreach ($entry in $ServerMap.GetEnumerator()) {
        if ([string]$entry.Value -eq [string]$ServerId) {
            return [string]$entry.Key
        }
    }

    return $null
}

function Write-DiscordServerMap {
    param(
        [hashtable]$ServerMap = @{}
    )

    if ($ServerMap.Count -eq 0) {
        Write-Host "No Discord server aliases are configured."
        return
    }

    Write-Host "Configured Discord server aliases:"
    foreach ($entry in ($ServerMap.GetEnumerator() | Sort-Object Key)) {
        Write-Host ("- {0}: {1}" -f [string]$entry.Key, [string]$entry.Value)
    }
}

Export-ModuleMember -Function ConvertTo-DiscordServerMap, Test-DiscordServerId, Resolve-DiscordServerReference, Get-DiscordServerAliasById, Write-DiscordServerMap
