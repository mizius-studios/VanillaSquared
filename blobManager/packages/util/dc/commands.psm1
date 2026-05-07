$configApiModule = Import-Module (Join-Path $PSScriptRoot "..\api\configAPI.psm1") -Force -DisableNameChecking -PassThru
$fileApiModule = Import-Module (Join-Path $PSScriptRoot "..\api\fileAPI.psm1") -Force -DisableNameChecking -PassThru
$networkApiModule = Import-Module (Join-Path $PSScriptRoot "..\api\networkAPI.psm1") -Force -DisableNameChecking -PassThru
$projectModule = Import-Module (Join-Path $PSScriptRoot "..\modrinth\project.psm1") -Force -DisableNameChecking -PassThru

$script:GetJsonConfigCommand = $configApiModule.ExportedCommands["Get-JsonConfig"]
$script:GetConfigValueCommand = $configApiModule.ExportedCommands["Get-ConfigValue"]
$script:ReadJsonFileCommand = $fileApiModule.ExportedCommands["Read-JsonFile"]
$script:ResolveLocalPathCommand = $fileApiModule.ExportedCommands["Resolve-LocalPath"]
$script:InvokeNetworkRequestCommand = $networkApiModule.ExportedCommands["Invoke-NetworkRequest"]
$script:JoinApiUriCommand = $networkApiModule.ExportedCommands["Join-ApiUri"]
$script:GetModrinthProjectErrorMessageCommand = $projectModule.ExportedCommands["Get-ModrinthProjectErrorMessage"]
$script:GetModrinthProjectSummaryLinesCommand = $projectModule.ExportedCommands["Get-ModrinthProjectSummaryLines"]

function Get-DiscordEffectivePrefixes {
    return @{
        Prefixes = @("/")
        IgnoredPrefixes = @()
        IgnoredCount = 0
    }
}

function Test-DiscordMessageContentIntentEnabled {
    param(
        [long]$Intents
    )

    return (($Intents -band 32768L) -eq 32768L)
}

function Get-DiscordMessageTemplates {
    param(
        [string]$MessagesRootPath
    )

    $resolvedRootPath = & $script:ResolveLocalPathCommand -PathValue $MessagesRootPath
    $commonPath = Join-Path $resolvedRootPath "common.json"
    $modrinthProjectPath = Join-Path $resolvedRootPath "modrinth.project.json"

    $commonFallback = @{
        usage = @{
            modrinthGetInfo = 'Usage: {{prefix}}modrinth getInfo <slug-or-id>'
        }
        errors = @{
            missingMessageContentIntent = 'This bot needs the MESSAGE_CONTENT intent enabled to read prefix commands in guild channels.'
            generic = 'Something went wrong.'
        }
    }

    $modrinthFallback = @{
        success = @{
            template = '{{summary}}'
        }
        errors = @{
            usage = 'Usage: {{prefix}}modrinth getInfo <slug-or-id>'
            notFound = 'Modrinth project not found: `{{project_ref}}`'
            apiFailure = '{{error_message}}'
        }
    }

    return @{
        RootPath = $resolvedRootPath
        Common = & $script:ReadJsonFileCommand -Path $commonPath -Fallback $commonFallback
        ModrinthProject = & $script:ReadJsonFileCommand -Path $modrinthProjectPath -Fallback $modrinthFallback
    }
}

function Format-DiscordTemplate {
    param(
        [string]$Template,
        [hashtable]$Values = @{}
    )

    if ([string]::IsNullOrWhiteSpace([string]$Template)) {
        return $Template
    }

    $formatted = [string]$Template
    foreach ($entry in $Values.GetEnumerator()) {
        $replacement = if ($null -eq $entry.Value) { "" } else { [string]$entry.Value }
        $formatted = $formatted.Replace(('{{{0}}}' -f [string]$entry.Key), $replacement)
    }

    return $formatted
}

function Get-DiscordPrefixCommand {
    param(
        [string]$Content,
        [string[]]$Prefixes
    )

    if ([string]::IsNullOrWhiteSpace([string]$Content)) {
        return $null
    }

    $orderedPrefixes = @($Prefixes | Sort-Object Length -Descending)
    foreach ($prefix in $orderedPrefixes) {
        if ([string]::IsNullOrWhiteSpace([string]$prefix)) {
            continue
        }

        if ($Content.StartsWith($prefix)) {
            return @{
                Prefix = $prefix
                Remainder = $Content.Substring($prefix.Length).Trim()
            }
        }
    }

    return $null
}

function Send-DiscordChannelMessage {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ChannelId,
        [string]$Content,
        [hashtable]$Headers = @{},
        [string]$ReplyToMessageId = $null,
        [int]$TimeoutSeconds = 30
    )

    $requestUri = & $script:JoinApiUriCommand -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/channels/$ChannelId/messages"
    $trimmedContent = [string]$Content
    if ($trimmedContent.Length -gt 2000) {
        $trimmedContent = $trimmedContent.Substring(0, 1997) + "..."
    }

    $body = @{
        content = $trimmedContent
        allowed_mentions = @{
            parse = @()
        }
    }

    if (-not [string]::IsNullOrWhiteSpace([string]$ReplyToMessageId)) {
        $body["message_reference"] = @{
            message_id = $ReplyToMessageId
        }
    }

    $result = & $script:InvokeNetworkRequestCommand -Uri $requestUri -Method "POST" -Headers $Headers -Body $body -TimeoutSeconds $TimeoutSeconds
    return @{
        RequestUri = $requestUri
        Result = $result
        Success = $result.Success
    }
}

function Send-DiscordInteractionResponse {
    param(
        [string]$BaseUrl,
        [string]$InteractionId,
        [string]$InteractionToken,
        [string]$Content,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30,
        [bool]$Ephemeral = $false
    )

    $requestUri = "$BaseUrl/interactions/$InteractionId/$InteractionToken/callback"
    $trimmedContent = [string]$Content
    if ($trimmedContent.Length -gt 2000) {
        $trimmedContent = $trimmedContent.Substring(0, 1997) + "..."
    }

    $flags = if ($Ephemeral) { 64 } else { 0 }
    $body = @{
        type = 4
        data = @{
            content = $trimmedContent
            flags = $flags
            allowed_mentions = @{
                parse = @()
            }
        }
    }

    $result = & $script:InvokeNetworkRequestCommand -Uri $requestUri -Method "POST" -Headers $Headers -Body $body -TimeoutSeconds $TimeoutSeconds
    return @{
        RequestUri = $requestUri
        Result = $result
        Success = $result.Success
    }
}

function Get-DiscordCommandTokenList {
    param(
        [string]$Input
    )

    if ([string]::IsNullOrWhiteSpace([string]$Input)) {
        return @()
    }

    return @([regex]::Split($Input.Trim(), '\s+') | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

function Get-ModrinthProjectForDiscord {
    param(
        [string]$ProjectRef,
        [string]$ModrinthConfigPath
    )

    $config = & $script:GetJsonConfigCommand -Path $ModrinthConfigPath -Fallback @{}
    $networkConfig = & $script:GetConfigValueCommand -Config $config -Key "network" -DefaultValue @{}
    $headers = & $script:GetConfigValueCommand -Config $networkConfig -Key "headers" -DefaultValue @{}
    $baseUrl = & $script:GetConfigValueCommand -Config $networkConfig -Key "baseUrl" -DefaultValue "https://api.modrinth.com"
    $apiVersion = & $script:GetConfigValueCommand -Config $networkConfig -Key "apiVersion" -DefaultValue "v2"
    $timeoutSeconds = [int](& $script:GetConfigValueCommand -Config $networkConfig -Key "timeoutSeconds" -DefaultValue 30)

    $requestUri = & $script:JoinApiUriCommand -BaseUrl $baseUrl -ApiVersion $apiVersion -Endpoint "/project/$ProjectRef"
    $result = & $script:InvokeNetworkRequestCommand -Uri $requestUri -Method "GET" -Headers $headers -TimeoutSeconds $timeoutSeconds

    return @{
        RequestUri = $requestUri
        Result = $result
        Project = $result.Data
        ErrorMessage = & $script:GetModrinthProjectErrorMessageCommand -Result $result -ProjectRef $ProjectRef
    }
}

function ConvertTo-ModrinthProjectDiscordMessage {
    param(
        $Project,
        [hashtable]$Templates
    )

    $summaryLines = @(& $script:GetModrinthProjectSummaryLinesCommand -Project $Project)
    $summaryText = [string]::Join("`n", $summaryLines)
    return Format-DiscordTemplate -Template ([string]$Templates.success.template) -Values @{
        summary = $summaryText
        title = [string]$Project.title
        slug = [string]$Project.slug
        id = [string]$Project.id
        project_type = [string]$Project.project_type
        description = [string]$Project.description
    }
}

function Get-DiscordSlashCommandDefinitions {
    $command = @{
        name = "modrinth"
        type = 1
        description = "Look up Modrinth project information"
        options = @(
            @{
                name = "getinfo"
                description = "Get information about a Modrinth project"
                type = 1
                options = @(
                    @{
                        name = "project"
                        description = "Modrinth project slug or id"
                        type = 3
                        required = $true
                    }
                )
            }
        )
    }

    return ,$command
}

function Get-DiscordCommandSyncComparableJson {
    param(
        $Commands
    )

    $items = @($Commands)
    $normalizedItems = @()

    foreach ($item in $items) {
        if ($null -eq $item) {
            continue
        }

        $normalizedItem = @{
            name = [string]$item.name
            type = [int]$item.type
            description = [string]$item.description
            options = @()
        }

        $options = @()
        if ($item.PSObject.Properties.Name -contains "options" -and $null -ne $item.options) {
            $options = @($item.options)
        }
        elseif ($item -is [hashtable] -and $item.ContainsKey("options") -and $null -ne $item["options"]) {
            $options = @($item["options"])
        }

        if ($options.Count -gt 0) {
            $normalizedOptions = @()
            foreach ($option in $options) {
                $normalizedOption = @{
                    name = [string]$option.name
                    type = [int]$option.type
                    description = [string]$option.description
                }

                if (($option.PSObject.Properties.Name -contains "required" -and $null -ne $option.required) -or ($option -is [hashtable] -and $option.ContainsKey("required"))) {
                    $normalizedOption["required"] = [bool]$option.required
                }

                $subOptions = @()
                if ($option.PSObject.Properties.Name -contains "options" -and $null -ne $option.options) {
                    $subOptions = @($option.options)
                }
                elseif ($option -is [hashtable] -and $option.ContainsKey("options") -and $null -ne $option["options"]) {
                    $subOptions = @($option["options"])
                }

                if ($subOptions.Count -gt 0) {
                    $normalizedSubOptions = @()
                    foreach ($subOption in $subOptions) {
                        $normalizedSubOptions += @{
                            name = [string]$subOption.name
                            type = [int]$subOption.type
                            description = [string]$subOption.description
                            required = [bool]$subOption.required
                        }
                    }

                    $normalizedOption["options"] = @($normalizedSubOptions | Sort-Object name, type)
                }

                $normalizedOptions += $normalizedOption
            }

            $normalizedItem["options"] = @($normalizedOptions | Sort-Object name, type)
        }

        $normalizedItems += $normalizedItem
    }

    return (($normalizedItems | Sort-Object name, type) | ConvertTo-Json -Depth 20 -Compress)
}

function Get-DiscordRetryAfterSeconds {
    param(
        [hashtable]$Result
    )

    if ($null -eq $Result -or [string]::IsNullOrWhiteSpace([string]$Result.RawContent)) {
        return $null
    }

    try {
        $parsed = $Result.RawContent | ConvertFrom-Json
        if ($null -ne $parsed -and $parsed.PSObject.Properties.Name -contains "retry_after") {
            return [double]$parsed.retry_after
        }
    }
    catch {
    }

    return $null
}

function Sync-DiscordGuildCommands {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ApplicationId,
        [string]$GuildId,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30
    )

    $requestUri = & $script:JoinApiUriCommand -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/applications/$ApplicationId/guilds/$GuildId/commands"
    $definitions = Get-DiscordSlashCommandDefinitions
    $bodyItems = @($definitions)
    $desiredComparableJson = Get-DiscordCommandSyncComparableJson -Commands $bodyItems

    $existingResult = & $script:InvokeNetworkRequestCommand -Uri $requestUri -Method "GET" -Headers $Headers -TimeoutSeconds $TimeoutSeconds
    if ($existingResult.Success) {
        $existingComparableJson = Get-DiscordCommandSyncComparableJson -Commands $existingResult.Data
        if ($existingComparableJson -eq $desiredComparableJson) {
            return @{
                RequestUri = $requestUri
                Result = $existingResult
                Success = $true
                Commands = $existingResult.Data
                Changed = $false
            }
        }
    }
    elseif ($existingResult.StatusCode -eq 429) {
        $retryAfterSeconds = Get-DiscordRetryAfterSeconds -Result $existingResult
        if ($null -ne $retryAfterSeconds -and $retryAfterSeconds -gt 0) {
            Start-Sleep -Milliseconds ([int][Math]::Ceiling($retryAfterSeconds * 1000))
            $existingResult = & $script:InvokeNetworkRequestCommand -Uri $requestUri -Method "GET" -Headers $Headers -TimeoutSeconds $TimeoutSeconds
            if ($existingResult.Success) {
                $existingComparableJson = Get-DiscordCommandSyncComparableJson -Commands $existingResult.Data
                if ($existingComparableJson -eq $desiredComparableJson) {
                    return @{
                        RequestUri = $requestUri
                        Result = $existingResult
                        Success = $true
                        Commands = $existingResult.Data
                        Changed = $false
                    }
                }
            }
        }
    }

    $jsonParts = @()
    foreach ($bodyItem in $bodyItems) {
        $jsonParts += (ConvertTo-Json -InputObject $bodyItem -Depth 20)
    }
    $jsonBody = "[" + ($jsonParts -join ",") + "]"
    $result = & $script:InvokeNetworkRequestCommand -Uri $requestUri -Method "PUT" -Headers $Headers -Body $jsonBody -TimeoutSeconds $TimeoutSeconds
    if (-not $result.Success -and $result.StatusCode -eq 429) {
        $retryAfterSeconds = Get-DiscordRetryAfterSeconds -Result $result
        if ($null -ne $retryAfterSeconds -and $retryAfterSeconds -gt 0) {
            Start-Sleep -Milliseconds ([int][Math]::Ceiling($retryAfterSeconds * 1000))
            $result = & $script:InvokeNetworkRequestCommand -Uri $requestUri -Method "PUT" -Headers $Headers -Body $jsonBody -TimeoutSeconds $TimeoutSeconds
        }
    }

    return @{
        RequestUri = $requestUri
        Result = $result
        Success = $result.Success
        Commands = $result.Data
        Changed = $result.Success
    }
}

function Invoke-DiscordPrefixCommand {
    param(
        $Message,
        [string[]]$Prefixes,
        [string]$DiscordBaseUrl,
        [string]$DiscordApiVersion,
        [hashtable]$DiscordHeaders,
        [string]$MessagesRootPath,
        [string]$ModrinthConfigPath,
        [int]$TimeoutSeconds = 30
    )

    if ($null -eq $Message) {
        return @{
            Handled = $false
            Reason = "No message payload."
        }
    }

    if ($Message.PSObject.Properties.Name -contains "author" -and $null -ne $Message.author) {
        if ($Message.author.PSObject.Properties.Name -contains "bot" -and [bool]$Message.author.bot) {
            return @{
                Handled = $false
                Reason = "Ignored bot message."
            }
        }
    }

    $content = if ($Message.PSObject.Properties.Name -contains "content") { [string]$Message.content } else { $null }
    $prefixMatch = Get-DiscordPrefixCommand -Content $content -Prefixes $Prefixes
    if ($null -eq $prefixMatch) {
        return @{
            Handled = $false
            Reason = "No configured prefix matched."
        }
    }

    $tokens = Get-DiscordCommandTokenList -Input $prefixMatch.Remainder
    if ($tokens.Count -eq 0) {
        return @{
            Handled = $false
            Reason = "Prefix used without command."
        }
    }

    $templates = Get-DiscordMessageTemplates -MessagesRootPath $MessagesRootPath
    $primaryCommand = [string]$tokens[0]
    $subCommand = if ($tokens.Count -gt 1) { [string]$tokens[1] } else { $null }
    $channelId = [string]$Message.channel_id
    $messageId = [string]$Message.id

    switch -Regex ($primaryCommand) {
        '^(modrinth|mr)$' {
            if ($subCommand -notin @("getInfo", "info")) {
                return @{
                    Handled = $false
                    Reason = "Unsupported modrinth command."
                }
            }

            if ($tokens.Count -lt 3) {
                $usageMessage = Format-DiscordTemplate -Template ([string]$templates.ModrinthProject.errors.usage) -Values @{
                    prefix = $prefixMatch.Prefix
                }

                $sendResult = Send-DiscordChannelMessage -BaseUrl $DiscordBaseUrl -ApiVersion $DiscordApiVersion -ChannelId $channelId -Content $usageMessage -Headers $DiscordHeaders -ReplyToMessageId $messageId -TimeoutSeconds $TimeoutSeconds
                return @{
                    Handled = $true
                    Success = $sendResult.Success
                    Reason = "Sent Modrinth usage message."
                    SendResult = $sendResult
                }
            }

            $projectRef = [string]$tokens[2]
            $projectLookup = Get-ModrinthProjectForDiscord -ProjectRef $projectRef -ModrinthConfigPath $ModrinthConfigPath
            if ($projectLookup.Result.Success -and $null -ne $projectLookup.Project) {
                $successMessage = ConvertTo-ModrinthProjectDiscordMessage -Project $projectLookup.Project -Templates $templates.ModrinthProject
                $sendResult = Send-DiscordChannelMessage -BaseUrl $DiscordBaseUrl -ApiVersion $DiscordApiVersion -ChannelId $channelId -Content $successMessage -Headers $DiscordHeaders -ReplyToMessageId $messageId -TimeoutSeconds $TimeoutSeconds
                return @{
                    Handled = $true
                    Success = $sendResult.Success
                    Reason = "Sent Modrinth project summary."
                    SendResult = $sendResult
                    Lookup = $projectLookup
                }
            }

            $errorTemplate = if ($projectLookup.Result.StatusCode -eq 404) {
                [string]$templates.ModrinthProject.errors.notFound
            }
            else {
                [string]$templates.ModrinthProject.errors.apiFailure
            }

            $errorMessage = Format-DiscordTemplate -Template $errorTemplate -Values @{
                project_ref = $projectRef
                error_message = $projectLookup.ErrorMessage
            }

            $sendResult = Send-DiscordChannelMessage -BaseUrl $DiscordBaseUrl -ApiVersion $DiscordApiVersion -ChannelId $channelId -Content $errorMessage -Headers $DiscordHeaders -ReplyToMessageId $messageId -TimeoutSeconds $TimeoutSeconds
            return @{
                Handled = $true
                Success = $sendResult.Success
                Reason = "Sent Modrinth error message."
                SendResult = $sendResult
                Lookup = $projectLookup
            }
        }
    }

    return @{
        Handled = $false
        Reason = "No supported Discord prefix command matched."
    }
}

function Get-DiscordInteractionOptionValue {
    param(
        $Options,
        [string]$Name
    )

    if ($null -eq $Options) {
        return $null
    }

    foreach ($option in @($Options)) {
        if ($null -ne $option -and [string]$option.name -eq $Name) {
            return $option.value
        }
    }

    return $null
}

function Invoke-DiscordInteractionCommand {
    param(
        $Interaction,
        [string]$DiscordBaseUrl,
        [hashtable]$DiscordHeaders,
        [string]$MessagesRootPath,
        [string]$ModrinthConfigPath,
        [int]$TimeoutSeconds = 30
    )

    if ($null -eq $Interaction) {
        return @{
            Handled = $false
            Reason = "No interaction payload."
        }
    }

    if ([int]$Interaction.type -ne 2) {
        return @{
            Handled = $false
            Reason = "Unsupported interaction type."
        }
    }

    $data = $Interaction.data
    if ($null -eq $data) {
        return @{
            Handled = $false
            Reason = "Interaction payload did not include command data."
        }
    }

    $commandName = [string]$data.name
    $templates = Get-DiscordMessageTemplates -MessagesRootPath $MessagesRootPath

    switch ($commandName) {
        "modrinth" {
            $subcommand = $null
            $projectRef = $null

            foreach ($option in @($data.options)) {
                if ($null -eq $option) {
                    continue
                }

                if ([int]$option.type -eq 1 -and [string]$option.name -eq "getinfo") {
                    $subcommand = "getinfo"
                    $projectRef = [string](Get-DiscordInteractionOptionValue -Options $option.options -Name "project")
                    break
                }
            }

            if ($subcommand -ne "getinfo" -or [string]::IsNullOrWhiteSpace($projectRef)) {
                $usageMessage = [string]$templates.ModrinthProject.errors.usage
                $sendResult = Send-DiscordInteractionResponse -BaseUrl $DiscordBaseUrl -InteractionId ([string]$Interaction.id) -InteractionToken ([string]$Interaction.token) -Content $usageMessage -Headers $DiscordHeaders -TimeoutSeconds $TimeoutSeconds -Ephemeral $true
                return @{
                    Handled = $true
                    Success = $sendResult.Success
                    Reason = "Sent Modrinth slash command usage response."
                    SendResult = $sendResult
                }
            }

            $projectLookup = Get-ModrinthProjectForDiscord -ProjectRef $projectRef -ModrinthConfigPath $ModrinthConfigPath
            if ($projectLookup.Result.Success -and $null -ne $projectLookup.Project) {
                $successMessage = ConvertTo-ModrinthProjectDiscordMessage -Project $projectLookup.Project -Templates $templates.ModrinthProject
                $sendResult = Send-DiscordInteractionResponse -BaseUrl $DiscordBaseUrl -InteractionId ([string]$Interaction.id) -InteractionToken ([string]$Interaction.token) -Content $successMessage -Headers $DiscordHeaders -TimeoutSeconds $TimeoutSeconds
                return @{
                    Handled = $true
                    Success = $sendResult.Success
                    Reason = "Sent Modrinth slash command response."
                    SendResult = $sendResult
                    Lookup = $projectLookup
                }
            }

            $errorTemplate = if ($projectLookup.Result.StatusCode -eq 404) {
                [string]$templates.ModrinthProject.errors.notFound
            }
            else {
                [string]$templates.ModrinthProject.errors.apiFailure
            }

            $errorMessage = Format-DiscordTemplate -Template $errorTemplate -Values @{
                project_ref = $projectRef
                error_message = $projectLookup.ErrorMessage
            }

            $sendResult = Send-DiscordInteractionResponse -BaseUrl $DiscordBaseUrl -InteractionId ([string]$Interaction.id) -InteractionToken ([string]$Interaction.token) -Content $errorMessage -Headers $DiscordHeaders -TimeoutSeconds $TimeoutSeconds -Ephemeral $true
            return @{
                Handled = $true
                Success = $sendResult.Success
                Reason = "Sent Modrinth slash command error response."
                SendResult = $sendResult
                Lookup = $projectLookup
            }
        }
    }

    return @{
        Handled = $false
        Reason = "No supported slash command matched."
    }
}

Export-ModuleMember -Function Get-DiscordEffectivePrefixes, Test-DiscordMessageContentIntentEnabled, Get-DiscordMessageTemplates, Format-DiscordTemplate, Get-DiscordPrefixCommand, Send-DiscordChannelMessage, Send-DiscordInteractionResponse, Get-ModrinthProjectForDiscord, ConvertTo-ModrinthProjectDiscordMessage, Get-DiscordSlashCommandDefinitions, Sync-DiscordGuildCommands, Invoke-DiscordPrefixCommand, Invoke-DiscordInteractionCommand
