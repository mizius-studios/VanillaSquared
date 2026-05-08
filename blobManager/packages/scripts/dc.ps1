$ErrorActionPreference = "Stop"
$Version = "1.0.0"
$PackageName = "dc"

$errorApiModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\errorAPI.psm1") -Force -DisableNameChecking -PassThru
$configApiModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\configAPI.psm1") -Force -DisableNameChecking -PassThru
$networkApiModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\networkAPI.psm1") -Force -DisableNameChecking -PassThru
$keyApiModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\keyAPI.psm1") -Force -DisableNameChecking -PassThru
$fileApiModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\fileAPI.psm1") -Force -DisableNameChecking -PassThru
$versionModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\commands\version.psm1") -Force -DisableNameChecking -PassThru
$helpModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\commands\help.psm1") -Force -DisableNameChecking -PassThru
$pingModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\commands\ping.psm1") -Force -DisableNameChecking -PassThru
$commonModule = Import-Module (Join-Path $PSScriptRoot "..\util\dc\common.psm1") -Force -DisableNameChecking -PassThru
$authModule = Import-Module (Join-Path $PSScriptRoot "..\util\dc\auth.psm1") -Force -DisableNameChecking -PassThru
$runtimeModule = Import-Module (Join-Path $PSScriptRoot "..\util\dc\runtime.psm1") -Force -DisableNameChecking -PassThru
$gatewayModule = Import-Module (Join-Path $PSScriptRoot "..\util\dc\gateway.psm1") -Force -DisableNameChecking -PassThru
$serverModule = Import-Module (Join-Path $PSScriptRoot "..\util\dc\server.psm1") -Force -DisableNameChecking -PassThru
$guildModule = Import-Module (Join-Path $PSScriptRoot "..\util\dc\guild.psm1") -Force -DisableNameChecking -PassThru
$backupModule = Import-Module (Join-Path $PSScriptRoot "..\util\dc\backup.psm1") -Force -DisableNameChecking -PassThru
$commandsModule = Import-Module (Join-Path $PSScriptRoot "..\util\dc\commands.psm1") -Force -DisableNameChecking -PassThru

$Cmd = @{
    NewMessageState = $errorApiModule.ExportedCommands["New-MessageState"]
    AddErrorMessage = $errorApiModule.ExportedCommands["Add-ErrorMessage"]
    AddWarningMessage = $errorApiModule.ExportedCommands["Add-WarningMessage"]
    ThrowIfErrors = $errorApiModule.ExportedCommands["Throw-IfErrors"]
    WriteWarnings = $errorApiModule.ExportedCommands["Write-Warnings"]
    GetJsonConfig = $configApiModule.ExportedCommands["Get-JsonConfig"]
    GetConfigValue = $configApiModule.ExportedCommands["Get-ConfigValue"]
    TestConfigVersion = $configApiModule.ExportedCommands["Test-ConfigVersion"]
    InvokeNetworkRequest = $networkApiModule.ExportedCommands["Invoke-NetworkRequest"]
    MergeAuthorizationHeader = $networkApiModule.ExportedCommands["Merge-AuthorizationHeader"]
    JoinApiUri = $networkApiModule.ExportedCommands["Join-ApiUri"]
    ResolveKeyValue = $keyApiModule.ExportedCommands["Resolve-KeyValue"]
    CommandGetKey = $keyApiModule.ExportedCommands["Command-GetKey"]
    CommandVersion = $versionModule.ExportedCommands["Command-Version"]
    CommandHelp = $helpModule.ExportedCommands["Command-Help"]
    CommandPing = $pingModule.ExportedCommands["Command-Ping"]
    ResolveLocalPath = $fileApiModule.ExportedCommands["Resolve-LocalPath"]
    RemoveFileIfExists = $fileApiModule.ExportedCommands["Remove-FileIfExists"]
    AddRequiredConfigError = $commonModule.ExportedCommands["Add-RequiredConfigError"]
    GetUtcTimestamp = $commonModule.ExportedCommands["Get-UtcTimestamp"]
    ConvertToDiscordDisplayName = $commonModule.ExportedCommands["ConvertTo-DiscordDisplayName"]
    TestDiscordIntentsValue = $commonModule.ExportedCommands["Test-DiscordIntentsValue"]
    GetDiscordAuthorizationHeaders = $authModule.ExportedCommands["Get-DiscordAuthorizationHeaders"]
    GetDiscordBotDisplayName = $authModule.ExportedCommands["Get-DiscordBotDisplayName"]
    GetDiscordAuthErrorMessage = $authModule.ExportedCommands["Get-DiscordAuthErrorMessage"]
    ConvertToDiscordServerMap = $serverModule.ExportedCommands["ConvertTo-DiscordServerMap"]
    TestDiscordServerId = $serverModule.ExportedCommands["Test-DiscordServerId"]
    ResolveDiscordServerReference = $serverModule.ExportedCommands["Resolve-DiscordServerReference"]
    GetDiscordServerAliasById = $serverModule.ExportedCommands["Get-DiscordServerAliasById"]
    WriteDiscordServerMap = $serverModule.ExportedCommands["Write-DiscordServerMap"]
    TestDiscordGuildAccess = $guildModule.ExportedCommands["Test-DiscordGuildAccess"]
    GetDiscordGuildAccessErrorMessage = $guildModule.ExportedCommands["Get-DiscordGuildAccessErrorMessage"]
    GetDiscordGuildDisplayName = $guildModule.ExportedCommands["Get-DiscordGuildDisplayName"]
    GetDiscordCurrentGuilds = $guildModule.ExportedCommands["Get-DiscordCurrentGuilds"]
    GetDiscordCurrentGuildsErrorMessage = $guildModule.ExportedCommands["Get-DiscordCurrentGuildsErrorMessage"]
    LeaveDiscordGuild = $guildModule.ExportedCommands["Leave-DiscordGuild"]
    GetDiscordLeaveGuildErrorMessage = $guildModule.ExportedCommands["Get-DiscordLeaveGuildErrorMessage"]
    ParseDiscordBackupArguments = $backupModule.ExportedCommands["Parse-DiscordBackupArguments"]
    GetDiscordBackupList = $backupModule.ExportedCommands["Get-DiscordBackupList"]
    GetDiscordBackupInfo = $backupModule.ExportedCommands["Get-DiscordBackupInfo"]
    TestDiscordBackup = $backupModule.ExportedCommands["Test-DiscordBackup"]
    RemoveDiscordBackup = $backupModule.ExportedCommands["Remove-DiscordBackup"]
    InvokeDiscordBackupCreate = $backupModule.ExportedCommands["Invoke-DiscordBackupCreate"]
    InvokeDiscordBackupRestore = $backupModule.ExportedCommands["Invoke-DiscordBackupRestore"]
    GetDiscordEffectivePrefixes = $commandsModule.ExportedCommands["Get-DiscordEffectivePrefixes"]
    TestDiscordMessageContentIntentEnabled = $commandsModule.ExportedCommands["Test-DiscordMessageContentIntentEnabled"]
    SyncDiscordGuildCommands = $commandsModule.ExportedCommands["Sync-DiscordGuildCommands"]
    GetDiscordRuntimeState = $runtimeModule.ExportedCommands["Get-DiscordRuntimeState"]
    SetDiscordRuntimeState = $runtimeModule.ExportedCommands["Set-DiscordRuntimeState"]
    UpdateDiscordRuntimeState = $runtimeModule.ExportedCommands["Update-DiscordRuntimeState"]
    InitializeDiscordRuntimeFiles = $runtimeModule.ExportedCommands["Initialize-DiscordRuntimeFiles"]
    WriteDiscordRuntimeLog = $runtimeModule.ExportedCommands["Write-DiscordRuntimeLog"]
    TestDiscordManagedProcess = $runtimeModule.ExportedCommands["Test-DiscordManagedProcess"]
    GetDiscordManagedStatus = $runtimeModule.ExportedCommands["Get-DiscordManagedStatus"]
    ClearDiscordStopSignal = $runtimeModule.ExportedCommands["Clear-DiscordStopSignal"]
    RequestDiscordStopSignal = $runtimeModule.ExportedCommands["Request-DiscordStopSignal"]
    StopDiscordManagedProcess = $runtimeModule.ExportedCommands["Stop-DiscordManagedProcess"]
    TestDiscordGatewayConnection = $gatewayModule.ExportedCommands["Test-DiscordGatewayConnection"]
    GetDiscordGatewayBotInfo = $gatewayModule.ExportedCommands["Get-DiscordGatewayBotInfo"]
    InvokeDiscordGatewayWorker = $gatewayModule.ExportedCommands["Invoke-DiscordGatewayWorker"]
}

foreach ($entry in $Cmd.GetEnumerator()) {
    if ($null -eq $entry.Value) {
        throw "Required command handle missing: $($entry.Key)"
    }
}

$configPath = Join-Path $PSScriptRoot "..\config\dc.json"
$config = & $Cmd.GetJsonConfig -Path $configPath -Fallback @{}
$networkConfig = & $Cmd.GetConfigValue -Config $config -Key "network" -DefaultValue @{}
$headers = & $Cmd.GetConfigValue -Config $networkConfig -Key "headers" -DefaultValue @{}
$baseUrl = & $Cmd.GetConfigValue -Config $networkConfig -Key "baseUrl" -DefaultValue "https://discord.com/api"
$apiVersion = & $Cmd.GetConfigValue -Config $networkConfig -Key "apiVersion" -DefaultValue "v10"
$connectionTestUrl = & $Cmd.GetConfigValue -Config $networkConfig -Key "connectionTestUrl" -DefaultValue "https://discord.com/api/v10/gateway"
$testEndpoint = & $Cmd.GetConfigValue -Config $networkConfig -Key "testEndpoint" -DefaultValue "/gateway"
$authenticatedTestEndpoint = & $Cmd.GetConfigValue -Config $networkConfig -Key "authenticatedTestEndpoint" -DefaultValue "/users/@me"
$gatewayBotEndpoint = & $Cmd.GetConfigValue -Config $networkConfig -Key "gatewayBotEndpoint" -DefaultValue "/gateway/bot"
$gatewayUrl = & $Cmd.GetConfigValue -Config $networkConfig -Key "gatewayUrl" -DefaultValue "wss://gateway.discord.gg/?v=10&encoding=json"
$timeoutSeconds = [int](& $Cmd.GetConfigValue -Config $networkConfig -Key "timeoutSeconds" -DefaultValue 30)
$authConfig = & $Cmd.GetConfigValue -Config $config -Key "auth" -DefaultValue @{}
$authKeyFile = & $Cmd.GetConfigValue -Config $authConfig -Key "keyFile" -DefaultValue "blobManager/packages/config/keys/personal.txt"
$authKeyName = & $Cmd.GetConfigValue -Config $authConfig -Key "keyName" -DefaultValue "DiscordBotToken"
$authEnvVar = & $Cmd.GetConfigValue -Config $authConfig -Key "envVar" -DefaultValue "DISCORD_BOT_TOKEN"
$runtimeConfig = & $Cmd.GetConfigValue -Config $config -Key "runtime" -DefaultValue @{}
$statePathValue = & $Cmd.GetConfigValue -Config $runtimeConfig -Key "statePath" -DefaultValue "blobManager/packages/data/dc/state.json"
$stopSignalPathValue = & $Cmd.GetConfigValue -Config $runtimeConfig -Key "stopSignalPath" -DefaultValue "blobManager/packages/data/dc/stop.signal"
$logPathValue = & $Cmd.GetConfigValue -Config $runtimeConfig -Key "logPath" -DefaultValue "blobManager/packages/data/dc/runtime.log"
$startTimeoutSeconds = [int](& $Cmd.GetConfigValue -Config $runtimeConfig -Key "startTimeoutSeconds" -DefaultValue 15)
$stopTimeoutSeconds = [int](& $Cmd.GetConfigValue -Config $runtimeConfig -Key "stopTimeoutSeconds" -DefaultValue 15)
$reconnectInitialDelaySeconds = [int](& $Cmd.GetConfigValue -Config $runtimeConfig -Key "reconnectInitialDelaySeconds" -DefaultValue 5)
$reconnectMaxDelaySeconds = [int](& $Cmd.GetConfigValue -Config $runtimeConfig -Key "reconnectMaxDelaySeconds" -DefaultValue 60)
$receivePollMilliseconds = [int](& $Cmd.GetConfigValue -Config $runtimeConfig -Key "receivePollMilliseconds" -DefaultValue 1000)
$messagesConfig = & $Cmd.GetConfigValue -Config $config -Key "messages" -DefaultValue @{}
$messagesRootPathValue = & $Cmd.GetConfigValue -Config $messagesConfig -Key "rootPath" -DefaultValue "blobManager/packages/data/dc/msgs"
$backupConfig = & $Cmd.GetConfigValue -Config $config -Key "backups" -DefaultValue @{}
$backupRootPathValue = & $Cmd.GetConfigValue -Config $backupConfig -Key "rootPath" -DefaultValue "blobManager/packages/data/dc/backups"
$serversConfig = & $Cmd.GetConfigValue -Config $config -Key "servers" -DefaultValue @{}
$serverMap = & $Cmd.ConvertToDiscordServerMap -ServersConfig $serversConfig
$botConfig = & $Cmd.GetConfigValue -Config $config -Key "bot" -DefaultValue @{}
$intentsValue = & $Cmd.GetConfigValue -Config $botConfig -Key "intents" -DefaultValue 513
$effectivePrefixInfo = & $Cmd.GetDiscordEffectivePrefixes
$effectivePrefixes = @($effectivePrefixInfo.Prefixes)
$presenceText = [string](& $Cmd.GetConfigValue -Config $botConfig -Key "presenceText" -DefaultValue "")
$botDisplayName = [string](& $Cmd.GetConfigValue -Config $botConfig -Key "displayName" -DefaultValue "blobManager Discord Bot")

$statePath = & $Cmd.ResolveLocalPath -PathValue $statePathValue
$stopSignalPath = & $Cmd.ResolveLocalPath -PathValue $stopSignalPathValue
$logPath = & $Cmd.ResolveLocalPath -PathValue $logPathValue
$messagesRootPath = & $Cmd.ResolveLocalPath -PathValue $messagesRootPathValue
$authKeyFilePath = & $Cmd.ResolveLocalPath -PathValue $authKeyFile
$modrinthConfigPath = & $Cmd.ResolveLocalPath -PathValue "blobManager/packages/config/modrinth.json"

$state = & $Cmd.NewMessageState
$versionStatus = & $Cmd.TestConfigVersion -PackageName $PackageName -ScriptVersion $Version -Config $config -State $state

$Command = if ($args.Count -gt 0) { $args[0] } else { $null }
$Rest = @()
if ($args.Count -gt 1) {
    $Rest += $args[1..($args.Count - 1)]
}

function Test-DcConfiguration {
    param(
        [switch]$RequireAuth
    )

    & $Cmd.AddRequiredConfigError -Value $baseUrl -ConfigKey "network.baseUrl" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\dc.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
    & $Cmd.AddRequiredConfigError -Value $apiVersion -ConfigKey "network.apiVersion" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\dc.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
    & $Cmd.AddRequiredConfigError -Value $connectionTestUrl -ConfigKey "network.connectionTestUrl" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\dc.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
    & $Cmd.AddRequiredConfigError -Value $testEndpoint -ConfigKey "network.testEndpoint" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\dc.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
    & $Cmd.AddRequiredConfigError -Value $authenticatedTestEndpoint -ConfigKey "network.authenticatedTestEndpoint" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\dc.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
    & $Cmd.AddRequiredConfigError -Value $gatewayBotEndpoint -ConfigKey "network.gatewayBotEndpoint" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\dc.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
    & $Cmd.AddRequiredConfigError -Value $gatewayUrl -ConfigKey "network.gatewayUrl" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\dc.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
    & $Cmd.AddRequiredConfigError -Value $statePathValue -ConfigKey "runtime.statePath" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\dc.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
    & $Cmd.AddRequiredConfigError -Value $stopSignalPathValue -ConfigKey "runtime.stopSignalPath" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\dc.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
    & $Cmd.AddRequiredConfigError -Value $logPathValue -ConfigKey "runtime.logPath" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\dc.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
    & $Cmd.AddRequiredConfigError -Value $messagesRootPathValue -ConfigKey "messages.rootPath" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\dc.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
    & $Cmd.AddRequiredConfigError -Value $backupRootPathValue -ConfigKey "backups.rootPath" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\dc.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
    if ($RequireAuth) {
        & $Cmd.AddRequiredConfigError -Value $authKeyFile -ConfigKey "auth.keyFile" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\dc.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $authKeyName -ConfigKey "auth.keyName" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\dc.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $authEnvVar -ConfigKey "auth.envVar" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\dc.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
    }

    if (-not (& $Cmd.TestDiscordIntentsValue -Value $intentsValue)) {
        & $Cmd.AddErrorMessage -State $state -Message "dc bot.intents must be a valid integer in blobManager\\packages\\config\\dc.json"
    }

    if ($timeoutSeconds -le 0) {
        & $Cmd.AddErrorMessage -State $state -Message "dc network.timeoutSeconds must be greater than 0 in blobManager\\packages\\config\\dc.json"
    }

    if ($startTimeoutSeconds -le 0) {
        & $Cmd.AddErrorMessage -State $state -Message "dc runtime.startTimeoutSeconds must be greater than 0 in blobManager\\packages\\config\\dc.json"
    }

    if ($stopTimeoutSeconds -le 0) {
        & $Cmd.AddErrorMessage -State $state -Message "dc runtime.stopTimeoutSeconds must be greater than 0 in blobManager\\packages\\config\\dc.json"
    }

    if ($reconnectInitialDelaySeconds -le 0 -or $reconnectMaxDelaySeconds -le 0) {
        & $Cmd.AddErrorMessage -State $state -Message "dc runtime reconnect delays must be greater than 0 in blobManager\\packages\\config\\dc.json"
    }

    if ($receivePollMilliseconds -le 0) {
        & $Cmd.AddErrorMessage -State $state -Message "dc runtime.receivePollMilliseconds must be greater than 0 in blobManager\\packages\\config\\dc.json"
    }

    if (-not (& $Cmd.TestDiscordMessageContentIntentEnabled -Intents ([long]$intentsValue))) {
        & $Cmd.AddWarningMessage -State $state -Message "dc bot.intents does not include MESSAGE_CONTENT (32768). '/' text commands may not work in guild channels."
    }

    foreach ($entry in $serverMap.GetEnumerator()) {
        if ([string]::IsNullOrWhiteSpace([string]$entry.Key)) {
            & $Cmd.AddErrorMessage -State $state -Message "dc servers cannot contain an empty alias key in blobManager\\packages\\config\\dc.json"
            continue
        }

        if (-not (& $Cmd.TestDiscordServerId -ServerId ([string]$entry.Value))) {
            & $Cmd.AddErrorMessage -State $state -Message ("dc servers.{0} must be a valid Discord server id in blobManager\\packages\\config\\dc.json" -f [string]$entry.Key)
        }
    }

    & $Cmd.ThrowIfErrors -State $state
}

function Get-DcAuthContext {
    $authHeadersResult = & $Cmd.GetDiscordAuthorizationHeaders -Headers $headers -KeyFilePath $authKeyFilePath -KeyName $authKeyName -EnvVarName $authEnvVar -ResolveKeyValue $Cmd.ResolveKeyValue -MergeAuthorizationHeader $Cmd.MergeAuthorizationHeader
    if (-not $authHeadersResult.Success) {
        Write-Host $authHeadersResult.Error -ForegroundColor Red
        Write-Host "Expected local file: $authKeyFilePath" -ForegroundColor Red
        Write-Host "Expected file format: $authKeyName=<your_discord_bot_token>" -ForegroundColor DarkYellow
        Write-Host "Fallback env var: $authEnvVar" -ForegroundColor DarkYellow
        exit 1
    }

    return $authHeadersResult
}

function Invoke-DcAuthProbe {
    $authHeadersResult = Get-DcAuthContext
    $requestUri = & $Cmd.JoinApiUri -BaseUrl $baseUrl -ApiVersion $apiVersion -Endpoint $authenticatedTestEndpoint
    $result = & $Cmd.InvokeNetworkRequest -Uri $requestUri -Method "GET" -Headers $authHeadersResult.Headers -TimeoutSeconds $timeoutSeconds
    return @{
        RequestUri = $requestUri
        AuthContext = $authHeadersResult
        Result = $result
    }
}

function Resolve-DcGatewayUri {
    param(
        [hashtable]$AuthContext
    )

    $requestUri = & $Cmd.JoinApiUri -BaseUrl $baseUrl -ApiVersion $apiVersion -Endpoint $gatewayBotEndpoint
    $gatewayBotResult = & $Cmd.GetDiscordGatewayBotInfo -RequestUri $requestUri -Headers $AuthContext.Headers -TimeoutSeconds $timeoutSeconds
    if ($gatewayBotResult.Success -and $null -ne $gatewayBotResult.Data -and $gatewayBotResult.Data.PSObject.Properties.Name -contains "url" -and -not [string]::IsNullOrWhiteSpace([string]$gatewayBotResult.Data.url)) {
        $resolvedGatewayUrl = [string]$gatewayBotResult.Data.url
        if ($resolvedGatewayUrl -notmatch "\?") {
            $resolvedGatewayUrl = "$resolvedGatewayUrl/?v=10&encoding=json"
        }
        elseif ($resolvedGatewayUrl -notmatch "encoding=") {
            $resolvedGatewayUrl = "$resolvedGatewayUrl&encoding=json"
        }

        return @{
            Success = $true
            GatewayUrl = $resolvedGatewayUrl
            Result = $gatewayBotResult
        }
    }

    return @{
        Success = $false
        GatewayUrl = $gatewayUrl
        Result = $gatewayBotResult
    }
}

function Resolve-DcServerReference {
    param(
        [string]$Reference
    )

    $resolution = & $Cmd.ResolveDiscordServerReference -Reference $Reference -ServerMap $serverMap
    if (-not $resolution.Success) {
        Write-Host $resolution.Error -ForegroundColor Red
        exit 1
    }

    return $resolution
}

function Write-DcBackupUsage {
    Write-Host "Usage: .\blob.ps1 dc backup create <alias-or-server-id> [--description ""...""] [--include <categories...>] [--exclude <categories...>]" -ForegroundColor Red
    Write-Host "Usage: .\blob.ps1 dc backup restore <alias-or-server-id> <backupId> [--include <categories...>] [--exclude <categories...>] [--force-members]" -ForegroundColor Red
    Write-Host "Usage: .\blob.ps1 dc backup list [<alias-or-server-id>]" -ForegroundColor Red
    Write-Host "Usage: .\blob.ps1 dc backup info <backupId>" -ForegroundColor Red
    Write-Host "Usage: .\blob.ps1 dc backup validate <backupId>" -ForegroundColor Red
    Write-Host "Usage: .\blob.ps1 dc backup delete <backupId>" -ForegroundColor Red
    Write-Host "Valid categories: roles, members, guild, channels, all" -ForegroundColor DarkYellow
    Write-Host "Default create selection: roles, guild, channels" -ForegroundColor DarkYellow
    Write-Host "Member restore requires both '--include members' and '--force-members'." -ForegroundColor DarkYellow
    Write-Host "Backup deletion requires typing the exact backup id before any files are removed." -ForegroundColor DarkYellow
}

function Invoke-DcBackupCommand {
    param(
        [string[]]$Arguments
    )

    if ($Arguments.Count -lt 1) {
        Write-DcBackupUsage
        exit 1
    }

    $backupCommand = [string]$Arguments[0]
    $backupArgs = @()
    if ($Arguments.Count -gt 1) {
        $backupArgs = @($Arguments[1..($Arguments.Count - 1)])
    }

    try {
        $parsed = & $Cmd.ParseDiscordBackupArguments -Arguments $backupArgs
    }
    catch {
        Write-Host $_.Exception.Message -ForegroundColor Red
        Write-DcBackupUsage
        exit 1
    }

    switch ($backupCommand) {
        "create" {
            Test-DcConfiguration -RequireAuth
            & $Cmd.WriteWarnings -State $state

            if ($parsed.Positionals.Count -ne 1) {
                Write-DcBackupUsage
                exit 1
            }

            $authContext = Get-DcAuthContext
            try {
                $result = & $Cmd.InvokeDiscordBackupCreate -Config $config -BaseUrl $baseUrl -ApiVersion $apiVersion -Headers $authContext.Headers -TimeoutSeconds $timeoutSeconds -ServerReference ([string]$parsed.Positionals[0]) -Description ([string]$parsed.Description) -IncludeValues $parsed.Include -ExcludeValues $parsed.Exclude -ServerMap $serverMap -JoinApiUri $Cmd.JoinApiUri -InvokeNetworkRequest $Cmd.InvokeNetworkRequest
            }
            catch {
                Write-Host $_.Exception.Message -ForegroundColor Red
                exit 1
            }

            Write-Host "Discord backup created." -ForegroundColor Green
            Write-Host "BackupId: $($result.BackupId)"
            Write-Host "Path: $($result.BackupPath)"
            Write-Host "Server: $($result.Manifest.guildName) | $($result.Manifest.guildId)"
            if (-not [string]::IsNullOrWhiteSpace([string]$result.Manifest.guildAlias)) {
                Write-Host "Alias: $($result.Manifest.guildAlias)"
            }
            Write-Host "Sections: $([string]::Join(', ', @($result.Manifest.includedSections)))"
            foreach ($entry in $result.Counts.GetEnumerator() | Sort-Object Key) {
                Write-Host ("{0}: {1}" -f $entry.Key, $entry.Value)
            }
            exit 0
        }

        "restore" {
            Test-DcConfiguration -RequireAuth
            & $Cmd.WriteWarnings -State $state

            if ($parsed.Positionals.Count -ne 2) {
                Write-DcBackupUsage
                exit 1
            }

            $authContext = Get-DcAuthContext
            try {
                $result = & $Cmd.InvokeDiscordBackupRestore -Config $config -BaseUrl $baseUrl -ApiVersion $apiVersion -Headers $authContext.Headers -TimeoutSeconds $timeoutSeconds -MessagesRootPath $messagesRootPath -ServerReference ([string]$parsed.Positionals[0]) -BackupId ([string]$parsed.Positionals[1]) -IncludeValues $parsed.Include -ExcludeValues $parsed.Exclude -ForceMembers:([bool]$parsed.ForceMembers) -ServerMap $serverMap -JoinApiUri $Cmd.JoinApiUri -InvokeNetworkRequest $Cmd.InvokeNetworkRequest
            }
            catch {
                Write-Host $_.Exception.Message -ForegroundColor Red
                exit 1
            }

            Write-Host "Discord backup restore completed." -ForegroundColor Green
            Write-Host "BackupId: $($result.backupId)"
            Write-Host "Target: $($result.targetGuildName) | $($result.targetGuildId)"
            Write-Host "Sections: $([string]::Join(', ', @($result.selectedSections)))"
            foreach ($step in @($result.steps)) {
                Write-Host ("Section '{0}' restored." -f [string]$step.section)
                $stepResult = $step.result
                $hasKey = {
                    param($obj, [string]$key)
                    if ($null -eq $obj -or [string]::IsNullOrWhiteSpace($key)) {
                        return $false
                    }
                    if ($obj -is [System.Collections.IDictionary]) {
                        return $obj.Contains($key)
                    }
                    return ($obj.PSObject.Properties.Name -contains $key)
                }

                if ((& $hasKey $stepResult "Skipped") -and [bool]$stepResult.Skipped) {
                    Write-Host "  Skipped due to insufficient permissions." -ForegroundColor DarkYellow
                }
                if (& $hasKey $stepResult "Actions") {
                    foreach ($action in @($stepResult.Actions)) {
                        Write-Host ("- {0}" -f [string]$action)
                    }
                }
                if (& $hasKey $stepResult "Warnings") {
                    foreach ($warning in @($stepResult.Warnings)) {
                        if (-not [string]::IsNullOrWhiteSpace([string]$warning)) {
                            Write-Host ("! {0}" -f [string]$warning) -ForegroundColor DarkYellow
                        }
                    }
                }
                if (& $hasKey $stepResult "Enforcement") {
                    foreach ($entry in @($stepResult.Enforcement)) {
                        Write-Host ("- Removed out-of-scope member {0}; DM success={1}" -f [string]$entry.userId, [bool]$entry.dmSucceeded)
                    }
                }
            }
            exit 0
        }

        "list" {
            Test-DcConfiguration
            & $Cmd.WriteWarnings -State $state

            if ($parsed.Positionals.Count -gt 1) {
                Write-DcBackupUsage
                exit 1
            }

            try {
                $backups = & $Cmd.GetDiscordBackupList -Config $config -ServerReference $(if ($parsed.Positionals.Count -eq 1) { [string]$parsed.Positionals[0] } else { $null }) -ServerMap $serverMap
            }
            catch {
                Write-Host $_.Exception.Message -ForegroundColor Red
                exit 1
            }

            if ($backups.Count -eq 0) {
                Write-Host "No Discord backups found."
                exit 0
            }

            Write-Host "Discord backups:"
            foreach ($backup in $backups) {
                $sectionText = [string]::Join(", ", @($backup.includedSections))
                if (-not [string]::IsNullOrWhiteSpace([string]$backup.guildAlias)) {
                    Write-Host ("- {0} | {1} | {2} | alias={3} | sections={4}" -f $backup.backupId, $backup.createdAtUtc, $backup.guildName, $backup.guildAlias, $sectionText)
                }
                else {
                    Write-Host ("- {0} | {1} | {2} | sections={3}" -f $backup.backupId, $backup.createdAtUtc, $backup.guildName, $sectionText)
                }
            }
            exit 0
        }

        "info" {
            Test-DcConfiguration
            & $Cmd.WriteWarnings -State $state

            if ($parsed.Positionals.Count -ne 1) {
                Write-DcBackupUsage
                exit 1
            }

            try {
                $info = & $Cmd.GetDiscordBackupInfo -Config $config -BackupId ([string]$parsed.Positionals[0])
            }
            catch {
                Write-Host $_.Exception.Message -ForegroundColor Red
                exit 1
            }

            Write-Host $info.InfoText
            Write-Host "Path: $($info.Paths.BackupPath)"
            exit 0
        }

        "validate" {
            Test-DcConfiguration
            & $Cmd.WriteWarnings -State $state

            if ($parsed.Positionals.Count -ne 1) {
                Write-DcBackupUsage
                exit 1
            }

            $validation = & $Cmd.TestDiscordBackup -Config $config -BackupId ([string]$parsed.Positionals[0])
            if ($validation.IsValid) {
                Write-Host "Backup is valid." -ForegroundColor Green
                Write-Host "BackupId: $($validation.Manifest.backupId)"
                Write-Host "Sections: $([string]::Join(', ', @($validation.Manifest.includedSections)))"
                exit 0
            }

            Write-Host "Backup validation failed." -ForegroundColor Red
            foreach ($error in @($validation.Errors)) {
                Write-Host ("- {0}" -f [string]$error) -ForegroundColor Red
            }
            exit 1
        }

        "delete" {
            Test-DcConfiguration
            & $Cmd.WriteWarnings -State $state

            if ($parsed.Positionals.Count -ne 1) {
                Write-DcBackupUsage
                exit 1
            }

            $backupId = [string]$parsed.Positionals[0]

            try {
                $info = & $Cmd.GetDiscordBackupInfo -Config $config -BackupId $backupId
            }
            catch {
                Write-Host $_.Exception.Message -ForegroundColor Red
                exit 1
            }

            Write-Host "Backup deletion confirmation required." -ForegroundColor Yellow
            Write-Host $info.InfoText
            Write-Host "Path: $($info.Paths.BackupPath)"

            $confirmation = Read-Host "Type the exact backup id to continue"
            if ([string]$confirmation -ne $backupId) {
                Write-Host "Backup deletion confirmation failed. No changes were made." -ForegroundColor Red
                exit 1
            }

            try {
                $result = & $Cmd.RemoveDiscordBackup -Config $config -BackupId $backupId
            }
            catch {
                Write-Host $_.Exception.Message -ForegroundColor Red
                exit 1
            }

            Write-Host "Discord backup deleted." -ForegroundColor Green
            Write-Host "BackupId: $($result.BackupId)"
            Write-Host "Path: $($result.BackupPath)"
            exit 0
        }

        default {
            Write-Host "Unknown dc backup command: $backupCommand" -ForegroundColor Red
            Write-DcBackupUsage
            exit 1
        }
    }
}

function Test-DcServerMembership {
    param(
        [string]$Reference
    )

    Test-DcConfiguration -RequireAuth
    $resolution = Resolve-DcServerReference -Reference $Reference
    $authContext = Get-DcAuthContext

    $guildCheck = & $Cmd.TestDiscordGuildAccess -BaseUrl $baseUrl -ApiVersion $apiVersion -ServerId $resolution.ServerId -Headers $authContext.Headers -TimeoutSeconds $timeoutSeconds -JoinApiUri $Cmd.JoinApiUri -InvokeNetworkRequest $Cmd.InvokeNetworkRequest
    return @{
        Resolution = $resolution
        AuthContext = $authContext
        GuildCheck = $guildCheck
    }
}

function Sync-DcSlashCommands {
    param(
        [string]$ApplicationId,
        [hashtable]$AuthContext
    )

    if ([string]::IsNullOrWhiteSpace([string]$ApplicationId)) {
        Write-Host "Cannot register Discord slash commands because the application id is missing." -ForegroundColor Red
        exit 1
    }

    $guildList = & $Cmd.GetDiscordCurrentGuilds -BaseUrl $baseUrl -ApiVersion $apiVersion -Headers $AuthContext.Headers -TimeoutSeconds $timeoutSeconds -JoinApiUri $Cmd.JoinApiUri -InvokeNetworkRequest $Cmd.InvokeNetworkRequest
    if (-not $guildList.Success) {
        Write-Host $guildList.Error -ForegroundColor Red
        if ($null -ne $guildList.Result.StatusCode) {
            Write-Host "Status: $($guildList.Result.StatusCode) $($guildList.Result.StatusDescription)" -ForegroundColor Red
        }
        exit 1
    }

    foreach ($guild in @($guildList.Guilds)) {
        if ($null -eq $guild -or -not ($guild.PSObject.Properties.Name -contains "id")) {
            continue
        }

        $guildId = [string]$guild.id
        if ([string]::IsNullOrWhiteSpace($guildId)) {
            continue
        }

        $syncResult = & $Cmd.SyncDiscordGuildCommands -BaseUrl $baseUrl -ApiVersion $apiVersion -ApplicationId $ApplicationId -GuildId $guildId -Headers $AuthContext.Headers -TimeoutSeconds $timeoutSeconds
        if (-not $syncResult.Success) {
            $guildName = if ($guild.PSObject.Properties.Name -contains "name") { [string]$guild.name } else { $guildId }
            Write-Host "Failed to register slash commands for guild '$guildName'." -ForegroundColor Red
            if ($null -ne $syncResult.Result.StatusCode) {
                Write-Host "Status: $($syncResult.Result.StatusCode) $($syncResult.Result.StatusDescription)" -ForegroundColor Red
            }
            if (-not [string]::IsNullOrWhiteSpace([string]$syncResult.Result.RawContent)) {
                Write-Host "Response: $($syncResult.Result.RawContent)" -ForegroundColor DarkYellow
            }
            exit 1
        }
    }
}

switch ($Command) {
    "-p" {
        Test-DcConfiguration
        & $Cmd.WriteWarnings -State $state

        $requestUri = & $Cmd.JoinApiUri -BaseUrl $baseUrl -ApiVersion $apiVersion -Endpoint $testEndpoint
        Write-Host "Resolved API route: $requestUri"
        $restResult = & $Cmd.CommandPing -PackageName $PackageName -ApiName "Discord REST gateway endpoint" -Uri $requestUri -Headers $headers -TimeoutSeconds $timeoutSeconds

        Write-Host "Testing Discord Gateway websocket..."
        Write-Host "Resolved Gateway URL: $gatewayUrl"
        $gatewayResult = & $Cmd.TestDiscordGatewayConnection -GatewayUri $gatewayUrl -Headers @{ "User-Agent" = $headers["User-Agent"] } -TimeoutSeconds $startTimeoutSeconds

        if ($gatewayResult.Success) {
            Write-Host $gatewayResult.Message -ForegroundColor Green
        }
        else {
            Write-Host $gatewayResult.Message -ForegroundColor Red
        }

        & $Cmd.WriteWarnings -State $state
        if ($restResult.Success -and $gatewayResult.Success) {
            exit 0
        }

        exit 1
    }

    "-getAuth" {
        $result = & $Cmd.CommandGetKey -PackageName $PackageName -KeyFilePath $authKeyFilePath -KeyName $authKeyName -EnvVarName $authEnvVar -ExpectedFormatExample "your_discord_bot_token"
        if ($result.Success) {
            exit 0
        }

        exit 1
    }

    "-auth" {
        Test-DcConfiguration -RequireAuth
        & $Cmd.WriteWarnings -State $state

        $probe = Invoke-DcAuthProbe
        Write-Host "Resolved API route: $($probe.RequestUri)"
        Write-Host "Auth source: $($probe.AuthContext.Source)"

        if ($probe.Result.Success -and $null -ne $probe.Result.Data) {
            $displayName = & $Cmd.GetDiscordBotDisplayName -Data $probe.Result.Data
            Write-Host "Authenticated request successful." -ForegroundColor Green
            Write-Host "Status: $($probe.Result.StatusCode) $($probe.Result.StatusDescription)"
            if (-not [string]::IsNullOrWhiteSpace([string]$displayName)) {
                Write-Host "Identity: $displayName"
            }
            if ($probe.Result.Data.PSObject.Properties.Name -contains "id") {
                Write-Host "Bot user id: $($probe.Result.Data.id)"
            }
            & $Cmd.WriteWarnings -State $state
            exit 0
        }

        Write-Host "Authenticated request failed." -ForegroundColor Red
        if ($null -ne $probe.Result.StatusCode) {
            Write-Host "Status: $($probe.Result.StatusCode) $($probe.Result.StatusDescription)" -ForegroundColor Red
        }

        $authError = & $Cmd.GetDiscordAuthErrorMessage -Result $probe.Result
        if (-not [string]::IsNullOrWhiteSpace([string]$authError)) {
            Write-Host $authError -ForegroundColor Red
        }

        if (-not [string]::IsNullOrWhiteSpace([string]$probe.Result.RawContent)) {
            Write-Host "Response: $($probe.Result.RawContent)" -ForegroundColor DarkYellow
        }

        exit 1
    }

    "start" {
        Test-DcConfiguration -RequireAuth
        & $Cmd.WriteWarnings -State $state
        & $Cmd.InitializeDiscordRuntimeFiles -StatePath $statePath -LogPath $logPath -StopSignalPath $stopSignalPath

        $status = & $Cmd.GetDiscordManagedStatus -StatePath $statePath
        if ($status.ProcessExists) {
            $currentState = if ($status.State.ContainsKey("State")) { [string]$status.State["State"] } else { "unknown" }
            Write-Host "Discord bot is already running. Restarting it..." -ForegroundColor Yellow
            Write-Host "State: $currentState"
            if ($status.State.ContainsKey("Pid")) {
                Write-Host "PID: $($status.State["Pid"])"
            }

            $stopResult = & $Cmd.StopDiscordManagedProcess -StatePath $statePath -StopSignalPath $stopSignalPath -StopTimeoutSeconds $stopTimeoutSeconds
            if ($stopResult.ContainsKey("Error") -and -not [string]::IsNullOrWhiteSpace([string]$stopResult["Error"])) {
                Write-Host $stopResult["Error"] -ForegroundColor Red
                exit 1
            }

            if ($stopResult["WasForceStopped"]) {
                Write-Host "Previous Discord bot process did not stop cleanly before timeout and was force-stopped." -ForegroundColor Yellow
            }
            else {
                Write-Host "Previous Discord bot process stopped." -ForegroundColor Green
            }
        }

        & $Cmd.ClearDiscordStopSignal -StopSignalPath $stopSignalPath

        $probe = Invoke-DcAuthProbe
        if (-not $probe.Result.Success -or $null -eq $probe.Result.Data) {
            Write-Host "Discord token validation failed before startup." -ForegroundColor Red
            if ($null -ne $probe.Result.StatusCode) {
                Write-Host "Status: $($probe.Result.StatusCode) $($probe.Result.StatusDescription)" -ForegroundColor Red
            }
            $authError = & $Cmd.GetDiscordAuthErrorMessage -Result $probe.Result
            if (-not [string]::IsNullOrWhiteSpace([string]$authError)) {
                Write-Host $authError -ForegroundColor Red
            }
            if (-not [string]::IsNullOrWhiteSpace([string]$probe.Result.RawContent)) {
                Write-Host "Response: $($probe.Result.RawContent)" -ForegroundColor DarkYellow
            }
            exit 1
        }

        $displayName = & $Cmd.GetDiscordBotDisplayName -Data $probe.Result.Data
        $applicationId = [string]$probe.Result.Data.id
        Sync-DcSlashCommands -ApplicationId $applicationId -AuthContext $probe.AuthContext
        $gatewayResolution = Resolve-DcGatewayUri -AuthContext $probe.AuthContext
        $resolvedGatewayUrl = $gatewayResolution.GatewayUrl

        $initialState = @{
            State = "starting"
            StartedAt = & $Cmd.GetUtcTimestamp
            UpdatedAt = & $Cmd.GetUtcTimestamp
            Pid = $null
            BotUserId = [string]$probe.Result.Data.id
            BotDisplayName = if (-not [string]::IsNullOrWhiteSpace([string]$displayName)) { $displayName } else { $botDisplayName }
            SessionId = $null
            GatewayUrl = $resolvedGatewayUrl
            LastReadyAt = $null
            LastHeartbeatAt = $null
            LastHeartbeatAckAt = $null
            LastSessionAt = $null
            LastError = $null
            LastEvent = "STARTING"
            StoppedAt = $null
        }
        & $Cmd.SetDiscordRuntimeState -StatePath $statePath -State $initialState
        & $Cmd.WriteDiscordRuntimeLog -LogPath $logPath -Message "Starting Discord bot manager for '$($initialState.BotDisplayName)'."

        $powerShellPath = (Get-Command powershell.exe -ErrorAction Stop).Source
        $workerArgs = @(
            "-NoProfile",
            "-ExecutionPolicy", "Bypass",
            "-File", "`"$PSCommandPath`"",
            "__run"
        )

        $process = Start-Process -FilePath $powerShellPath -ArgumentList $workerArgs -WorkingDirectory (Get-Location).Path -WindowStyle Hidden -PassThru
        & $Cmd.UpdateDiscordRuntimeState -StatePath $statePath -Updates @{
            Pid = $process.Id
        } | Out-Null

        $deadline = (Get-Date).ToUniversalTime().AddSeconds($startTimeoutSeconds)
        do {
            Start-Sleep -Milliseconds 500
            $runtimeStatus = & $Cmd.GetDiscordManagedStatus -StatePath $statePath
            $runtimeState = $runtimeStatus.State
            $runtimeValue = if ($runtimeState.ContainsKey("State")) { [string]$runtimeState["State"] } else { $null }

            if ($runtimeValue -eq "connected") {
                Write-Host "Discord bot started successfully." -ForegroundColor Green
                Write-Host "PID: $($process.Id)"
                Write-Host "Identity: $($runtimeState["BotDisplayName"])"
                Write-Host "Gateway: $resolvedGatewayUrl"
                exit 0
            }

            if ($runtimeValue -eq "failed") {
                Write-Host "Discord bot failed during startup." -ForegroundColor Red
                if ($runtimeState.ContainsKey("LastError") -and -not [string]::IsNullOrWhiteSpace([string]$runtimeState["LastError"])) {
                    Write-Host "Error: $($runtimeState["LastError"])" -ForegroundColor Red
                }
                exit 1
            }
        }
        while ((Get-Date).ToUniversalTime() -lt $deadline)

        $finalStatus = & $Cmd.GetDiscordManagedStatus -StatePath $statePath
        if ($finalStatus.ProcessExists) {
            Write-Host "Discord bot process started. Gateway handshake is still in progress." -ForegroundColor Yellow
            Write-Host "PID: $($process.Id)"
            Write-Host "Run: .\blob.ps1 dc status"
            exit 0
        }

        Write-Host "Discord bot process exited before reaching a healthy state." -ForegroundColor Red
        if ($finalStatus.State.ContainsKey("LastError") -and -not [string]::IsNullOrWhiteSpace([string]$finalStatus.State["LastError"])) {
            Write-Host "Error: $($finalStatus.State["LastError"])" -ForegroundColor Red
        }
        exit 1
    }

    "status" {
        Test-DcConfiguration
        & $Cmd.WriteWarnings -State $state

        $status = & $Cmd.GetDiscordManagedStatus -StatePath $statePath
        $runtimeState = $status.State
        if ($runtimeState.Count -eq 0) {
            Write-Host "Discord bot has not been started yet."
            exit 0
        }

        $stateValue = if ($runtimeState.ContainsKey("State")) { [string]$runtimeState["State"] } else { "unknown" }
        if (-not $status.ProcessExists -and $stateValue -ne "stopped" -and $stateValue -ne "failed") {
            $stateValue = "failed"
            & $Cmd.UpdateDiscordRuntimeState -StatePath $statePath -Updates @{
                State = "failed"
                LastError = "Managed process is not running."
            } | Out-Null
            $runtimeState = & $Cmd.GetDiscordRuntimeState -StatePath $statePath
        }

        Write-Host "Discord bot status"
        Write-Host "Process running: $($status.ProcessExists)"
        if ($runtimeState.ContainsKey("Pid") -and $null -ne $runtimeState["Pid"]) {
            Write-Host "PID: $($runtimeState["Pid"])"
        }
        Write-Host "State: $stateValue"

        foreach ($propertyName in @("BotDisplayName", "BotUserId", "SessionId", "GatewayUrl", "StartedAt", "LastReadyAt", "LastHeartbeatAt", "LastHeartbeatAckAt", "LastSessionAt", "StoppedAt")) {
            if ($runtimeState.ContainsKey($propertyName) -and -not [string]::IsNullOrWhiteSpace([string]$runtimeState[$propertyName])) {
                Write-Host ("{0}: {1}" -f $propertyName, $runtimeState[$propertyName])
            }
        }

        if ($runtimeState.ContainsKey("LastError") -and -not [string]::IsNullOrWhiteSpace([string]$runtimeState["LastError"])) {
            Write-Host "LastError: $($runtimeState["LastError"])" -ForegroundColor Yellow
        }

        if ($serverMap.Count -gt 0) {
            Write-Host ("ConfiguredServers: {0}" -f $serverMap.Count)
        }

        exit 0
    }

    "servers" {
        Test-DcConfiguration
        & $Cmd.WriteWarnings -State $state

        if ($Rest.Count -lt 1) {
            Write-Host "Usage: .\blob.ps1 dc servers -g" -ForegroundColor Red
            Write-Host "Usage: .\blob.ps1 dc servers -l <alias-or-server-id>" -ForegroundColor Red
            if ($serverMap.Count -gt 0) {
                & $Cmd.WriteDiscordServerMap -ServerMap $serverMap
            }
            exit 1
        }

        $serverCommand = [string]$Rest[0]
        switch ($serverCommand) {
            "-g" {
                Test-DcConfiguration -RequireAuth
                $authContext = Get-DcAuthContext
                $guildList = & $Cmd.GetDiscordCurrentGuilds -BaseUrl $baseUrl -ApiVersion $apiVersion -Headers $authContext.Headers -TimeoutSeconds $timeoutSeconds -JoinApiUri $Cmd.JoinApiUri -InvokeNetworkRequest $Cmd.InvokeNetworkRequest

                Write-Host "Resolved API route: $($guildList.RequestUri)"
                Write-Host "Auth source: $($authContext.Source)"

                if (-not $guildList.Success) {
                    Write-Host $guildList.Error -ForegroundColor Red
                    if ($null -ne $guildList.Result.StatusCode) {
                        Write-Host "Status: $($guildList.Result.StatusCode) $($guildList.Result.StatusDescription)" -ForegroundColor Red
                    }
                    if (-not [string]::IsNullOrWhiteSpace([string]$guildList.Result.RawContent)) {
                        Write-Host "Response: $($guildList.Result.RawContent)" -ForegroundColor DarkYellow
                    }
                    exit 1
                }

                if ($guildList.Guilds.Count -eq 0) {
                    Write-Host "The bot is not in any Discord servers."
                    exit 0
                }

                Write-Host "Discord servers for the current bot:"
                foreach ($guild in ($guildList.Guilds | Sort-Object name, id)) {
                    $guildName = & $Cmd.GetDiscordGuildDisplayName -Guild $guild
                    $guildId = if ($guild.PSObject.Properties.Name -contains "id") { [string]$guild.id } else { $null }
                    $alias = if (-not [string]::IsNullOrWhiteSpace([string]$guildId)) { & $Cmd.GetDiscordServerAliasById -ServerId $guildId -ServerMap $serverMap } else { $null }

                    if (-not [string]::IsNullOrWhiteSpace([string]$alias)) {
                        Write-Host ("- {0} | {1} | alias={2}" -f $guildName, $guildId, $alias)
                    }
                    else {
                        Write-Host ("- {0} | {1}" -f $guildName, $guildId)
                    }
                }

                exit 0
            }
            "-l" {
                if ($Rest.Count -lt 2) {
                    Write-Host "Usage: .\blob.ps1 dc servers -l <alias-or-server-id>" -ForegroundColor Red
                    exit 1
                }

                $serverCheck = Test-DcServerMembership -Reference ([string]$Rest[1])
                $resolution = $serverCheck.Resolution
                $guildCheck = $serverCheck.GuildCheck

                if (-not $guildCheck.Success) {
                    Write-Host $guildCheck.Error -ForegroundColor Red
                    if ($null -ne $guildCheck.Result.StatusCode) {
                        Write-Host "Status: $($guildCheck.Result.StatusCode) $($guildCheck.Result.StatusDescription)" -ForegroundColor Red
                    }
                    if (-not [string]::IsNullOrWhiteSpace([string]$guildCheck.Result.RawContent)) {
                        Write-Host "Response: $($guildCheck.Result.RawContent)" -ForegroundColor DarkYellow
                    }
                    exit 1
                }

                $leaveResult = & $Cmd.LeaveDiscordGuild -BaseUrl $baseUrl -ApiVersion $apiVersion -ServerId $resolution.ServerId -Headers $serverCheck.AuthContext.Headers -TimeoutSeconds $timeoutSeconds -JoinApiUri $Cmd.JoinApiUri -InvokeNetworkRequest $Cmd.InvokeNetworkRequest
                Write-Host "Resolved API route: $($leaveResult.RequestUri)"
                Write-Host "Auth source: $($serverCheck.AuthContext.Source)"

                if (-not $leaveResult.Success) {
                    Write-Host $leaveResult.Error -ForegroundColor Red
                    if ($null -ne $leaveResult.Result.StatusCode) {
                        Write-Host "Status: $($leaveResult.Result.StatusCode) $($leaveResult.Result.StatusDescription)" -ForegroundColor Red
                    }
                    if (-not [string]::IsNullOrWhiteSpace([string]$leaveResult.Result.RawContent)) {
                        Write-Host "Response: $($leaveResult.Result.RawContent)" -ForegroundColor DarkYellow
                    }
                    exit 1
                }

                $guildName = & $Cmd.GetDiscordGuildDisplayName -Guild $guildCheck.Guild
                Write-Host "Bot left the Discord server." -ForegroundColor Green
                if (-not [string]::IsNullOrWhiteSpace([string]$resolution.Alias)) {
                    Write-Host "Alias: $($resolution.Alias)"
                }
                if (-not [string]::IsNullOrWhiteSpace([string]$guildName)) {
                    Write-Host "Server: $guildName"
                }
                Write-Host "ServerId: $($resolution.ServerId)"
                exit 0
            }
            default {
                Write-Host "Unknown dc servers command: $serverCommand" -ForegroundColor Red
                Write-Host "Run: .\blob.ps1 dc -?"
                exit 1
            }
        }
    }

    "resolveServer" {
        Test-DcConfiguration
        & $Cmd.WriteWarnings -State $state

        if ($Rest.Count -lt 1) {
            Write-Host "Usage: .\blob.ps1 dc resolveServer <alias-or-server-id>" -ForegroundColor Red
            exit 1
        }

        $resolution = & $Cmd.ResolveDiscordServerReference -Reference ([string]$Rest[0]) -ServerMap $serverMap
        if (-not $resolution.Success) {
            Write-Host $resolution.Error -ForegroundColor Red
            exit 1
        }

        Write-Host "Resolved Discord server reference."
        Write-Host "Input: $($resolution.Input)"
        if (-not [string]::IsNullOrWhiteSpace([string]$resolution.Alias)) {
            Write-Host "Alias: $($resolution.Alias)"
        }
        Write-Host "ServerId: $($resolution.ServerId)"
        Write-Host "Source: $($resolution.Source)"
        exit 0
    }

    "checkServer" {
        Test-DcConfiguration
        & $Cmd.WriteWarnings -State $state

        if ($Rest.Count -lt 1) {
            Write-Host "Usage: .\blob.ps1 dc checkServer <alias-or-server-id>" -ForegroundColor Red
            exit 1
        }

        $serverCheck = Test-DcServerMembership -Reference ([string]$Rest[0])
        $resolution = $serverCheck.Resolution
        $guildCheck = $serverCheck.GuildCheck

        Write-Host "Resolved Discord server reference."
        Write-Host "Input: $($resolution.Input)"
        if (-not [string]::IsNullOrWhiteSpace([string]$resolution.Alias)) {
            Write-Host "Alias: $($resolution.Alias)"
        }
        Write-Host "ServerId: $($resolution.ServerId)"
        Write-Host "Source: $($resolution.Source)"
        Write-Host "Resolved API route: $($guildCheck.RequestUri)"
        Write-Host "Auth source: $($serverCheck.AuthContext.Source)"

        if ($guildCheck.Success) {
            $guildName = & $Cmd.GetDiscordGuildDisplayName -Guild $guildCheck.Guild
            Write-Host "Bot has access to the server." -ForegroundColor Green
            if (-not [string]::IsNullOrWhiteSpace([string]$guildName)) {
                Write-Host "Server: $guildName"
            }
            if ($guildCheck.Guild.PSObject.Properties.Name -contains "id") {
                Write-Host "Guild id: $($guildCheck.Guild.id)"
            }
            exit 0
        }

        Write-Host $guildCheck.Error -ForegroundColor Red
        if ($null -ne $guildCheck.Result.StatusCode) {
            Write-Host "Status: $($guildCheck.Result.StatusCode) $($guildCheck.Result.StatusDescription)" -ForegroundColor Red
        }
        if (-not [string]::IsNullOrWhiteSpace([string]$guildCheck.Result.RawContent)) {
            Write-Host "Response: $($guildCheck.Result.RawContent)" -ForegroundColor DarkYellow
        }
        exit 1
    }

    "backup" {
        Invoke-DcBackupCommand -Arguments $Rest
    }

    "stop" {
        Test-DcConfiguration
        & $Cmd.WriteWarnings -State $state

        $stopResult = & $Cmd.StopDiscordManagedProcess -StatePath $statePath -StopSignalPath $stopSignalPath -StopTimeoutSeconds $stopTimeoutSeconds
        if ($stopResult.ContainsKey("Error") -and -not [string]::IsNullOrWhiteSpace([string]$stopResult["Error"])) {
            Write-Host $stopResult["Error"] -ForegroundColor Red
            exit 1
        }

        if (-not $stopResult["WasRunning"]) {
            Write-Host "Discord bot is not running."
            exit 0
        }

        if ($stopResult["WasForceStopped"]) {
            Write-Host "Discord bot did not stop cleanly before timeout and was force-stopped." -ForegroundColor Yellow
            exit 0
        }

        Write-Host "Discord bot stopped."
        exit 0
    }

    "__run" {
        Test-DcConfiguration -RequireAuth
        & $Cmd.InitializeDiscordRuntimeFiles -StatePath $statePath -LogPath $logPath -StopSignalPath $stopSignalPath

        $probe = Invoke-DcAuthProbe
        if (-not $probe.Result.Success -or $null -eq $probe.Result.Data) {
            $authError = & $Cmd.GetDiscordAuthErrorMessage -Result $probe.Result
            & $Cmd.UpdateDiscordRuntimeState -StatePath $statePath -Updates @{
                State = "failed"
                LastError = $authError
                StoppedAt = & $Cmd.GetUtcTimestamp
            } | Out-Null
            & $Cmd.WriteDiscordRuntimeLog -LogPath $logPath -Message "Startup auth failed: $authError"
            exit 1
        }

        $gatewayResolution = Resolve-DcGatewayUri -AuthContext $probe.AuthContext
        $resolvedGatewayUrl = $gatewayResolution.GatewayUrl
        if (-not $gatewayResolution.Success) {
            & $Cmd.WriteDiscordRuntimeLog -LogPath $logPath -Message "Gateway bot lookup failed. Falling back to configured gateway URL."
        }

        $applicationId = [string]$probe.Result.Data.id
        Sync-DcSlashCommands -ApplicationId $applicationId -AuthContext $probe.AuthContext

        $identity = @{
            id = [string]$probe.Result.Data.id
            username = [string]$probe.Result.Data.username
            global_name = if ($probe.Result.Data.PSObject.Properties.Name -contains "global_name") { [string]$probe.Result.Data.global_name } else { $null }
            discriminator = if ($probe.Result.Data.PSObject.Properties.Name -contains "discriminator") { [string]$probe.Result.Data.discriminator } else { $null }
        }

        & $Cmd.InvokeDiscordGatewayWorker -GatewayUri $resolvedGatewayUrl -GatewayHeaders @{ "User-Agent" = $headers["User-Agent"] } -StatePath $statePath -LogPath $logPath -StopSignalPath $stopSignalPath -Token $probe.AuthContext.Token -Intents ([long]$intentsValue) -PresenceText $presenceText -BotDisplayName $botDisplayName -Identity $identity -DiscordBaseUrl $baseUrl -DiscordApiVersion $apiVersion -DiscordHeaders $probe.AuthContext.Headers -Prefixes $effectivePrefixes -MessagesRootPath $messagesRootPath -ModrinthConfigPath $modrinthConfigPath -ReconnectInitialDelaySeconds $reconnectInitialDelaySeconds -ReconnectMaxDelaySeconds $reconnectMaxDelaySeconds -ReceivePollMilliseconds $receivePollMilliseconds
        exit 0
    }

    "-v" {
        & $Cmd.CommandVersion -PackageName $PackageName -Version $Version -ConfigVersion $versionStatus.ConfigVersion -WarningMessage $versionStatus.WarningMessage
        exit 0
    }

    "-?" {
        & $Cmd.WriteWarnings -State $state
        & $Cmd.CommandHelp -PackageName $PackageName -Commands @("dc -p", "dc -auth", "dc -getAuth", "dc start", "dc status", "dc stop", "dc servers -g", "dc servers -l <alias-or-server-id>", "dc resolveServer <alias-or-server-id>", "dc checkServer <alias-or-server-id>", "dc backup create <alias-or-server-id> [--description ""...""] [--include <categories...>] [--exclude <categories...>]", "dc backup restore <alias-or-server-id> <backupId> [--include <categories...>] [--exclude <categories...>] [--force-members]", "dc backup list [<alias-or-server-id>]", "dc backup info <backupId>", "dc backup validate <backupId>", "dc backup delete <backupId>", "dc -v", "dc -?")
        Write-Host "Auth key file: $authKeyFile"
        Write-Host "Auth key format: $authKeyName=<your_discord_bot_token>"
        Write-Host "Automation env var: $authEnvVar"
        Write-Host "Runtime state path: $statePathValue"
        Write-Host "Runtime log path: $logPathValue"
        Write-Host "Message templates path: $messagesRootPathValue"
        Write-Host "Backup root path: $backupRootPathValue"
        Write-Host ("Effective prefixes: {0}" -f ([string]::Join(", ", $effectivePrefixes)))
        Write-Host "Server aliases live under: servers"
        Write-Host "Backup categories: roles, members, guild, channels, all"
        Write-Host "Backup delete safety: type the exact backup id to confirm deletion"
        exit 0
    }

    default {
        & $Cmd.WriteWarnings -State $state
        Write-Host "Unknown dc command: $Command"
        Write-Host "Run: .\blob.ps1 dc -?"
        exit 1
    }
}
