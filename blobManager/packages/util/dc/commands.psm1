$configApiModule = Import-Module (Join-Path $PSScriptRoot "..\api\configAPI.psm1") -Force -DisableNameChecking -PassThru
$fileApiModule = Import-Module (Join-Path $PSScriptRoot "..\api\fileAPI.psm1") -Force -DisableNameChecking -PassThru
$networkApiModule = Import-Module (Join-Path $PSScriptRoot "..\api\networkAPI.psm1") -Force -DisableNameChecking -PassThru
$projectModule = Import-Module (Join-Path $PSScriptRoot "..\modrinth\project.psm1") -Force -DisableNameChecking -PassThru
$commonModule = Import-Module (Join-Path $PSScriptRoot "common.psm1") -Force -DisableNameChecking -PassThru

$script:GetJsonConfigCommand = $configApiModule.ExportedCommands["Get-JsonConfig"]
$script:GetConfigValueCommand = $configApiModule.ExportedCommands["Get-ConfigValue"]
$script:ReadJsonFileCommand = $fileApiModule.ExportedCommands["Read-JsonFile"]
$script:ResolveLocalPathCommand = $fileApiModule.ExportedCommands["Resolve-LocalPath"]
$script:InvokeNetworkRequestCommand = $networkApiModule.ExportedCommands["Invoke-NetworkRequest"]
$script:JoinApiUriCommand = $networkApiModule.ExportedCommands["Join-ApiUri"]
$script:GetModrinthProjectErrorMessageCommand = $projectModule.ExportedCommands["Get-ModrinthProjectErrorMessage"]
$script:GetModrinthProjectSummaryLinesCommand = $projectModule.ExportedCommands["Get-ModrinthProjectSummaryLines"]
$script:ConvertToDiscordDisplayNameCommand = $commonModule.ExportedCommands["ConvertTo-DiscordDisplayName"]

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

function Get-DiscordMessageConfigPath {
    param(
        [string]$MessagesRootPath,
        [string]$RelativePath
    )

    $resolvedRootPath = & $script:ResolveLocalPathCommand -PathValue $MessagesRootPath
    if ([string]::IsNullOrWhiteSpace([string]$resolvedRootPath) -or [string]::IsNullOrWhiteSpace([string]$RelativePath)) {
        return $null
    }

    return Join-Path $resolvedRootPath $RelativePath
}

function Get-DiscordMessageConfig {
    param(
        [string]$MessagesRootPath,
        [string]$RelativePath,
        [hashtable]$Fallback = @{}
    )

    $configPath = Get-DiscordMessageConfigPath -MessagesRootPath $MessagesRootPath -RelativePath $RelativePath
    return & $script:ReadJsonFileCommand -Path $configPath -Fallback $Fallback
}

function ConvertTo-DiscordTemplateValue {
    param(
        $Value
    )

    if ($null -eq $Value) {
        return ""
    }

    if ($Value -is [bool]) {
        return ([string]$Value).ToLowerInvariant()
    }

    if ($Value -is [datetime]) {
        return $Value.ToString("o")
    }

    if ($Value -is [string]) {
        return $Value
    }

    if ($Value -is [System.Collections.IDictionary]) {
        return ($Value | ConvertTo-Json -Depth 20 -Compress)
    }

    if ($Value -is [System.Collections.IEnumerable] -and $Value -isnot [string]) {
        $items = @($Value)
        if ($items.Count -eq 0) {
            return ""
        }

        $containsComplexItem = $false
        foreach ($item in $items) {
            if ($null -eq $item) {
                continue
            }

            if ($item -is [System.Collections.IDictionary]) {
                $containsComplexItem = $true
                break
            }

            if ($item -is [System.Collections.IEnumerable] -and $item -isnot [string]) {
                $containsComplexItem = $true
                break
            }

            if ($item -is [psobject] -and $item.PSObject.Properties.Count -gt 0 -and $item -isnot [string]) {
                $containsComplexItem = $true
                break
            }
        }

        if ($containsComplexItem) {
            return ($Value | ConvertTo-Json -Depth 20 -Compress)
        }

        $renderedItems = @()
        foreach ($item in $items) {
            $renderedItems += (ConvertTo-DiscordTemplateValue -Value $item)
        }
        return ($renderedItems -join ", ")
    }

    if ($Value -is [psobject] -and $Value.PSObject.Properties.Count -gt 0) {
        return ($Value | ConvertTo-Json -Depth 20 -Compress)
    }

    return [string]$Value
}

function Add-DiscordTemplateValuesFromObject {
    param(
        $Object,
        [hashtable]$Values,
        [string]$Prefix = "",
        [bool]$IncludeUnprefixed = $false
    )

    if ($null -eq $Object -or $null -eq $Values) {
        return
    }

    foreach ($property in $Object.PSObject.Properties) {
        $propertyName = [string]$property.Name
        $renderedValue = ConvertTo-DiscordTemplateValue -Value $property.Value

        if ($IncludeUnprefixed -and -not $Values.ContainsKey($propertyName)) {
            $Values[$propertyName] = $renderedValue
        }

        if (-not [string]::IsNullOrWhiteSpace([string]$Prefix)) {
            $Values["$Prefix$propertyName"] = $renderedValue
        }
    }
}

function Merge-DiscordTemplateValues {
    param(
        [hashtable]$BaseValues = @{},
        [hashtable]$AdditionalValues = @{}
    )

    $merged = @{}

    foreach ($entry in $BaseValues.GetEnumerator()) {
        $merged[[string]$entry.Key] = $entry.Value
    }

    foreach ($entry in $AdditionalValues.GetEnumerator()) {
        $merged[[string]$entry.Key] = $entry.Value
    }

    return $merged
}

function Format-DiscordTemplate {
    param(
        [string]$Template,
        [hashtable]$Values = @{}
    )

    if ($null -eq $Template) {
        return $null
    }

    return ([regex]::Replace([string]$Template, '{{\s*([a-zA-Z0-9_.-]+)\s*}}', {
        param($match)

        $key = [string]$match.Groups[1].Value
        if ($Values.ContainsKey($key)) {
            return [string]$Values[$key]
        }

        return ""
    }))
}

function Resolve-DiscordTemplateObject {
    param(
        $InputObject,
        [hashtable]$Values = @{}
    )

    if ($null -eq $InputObject) {
        return $null
    }

    if ($InputObject -is [string]) {
        return (Format-DiscordTemplate -Template $InputObject -Values $Values)
    }

    if ($InputObject -is [System.Collections.IDictionary]) {
        $resolved = @{}
        foreach ($entry in $InputObject.GetEnumerator()) {
            $resolved[[string]$entry.Key] = Resolve-DiscordTemplateObject -InputObject $entry.Value -Values $Values
        }
        return $resolved
    }

    if ($InputObject -is [System.Collections.IEnumerable] -and $InputObject -isnot [string]) {
        $items = New-Object System.Collections.ArrayList
        foreach ($item in $InputObject) {
            [void]$items.Add((Resolve-DiscordTemplateObject -InputObject $item -Values $Values))
        }
        return $items.ToArray()
    }

    if ($InputObject -is [psobject] -and $InputObject.PSObject.Properties.Count -gt 0) {
        $resolved = @{}
        foreach ($property in $InputObject.PSObject.Properties) {
            $resolved[[string]$property.Name] = Resolve-DiscordTemplateObject -InputObject $property.Value -Values $Values
        }
        return $resolved
    }

    return $InputObject
}

function Get-DiscordGenericFallbackMessageConfig {
    return @{
        content = "Something went wrong."
        embed = @{
            title = "Error"
            description = "The configured Discord response could not be rendered."
            color = 15158332
        }
    }
}

function ConvertTo-DiscordMessagePayload {
    param(
        [hashtable]$MessageConfig
    )

    if ($null -eq $MessageConfig) {
        return $null
    }

    $body = @{
        allowed_mentions = @{
            parse = @()
        }
    }

    $content = if ($MessageConfig.ContainsKey("content")) { [string]$MessageConfig.content } else { $null }
    if (-not [string]::IsNullOrWhiteSpace([string]$content)) {
        if ($content.Length -gt 2000) {
            $content = $content.Substring(0, 1997) + "..."
        }
        $body["content"] = $content
    }

    if ($MessageConfig.ContainsKey("embed") -and $null -ne $MessageConfig.embed) {
        $embed = $MessageConfig.embed
        if ($embed -is [System.Collections.IDictionary] -and $embed.Count -gt 0) {
            $body["embeds"] = @($embed)
        }
    }

    if (-not $body.ContainsKey("content") -and -not $body.ContainsKey("embeds")) {
        return $null
    }

    return $body
}

function Get-DiscordRenderedMessageConfig {
    param(
        [string]$MessagesRootPath,
        [string]$RelativePath,
        [hashtable]$Fallback,
        [hashtable]$TemplateValues = @{}
    )

    $config = Get-DiscordMessageConfig -MessagesRootPath $MessagesRootPath -RelativePath $RelativePath -Fallback $Fallback
    $rendered = Resolve-DiscordTemplateObject -InputObject $config -Values $TemplateValues
    if ($rendered -isnot [hashtable]) {
        return $Fallback
    }

    return $rendered
}

function Send-DiscordChannelMessage {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$ChannelId,
        [hashtable]$MessageConfig,
        [hashtable]$Headers = @{},
        [string]$ReplyToMessageId = $null,
        [int]$TimeoutSeconds = 30
    )

    $requestUri = & $script:JoinApiUriCommand -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/channels/$ChannelId/messages"
    $body = ConvertTo-DiscordMessagePayload -MessageConfig $MessageConfig
    if ($null -eq $body) {
        $body = ConvertTo-DiscordMessagePayload -MessageConfig (Get-DiscordGenericFallbackMessageConfig)
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
        [hashtable]$MessageConfig,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30,
        [bool]$Ephemeral = $false
    )

    $requestUri = "$BaseUrl/interactions/$InteractionId/$InteractionToken/callback"
    $body = ConvertTo-DiscordMessagePayload -MessageConfig $MessageConfig
    if ($null -eq $body) {
        $body = ConvertTo-DiscordMessagePayload -MessageConfig (Get-DiscordGenericFallbackMessageConfig)
    }

    $flags = if ($Ephemeral) { 64 } else { 0 }
    $body["flags"] = $flags

    $result = & $script:InvokeNetworkRequestCommand -Uri $requestUri -Method "POST" -Headers $Headers -Body @{
        type = 4
        data = $body
    } -TimeoutSeconds $TimeoutSeconds

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

function Get-DiscordUserFromInteraction {
    param(
        $Interaction
    )

    if ($null -eq $Interaction) {
        return $null
    }

    if ($Interaction.PSObject.Properties.Name -contains "member" -and $null -ne $Interaction.member) {
        if ($Interaction.member.PSObject.Properties.Name -contains "user" -and $null -ne $Interaction.member.user) {
            return $Interaction.member.user
        }
    }

    if ($Interaction.PSObject.Properties.Name -contains "user" -and $null -ne $Interaction.user) {
        return $Interaction.user
    }

    return $null
}

function Get-DiscordBaseTemplateValues {
    param(
        [string]$CommandName,
        [string]$SubcommandName = $null,
        [string]$Prefix = $null,
        [bool]$IsSlashCommand = $false,
        [string]$ChannelId = $null,
        [string]$GuildId = $null,
        [string]$MessageId = $null,
        [string]$InteractionId = $null,
        $User = $null
    )

    $displayName = & $script:ConvertToDiscordDisplayNameCommand -User $User
    $userId = if ($null -ne $User -and $User.PSObject.Properties.Name -contains "id") { [string]$User.id } else { $null }

    $values = @{
        request_command = if ([string]::IsNullOrWhiteSpace([string]$CommandName)) { "" } else { [string]$CommandName }
        request_subcommand = if ([string]::IsNullOrWhiteSpace([string]$SubcommandName)) { "" } else { [string]$SubcommandName }
        request_prefix = if ([string]::IsNullOrWhiteSpace([string]$Prefix)) { "" } else { [string]$Prefix }
        request_is_slash_command = ([string]$IsSlashCommand).ToLowerInvariant()
        request_channel_id = if ([string]::IsNullOrWhiteSpace([string]$ChannelId)) { "" } else { [string]$ChannelId }
        request_guild_id = if ([string]::IsNullOrWhiteSpace([string]$GuildId)) { "" } else { [string]$GuildId }
        request_message_id = if ([string]::IsNullOrWhiteSpace([string]$MessageId)) { "" } else { [string]$MessageId }
        request_interaction_id = if ([string]::IsNullOrWhiteSpace([string]$InteractionId)) { "" } else { [string]$InteractionId }
        request_user = if ([string]::IsNullOrWhiteSpace([string]$displayName)) { "" } else { [string]$displayName }
        request_user_id = if ([string]::IsNullOrWhiteSpace([string]$userId)) { "" } else { [string]$userId }
        request_user_mention = if ([string]::IsNullOrWhiteSpace([string]$userId)) { "" } else { "<@$userId>" }
        request_command_path = if ([string]::IsNullOrWhiteSpace([string]$SubcommandName)) { [string]$CommandName } else { "$CommandName $SubcommandName" }
    }

    Add-DiscordTemplateValuesFromObject -Object $User -Values $values -Prefix "request_user_" -IncludeUnprefixed:$false
    return $values
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

function Get-ModrinthProjectDiscordTemplateValues {
    param(
        $Project,
        [string]$ProjectRef = $null,
        [string]$ErrorMessage = $null,
        [hashtable]$BaseValues = @{}
    )

    $values = Merge-DiscordTemplateValues -BaseValues $BaseValues
    $summaryLines = @(& $script:GetModrinthProjectSummaryLinesCommand -Project $Project)
    $summaryText = [string]::Join("`n", $summaryLines)

    $values["summary"] = $summaryText
    $values["summary_lines"] = $summaryText
    $values["project_ref"] = if ([string]::IsNullOrWhiteSpace([string]$ProjectRef)) { "" } else { [string]$ProjectRef }
    $values["error_message"] = if ([string]::IsNullOrWhiteSpace([string]$ErrorMessage)) { "" } else { [string]$ErrorMessage }

    if ($null -ne $Project) {
        Add-DiscordTemplateValuesFromObject -Object $Project -Values $values -Prefix "project_" -IncludeUnprefixed:$true

        $projectType = if ($Project.PSObject.Properties.Name -contains "project_type") { [string]$Project.project_type } else { "" }
        $slug = if ($Project.PSObject.Properties.Name -contains "slug") { [string]$Project.slug } else { "" }
        $projectId = if ($Project.PSObject.Properties.Name -contains "id") { [string]$Project.id } else { "" }

        if (-not [string]::IsNullOrWhiteSpace($projectType) -and -not [string]::IsNullOrWhiteSpace($slug)) {
            $values["project_url"] = "https://modrinth.com/$projectType/$slug"
        }
        elseif (-not [string]::IsNullOrWhiteSpace($projectId)) {
            $values["project_url"] = "https://modrinth.com/project/$projectId"
        }
        else {
            $values["project_url"] = ""
        }
    }
    else {
        $values["project_url"] = ""
    }

    return $values
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

    $primaryCommand = [string]$tokens[0]
    $subCommand = if ($tokens.Count -gt 1) { [string]$tokens[1] } else { $null }
    $channelId = [string]$Message.channel_id
    $messageId = [string]$Message.id
    $guildId = if ($Message.PSObject.Properties.Name -contains "guild_id") { [string]$Message.guild_id } else { $null }

    switch -Regex ($primaryCommand) {
        '^(modrinth|mr)$' {
            $baseValues = Get-DiscordBaseTemplateValues -CommandName "modrinth" -SubcommandName "getInfo" -Prefix $prefixMatch.Prefix -IsSlashCommand:$false -ChannelId $channelId -GuildId $guildId -MessageId $messageId -User $Message.author

            if ($subCommand -notin @("getInfo", "info")) {
                return @{
                    Handled = $false
                    Reason = "Unsupported modrinth command."
                }
            }

            if ($tokens.Count -lt 3) {
                $usageMessage = Get-DiscordRenderedMessageConfig -MessagesRootPath $MessagesRootPath -RelativePath "modrinth\project\errors\usage.json" -Fallback @{
                    content = "Usage: {{request_prefix}}modrinth getInfo <slug-or-id>"
                    embed = @{
                        title = "Modrinth Project Lookup"
                        description = 'Usage: `{{request_prefix}}modrinth getInfo <slug-or-id>`'
                        color = 15844367
                    }
                } -TemplateValues $baseValues

                $sendResult = Send-DiscordChannelMessage -BaseUrl $DiscordBaseUrl -ApiVersion $DiscordApiVersion -ChannelId $channelId -MessageConfig $usageMessage -Headers $DiscordHeaders -ReplyToMessageId $messageId -TimeoutSeconds $TimeoutSeconds
                return @{
                    Handled = $true
                    Success = $sendResult.Success
                    Reason = "Sent Modrinth usage message."
                    SendResult = $sendResult
                }
            }

            $projectRef = [string]$tokens[2]
            $projectLookup = Get-ModrinthProjectForDiscord -ProjectRef $projectRef -ModrinthConfigPath $ModrinthConfigPath
            $templateValues = Get-ModrinthProjectDiscordTemplateValues -Project $projectLookup.Project -ProjectRef $projectRef -ErrorMessage $projectLookup.ErrorMessage -BaseValues $baseValues

            if ($projectLookup.Result.Success -and $null -ne $projectLookup.Project) {
                $successMessage = Get-DiscordRenderedMessageConfig -MessagesRootPath $MessagesRootPath -RelativePath "modrinth\project\success.json" -Fallback @{
                    content = "{{summary}}"
                    embed = @{
                        title = "{{title}}"
                        url = "{{project_url}}"
                        description = "{{description}}"
                        color = 3447003
                    }
                } -TemplateValues $templateValues

                $sendResult = Send-DiscordChannelMessage -BaseUrl $DiscordBaseUrl -ApiVersion $DiscordApiVersion -ChannelId $channelId -MessageConfig $successMessage -Headers $DiscordHeaders -ReplyToMessageId $messageId -TimeoutSeconds $TimeoutSeconds
                return @{
                    Handled = $true
                    Success = $sendResult.Success
                    Reason = "Sent Modrinth project summary."
                    SendResult = $sendResult
                    Lookup = $projectLookup
                }
            }

            $errorPath = if ($projectLookup.Result.StatusCode -eq 404) { "modrinth\project\errors\notFound.json" } else { "modrinth\project\errors\apiFailure.json" }
            $errorFallback = if ($projectLookup.Result.StatusCode -eq 404) {
                @{
                    content = 'Modrinth project not found: `{{project_ref}}`'
                    embed = @{
                        title = "Project Not Found"
                        description = 'No Modrinth project matched `{{project_ref}}`.'
                        color = 15158332
                    }
                }
            }
            else {
                @{
                    content = "{{error_message}}"
                    embed = @{
                        title = "Modrinth Request Failed"
                        description = "{{error_message}}"
                        color = 15158332
                    }
                }
            }

            $errorMessage = Get-DiscordRenderedMessageConfig -MessagesRootPath $MessagesRootPath -RelativePath $errorPath -Fallback $errorFallback -TemplateValues $templateValues
            $sendResult = Send-DiscordChannelMessage -BaseUrl $DiscordBaseUrl -ApiVersion $DiscordApiVersion -ChannelId $channelId -MessageConfig $errorMessage -Headers $DiscordHeaders -ReplyToMessageId $messageId -TimeoutSeconds $TimeoutSeconds
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

            $user = Get-DiscordUserFromInteraction -Interaction $Interaction
            $channelId = if ($Interaction.PSObject.Properties.Name -contains "channel_id") { [string]$Interaction.channel_id } else { $null }
            $guildId = if ($Interaction.PSObject.Properties.Name -contains "guild_id") { [string]$Interaction.guild_id } else { $null }
            $baseValues = Get-DiscordBaseTemplateValues -CommandName "modrinth" -SubcommandName "getInfo" -IsSlashCommand:$true -ChannelId $channelId -GuildId $guildId -InteractionId ([string]$Interaction.id) -User $user

            if ($subcommand -ne "getinfo" -or [string]::IsNullOrWhiteSpace($projectRef)) {
                $usageMessage = Get-DiscordRenderedMessageConfig -MessagesRootPath $MessagesRootPath -RelativePath "modrinth\project\errors\usage.json" -Fallback @{
                    content = "Usage: /modrinth getinfo project:<slug-or-id>"
                    embed = @{
                        title = "Modrinth Project Lookup"
                        description = "Provide a Modrinth project slug or id."
                        color = 15844367
                    }
                    ephemeral = $true
                } -TemplateValues $baseValues

                $sendResult = Send-DiscordInteractionResponse -BaseUrl $DiscordBaseUrl -InteractionId ([string]$Interaction.id) -InteractionToken ([string]$Interaction.token) -MessageConfig $usageMessage -Headers $DiscordHeaders -TimeoutSeconds $TimeoutSeconds -Ephemeral ([bool]($usageMessage.ephemeral))
                return @{
                    Handled = $true
                    Success = $sendResult.Success
                    Reason = "Sent Modrinth slash command usage response."
                    SendResult = $sendResult
                }
            }

            $projectLookup = Get-ModrinthProjectForDiscord -ProjectRef $projectRef -ModrinthConfigPath $ModrinthConfigPath
            $templateValues = Get-ModrinthProjectDiscordTemplateValues -Project $projectLookup.Project -ProjectRef $projectRef -ErrorMessage $projectLookup.ErrorMessage -BaseValues $baseValues

            if ($projectLookup.Result.Success -and $null -ne $projectLookup.Project) {
                $successMessage = Get-DiscordRenderedMessageConfig -MessagesRootPath $MessagesRootPath -RelativePath "modrinth\project\success.json" -Fallback @{
                    content = "{{summary}}"
                    embed = @{
                        title = "{{title}}"
                        url = "{{project_url}}"
                        description = "{{description}}"
                        color = 3447003
                    }
                } -TemplateValues $templateValues

                $sendResult = Send-DiscordInteractionResponse -BaseUrl $DiscordBaseUrl -InteractionId ([string]$Interaction.id) -InteractionToken ([string]$Interaction.token) -MessageConfig $successMessage -Headers $DiscordHeaders -TimeoutSeconds $TimeoutSeconds -Ephemeral ([bool]($successMessage.ephemeral))
                return @{
                    Handled = $true
                    Success = $sendResult.Success
                    Reason = "Sent Modrinth slash command response."
                    SendResult = $sendResult
                    Lookup = $projectLookup
                }
            }

            $errorPath = if ($projectLookup.Result.StatusCode -eq 404) { "modrinth\project\errors\notFound.json" } else { "modrinth\project\errors\apiFailure.json" }
            $errorFallback = if ($projectLookup.Result.StatusCode -eq 404) {
                @{
                    content = 'Modrinth project not found: `{{project_ref}}`'
                    embed = @{
                        title = "Project Not Found"
                        description = 'No Modrinth project matched `{{project_ref}}`.'
                        color = 15158332
                    }
                    ephemeral = $true
                }
            }
            else {
                @{
                    content = "{{error_message}}"
                    embed = @{
                        title = "Modrinth Request Failed"
                        description = "{{error_message}}"
                        color = 15158332
                    }
                    ephemeral = $true
                }
            }

            $errorMessage = Get-DiscordRenderedMessageConfig -MessagesRootPath $MessagesRootPath -RelativePath $errorPath -Fallback $errorFallback -TemplateValues $templateValues
            $sendResult = Send-DiscordInteractionResponse -BaseUrl $DiscordBaseUrl -InteractionId ([string]$Interaction.id) -InteractionToken ([string]$Interaction.token) -MessageConfig $errorMessage -Headers $DiscordHeaders -TimeoutSeconds $TimeoutSeconds -Ephemeral ([bool]($errorMessage.ephemeral))
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

Export-ModuleMember -Function Get-DiscordEffectivePrefixes, Test-DiscordMessageContentIntentEnabled, Get-DiscordMessageConfig, Format-DiscordTemplate, Resolve-DiscordTemplateObject, Get-DiscordPrefixCommand, Send-DiscordChannelMessage, Send-DiscordInteractionResponse, Get-DiscordBaseTemplateValues, Get-ModrinthProjectForDiscord, Get-ModrinthProjectDiscordTemplateValues, Get-DiscordSlashCommandDefinitions, Sync-DiscordGuildCommands, Invoke-DiscordPrefixCommand, Invoke-DiscordInteractionCommand
