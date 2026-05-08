function Invoke-DiscordGuildRequest {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$Endpoint,
        [ValidateSet("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD")]
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        $Body = $null,
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $requestUri = & $JoinApiUri -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint $Endpoint
    $requestParams = @{
        Uri = $requestUri
        Method = $Method
        Headers = $Headers
        TimeoutSeconds = $TimeoutSeconds
    }

    if ($null -ne $Body -and $Method -ne "GET" -and $Method -ne "HEAD") {
        $requestParams["Body"] = $Body
    }

    $result = & $InvokeNetworkRequest @requestParams
    return @{
        RequestUri = $requestUri
        Result = $result
        Success = $result.Success
        Data = $result.Data
    }
}

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

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/guilds/$ServerId" -Method "GET" -Headers $Headers -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    if ($request.Success -and $null -ne $request.Data) {
        return @{
            Success = $true
            RequestUri = $request.RequestUri
            Result = $request.Result
            Guild = $request.Data
            Error = $null
        }
    }

    return @{
        Success = $false
        RequestUri = $request.RequestUri
        Result = $request.Result
        Guild = $null
        Error = Get-DiscordGuildAccessErrorMessage -ServerId $ServerId -Result $request.Result
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

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/users/@me/guilds" -Method "GET" -Headers $Headers -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    if ($request.Success -and $null -ne $request.Data) {
        $guilds = @()
        if ($request.Data -is [System.Collections.IEnumerable] -and $request.Data -isnot [string]) {
            $guilds = @($request.Data)
        }
        elseif ($null -ne $request.Data) {
            $guilds = @($request.Data)
        }

        return @{
            Success = $true
            RequestUri = $request.RequestUri
            Result = $request.Result
            Guilds = $guilds
            Error = $null
        }
    }

    return @{
        Success = $false
        RequestUri = $request.RequestUri
        Result = $request.Result
        Guilds = @()
        Error = Get-DiscordCurrentGuildsErrorMessage -Result $request.Result
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

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/users/@me/guilds/$ServerId" -Method "DELETE" -Headers $Headers -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    if ($request.Success) {
        return @{
            Success = $true
            RequestUri = $request.RequestUri
            Result = $request.Result
            Error = $null
        }
    }

    return @{
        Success = $false
        RequestUri = $request.RequestUri
        Result = $request.Result
        Error = Get-DiscordLeaveGuildErrorMessage -ServerId $ServerId -Result $request.Result
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

function Get-DiscordGuild {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ServerId,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/guilds/$ServerId" -Method "GET" -Headers $Headers -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    $errorMessage = $null
    if (-not $request.Success) {
        $errorMessage = Get-DiscordGuildAccessErrorMessage -ServerId $ServerId -Result $request.Result
    }
    return @{
        Success = ($request.Success -and $null -ne $request.Data)
        RequestUri = $request.RequestUri
        Result = $request.Result
        Guild = $request.Data
        Error = $errorMessage
    }
}

function Update-DiscordGuild {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ServerId,
        [hashtable]$Headers = @{},
        [hashtable]$Body,
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/guilds/$ServerId" -Method "PATCH" -Headers $Headers -Body $Body -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    return @{
        Success = $request.Success
        RequestUri = $request.RequestUri
        Result = $request.Result
        Guild = $request.Data
    }
}

function Get-DiscordGuildRoles {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ServerId,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/guilds/$ServerId/roles" -Method "GET" -Headers $Headers -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    $roles = @()
    if ($null -ne $request.Data) {
        $roles = @($request.Data)
    }
    return @{
        Success = ($request.Success -and $null -ne $request.Data)
        RequestUri = $request.RequestUri
        Result = $request.Result
        Roles = $roles
    }
}

function New-DiscordGuildRole {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ServerId,
        [hashtable]$Headers = @{},
        [hashtable]$Body = @{},
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/guilds/$ServerId/roles" -Method "POST" -Headers $Headers -Body $Body -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    return @{
        Success = ($request.Success -and $null -ne $request.Data)
        RequestUri = $request.RequestUri
        Result = $request.Result
        Role = $request.Data
    }
}

function Update-DiscordGuildRole {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ServerId,
        [string]$RoleId,
        [hashtable]$Headers = @{},
        [hashtable]$Body = @{},
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/guilds/$ServerId/roles/$RoleId" -Method "PATCH" -Headers $Headers -Body $Body -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    return @{
        Success = ($request.Success -and $null -ne $request.Data)
        RequestUri = $request.RequestUri
        Result = $request.Result
        Role = $request.Data
    }
}

function Set-DiscordGuildRolePositions {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ServerId,
        [hashtable]$Headers = @{},
        [object[]]$Positions = @(),
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/guilds/$ServerId/roles" -Method "PATCH" -Headers $Headers -Body $Positions -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    $roles = @()
    if ($null -ne $request.Data) {
        $roles = @($request.Data)
    }
    return @{
        Success = $request.Success
        RequestUri = $request.RequestUri
        Result = $request.Result
        Roles = $roles
    }
}

function Get-DiscordGuildChannels {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ServerId,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/guilds/$ServerId/channels" -Method "GET" -Headers $Headers -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    $channels = @()
    if ($null -ne $request.Data) {
        $channels = @($request.Data)
    }
    return @{
        Success = ($request.Success -and $null -ne $request.Data)
        RequestUri = $request.RequestUri
        Result = $request.Result
        Channels = $channels
    }
}

function New-DiscordGuildChannel {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ServerId,
        [hashtable]$Headers = @{},
        [hashtable]$Body = @{},
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/guilds/$ServerId/channels" -Method "POST" -Headers $Headers -Body $Body -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    return @{
        Success = ($request.Success -and $null -ne $request.Data)
        RequestUri = $request.RequestUri
        Result = $request.Result
        Channel = $request.Data
    }
}

function Update-DiscordChannel {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ChannelId,
        [hashtable]$Headers = @{},
        [hashtable]$Body = @{},
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/channels/$ChannelId" -Method "PATCH" -Headers $Headers -Body $Body -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    return @{
        Success = ($request.Success -and $null -ne $request.Data)
        RequestUri = $request.RequestUri
        Result = $request.Result
        Channel = $request.Data
    }
}

function Remove-DiscordChannel {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ChannelId,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/channels/$ChannelId" -Method "DELETE" -Headers $Headers -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    return @{
        Success = $request.Success
        RequestUri = $request.RequestUri
        Result = $request.Result
    }
}

function Set-DiscordGuildChannelPositions {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ServerId,
        [hashtable]$Headers = @{},
        [object[]]$Positions = @(),
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/guilds/$ServerId/channels" -Method "PATCH" -Headers $Headers -Body $Positions -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    $channels = @()
    if ($null -ne $request.Data) {
        $channels = @($request.Data)
    }
    return @{
        Success = $request.Success
        RequestUri = $request.RequestUri
        Result = $request.Result
        Channels = $channels
    }
}

function Get-DiscordGuildMembers {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ServerId,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30,
        [int]$PageSize = 1000,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $members = New-Object System.Collections.ArrayList
    $after = $null
    $lastRequestUri = $null

    do {
        $endpoint = "/guilds/$ServerId/members?limit=$PageSize"
        if (-not [string]::IsNullOrWhiteSpace([string]$after)) {
            $endpoint = "$endpoint&after=$after"
        }

        $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint $endpoint -Method "GET" -Headers $Headers -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
        $lastRequestUri = $request.RequestUri

        if (-not $request.Success -or $null -eq $request.Data) {
            return @{
                Success = $false
                RequestUri = $lastRequestUri
                Result = $request.Result
                Members = @()
            }
        }

        $page = @($request.Data)
        foreach ($member in $page) {
            [void]$members.Add($member)
        }

        if ($page.Count -lt $PageSize) {
            break
        }

        $lastMember = $page[$page.Count - 1]
        if ($null -eq $lastMember -or $lastMember.PSObject.Properties.Name -notcontains "user" -or $null -eq $lastMember.user -or $lastMember.user.PSObject.Properties.Name -notcontains "id") {
            break
        }

        $after = [string]$lastMember.user.id
    }
    while ($true)

    return @{
        Success = $true
        RequestUri = $lastRequestUri
        Result = @{ Success = $true; StatusCode = 200; StatusDescription = "OK"; RawContent = $null; Data = $null }
        Members = @($members.ToArray())
    }
}

function Update-DiscordGuildMember {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ServerId,
        [string]$UserId,
        [hashtable]$Headers = @{},
        [hashtable]$Body = @{},
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/guilds/$ServerId/members/$UserId" -Method "PATCH" -Headers $Headers -Body $Body -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    return @{
        Success = $request.Success
        RequestUri = $request.RequestUri
        Result = $request.Result
        Member = $request.Data
    }
}

function Remove-DiscordGuildMember {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ServerId,
        [string]$UserId,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/guilds/$ServerId/members/$UserId" -Method "DELETE" -Headers $Headers -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    return @{
        Success = $request.Success
        RequestUri = $request.RequestUri
        Result = $request.Result
    }
}

function Remove-DiscordGuildRole {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ServerId,
        [string]$RoleId,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/guilds/$ServerId/roles/$RoleId" -Method "DELETE" -Headers $Headers -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    return @{
        Success = $request.Success
        RequestUri = $request.RequestUri
        Result = $request.Result
    }
}

function New-DiscordDmChannel {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$RecipientId,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/users/@me/channels" -Method "POST" -Headers $Headers -Body @{ recipient_id = $RecipientId } -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    return @{
        Success = ($request.Success -and $null -ne $request.Data)
        RequestUri = $request.RequestUri
        Result = $request.Result
        Channel = $request.Data
    }
}

function Send-DiscordChannelMessage {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ChannelId,
        [string]$Content,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $request = Invoke-DiscordGuildRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/channels/$ChannelId/messages" -Method "POST" -Headers $Headers -Body @{ content = $Content } -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    return @{
        Success = ($request.Success -and $null -ne $request.Data)
        RequestUri = $request.RequestUri
        Result = $request.Result
        Message = $request.Data
    }
}

Export-ModuleMember -Function Invoke-DiscordGuildRequest, Test-DiscordGuildAccess, Get-DiscordGuildAccessErrorMessage, Get-DiscordGuildDisplayName, Get-DiscordCurrentGuilds, Get-DiscordCurrentGuildsErrorMessage, Leave-DiscordGuild, Get-DiscordLeaveGuildErrorMessage, Get-DiscordGuild, Update-DiscordGuild, Get-DiscordGuildRoles, New-DiscordGuildRole, Update-DiscordGuildRole, Set-DiscordGuildRolePositions, Get-DiscordGuildChannels, New-DiscordGuildChannel, Update-DiscordChannel, Remove-DiscordChannel, Set-DiscordGuildChannelPositions, Get-DiscordGuildMembers, Update-DiscordGuildMember, Remove-DiscordGuildMember, Remove-DiscordGuildRole, New-DiscordDmChannel, Send-DiscordChannelMessage
