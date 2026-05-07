$runtimeModule = Import-Module (Join-Path $PSScriptRoot "runtime.psm1") -Force -DisableNameChecking -PassThru
$commonModule = Import-Module (Join-Path $PSScriptRoot "common.psm1") -Force -DisableNameChecking -PassThru
$networkApiModule = Import-Module (Join-Path $PSScriptRoot "..\api\networkAPI.psm1") -Force -DisableNameChecking -PassThru
$commandsModule = Import-Module (Join-Path $PSScriptRoot "commands.psm1") -Force -DisableNameChecking -PassThru

$script:UpdateDiscordRuntimeStateCommand = $runtimeModule.ExportedCommands["Update-DiscordRuntimeState"]
$script:WriteDiscordRuntimeLogCommand = $runtimeModule.ExportedCommands["Write-DiscordRuntimeLog"]
$script:ClearDiscordStopSignalCommand = $runtimeModule.ExportedCommands["Clear-DiscordStopSignal"]
$script:RequestDiscordStopSignalCommand = $runtimeModule.ExportedCommands["Request-DiscordStopSignal"]
$script:ConvertToDiscordDisplayNameCommand = $commonModule.ExportedCommands["ConvertTo-DiscordDisplayName"]
$script:GetUtcTimestampCommand = $commonModule.ExportedCommands["Get-UtcTimestamp"]
$script:InvokeNetworkRequestCommand = $networkApiModule.ExportedCommands["Invoke-NetworkRequest"]
$script:InvokeDiscordPrefixCommandCommand = $commandsModule.ExportedCommands["Invoke-DiscordPrefixCommand"]
$script:InvokeDiscordInteractionCommandCommand = $commandsModule.ExportedCommands["Invoke-DiscordInteractionCommand"]

function Write-DiscordGatewayLog {
    param(
        [string]$LogPath,
        [string]$Message
    )

    if ($null -eq $script:WriteDiscordRuntimeLogCommand) {
        throw "Required command handle missing: Write-DiscordRuntimeLog"
    }

    & $script:WriteDiscordRuntimeLogCommand -LogPath $LogPath -Message $Message
}

function Send-DiscordGatewayPayload {
    param(
        [System.Net.WebSockets.ClientWebSocket]$Socket,
        $Payload,
        [int]$TimeoutMilliseconds = 5000
    )

    $jsonPayload = $Payload | ConvertTo-Json -Depth 20 -Compress
    $payloadBytes = [System.Text.Encoding]::UTF8.GetBytes($jsonPayload)
    $segment = New-Object System.ArraySegment[byte] -ArgumentList (, $payloadBytes)
    $cancellation = New-Object System.Threading.CancellationTokenSource
    $cancellation.CancelAfter($TimeoutMilliseconds)
    try {
        $Socket.SendAsync($segment, [System.Net.WebSockets.WebSocketMessageType]::Text, $true, $cancellation.Token).GetAwaiter().GetResult()
    }
    finally {
        $cancellation.Dispose()
    }
}

function Receive-DiscordGatewayPayload {
    param(
        [System.Net.WebSockets.ClientWebSocket]$Socket,
        [int]$ReceiveTimeoutMilliseconds = 1000,
        [hashtable]$ReceiveState = $null
    )

    if ($null -eq $ReceiveState) {
        $buffer = New-Object byte[] 8192
        $segment = New-Object System.ArraySegment[byte] -ArgumentList (, $buffer)
        $ReceiveState = @{
            Buffer = $buffer
            Segment = $segment
            Stream = New-Object System.IO.MemoryStream
            Task = $null
        }
    }

    try {
        while ($true) {
            if ($null -eq $ReceiveState.Task) {
                $ReceiveState.Task = $Socket.ReceiveAsync($ReceiveState.Segment, [System.Threading.CancellationToken]::None)
            }

            if (-not $ReceiveState.Task.Wait($ReceiveTimeoutMilliseconds)) {
                return @{
                    TimedOut = $true
                    Success = $false
                    Closed = $false
                    ReceiveState = $ReceiveState
                    Data = $null
                    RawContent = $null
                }
            }

            $result = $ReceiveState.Task.GetAwaiter().GetResult()
            $ReceiveState.Task = $null

            if ($result.MessageType -eq [System.Net.WebSockets.WebSocketMessageType]::Close) {
                $ReceiveState.Stream.Dispose()
                return @{
                    TimedOut = $false
                    Success = $false
                    Closed = $true
                    CloseStatus = [int]$Socket.CloseStatus
                    CloseStatusDescription = [string]$Socket.CloseStatusDescription
                    ReceiveState = $null
                    Data = $null
                    RawContent = $null
                }
            }

            if ($result.Count -gt 0) {
                $ReceiveState.Stream.Write($ReceiveState.Buffer, 0, $result.Count)
            }

            if (-not $result.EndOfMessage) {
                continue
            }

            $rawContent = [System.Text.Encoding]::UTF8.GetString($ReceiveState.Stream.ToArray())
            $ReceiveState.Stream.Dispose()
            if ([string]::IsNullOrWhiteSpace($rawContent)) {
                return @{
                    TimedOut = $false
                    Success = $false
                    Closed = $false
                    ReceiveState = $null
                    Data = $null
                    RawContent = $rawContent
                }
            }

            return @{
                TimedOut = $false
                Success = $true
                Closed = $false
                ReceiveState = $null
                Data = $rawContent | ConvertFrom-Json
                RawContent = $rawContent
            }
        }
    }
    catch {
        if ($null -ne $ReceiveState -and $null -ne $ReceiveState.Stream) {
            $ReceiveState.Stream.Dispose()
        }

        throw
    }
}

function Close-DiscordGatewaySocket {
    param(
        [System.Net.WebSockets.ClientWebSocket]$Socket,
        [string]$Reason = "Shutdown"
    )

    if ($null -eq $Socket) {
        return
    }

    try {
        if ($Socket.State -eq [System.Net.WebSockets.WebSocketState]::Open -or $Socket.State -eq [System.Net.WebSockets.WebSocketState]::CloseReceived) {
            $cancellation = New-Object System.Threading.CancellationTokenSource
            $cancellation.CancelAfter(5000)
            try {
                $Socket.CloseAsync([System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure, $Reason, $cancellation.Token).GetAwaiter().GetResult()
            }
            finally {
                $cancellation.Dispose()
            }
        }
    }
    catch {
    }
    finally {
        $Socket.Dispose()
    }
}

function Test-DiscordGatewayConnection {
    param(
        [string]$GatewayUri,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 15
    )

    $socket = New-Object System.Net.WebSockets.ClientWebSocket
    try {
        foreach ($header in $Headers.GetEnumerator()) {
            if ([string]$header.Key -ieq "User-Agent") {
                continue
            }

            $socket.Options.SetRequestHeader([string]$header.Key, [string]$header.Value)
        }

        $connectCancellation = New-Object System.Threading.CancellationTokenSource
        $connectCancellation.CancelAfter($TimeoutSeconds * 1000)
        try {
            $socket.ConnectAsync([System.Uri]$GatewayUri, $connectCancellation.Token).GetAwaiter().GetResult()
        }
        finally {
            $connectCancellation.Dispose()
        }

        $helloPayload = Receive-DiscordGatewayPayload -Socket $socket -ReceiveTimeoutMilliseconds ($TimeoutSeconds * 1000)
        if ($helloPayload.Success -and $helloPayload.Data.op -eq 10) {
            return @{
                Success = $true
                Message = "Gateway handshake received."
                Payload = $helloPayload.Data
            }
        }

        if ($helloPayload.Closed) {
            return @{
                Success = $false
                Message = "Gateway connection closed during handshake: $($helloPayload.CloseStatus) $($helloPayload.CloseStatusDescription)"
                Payload = $null
            }
        }

        return @{
            Success = $false
            Message = "Gateway did not return a Discord HELLO payload."
            Payload = $helloPayload.Data
        }
    }
    catch {
        return @{
            Success = $false
            Message = $_.Exception.Message
            Payload = $null
        }
    }
    finally {
        Close-DiscordGatewaySocket -Socket $socket -Reason "Probe"
    }
}

function Get-DiscordGatewayBotInfo {
    param(
        [string]$RequestUri,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30
    )

    if ($null -eq $script:InvokeNetworkRequestCommand) {
        throw "Required command handle missing: Invoke-NetworkRequest"
    }

    return & $script:InvokeNetworkRequestCommand -Uri $RequestUri -Method "GET" -Headers $Headers -TimeoutSeconds $TimeoutSeconds
}

function Invoke-DiscordGatewayWorker {
    param(
        [string]$GatewayUri,
        [hashtable]$GatewayHeaders,
        [string]$StatePath,
        [string]$LogPath,
        [string]$StopSignalPath,
        [string]$Token,
        [long]$Intents,
        [string]$PresenceText,
        [string]$BotDisplayName,
        [hashtable]$Identity,
        [string]$DiscordBaseUrl,
        [string]$DiscordApiVersion,
        [hashtable]$DiscordHeaders,
        [string[]]$Prefixes,
        [string]$MessagesRootPath,
        [string]$ModrinthConfigPath,
        [int]$ReconnectInitialDelaySeconds = 5,
        [int]$ReconnectMaxDelaySeconds = 60,
        [int]$ReceivePollMilliseconds = 1000
    )

    if ($null -eq $script:UpdateDiscordRuntimeStateCommand -or $null -eq $script:ConvertToDiscordDisplayNameCommand -or $null -eq $script:GetUtcTimestampCommand) {
        throw "Required command handles are missing for Discord gateway worker."
    }

    $reconnectDelaySeconds = [Math]::Max(1, $ReconnectInitialDelaySeconds)
    $socket = $null
    $sequence = $null
    $stopRequested = $false
    $receiveState = $null

    if ($null -ne $Identity) {
        $resolvedDisplayName = & $script:ConvertToDiscordDisplayNameCommand -User $Identity
        & $script:UpdateDiscordRuntimeStateCommand -StatePath $StatePath -Updates @{
            BotUserId = if ($Identity.ContainsKey("id")) { [string]$Identity["id"] } else { $null }
            BotDisplayName = if (-not [string]::IsNullOrWhiteSpace([string]$resolvedDisplayName)) { $resolvedDisplayName } else { $BotDisplayName }
        } | Out-Null
    }

    while ($true) {
        if (Test-Path $StopSignalPath -PathType Leaf) {
            $stopRequested = $true
            break
        }

        try {
            & $script:UpdateDiscordRuntimeStateCommand -StatePath $StatePath -Updates @{
                State = "starting"
                LastError = $null
                GatewayUrl = $GatewayUri
            } | Out-Null
            Write-DiscordGatewayLog -LogPath $LogPath -Message "Connecting to Discord gateway: $GatewayUri"

            $socket = New-Object System.Net.WebSockets.ClientWebSocket
            foreach ($header in $GatewayHeaders.GetEnumerator()) {
                if ([string]$header.Key -ieq "User-Agent") {
                    continue
                }

                $socket.Options.SetRequestHeader([string]$header.Key, [string]$header.Value)
            }

            $connectCancellation = New-Object System.Threading.CancellationTokenSource
            $connectCancellation.CancelAfter(30000)
            try {
                $socket.ConnectAsync([System.Uri]$GatewayUri, $connectCancellation.Token).GetAwaiter().GetResult()
            }
            finally {
                $connectCancellation.Dispose()
            }

            $helloPayload = Receive-DiscordGatewayPayload -Socket $socket -ReceiveTimeoutMilliseconds 30000
            if (-not $helloPayload.Success -or $helloPayload.Data.op -ne 10) {
                throw "Discord gateway did not return a valid HELLO payload."
            }

            $heartbeatInterval = [int]$helloPayload.Data.d.heartbeat_interval
            if ($heartbeatInterval -le 0) {
                throw "Discord gateway heartbeat interval was invalid."
            }

            $identifyProperties = @{
                '$os' = "windows"
                '$browser' = "blobManager"
                '$device' = "blobManager"
            }

            $identifyData = @{
                token = $Token
                intents = $Intents
                properties = $identifyProperties
            }

            if (-not [string]::IsNullOrWhiteSpace([string]$PresenceText)) {
                $identifyData["presence"] = @{
                    since = $null
                    afk = $false
                    status = "online"
                    activities = @(
                        @{
                            name = $PresenceText
                            type = 0
                        }
                    )
                }
            }

            Send-DiscordGatewayPayload -Socket $socket -Payload @{
                op = 2
                d = $identifyData
            }

            $nextHeartbeatAt = (Get-Date).ToUniversalTime().AddMilliseconds($heartbeatInterval)
            $sequence = $null
            $receiveState = $null

            while ($socket.State -eq [System.Net.WebSockets.WebSocketState]::Open) {
                if (Test-Path $StopSignalPath -PathType Leaf) {
                    $stopRequested = $true
                    break
                }

                $utcNow = (Get-Date).ToUniversalTime()
                if ($utcNow -ge $nextHeartbeatAt) {
                    Send-DiscordGatewayPayload -Socket $socket -Payload @{
                        op = 1
                        d = $sequence
                    }

                    & $script:UpdateDiscordRuntimeStateCommand -StatePath $StatePath -Updates @{
                        LastHeartbeatAt = & $script:GetUtcTimestampCommand
                    } | Out-Null

                    $nextHeartbeatAt = $utcNow.AddMilliseconds($heartbeatInterval)
                }

                $message = Receive-DiscordGatewayPayload -Socket $socket -ReceiveTimeoutMilliseconds $ReceivePollMilliseconds -ReceiveState $receiveState
                if ($message.TimedOut) {
                    $receiveState = $message.ReceiveState
                    continue
                }

                $receiveState = $null

                if ($message.Closed) {
                    $closeStatus = [int]$message.CloseStatus
                    $closeDescription = [string]$message.CloseStatusDescription
                    Write-DiscordGatewayLog -LogPath $LogPath -Message "Gateway socket closed: $closeStatus $closeDescription"

                    switch ($closeStatus) {
                        4004 { throw "Discord rejected the bot token during gateway identify." }
                        4010 { throw "Discord rejected the gateway shard configuration." }
                        4011 { throw "Discord requires sharding for this bot." }
                        4012 { throw "Discord rejected the configured gateway API version." }
                        4013 { throw "Discord rejected the configured intents value." }
                        4014 { throw "Discord disallowed one or more configured intents for this bot." }
                    }

                    break
                }

                if (-not $message.Success -or $null -eq $message.Data) {
                    continue
                }

                $payload = $message.Data
                if ($null -ne $payload.s) {
                    $sequence = $payload.s
                }

                switch ([int]$payload.op) {
                    0 {
                        $eventType = [string]$payload.t
                        switch ($eventType) {
                            "READY" {
                                $readyUser = $payload.d.user
                                $readyDisplayName = & $script:ConvertToDiscordDisplayNameCommand -User $readyUser
                                & $script:UpdateDiscordRuntimeStateCommand -StatePath $StatePath -Updates @{
                                    State = "connected"
                                    SessionId = [string]$payload.d.session_id
                                    BotUserId = [string]$readyUser.id
                                    BotDisplayName = if (-not [string]::IsNullOrWhiteSpace([string]$readyDisplayName)) { $readyDisplayName } else { $BotDisplayName }
                                    LastReadyAt = & $script:GetUtcTimestampCommand
                                    LastEvent = "READY"
                                    LastSessionAt = & $script:GetUtcTimestampCommand
                                } | Out-Null

                                Write-DiscordGatewayLog -LogPath $LogPath -Message "Gateway READY received for bot '$readyDisplayName'."
                                $reconnectDelaySeconds = [Math]::Max(1, $ReconnectInitialDelaySeconds)
                            }
                            "RESUMED" {
                                & $script:UpdateDiscordRuntimeStateCommand -StatePath $StatePath -Updates @{
                                    State = "connected"
                                    LastEvent = "RESUMED"
                                    LastSessionAt = & $script:GetUtcTimestampCommand
                                } | Out-Null
                                Write-DiscordGatewayLog -LogPath $LogPath -Message "Gateway session resumed."
                            }
                            "MESSAGE_CREATE" {
                                $commandResult = & $script:InvokeDiscordPrefixCommandCommand -Message $payload.d -Prefixes $Prefixes -DiscordBaseUrl $DiscordBaseUrl -DiscordApiVersion $DiscordApiVersion -DiscordHeaders $DiscordHeaders -MessagesRootPath $MessagesRootPath -ModrinthConfigPath $ModrinthConfigPath
                                if ($commandResult.Handled) {
                                    $logMessage = if ($commandResult.ContainsKey("Reason") -and -not [string]::IsNullOrWhiteSpace([string]$commandResult.Reason)) {
                                        [string]$commandResult.Reason
                                    }
                                    else {
                                        "Processed Discord prefix command."
                                    }

                                    Write-DiscordGatewayLog -LogPath $LogPath -Message $logMessage
                                }
                            }
                            "INTERACTION_CREATE" {
                                $interactionResult = & $script:InvokeDiscordInteractionCommandCommand -Interaction $payload.d -DiscordBaseUrl $DiscordBaseUrl -DiscordHeaders $DiscordHeaders -MessagesRootPath $MessagesRootPath -ModrinthConfigPath $ModrinthConfigPath
                                if ($interactionResult.Handled) {
                                    $logMessage = if ($interactionResult.ContainsKey("Reason") -and -not [string]::IsNullOrWhiteSpace([string]$interactionResult.Reason)) {
                                        [string]$interactionResult.Reason
                                    }
                                    else {
                                        "Processed Discord interaction command."
                                    }

                                    Write-DiscordGatewayLog -LogPath $LogPath -Message $logMessage
                                }
                            }
                        }
                    }
                    1 {
                        Send-DiscordGatewayPayload -Socket $socket -Payload @{
                            op = 1
                            d = $sequence
                        }
                        & $script:UpdateDiscordRuntimeStateCommand -StatePath $StatePath -Updates @{
                            LastHeartbeatAt = & $script:GetUtcTimestampCommand
                        } | Out-Null
                    }
                    7 {
                        & $script:UpdateDiscordRuntimeStateCommand -StatePath $StatePath -Updates @{
                            State = "reconnecting"
                            LastEvent = "RECONNECT"
                        } | Out-Null
                        Write-DiscordGatewayLog -LogPath $LogPath -Message "Gateway requested reconnect."
                        break
                    }
                    9 {
                        $canResume = $false
                        if ($null -ne $payload.d) {
                            $canResume = [bool]$payload.d
                        }

                        if (-not $canResume) {
                            throw "Discord invalidated the current session."
                        }

                        & $script:UpdateDiscordRuntimeStateCommand -StatePath $StatePath -Updates @{
                            State = "reconnecting"
                            LastEvent = "INVALID_SESSION"
                        } | Out-Null
                        Write-DiscordGatewayLog -LogPath $LogPath -Message "Gateway invalid session event received. Reconnecting."
                        Start-Sleep -Seconds 2
                        break
                    }
                    11 {
                        & $script:UpdateDiscordRuntimeStateCommand -StatePath $StatePath -Updates @{
                            LastHeartbeatAckAt = & $script:GetUtcTimestampCommand
                        } | Out-Null
                    }
                }
            }

            if ($stopRequested) {
                break
            }

            & $script:UpdateDiscordRuntimeStateCommand -StatePath $StatePath -Updates @{
                State = "reconnecting"
                LastError = $null
            } | Out-Null
            Write-DiscordGatewayLog -LogPath $LogPath -Message "Gateway disconnected. Reconnecting in $reconnectDelaySeconds second(s)."
        }
        catch {
            $errorMessage = $_.Exception.Message
            & $script:UpdateDiscordRuntimeStateCommand -StatePath $StatePath -Updates @{
                State = "reconnecting"
                LastError = $errorMessage
            } | Out-Null
            Write-DiscordGatewayLog -LogPath $LogPath -Message "Gateway worker error: $errorMessage"

            if ($errorMessage -like "*bot token*" -or $errorMessage -like "*intents*" -or $errorMessage -like "*sharding*" -or $errorMessage -like "*API version*") {
                & $script:UpdateDiscordRuntimeStateCommand -StatePath $StatePath -Updates @{
                    State = "failed"
                    LastError = $errorMessage
                } | Out-Null
                break
            }
        }
        finally {
            if ($null -ne $receiveState -and $null -ne $receiveState.Stream) {
                $receiveState.Stream.Dispose()
                $receiveState = $null
            }

            if ($null -ne $socket) {
                Close-DiscordGatewaySocket -Socket $socket -Reason "Reconnect"
                $socket = $null
            }
        }

        if (Test-Path $StopSignalPath -PathType Leaf) {
            $stopRequested = $true
            break
        }

        Start-Sleep -Seconds $reconnectDelaySeconds
        $reconnectDelaySeconds = [Math]::Min($ReconnectMaxDelaySeconds, [Math]::Max(1, $reconnectDelaySeconds * 2))
    }

    if ($stopRequested) {
        if ($null -eq $script:ClearDiscordStopSignalCommand) {
            throw "Required command handle missing: Clear-DiscordStopSignal"
        }

        & $script:ClearDiscordStopSignalCommand -StopSignalPath $StopSignalPath
        & $script:UpdateDiscordRuntimeStateCommand -StatePath $StatePath -Updates @{
            State = "stopped"
            LastError = $null
            StoppedAt = & $script:GetUtcTimestampCommand
        } | Out-Null
        Write-DiscordGatewayLog -LogPath $LogPath -Message "Gateway worker stopped by request."
    }
}

Export-ModuleMember -Function Test-DiscordGatewayConnection, Get-DiscordGatewayBotInfo, Invoke-DiscordGatewayWorker
