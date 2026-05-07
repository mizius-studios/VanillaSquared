function Test-DiscordGuildAccess {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ServerId,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    if ([string]::IsNullOrWhiteSpace([string]$ServerId)) {
        return @{
            Success = $false
            RequestUri = $null
            Result = $null
            Guild = $null
            Error = "Discord server id is required."
        }
    }

    $requestUri = & $JoinApiUri -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/guilds/$ServerId"
    $result = & $InvokeNetworkRequest -Uri $requestUri -Method "GET" -Headers $Headers -TimeoutSeconds $TimeoutSeconds

    if ($result.Success -and $null -ne $result.Data) {
        return @{
            Success = $true
            RequestUri = $requestUri
            Result = $result
            Guild = $result.Data
            Error = $null
        }
    }

    return @{
        Success = $false
        RequestUri = $requestUri
        Result = $result
        Guild = $null
        Error = Get-DiscordGuildAccessErrorMessage -ServerId $ServerId -Result $result
    }
}

function Get-DiscordGuildAccessErrorMessage {
    param(
        [string]$ServerId,
        [hashtable]$Result
    )

    if ($null -eq $Result) {
        return "Discord guild access check failed for server '$ServerId'."
    }

    switch ([int]$Result.StatusCode) {
        401 { return "Discord rejected the bot token while checking server '$ServerId'." }
        403 { return "The bot can reach server '$ServerId' but does not have permission to access that guild resource." }
        404 { return "The bot is not in server '$ServerId', or the server does not exist." }
        429 { return "Discord rate limited the server check for '$ServerId'. Try again shortly." }
        default {
            if (-not [string]::IsNullOrWhiteSpace([string]$Result.ErrorMessage)) {
                return $Result.ErrorMessage
            }

            return "Discord guild access check failed for server '$ServerId'."
        }
    }
}

function Get-DiscordGuildDisplayName {
    param(
        $Guild
    )

    if ($null -eq $Guild) {
        return $null
    }

    if ($Guild.PSObject.Properties.Name -contains "name" -and -not [string]::IsNullOrWhiteSpace([string]$Guild.name)) {
        return [string]$Guild.name
    }

    if ($Guild.PSObject.Properties.Name -contains "id" -and -not [string]::IsNullOrWhiteSpace([string]$Guild.id)) {
        return [string]$Guild.id
    }

    return $null
}

function Get-DiscordCurrentGuilds {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $requestUri = & $JoinApiUri -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/users/@me/guilds"
    $result = & $InvokeNetworkRequest -Uri $requestUri -Method "GET" -Headers $Headers -TimeoutSeconds $TimeoutSeconds

    if ($result.Success -and $null -ne $result.Data) {
        $guilds = @()
        if ($result.Data -is [System.Collections.IEnumerable] -and $result.Data -isnot [string]) {
            $guilds = @($result.Data)
        }
        elseif ($null -ne $result.Data) {
            $guilds = @($result.Data)
        }

        return @{
            Success = $true
            RequestUri = $requestUri
            Result = $result
            Guilds = $guilds
            Error = $null
        }
    }

    return @{
        Success = $false
        RequestUri = $requestUri
        Result = $result
        Guilds = @()
        Error = Get-DiscordCurrentGuildsErrorMessage -Result $result
    }
}

function Get-DiscordCurrentGuildsErrorMessage {
    param(
        [hashtable]$Result
    )

    if ($null -eq $Result) {
        return "Discord guild list request failed."
    }

    switch ([int]$Result.StatusCode) {
        401 { return "Discord rejected the bot token while listing servers." }
        403 { return "Discord denied access while listing servers for the bot." }
        429 { return "Discord rate limited the server list request. Try again shortly." }
        default {
            if (-not [string]::IsNullOrWhiteSpace([string]$Result.ErrorMessage)) {
                return $Result.ErrorMessage
            }

            return "Discord guild list request failed."
        }
    }
}

function Leave-DiscordGuild {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ServerId,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $requestUri = & $JoinApiUri -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/users/@me/guilds/$ServerId"
    $result = & $InvokeNetworkRequest -Uri $requestUri -Method "DELETE" -Headers $Headers -TimeoutSeconds $TimeoutSeconds

    if ($result.Success) {
        return @{
            Success = $true
            RequestUri = $requestUri
            Result = $result
            Error = $null
        }
    }

    return @{
        Success = $false
        RequestUri = $requestUri
        Result = $result
        Error = Get-DiscordLeaveGuildErrorMessage -ServerId $ServerId -Result $result
    }
}

function Get-DiscordLeaveGuildErrorMessage {
    param(
        [string]$ServerId,
        [hashtable]$Result
    )

    if ($null -eq $Result) {
        return "Discord leave-server request failed for '$ServerId'."
    }

    switch ([int]$Result.StatusCode) {
        401 { return "Discord rejected the bot token while leaving server '$ServerId'." }
        403 { return "Discord denied the request for the bot to leave server '$ServerId'." }
        404 { return "The bot is not in server '$ServerId', or the server does not exist." }
        429 { return "Discord rate limited the leave-server request for '$ServerId'. Try again shortly." }
        default {
            if (-not [string]::IsNullOrWhiteSpace([string]$Result.ErrorMessage)) {
                return $Result.ErrorMessage
            }

            return "Discord leave-server request failed for '$ServerId'."
        }
    }
}

Export-ModuleMember -Function Test-DiscordGuildAccess, Get-DiscordGuildAccessErrorMessage, Get-DiscordGuildDisplayName, Get-DiscordCurrentGuilds, Get-DiscordCurrentGuildsErrorMessage, Leave-DiscordGuild, Get-DiscordLeaveGuildErrorMessage
