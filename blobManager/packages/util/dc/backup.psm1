$fileApiModule = Import-Module (Join-Path $PSScriptRoot "..\api\fileAPI.psm1") -Force -DisableNameChecking -PassThru
$commonModule = Import-Module (Join-Path $PSScriptRoot "common.psm1") -Force -DisableNameChecking -PassThru
$serverModule = Import-Module (Join-Path $PSScriptRoot "server.psm1") -Force -DisableNameChecking -PassThru
$guildModule = Import-Module (Join-Path $PSScriptRoot "guild.psm1") -Force -DisableNameChecking -PassThru
$commandsModule = Import-Module (Join-Path $PSScriptRoot "commands.psm1") -Force -DisableNameChecking -PassThru

$script:File = @{
    ResolveLocalPath = $fileApiModule.ExportedCommands["Resolve-LocalPath"]
    EnsureDirectoryPath = $fileApiModule.ExportedCommands["Ensure-DirectoryPath"]
    ReadJsonFile = $fileApiModule.ExportedCommands["Read-JsonFile"]
    WriteJsonFile = $fileApiModule.ExportedCommands["Write-JsonFile"]
    ReadTextFile = $fileApiModule.ExportedCommands["Read-TextFile"]
    WriteTextFile = $fileApiModule.ExportedCommands["Write-TextFile"]
    RemoveDirectoryIfExists = $fileApiModule.ExportedCommands["Remove-DirectoryIfExists"]
}

$script:Common = @{
    GetUtcTimestamp = $commonModule.ExportedCommands["Get-UtcTimestamp"]
    ConvertToDiscordDisplayName = $commonModule.ExportedCommands["ConvertTo-DiscordDisplayName"]
}

$script:Server = @{
    ResolveDiscordServerReference = $serverModule.ExportedCommands["Resolve-DiscordServerReference"]
    GetDiscordServerAliasById = $serverModule.ExportedCommands["Get-DiscordServerAliasById"]
}

$script:Guild = @{
    GetDiscordGuild = $guildModule.ExportedCommands["Get-DiscordGuild"]
    UpdateDiscordGuild = $guildModule.ExportedCommands["Update-DiscordGuild"]
    GetDiscordGuildRoles = $guildModule.ExportedCommands["Get-DiscordGuildRoles"]
    NewDiscordGuildRole = $guildModule.ExportedCommands["New-DiscordGuildRole"]
    UpdateDiscordGuildRole = $guildModule.ExportedCommands["Update-DiscordGuildRole"]
    SetDiscordGuildRolePositions = $guildModule.ExportedCommands["Set-DiscordGuildRolePositions"]
    GetDiscordGuildChannels = $guildModule.ExportedCommands["Get-DiscordGuildChannels"]
    NewDiscordGuildChannel = $guildModule.ExportedCommands["New-DiscordGuildChannel"]
    UpdateDiscordChannel = $guildModule.ExportedCommands["Update-DiscordChannel"]
    SetDiscordGuildChannelPositions = $guildModule.ExportedCommands["Set-DiscordGuildChannelPositions"]
    GetDiscordGuildMembers = $guildModule.ExportedCommands["Get-DiscordGuildMembers"]
    UpdateDiscordGuildMember = $guildModule.ExportedCommands["Update-DiscordGuildMember"]
    RemoveDiscordGuildMember = $guildModule.ExportedCommands["Remove-DiscordGuildMember"]
    RemoveDiscordChannel = $guildModule.ExportedCommands["Remove-DiscordChannel"]
    RemoveDiscordGuildRole = $guildModule.ExportedCommands["Remove-DiscordGuildRole"]
    NewDiscordDmChannel = $guildModule.ExportedCommands["New-DiscordDmChannel"]
    SendDiscordChannelMessage = $guildModule.ExportedCommands["Send-DiscordChannelMessage"]
}

$script:Commands = @{
    GetDiscordMessageConfig = $commandsModule.ExportedCommands["Get-DiscordMessageConfig"]
    ResolveDiscordTemplateObject = $commandsModule.ExportedCommands["Resolve-DiscordTemplateObject"]
    SendDiscordChannelMessage = $commandsModule.ExportedCommands["Send-DiscordChannelMessage"]
}

$script:BackupSchema = "discord-guild-backup"
$script:BackupSchemaVersion = 1
$script:SelectionCategories = @("roles", "members", "guild", "channels")
$script:SelectionAliases = @{
    all = @("roles", "members", "guild", "channels")
}
$script:DefaultCreateSections = @("roles", "guild", "channels")
$script:SectionFiles = @{
    guild = "guild.json"
    roles = "roles.json"
    members = "members.json"
    channels = "channels.json"
}
$script:ChannelTypes = @{
    GuildText = 0
    Dm = 1
    GuildVoice = 2
    GroupDm = 3
    GuildCategory = 4
    GuildAnnouncement = 5
    AnnouncementThread = 10
    PublicThread = 11
    PrivateThread = 12
    GuildStageVoice = 13
    GuildDirectory = 14
    GuildForum = 15
    GuildMedia = 16
}

function Get-DiscordBackupOperatorIdentity {
    $windowsIdentity = $null
    try {
        $windowsIdentity = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
    }
    catch {
        $windowsIdentity = $null
    }

    if (-not [string]::IsNullOrWhiteSpace([string]$windowsIdentity)) {
        return $windowsIdentity
    }

    if (-not [string]::IsNullOrWhiteSpace([string]$env:USERNAME)) {
        if (-not [string]::IsNullOrWhiteSpace([string]$env:COMPUTERNAME)) {
            return "$($env:COMPUTERNAME)\$($env:USERNAME)"
        }

        return [string]$env:USERNAME
    }

    return [Environment]::UserName
}

function Resolve-DiscordBackupRootPath {
    param(
        [hashtable]$Config
    )

    $backupsConfig = @{}
    if ($null -ne $Config -and $Config.ContainsKey("backups") -and $null -ne $Config["backups"]) {
        $backupsConfig = $Config["backups"]
    }

    $rootPathValue = if ($backupsConfig.ContainsKey("rootPath")) { [string]$backupsConfig["rootPath"] } else { "blobManager/packages/data/dc/backups" }
    return & $script:File.ResolveLocalPath -PathValue $rootPathValue
}

function Get-DiscordBackupSettings {
    param(
        [hashtable]$Config
    )

    $backupsConfig = @{}
    if ($null -ne $Config -and $Config.ContainsKey("backups") -and $null -ne $Config["backups"]) {
        $backupsConfig = $Config["backups"]
    }

    $rootPathValue = if ($backupsConfig.ContainsKey("rootPath")) { [string]$backupsConfig["rootPath"] } else { "blobManager/packages/data/dc/backups" }
    $restoreDmFailureIsFatal = if ($backupsConfig.ContainsKey("restoreDmFailureIsFatal")) { [bool]$backupsConfig["restoreDmFailureIsFatal"] } else { $false }
    $extraChannelDeleteMode = if ($backupsConfig.ContainsKey("extraChannelDeleteMode")) { [string]$backupsConfig["extraChannelDeleteMode"] } else { "skip_protected" }
    if ([string]::IsNullOrWhiteSpace($extraChannelDeleteMode)) {
        $extraChannelDeleteMode = "skip_protected"
    }
    else {
        $extraChannelDeleteMode = $extraChannelDeleteMode.Trim().ToLowerInvariant()
    }

    if ($extraChannelDeleteMode -ne "skip_protected" -and $extraChannelDeleteMode -ne "strict") {
        $extraChannelDeleteMode = "skip_protected"
    }

    return @{
        RootPath = Resolve-DiscordBackupRootPath -Config $Config
        RootPathValue = $rootPathValue
        RestoreDmFailureIsFatal = $restoreDmFailureIsFatal
        ExtraChannelDeleteMode = $extraChannelDeleteMode
    }
}

function Get-DiscordBackupPathSet {
    param(
        [string]$RootPath,
        [string]$BackupId
    )

    $backupPath = Join-Path $RootPath $BackupId
    return @{
        RootPath = $RootPath
        BackupId = $BackupId
        BackupPath = $backupPath
        InfoPath = Join-Path $backupPath "info.txt"
        ManifestPath = Join-Path $backupPath "manifest.json"
        GuildPath = Join-Path $backupPath "guild.json"
        RolesPath = Join-Path $backupPath "roles.json"
        MembersPath = Join-Path $backupPath "members.json"
        ChannelsPath = Join-Path $backupPath "channels.json"
    }
}

function New-DiscordBackupId {
    $timestamp = (Get-Date).ToUniversalTime().ToString("yyyyMMddTHHmmssZ")
    $suffix = [guid]::NewGuid().ToString("N").Substring(0, 8)
    return "dc-$timestamp-$suffix"
}

function ConvertTo-DiscordBackupInfoText {
    param(
        [hashtable]$Manifest
    )

    $lines = @(
        "Backup Id: $($Manifest.backupId)",
        "Created At (UTC): $($Manifest.createdAtUtc)",
        "Creator: $($Manifest.creator)",
        "Guild Id: $($Manifest.guildId)",
        "Guild Alias: $($Manifest.guildAlias)",
        "Guild Name: $($Manifest.guildName)",
        "Description: $($Manifest.description)",
        "Included Sections: $([string]::Join(', ', @($Manifest.includedSections)))",
        "Schema: $($Manifest.schema)",
        "Schema Version: $($Manifest.schemaVersion)"
    )

    return [string]::Join([Environment]::NewLine, $lines)
}

function Get-DiscordBackupCategorySet {
    $set = New-Object "System.Collections.Generic.HashSet[string]" ([System.StringComparer]::OrdinalIgnoreCase)
    foreach ($category in $script:SelectionCategories) {
        [void]$set.Add([string]$category)
    }

    return $set
}

function ConvertTo-DiscordBackupCategoryList {
    param(
        [object[]]$Values
    )

    $categories = New-Object System.Collections.ArrayList
    if ($null -eq $Values) {
        return @()
    }

    foreach ($value in $Values) {
        if ($null -eq $value) {
            continue
        }

        foreach ($segment in ([string]$value -split ",")) {
            $trimmed = $segment.Trim()
            if (-not [string]::IsNullOrWhiteSpace($trimmed)) {
                $normalized = $trimmed.ToLowerInvariant()
                if ($script:SelectionAliases.ContainsKey($normalized)) {
                    foreach ($aliasValue in @($script:SelectionAliases[$normalized])) {
                        [void]$categories.Add([string]$aliasValue)
                    }
                }
                else {
                    [void]$categories.Add($normalized)
                }
            }
        }
    }

    return @($categories.ToArray())
}

function Resolve-DiscordBackupSelection {
    param(
        [string]$Mode,
        [object[]]$IncludeValues,
        [object[]]$ExcludeValues,
        [string[]]$AvailableSections = @()
    )

    $allowed = Get-DiscordBackupCategorySet
    $includeItems = ConvertTo-DiscordBackupCategoryList -Values $IncludeValues
    $excludeItems = ConvertTo-DiscordBackupCategoryList -Values $ExcludeValues
    $seen = New-Object "System.Collections.Generic.HashSet[string]" ([System.StringComparer]::OrdinalIgnoreCase)

    foreach ($name in @($includeItems + $excludeItems)) {
        if (-not $allowed.Contains($name)) {
            throw "Invalid backup category '$name'. Valid values: $([string]::Join(', ', $script:SelectionCategories))."
        }

        if (-not $seen.Add($name)) {
            throw "Duplicate backup category '$name' is not allowed."
        }
    }

    if ($Mode -eq "create") {
        $selected = New-Object "System.Collections.Generic.HashSet[string]" ([System.StringComparer]::OrdinalIgnoreCase)
        $seed = if ($includeItems.Count -gt 0) { $includeItems } else { $script:DefaultCreateSections }
        foreach ($item in $seed) {
            [void]$selected.Add($item)
        }
        foreach ($item in $excludeItems) {
            [void]$selected.Remove($item)
        }
        return @($script:SelectionCategories | Where-Object { $selected.Contains($_) })
    }

    $availableSet = New-Object "System.Collections.Generic.HashSet[string]" ([System.StringComparer]::OrdinalIgnoreCase)
    foreach ($section in $AvailableSections) {
        [void]$availableSet.Add([string]$section)
    }

    if ($includeItems.Count -gt 0) {
        foreach ($item in $includeItems) {
            if (-not $availableSet.Contains($item)) {
                throw "Backup does not contain the '$item' section."
            }
        }
    }

    foreach ($item in $excludeItems) {
        if (-not $availableSet.Contains($item)) {
            throw "Backup does not contain the '$item' section."
        }
    }

    $selected = New-Object "System.Collections.Generic.HashSet[string]" ([System.StringComparer]::OrdinalIgnoreCase)
    $seed = if ($includeItems.Count -gt 0) { $includeItems } else { $AvailableSections }
    foreach ($item in $seed) {
        [void]$selected.Add($item)
    }
    foreach ($item in $excludeItems) {
        [void]$selected.Remove($item)
    }

    return @($script:SelectionCategories | Where-Object { $selected.Contains($_) })
}

function Parse-DiscordBackupArguments {
    param(
        [string[]]$Arguments
    )

    $parsed = @{
        Positionals = New-Object System.Collections.ArrayList
        Description = $null
        Include = @()
        Exclude = @()
        ForceMembers = $false
    }

    $index = 0
    while ($index -lt $Arguments.Count) {
        $token = [string]$Arguments[$index]
        switch ($token) {
            "--description" {
                $index++
                if ($index -ge $Arguments.Count) {
                    throw "Missing value for --description."
                }
                $parsed.Description = [string]$Arguments[$index]
            }
            "--include" {
                $index++
                if ($index -ge $Arguments.Count) {
                    throw "Missing value for --include."
                }
                $values = New-Object System.Collections.ArrayList
                while ($index -lt $Arguments.Count -and -not ([string]$Arguments[$index]).StartsWith("--")) {
                    [void]$values.Add([string]$Arguments[$index])
                    $index++
                }
                $index--
                if ($values.Count -eq 0) {
                    throw "Missing value for --include."
                }
                $parsed.Include = @($values.ToArray())
            }
            "--exclude" {
                $index++
                if ($index -ge $Arguments.Count) {
                    throw "Missing value for --exclude."
                }
                $values = New-Object System.Collections.ArrayList
                while ($index -lt $Arguments.Count -and -not ([string]$Arguments[$index]).StartsWith("--")) {
                    [void]$values.Add([string]$Arguments[$index])
                    $index++
                }
                $index--
                if ($values.Count -eq 0) {
                    throw "Missing value for --exclude."
                }
                $parsed.Exclude = @($values.ToArray())
            }
            "--force-members" {
                $parsed.ForceMembers = $true
            }
            default {
                [void]$parsed.Positionals.Add($token)
            }
        }
        $index++
    }

    return $parsed
}

function Resolve-DiscordBackupServer {
    param(
        [string]$ServerReference,
        [hashtable]$ServerMap
    )

    $resolution = & $script:Server.ResolveDiscordServerReference -Reference $ServerReference -ServerMap $ServerMap
    if (-not $resolution.Success) {
        throw $resolution.Error
    }

    return $resolution
}

function Get-DiscordBackupManifest {
    param(
        [string]$BackupId,
        [string]$CreatedAtUtc,
        [string]$Creator,
        [string]$GuildId,
        [string]$GuildAlias,
        [string]$GuildName,
        [string]$Description,
        [string[]]$IncludedSections
    )

    return [ordered]@{
        backupId = $BackupId
        createdAtUtc = $CreatedAtUtc
        creator = $Creator
        guildId = $GuildId
        guildAlias = $GuildAlias
        guildName = $GuildName
        description = $Description
        includedSections = @($IncludedSections)
        schema = $script:BackupSchema
        schemaVersion = $script:BackupSchemaVersion
    }
}

function ConvertTo-DiscordGuildSnapshot {
    param(
        $Guild
    )

    $description = if ($Guild.PSObject.Properties.Name -contains "description") { [string]$Guild.description } else { $null }
    $preferredLocale = if ($Guild.PSObject.Properties.Name -contains "preferred_locale") { [string]$Guild.preferred_locale } else { $null }
    return [ordered]@{
        id = [string]$Guild.id
        name = [string]$Guild.name
        description = $description
        preferred_locale = $preferredLocale
    }
}

function ConvertTo-DiscordRoleSnapshot {
    param(
        [object[]]$Roles,
        [string]$GuildId
    )

    $snapshots = New-Object System.Collections.ArrayList
    foreach ($role in ($Roles | Sort-Object position, id)) {
        $icon = if ($role.PSObject.Properties.Name -contains "icon") { [string]$role.icon } else { $null }
        $unicodeEmoji = if ($role.PSObject.Properties.Name -contains "unicode_emoji") { [string]$role.unicode_emoji } else { $null }
        [void]$snapshots.Add([ordered]@{
            id = [string]$role.id
            guild_id = $GuildId
            is_everyone = ([string]$role.id -eq [string]$GuildId)
            name = [string]$role.name
            color = [int]$role.color
            hoist = [bool]$role.hoist
            icon = $icon
            unicode_emoji = $unicodeEmoji
            position = [int]$role.position
            permissions = [string]$role.permissions
            managed = [bool]$role.managed
            mentionable = [bool]$role.mentionable
        })
    }

    return @($snapshots.ToArray())
}

function ConvertTo-DiscordMemberSnapshot {
    param(
        [object[]]$Members
    )

    $snapshots = New-Object System.Collections.ArrayList
    foreach ($member in $Members) {
        if ($null -eq $member.user -or $member.user.PSObject.Properties.Name -notcontains "id") {
            continue
        }

        $username = if ($member.user.PSObject.Properties.Name -contains "username") { [string]$member.user.username } else { $null }
        $globalName = if ($member.user.PSObject.Properties.Name -contains "global_name") { [string]$member.user.global_name } else { $null }
        $joinedAt = if ($member.PSObject.Properties.Name -contains "joined_at") { [string]$member.joined_at } else { $null }
        $nick = if ($member.PSObject.Properties.Name -contains "nick") { [string]$member.nick } else { $null }
        $roles = if ($member.PSObject.Properties.Name -contains "roles" -and $null -ne $member.roles) { @($member.roles | ForEach-Object { [string]$_ }) } else { @() }
        $communicationDisabledUntil = if ($member.PSObject.Properties.Name -contains "communication_disabled_until") { [string]$member.communication_disabled_until } else { $null }
        [void]$snapshots.Add([ordered]@{
            user_id = [string]$member.user.id
            username = $username
            global_name = $globalName
            joined_at = $joinedAt
            nick = $nick
            roles = $roles
            communication_disabled_until = $communicationDisabledUntil
        })
    }

    return @($snapshots.ToArray())
}

function ConvertTo-DiscordChannelSnapshot {
    param(
        [object[]]$Channels
    )

    $snapshots = New-Object System.Collections.ArrayList
    foreach ($channel in ($Channels | Sort-Object { if ($_.type -eq $script:ChannelTypes.GuildCategory) { 0 } else { 1 } }, position, id)) {
        $permissionOverwrites = @()
        if ($channel.PSObject.Properties.Name -contains "permission_overwrites" -and $null -ne $channel.permission_overwrites) {
            $permissionOverwrites = @($channel.permission_overwrites | ForEach-Object {
                [ordered]@{
                    id = [string]$_.id
                    type = [int]$_.type
                    allow = [string]$_.allow
                    deny = [string]$_.deny
                }
            })
        }

        $position = if ($channel.PSObject.Properties.Name -contains "position") { [int]$channel.position } else { 0 }
        $parentId = if ($channel.PSObject.Properties.Name -contains "parent_id") { [string]$channel.parent_id } else { $null }
        $topic = if ($channel.PSObject.Properties.Name -contains "topic") { [string]$channel.topic } else { $null }
        $nsfw = if ($channel.PSObject.Properties.Name -contains "nsfw") { [bool]$channel.nsfw } else { $null }
        $bitrate = if ($channel.PSObject.Properties.Name -contains "bitrate") { [int]$channel.bitrate } else { $null }
        $userLimit = if ($channel.PSObject.Properties.Name -contains "user_limit") { [int]$channel.user_limit } else { $null }
        $rateLimitPerUser = if ($channel.PSObject.Properties.Name -contains "rate_limit_per_user") { [int]$channel.rate_limit_per_user } else { $null }
        $defaultAutoArchiveDuration = if ($channel.PSObject.Properties.Name -contains "default_auto_archive_duration") { [int]$channel.default_auto_archive_duration } else { $null }
        $defaultThreadRateLimitPerUser = if ($channel.PSObject.Properties.Name -contains "default_thread_rate_limit_per_user") { [int]$channel.default_thread_rate_limit_per_user } else { $null }
        $rtcRegion = if ($channel.PSObject.Properties.Name -contains "rtc_region") { [string]$channel.rtc_region } else { $null }
        $videoQualityMode = if ($channel.PSObject.Properties.Name -contains "video_quality_mode") { [int]$channel.video_quality_mode } else { $null }
        [void]$snapshots.Add([ordered]@{
            id = [string]$channel.id
            name = [string]$channel.name
            type = [int]$channel.type
            position = $position
            parent_id = $parentId
            topic = $topic
            nsfw = $nsfw
            bitrate = $bitrate
            user_limit = $userLimit
            rate_limit_per_user = $rateLimitPerUser
            default_auto_archive_duration = $defaultAutoArchiveDuration
            default_thread_rate_limit_per_user = $defaultThreadRateLimitPerUser
            rtc_region = $rtcRegion
            video_quality_mode = $videoQualityMode
            permission_overwrites = $permissionOverwrites
        })
    }

    return @($snapshots.ToArray())
}

function Save-DiscordBackupSection {
    param(
        [string]$Path,
        $Data
    )

    & $script:File.WriteJsonFile -Path $Path -Data $Data -Depth 30
}

function Get-DiscordBackupSummaryEntry {
    param(
        [string]$BackupPath
    )

    $manifestPath = Join-Path $BackupPath "manifest.json"
    if (-not (Test-Path $manifestPath -PathType Leaf)) {
        return $null
    }

    $manifest = & $script:File.ReadJsonFile -Path $manifestPath -Fallback $null
    if ($null -eq $manifest) {
        return $null
    }

    return [ordered]@{
        backupId = [string]$manifest.backupId
        createdAtUtc = [string]$manifest.createdAtUtc
        creator = [string]$manifest.creator
        guildId = [string]$manifest.guildId
        guildAlias = [string]$manifest.guildAlias
        guildName = [string]$manifest.guildName
        description = [string]$manifest.description
        includedSections = @($manifest.includedSections)
        path = $BackupPath
    }
}

function Get-DiscordBackupList {
    param(
        [hashtable]$Config,
        [string]$ServerReference = $null,
        [hashtable]$ServerMap = @{}
    )

    $settings = Get-DiscordBackupSettings -Config $Config
    $rootPath = & $script:File.EnsureDirectoryPath -Path $settings.RootPath
    $targetServerId = $null
    if (-not [string]::IsNullOrWhiteSpace([string]$ServerReference)) {
        $targetServerId = (Resolve-DiscordBackupServer -ServerReference $ServerReference -ServerMap $ServerMap).ServerId
    }

    $entries = New-Object System.Collections.ArrayList
    foreach ($directory in (Get-ChildItem -LiteralPath $rootPath -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending)) {
        $entry = Get-DiscordBackupSummaryEntry -BackupPath $directory.FullName
        if ($null -eq $entry) {
            continue
        }

        if ($null -ne $targetServerId -and [string]$entry.guildId -ne [string]$targetServerId) {
            continue
        }

        [void]$entries.Add($entry)
    }

    return @($entries.ToArray())
}

function Test-DiscordBackup {
    param(
        [hashtable]$Config,
        [string]$BackupId
    )

    $settings = Get-DiscordBackupSettings -Config $Config
    $paths = Get-DiscordBackupPathSet -RootPath $settings.RootPath -BackupId $BackupId
    $errors = New-Object System.Collections.ArrayList
    $warnings = New-Object System.Collections.ArrayList

    if (-not (Test-Path $paths.BackupPath -PathType Container)) {
        [void]$errors.Add("Backup '$BackupId' does not exist under '$($settings.RootPathValue)'.")
        return @{
            IsValid = $false
            Errors = @($errors.ToArray())
            Warnings = @($warnings.ToArray())
            Manifest = $null
            Paths = $paths
        }
    }

    foreach ($requiredPath in @($paths.InfoPath, $paths.ManifestPath)) {
        if (-not (Test-Path $requiredPath -PathType Leaf)) {
            [void]$errors.Add("Missing required backup file '$([System.IO.Path]::GetFileName($requiredPath))'.")
        }
    }

    $manifest = & $script:File.ReadJsonFile -Path $paths.ManifestPath -Fallback $null
    if ($null -eq $manifest) {
        [void]$errors.Add("manifest.json is missing or invalid JSON.")
    }
    else {
        foreach ($requiredKey in @("backupId", "createdAtUtc", "creator", "guildId", "guildName", "includedSections", "schema", "schemaVersion")) {
            if (-not $manifest.ContainsKey($requiredKey) -or $null -eq $manifest[$requiredKey] -or ([string]$manifest[$requiredKey]).Length -eq 0) {
                [void]$errors.Add("manifest.json is missing '$requiredKey'.")
            }
        }

        if ([string]$manifest.schema -ne $script:BackupSchema) {
            [void]$errors.Add("Unsupported backup schema '$($manifest.schema)'.")
        }

        if ([int]$manifest.schemaVersion -ne $script:BackupSchemaVersion) {
            [void]$errors.Add("Unsupported backup schema version '$($manifest.schemaVersion)'.")
        }

        foreach ($section in @($manifest.includedSections)) {
            if (-not $script:SectionFiles.ContainsKey([string]$section)) {
                [void]$errors.Add("manifest.json contains unknown section '$section'.")
                continue
            }

            $sectionPath = Join-Path $paths.BackupPath $script:SectionFiles[[string]$section]
            if (-not (Test-Path $sectionPath -PathType Leaf)) {
                [void]$errors.Add("Backup section '$section' is listed in manifest.json but '$($script:SectionFiles[[string]$section])' is missing.")
            }
        }
    }

    return @{
        IsValid = ($errors.Count -eq 0)
        Errors = @($errors.ToArray())
        Warnings = @($warnings.ToArray())
        Manifest = $manifest
        Paths = $paths
    }
}

function Get-DiscordBackupInfo {
    param(
        [hashtable]$Config,
        [string]$BackupId
    )

    $validation = Test-DiscordBackup -Config $Config -BackupId $BackupId
    if (-not $validation.IsValid) {
        throw [string]::Join(" ", @($validation.Errors))
    }

    $paths = $validation.Paths
    return @{
        Manifest = $validation.Manifest
        InfoText = & $script:File.ReadTextFile -Path $paths.InfoPath -Fallback ""
        Paths = $paths
        Validation = $validation
    }
}

function Read-DiscordBackupSection {
    param(
        [string]$Path
    )

    return & $script:File.ReadJsonFile -Path $Path -Fallback $null
}

function Get-DiscordBackupData {
    param(
        [hashtable]$Config,
        [string]$BackupId
    )

    $info = Get-DiscordBackupInfo -Config $Config -BackupId $BackupId
    $paths = $info.Paths
    $manifest = $info.Manifest

    $data = @{
        Manifest = $manifest
        Paths = $paths
        InfoText = $info.InfoText
    }

    foreach ($section in @($manifest.includedSections)) {
        switch ([string]$section) {
            "guild" { $data["Guild"] = Read-DiscordBackupSection -Path $paths.GuildPath }
            "roles" { $data["Roles"] = Read-DiscordBackupSection -Path $paths.RolesPath }
            "members" { $data["Members"] = Read-DiscordBackupSection -Path $paths.MembersPath }
            "channels" { $data["Channels"] = Read-DiscordBackupSection -Path $paths.ChannelsPath }
        }
    }

    return $data
}

function Remove-DiscordBackup {
    param(
        [hashtable]$Config,
        [string]$BackupId
    )

    $settings = Get-DiscordBackupSettings -Config $Config
    $info = Get-DiscordBackupInfo -Config $Config -BackupId $BackupId
    $paths = $info.Paths
    $rootPath = [System.IO.Path]::GetFullPath([string]$settings.RootPath)
    $backupPath = [System.IO.Path]::GetFullPath([string]$paths.BackupPath)
    $rootPathWithSeparator = $rootPath.TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar

    if (-not $backupPath.StartsWith($rootPathWithSeparator, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Backup '$BackupId' resolves outside the configured backup root."
    }

    if ([System.IO.Path]::GetFileName($backupPath) -ne [string]$BackupId) {
        throw "Backup '$BackupId' does not resolve to the expected backup directory name."
    }

    if (-not (Test-Path -LiteralPath $backupPath -PathType Container)) {
        throw "Backup '$BackupId' does not exist under '$($settings.RootPathValue)'."
    }

    & $script:File.RemoveDirectoryIfExists -Path $backupPath

    return [ordered]@{
        BackupId = [string]$info.Manifest.backupId
        BackupPath = $backupPath
        Manifest = $info.Manifest
        InfoText = $info.InfoText
        Summary = [ordered]@{
            GuildId = [string]$info.Manifest.guildId
            GuildAlias = [string]$info.Manifest.guildAlias
            GuildName = [string]$info.Manifest.guildName
            CreatedAtUtc = [string]$info.Manifest.createdAtUtc
            IncludedSections = @($info.Manifest.includedSections)
        }
    }
}

function New-DiscordRequestContext {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [hashtable]$Headers,
        [int]$TimeoutSeconds,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    return @{
        BaseUrl = $BaseUrl
        ApiVersion = $ApiVersion
        Headers = $Headers
        TimeoutSeconds = $TimeoutSeconds
        JoinApiUri = $JoinApiUri
        InvokeNetworkRequest = $InvokeNetworkRequest
    }
}

function Get-DiscordGuildForBackup {
    param(
        [hashtable]$RequestContext,
        [string]$ServerId
    )

    $result = & $script:Guild.GetDiscordGuild -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -ServerId $ServerId -Headers $RequestContext.Headers -TimeoutSeconds $RequestContext.TimeoutSeconds -JoinApiUri $RequestContext.JoinApiUri -InvokeNetworkRequest $RequestContext.InvokeNetworkRequest
    if (-not $result.Success -or $null -eq $result.Guild) {
        $statusText = if ($null -ne $result.Result.StatusCode) { "$($result.Result.StatusCode) $($result.Result.StatusDescription)" } else { "request failure" }
        throw "Failed to read guild '$ServerId' from Discord: $statusText"
    }

    return $result.Guild
}

function Get-DiscordRoleListForBackup {
    param(
        [hashtable]$RequestContext,
        [string]$ServerId
    )

    $result = & $script:Guild.GetDiscordGuildRoles -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -ServerId $ServerId -Headers $RequestContext.Headers -TimeoutSeconds $RequestContext.TimeoutSeconds -JoinApiUri $RequestContext.JoinApiUri -InvokeNetworkRequest $RequestContext.InvokeNetworkRequest
    if (-not $result.Success) {
        $statusText = if ($null -ne $result.Result.StatusCode) { "$($result.Result.StatusCode) $($result.Result.StatusDescription)" } else { "request failure" }
        throw "Failed to read roles for guild '$ServerId': $statusText"
    }

    return @($result.Roles)
}

function Get-DiscordChannelListForBackup {
    param(
        [hashtable]$RequestContext,
        [string]$ServerId
    )

    $result = & $script:Guild.GetDiscordGuildChannels -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -ServerId $ServerId -Headers $RequestContext.Headers -TimeoutSeconds $RequestContext.TimeoutSeconds -JoinApiUri $RequestContext.JoinApiUri -InvokeNetworkRequest $RequestContext.InvokeNetworkRequest
    if (-not $result.Success) {
        $statusText = if ($null -ne $result.Result.StatusCode) { "$($result.Result.StatusCode) $($result.Result.StatusDescription)" } else { "request failure" }
        throw "Failed to read channels for guild '$ServerId': $statusText"
    }

    return @($result.Channels)
}

function Get-DiscordMemberListForBackup {
    param(
        [hashtable]$RequestContext,
        [string]$ServerId
    )

    $result = & $script:Guild.GetDiscordGuildMembers -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -ServerId $ServerId -Headers $RequestContext.Headers -TimeoutSeconds $RequestContext.TimeoutSeconds -JoinApiUri $RequestContext.JoinApiUri -InvokeNetworkRequest $RequestContext.InvokeNetworkRequest
    if (-not $result.Success) {
        $statusText = if ($null -ne $result.Result.StatusCode) { "$($result.Result.StatusCode) $($result.Result.StatusDescription)" } else { "request failure" }
        throw "Failed to read members for guild '$ServerId': $statusText"
    }

    return @($result.Members)
}

function Invoke-DiscordBackupCreate {
    param(
        [hashtable]$Config,
        [string]$BaseUrl,
        [string]$ApiVersion,
        [hashtable]$Headers,
        [int]$TimeoutSeconds,
        [string]$ServerReference,
        [string]$Description = "",
        [object[]]$IncludeValues = @(),
        [object[]]$ExcludeValues = @(),
        [hashtable]$ServerMap = @{},
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $selection = Resolve-DiscordBackupSelection -Mode "create" -IncludeValues $IncludeValues -ExcludeValues $ExcludeValues
    $settings = Get-DiscordBackupSettings -Config $Config
    $rootPath = & $script:File.EnsureDirectoryPath -Path $settings.RootPath
    $serverResolution = Resolve-DiscordBackupServer -ServerReference $ServerReference -ServerMap $ServerMap
    $requestContext = New-DiscordRequestContext -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Headers $Headers -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    $guild = Get-DiscordGuildForBackup -RequestContext $requestContext -ServerId $serverResolution.ServerId
    $backupId = New-DiscordBackupId
    $paths = Get-DiscordBackupPathSet -RootPath $rootPath -BackupId $backupId
    [void](& $script:File.EnsureDirectoryPath -Path $paths.BackupPath)
    $guildAlias = $serverResolution.Alias
    if ([string]::IsNullOrWhiteSpace([string]$guildAlias)) {
        $guildAlias = & $script:Server.GetDiscordServerAliasById -ServerId ([string]$guild.id) -ServerMap $ServerMap
    }

    $manifest = Get-DiscordBackupManifest -BackupId $backupId -CreatedAtUtc (& $script:Common.GetUtcTimestamp) -Creator (Get-DiscordBackupOperatorIdentity) -GuildId ([string]$guild.id) -GuildAlias ([string]$guildAlias) -GuildName ([string]$guild.name) -Description ([string]$Description) -IncludedSections $selection

    & $script:File.WriteTextFile -Path $paths.InfoPath -Content (ConvertTo-DiscordBackupInfoText -Manifest $manifest)
    & $script:File.WriteJsonFile -Path $paths.ManifestPath -Data $manifest -Depth 10

    $counts = [ordered]@{}
    foreach ($section in $selection) {
        switch ($section) {
            "guild" {
                $snapshot = ConvertTo-DiscordGuildSnapshot -Guild $guild
                Save-DiscordBackupSection -Path $paths.GuildPath -Data $snapshot
                $counts["guild"] = 1
            }
            "roles" {
                $roles = Get-DiscordRoleListForBackup -RequestContext $requestContext -ServerId $serverResolution.ServerId
                $snapshot = ConvertTo-DiscordRoleSnapshot -Roles $roles -GuildId ([string]$guild.id)
                Save-DiscordBackupSection -Path $paths.RolesPath -Data $snapshot
                $counts["roles"] = $snapshot.Count
            }
            "channels" {
                $channels = Get-DiscordChannelListForBackup -RequestContext $requestContext -ServerId $serverResolution.ServerId
                $snapshot = ConvertTo-DiscordChannelSnapshot -Channels $channels
                Save-DiscordBackupSection -Path $paths.ChannelsPath -Data $snapshot
                $counts["channels"] = $snapshot.Count
            }
            "members" {
                $members = Get-DiscordMemberListForBackup -RequestContext $requestContext -ServerId $serverResolution.ServerId
                $snapshot = ConvertTo-DiscordMemberSnapshot -Members $members
                Save-DiscordBackupSection -Path $paths.MembersPath -Data $snapshot
                $counts["members"] = $snapshot.Count
            }
        }
    }

    return @{
        BackupId = $backupId
        BackupPath = $paths.BackupPath
        Manifest = $manifest
        Counts = $counts
    }
}

function New-DiscordRolePayload {
    param(
        [hashtable]$Role
    )

    $payload = [ordered]@{
        name = [string]$Role.name
        color = [int]$Role.color
        hoist = [bool]$Role.hoist
        permissions = [string]$Role.permissions
        mentionable = [bool]$Role.mentionable
    }

    if ($Role.ContainsKey("unicode_emoji") -and -not [string]::IsNullOrWhiteSpace([string]$Role.unicode_emoji)) {
        $payload["unicode_emoji"] = [string]$Role.unicode_emoji
    }

    return $payload
}

function Get-DiscordRoleMatch {
    param(
        [hashtable]$SavedRole,
        [object[]]$CurrentRoles,
        [hashtable]$RoleIdMap,
        [string]$GuildId
    )

    $savedRoleId = [string]$SavedRole.id
    if ($RoleIdMap.ContainsKey($savedRoleId)) {
        $mappedId = [string]$RoleIdMap[$savedRoleId]
        return ($CurrentRoles | Where-Object { [string]$_.id -eq $mappedId } | Select-Object -First 1)
    }

    $byId = $CurrentRoles | Where-Object { [string]$_.id -eq $savedRoleId } | Select-Object -First 1
    if ($null -ne $byId) {
        return $byId
    }

    if ([bool]$SavedRole.is_everyone) {
        return ($CurrentRoles | Where-Object { [string]$_.id -eq $GuildId } | Select-Object -First 1)
    }

    return ($CurrentRoles | Where-Object {
        -not [bool]$_.managed -and
        [string]$_.name -eq [string]$SavedRole.name
    } | Sort-Object id | Select-Object -First 1)
}

function Restore-DiscordGuildSettings {
    param(
        [hashtable]$RequestContext,
        [string]$ServerId,
        [hashtable]$GuildSnapshot
    )

    $payload = [ordered]@{}
    foreach ($property in @("name", "description", "preferred_locale")) {
        if ($GuildSnapshot.ContainsKey($property)) {
            $payload[$property] = $GuildSnapshot[$property]
        }
    }

    $result = & $script:Guild.UpdateDiscordGuild -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -ServerId $ServerId -Headers $RequestContext.Headers -Body $payload -TimeoutSeconds $RequestContext.TimeoutSeconds -JoinApiUri $RequestContext.JoinApiUri -InvokeNetworkRequest $RequestContext.InvokeNetworkRequest
    if (-not $result.Success) {
        $statusText = Get-DiscordBackupRequestStatusText -Result $result
        if (Test-DiscordBackupPermissionDenied -Result $result) {
            return @{
                Restored = $false
                Skipped = $true
                Warning = "Skipping guild settings restore because the bot received $statusText."
                Payload = $payload
            }
        }
        throw "Guild settings restore failed: $statusText"
    }

    return @{
        Restored = $true
        Payload = $payload
    }
}

function Restore-DiscordRoles {
    param(
        [hashtable]$RequestContext,
        [string]$ServerId,
        [string]$GuildId,
        [object[]]$SavedRoles
    )

    $currentRoles = Get-DiscordRoleListForBackup -RequestContext $RequestContext -ServerId $ServerId
    $roleIdMap = @{}
    $actions = New-Object System.Collections.ArrayList
    $warnings = New-Object System.Collections.ArrayList

    foreach ($savedRole in ($SavedRoles | Sort-Object position, id)) {
        $match = Get-DiscordRoleMatch -SavedRole $savedRole -CurrentRoles $currentRoles -RoleIdMap $roleIdMap -GuildId $GuildId
        $payload = New-DiscordRolePayload -Role $savedRole

        if ($null -eq $match) {
            if ([bool]$savedRole.managed -or [bool]$savedRole.is_everyone) {
                [void]$actions.Add("Skipped role '$($savedRole.name)' because Discord manages it and it could not be matched.")
                continue
            }

            $createResult = & $script:Guild.NewDiscordGuildRole -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -ServerId $ServerId -Headers $RequestContext.Headers -Body $payload -TimeoutSeconds $RequestContext.TimeoutSeconds -JoinApiUri $RequestContext.JoinApiUri -InvokeNetworkRequest $RequestContext.InvokeNetworkRequest
            if (-not $createResult.Success -or $null -eq $createResult.Role) {
                $statusText = Get-DiscordBackupRequestStatusText -Result $createResult
                if (Test-DiscordBackupPermissionDenied -Result $createResult) {
                    [void]$warnings.Add("Skipping role create for '$($savedRole.name)' because the bot received $statusText.")
                    continue
                }
                throw "Role create failed for '$($savedRole.name)': $statusText"
            }

            $match = $createResult.Role
            $currentRoles += $match
            [void]$actions.Add("Created role '$($savedRole.name)'.")
        }
        else {
            $updateResult = & $script:Guild.UpdateDiscordGuildRole -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -ServerId $ServerId -RoleId ([string]$match.id) -Headers $RequestContext.Headers -Body $payload -TimeoutSeconds $RequestContext.TimeoutSeconds -JoinApiUri $RequestContext.JoinApiUri -InvokeNetworkRequest $RequestContext.InvokeNetworkRequest
            if (-not $updateResult.Success -or $null -eq $updateResult.Role) {
                $statusText = Get-DiscordBackupRequestStatusText -Result $updateResult
                if (Test-DiscordBackupPermissionDenied -Result $updateResult) {
                    [void]$warnings.Add("Skipping role update for '$($savedRole.name)' because the bot received $statusText.")
                    continue
                }
                throw "Role update failed for '$($savedRole.name)': $statusText"
            }

            $match = $updateResult.Role
            $currentRoles = @($currentRoles | Where-Object { [string]$_.id -ne [string]$match.id }) + $match
            [void]$actions.Add("Updated role '$($savedRole.name)'.")
        }

        $roleIdMap[[string]$savedRole.id] = [string]$match.id
    }

    $positionPayload = New-Object System.Collections.ArrayList
    foreach ($savedRole in ($SavedRoles | Sort-Object position, id)) {
        $savedRoleId = [string]$savedRole.id
        if (-not $roleIdMap.ContainsKey($savedRoleId)) {
            continue
        }

        [void]$positionPayload.Add([ordered]@{
            id = [string]$roleIdMap[$savedRoleId]
            position = [int]$savedRole.position
        })
    }

    if ($positionPayload.Count -gt 0) {
        $positionResult = & $script:Guild.SetDiscordGuildRolePositions -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -ServerId $ServerId -Headers $RequestContext.Headers -Positions @($positionPayload.ToArray()) -TimeoutSeconds $RequestContext.TimeoutSeconds -JoinApiUri $RequestContext.JoinApiUri -InvokeNetworkRequest $RequestContext.InvokeNetworkRequest
        if (-not $positionResult.Success) {
            $statusText = Get-DiscordBackupRequestStatusText -Result $positionResult
            if (Test-DiscordBackupPermissionDenied -Result $positionResult -or ([int]$positionResult.Result.StatusCode -eq 400)) {
                [void]$warnings.Add("Skipping role position restore because the bot received $statusText.")
            }
            else {
                throw "Role position restore failed: $statusText"
            }
        }
    }

    return @{
        RoleIdMap = $roleIdMap
        Actions = @($actions.ToArray())
        Warnings = @($warnings.ToArray())
    }
}

function ConvertTo-DiscordChannelPayload {
    param(
        [hashtable]$SavedChannel,
        [hashtable]$RoleIdMap,
        [hashtable]$ChannelIdMap,
        [switch]$IncludeType
    )

    $payload = [ordered]@{
        name = [string]$SavedChannel.name
    }

    if ($IncludeType.IsPresent) {
        $payload["type"] = [int]$SavedChannel.type
    }

    foreach ($property in @("topic", "nsfw", "bitrate", "user_limit", "rate_limit_per_user", "default_auto_archive_duration", "default_thread_rate_limit_per_user", "rtc_region", "video_quality_mode")) {
        if ($SavedChannel.ContainsKey($property) -and $null -ne $SavedChannel[$property] -and -not [string]::IsNullOrWhiteSpace([string]$SavedChannel[$property])) {
            $payload[$property] = $SavedChannel[$property]
        }
    }

    if ($SavedChannel.ContainsKey("parent_id") -and -not [string]::IsNullOrWhiteSpace([string]$SavedChannel.parent_id) -and $ChannelIdMap.ContainsKey([string]$SavedChannel.parent_id)) {
        $payload["parent_id"] = [string]$ChannelIdMap[[string]$SavedChannel.parent_id]
    }

    if ($SavedChannel.ContainsKey("permission_overwrites") -and $null -ne $SavedChannel.permission_overwrites) {
        $overwrites = New-Object System.Collections.ArrayList
        foreach ($overwrite in @($SavedChannel.permission_overwrites)) {
            $targetId = [string]$overwrite.id
            if ($RoleIdMap.ContainsKey($targetId)) {
                $targetId = [string]$RoleIdMap[$targetId]
            }

            [void]$overwrites.Add([ordered]@{
                id = $targetId
                type = [int]$overwrite.type
                allow = [string]$overwrite.allow
                deny = [string]$overwrite.deny
            })
        }
        $payload["permission_overwrites"] = @($overwrites.ToArray())
    }

    return $payload
}

function Get-DiscordChannelMatch {
    param(
        [hashtable]$SavedChannel,
        [object[]]$CurrentChannels,
        [hashtable]$ChannelIdMap
    )

    $savedId = [string]$SavedChannel.id
    if ($ChannelIdMap.ContainsKey($savedId)) {
        $mappedId = [string]$ChannelIdMap[$savedId]
        return ($CurrentChannels | Where-Object { [string]$_.id -eq $mappedId } | Select-Object -First 1)
    }

    $byId = $CurrentChannels | Where-Object { [string]$_.id -eq $savedId } | Select-Object -First 1
    if ($null -ne $byId) {
        return $byId
    }

    $expectedParentId = $null
    if ($SavedChannel.ContainsKey("parent_id")) {
        $savedParentId = [string]$SavedChannel.parent_id
        if ($ChannelIdMap.ContainsKey($savedParentId)) {
            $expectedParentId = [string]$ChannelIdMap[$savedParentId]
        }
        else {
            $expectedParentId = $savedParentId
        }
    }

    return ($CurrentChannels | Where-Object {
        $currentParentId = if ($_.PSObject.Properties.Name -contains "parent_id") { [string]$_.parent_id } else { $null }
        [int]$_.type -eq [int]$SavedChannel.type -and
        [string]$_.name -eq [string]$SavedChannel.name -and
        (
            ([string]::IsNullOrWhiteSpace([string]$expectedParentId) -and [string]::IsNullOrWhiteSpace([string]$currentParentId)) -or
            ([string]$currentParentId -eq [string]$expectedParentId)
        )
    } | Sort-Object position, id | Select-Object -First 1)
}

function Restore-DiscordChannels {
    param(
        [hashtable]$RequestContext,
        [string]$ServerId,
        [object[]]$SavedChannels,
        [hashtable]$RoleIdMap
    )

    $currentChannels = Get-DiscordChannelListForBackup -RequestContext $RequestContext -ServerId $ServerId
    $channelIdMap = @{}
    $actions = New-Object System.Collections.ArrayList
    $warnings = New-Object System.Collections.ArrayList
    $orderedChannels = @($SavedChannels | Sort-Object { if ([int]$_.type -eq $script:ChannelTypes.GuildCategory) { 0 } else { 1 } }, position, id)

    foreach ($savedChannel in $orderedChannels) {
        $match = Get-DiscordChannelMatch -SavedChannel $savedChannel -CurrentChannels $currentChannels -ChannelIdMap $channelIdMap

        if ($null -eq $match) {
            $payload = ConvertTo-DiscordChannelPayload -SavedChannel $savedChannel -RoleIdMap $RoleIdMap -ChannelIdMap $channelIdMap -IncludeType
            $createResult = & $script:Guild.NewDiscordGuildChannel -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -ServerId $ServerId -Headers $RequestContext.Headers -Body $payload -TimeoutSeconds $RequestContext.TimeoutSeconds -JoinApiUri $RequestContext.JoinApiUri -InvokeNetworkRequest $RequestContext.InvokeNetworkRequest
            if (-not $createResult.Success -or $null -eq $createResult.Channel) {
                $statusText = Get-DiscordBackupRequestStatusText -Result $createResult
                if (Test-DiscordBackupPermissionDenied -Result $createResult) {
                    [void]$warnings.Add("Skipping channel create for '$($savedChannel.name)' because the bot received $statusText.")
                    continue
                }
                throw "Channel create failed for '$($savedChannel.name)': $statusText"
            }

            $match = $createResult.Channel
            $currentChannels += $match
            [void]$actions.Add("Created channel '$($savedChannel.name)'.")
        }
        else {
            $payload = ConvertTo-DiscordChannelPayload -SavedChannel $savedChannel -RoleIdMap $RoleIdMap -ChannelIdMap $channelIdMap
            $updateResult = & $script:Guild.UpdateDiscordChannel -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -ChannelId ([string]$match.id) -Headers $RequestContext.Headers -Body $payload -TimeoutSeconds $RequestContext.TimeoutSeconds -JoinApiUri $RequestContext.JoinApiUri -InvokeNetworkRequest $RequestContext.InvokeNetworkRequest
            if (-not $updateResult.Success -or $null -eq $updateResult.Channel) {
                $statusText = Get-DiscordBackupRequestStatusText -Result $updateResult
                if (Test-DiscordBackupPermissionDenied -Result $updateResult) {
                    [void]$warnings.Add("Skipping channel update for '$($savedChannel.name)' because the bot received $statusText.")
                    continue
                }
                throw "Channel update failed for '$($savedChannel.name)': $statusText"
            }

            $match = $updateResult.Channel
            $currentChannels = @($currentChannels | Where-Object { [string]$_.id -ne [string]$match.id }) + $match
            [void]$actions.Add("Updated channel '$($savedChannel.name)'.")
        }

        $channelIdMap[[string]$savedChannel.id] = [string]$match.id
    }

    $positionPayload = New-Object System.Collections.ArrayList
    foreach ($savedChannel in ($orderedChannels | Sort-Object position, id)) {
        $savedId = [string]$savedChannel.id
        if (-not $channelIdMap.ContainsKey($savedId)) {
            continue
        }

        $item = [ordered]@{
            id = [string]$channelIdMap[$savedId]
            position = [int]$savedChannel.position
        }

        [void]$positionPayload.Add($item)
    }

    if ($positionPayload.Count -gt 0) {
        $positionResult = & $script:Guild.SetDiscordGuildChannelPositions -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -ServerId $ServerId -Headers $RequestContext.Headers -Positions @($positionPayload.ToArray()) -TimeoutSeconds $RequestContext.TimeoutSeconds -JoinApiUri $RequestContext.JoinApiUri -InvokeNetworkRequest $RequestContext.InvokeNetworkRequest
        if (-not $positionResult.Success) {
            $statusText = Get-DiscordBackupRequestStatusText -Result $positionResult
            if (Test-DiscordBackupPermissionDenied -Result $positionResult -or ([int]$positionResult.Result.StatusCode -eq 400)) {
                [void]$warnings.Add("Skipping channel position restore because the bot received $statusText.")
            }
            else {
                throw "Channel position restore failed: $statusText"
            }
        }
    }

    return @{
        ChannelIdMap = $channelIdMap
        Actions = @($actions.ToArray())
        Warnings = @($warnings.ToArray())
    }
}

function Restore-DiscordMembers {
    param(
        [hashtable]$RequestContext,
        [string]$ServerId,
        [string]$GuildName,
        [string]$BackupId,
        [object[]]$SavedMembers,
        [hashtable]$RoleIdMap,
        [hashtable]$Settings,
        [string]$MessagesRootPath
    )

    $currentMembers = Get-DiscordMemberListForBackup -RequestContext $RequestContext -ServerId $ServerId
    $currentById = @{}
    foreach ($member in $currentMembers) {
        if ($null -ne $member.user -and $member.user.PSObject.Properties.Name -contains "id") {
            $currentById[[string]$member.user.id] = $member
        }
    }

    $actions = New-Object System.Collections.ArrayList
    $extraMembers = New-Object System.Collections.ArrayList
    $savedMemberIds = New-Object "System.Collections.Generic.HashSet[string]" ([System.StringComparer]::OrdinalIgnoreCase)
    $warnings = New-Object System.Collections.ArrayList

    foreach ($savedMember in $SavedMembers) {
        $userId = [string]$savedMember.user_id
        [void]$savedMemberIds.Add($userId)
        if (-not $currentById.ContainsKey($userId)) {
            [void]$actions.Add("Skipped member '$userId' because they are no longer in the guild.")
            continue
        }

        $roles = New-Object System.Collections.ArrayList
        foreach ($savedRoleId in @($savedMember.roles)) {
            if ($RoleIdMap.ContainsKey([string]$savedRoleId)) {
                [void]$roles.Add([string]$RoleIdMap[[string]$savedRoleId])
            }
        }

        $payload = [ordered]@{
            roles = @($roles.ToArray())
        }

        if ($savedMember.ContainsKey("nick")) {
            $payload["nick"] = if ([string]::IsNullOrWhiteSpace([string]$savedMember.nick)) { $null } else { [string]$savedMember.nick }
        }

        if ($savedMember.ContainsKey("communication_disabled_until") -and -not [string]::IsNullOrWhiteSpace([string]$savedMember.communication_disabled_until)) {
            $payload["communication_disabled_until"] = [string]$savedMember.communication_disabled_until
        }

        $updateResult = & $script:Guild.UpdateDiscordGuildMember -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -ServerId $ServerId -UserId $userId -Headers $RequestContext.Headers -Body $payload -TimeoutSeconds $RequestContext.TimeoutSeconds -JoinApiUri $RequestContext.JoinApiUri -InvokeNetworkRequest $RequestContext.InvokeNetworkRequest
        if (-not $updateResult.Success) {
            $statusText = Get-DiscordBackupRequestStatusText -Result $updateResult
            if (Test-DiscordBackupPermissionDenied -Result $updateResult) {
                [void]$warnings.Add("Skipping member update for '$userId' because the bot received $statusText.")
                continue
            }
            throw "Member restore failed for '$userId': $statusText"
        }

        [void]$actions.Add("Updated member '$userId'.")
    }

    foreach ($member in $currentMembers) {
        $userId = if ($null -ne $member.user -and $member.user.PSObject.Properties.Name -contains "id") { [string]$member.user.id } else { $null }
        if ([string]::IsNullOrWhiteSpace([string]$userId)) {
            continue
        }

        if (-not $savedMemberIds.Contains($userId)) {
            [void]$extraMembers.Add($member)
        }
    }

    $enforcementLog = New-Object System.Collections.ArrayList
    foreach ($member in @($extraMembers.ToArray())) {
        $userId = [string]$member.user.id
        $displayName = & $script:Common.ConvertToDiscordDisplayName -User $member.user
        $templateValues = @{
            backupId = $BackupId
            guildId = $ServerId
            guildName = $GuildName
            userId = $userId
            userName = $displayName
        }

        $dmMessageConfig = Get-DiscordBackupRenderedMessageConfig -MessagesRootPath $MessagesRootPath -RelativePath "backup\memberRestore\kickDm.json" -Fallback @{
            content = "You were removed from {{guildName}} because the server was restored from backup {{backupId}} and your membership was not part of that snapshot."
        } -TemplateValues $templateValues
        $auditConfig = Get-DiscordBackupRenderedMessageConfig -MessagesRootPath $MessagesRootPath -RelativePath "backup\memberRestore\kickAudit.json" -Fallback @{
            text = "Restored backup {{backupId}}; removing out-of-scope member {{userId}} from guild {{guildId}}."
        } -TemplateValues $templateValues
        $auditReason = if ($auditConfig.ContainsKey("text") -and -not [string]::IsNullOrWhiteSpace([string]$auditConfig.text)) { [string]$auditConfig.text } else { "Restored backup $BackupId; removing out-of-scope member $userId from guild $ServerId." }
        $dmAttempt = Send-DiscordMemberRemovalDm -RequestContext $RequestContext -UserId $userId -MessageConfig $dmMessageConfig

        if (-not $dmAttempt.Success -and $Settings.RestoreDmFailureIsFatal) {
            throw "DM failed for out-of-scope member '$userId' and restoreDmFailureIsFatal is enabled."
        }

        $kickHeaders = @{}
        foreach ($header in $RequestContext.Headers.GetEnumerator()) {
            $kickHeaders[[string]$header.Key] = $header.Value
        }
        $kickHeaders["X-Audit-Log-Reason"] = [uri]::EscapeDataString($auditReason)

        $kickResult = & $script:Guild.RemoveDiscordGuildMember -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -ServerId $ServerId -UserId $userId -Headers $kickHeaders -TimeoutSeconds $RequestContext.TimeoutSeconds -JoinApiUri $RequestContext.JoinApiUri -InvokeNetworkRequest $RequestContext.InvokeNetworkRequest
        if (-not $kickResult.Success) {
            $statusText = Get-DiscordBackupRequestStatusText -Result $kickResult
            if (Test-DiscordBackupPermissionDenied -Result $kickResult) {
                [void]$warnings.Add("Skipping kick for out-of-scope member '$userId' because the bot received $statusText.")
                continue
            }
            throw "Kick failed for out-of-scope member '$userId': $statusText"
        }

        [void]$enforcementLog.Add([ordered]@{
            userId = $userId
            userName = $displayName
            dmAttempted = $true
            dmSucceeded = [bool]$dmAttempt.Success
            dmError = $dmAttempt.Error
            kickSucceeded = $true
        })
    }

    return @{
        Actions = @($actions.ToArray())
        Enforcement = @($enforcementLog.ToArray())
        Warnings = @($warnings.ToArray())
    }
}

function Remove-DiscordExtraChannels {
    param(
        [hashtable]$RequestContext,
        [string]$ServerId,
        [object[]]$SavedChannels,
        [hashtable]$ChannelIdMap = @{},
        [string]$ExtraChannelDeleteMode = "skip_protected"
    )

    $savedChannelIds = New-Object "System.Collections.Generic.HashSet[string]" ([System.StringComparer]::OrdinalIgnoreCase)
    foreach ($savedChannel in @($SavedChannels)) {
        if ($null -ne $savedChannel -and $savedChannel.PSObject.Properties.Name -contains "id") {
            $savedChannelId = [string]$savedChannel.id
            if ($ChannelIdMap.ContainsKey($savedChannelId)) {
                [void]$savedChannelIds.Add([string]$ChannelIdMap[$savedChannelId])
            }
            else {
                [void]$savedChannelIds.Add($savedChannelId)
            }
        }
    }

    $currentChannels = Get-DiscordChannelListForBackup -RequestContext $RequestContext -ServerId $ServerId
    $actions = New-Object System.Collections.ArrayList
    $warnings = New-Object System.Collections.ArrayList

    $extraChannels = @($currentChannels | Where-Object {
        $channelId = [string]$_.id
        -not [string]::IsNullOrWhiteSpace($channelId) -and -not $savedChannelIds.Contains($channelId)
    } | Sort-Object { if ([int]$_.type -eq $script:ChannelTypes.GuildCategory) { 1 } else { 0 } }, position, id)

    foreach ($channel in $extraChannels) {
        $channelId = [string]$channel.id
        $channelName = if ($channel.PSObject.Properties.Name -contains "name") { [string]$channel.name } else { $channelId }
        $deleteResult = & $script:Guild.RemoveDiscordChannel -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -ChannelId $channelId -Headers $RequestContext.Headers -TimeoutSeconds $RequestContext.TimeoutSeconds -JoinApiUri $RequestContext.JoinApiUri -InvokeNetworkRequest $RequestContext.InvokeNetworkRequest
        if (-not $deleteResult.Success) {
            $statusText = Get-DiscordBackupRequestStatusText -Result $deleteResult
            $statusCode = if ($null -ne $deleteResult -and $null -ne $deleteResult.Result -and $null -ne $deleteResult.Result.StatusCode) { [int]$deleteResult.Result.StatusCode } else { $null }
            $isProtectedDeleteFailure = ($statusCode -eq 400)
            if (
                (Test-DiscordBackupPermissionDenied -Result $deleteResult) -or
                ($statusCode -eq 404) -or
                ($isProtectedDeleteFailure -and $ExtraChannelDeleteMode -eq "skip_protected")
            ) {
                if ($isProtectedDeleteFailure -and $ExtraChannelDeleteMode -eq "skip_protected") {
                    [void]$warnings.Add("Skipping delete for protected extra channel '$channelName' (community rules/updates may be protected by Discord): $statusText.")
                    continue
                }
                [void]$warnings.Add("Skipping delete for extra channel '$channelName' because the bot received $statusText.")
                continue
            }
            throw "Channel delete failed for extra channel '$channelName': $statusText"
        }

        [void]$actions.Add("Deleted extra channel '$channelName'.")
    }

    return @{
        Actions = @($actions.ToArray())
        Warnings = @($warnings.ToArray())
    }
}

function Remove-DiscordExtraRoles {
    param(
        [hashtable]$RequestContext,
        [string]$ServerId,
        [string]$GuildId,
        [object[]]$SavedRoles,
        [hashtable]$RoleIdMap = @{}
    )

    $savedRoleIds = New-Object "System.Collections.Generic.HashSet[string]" ([System.StringComparer]::OrdinalIgnoreCase)
    foreach ($savedRole in @($SavedRoles)) {
        if ($null -ne $savedRole -and $savedRole.PSObject.Properties.Name -contains "id") {
            $savedRoleId = [string]$savedRole.id
            if ($RoleIdMap.ContainsKey($savedRoleId)) {
                [void]$savedRoleIds.Add([string]$RoleIdMap[$savedRoleId])
            }
            else {
                [void]$savedRoleIds.Add($savedRoleId)
            }
        }
    }

    $currentRoles = Get-DiscordRoleListForBackup -RequestContext $RequestContext -ServerId $ServerId
    $actions = New-Object System.Collections.ArrayList
    $warnings = New-Object System.Collections.ArrayList

    $extraRoles = @($currentRoles | Where-Object {
        $roleId = [string]$_.id
        -not [string]::IsNullOrWhiteSpace($roleId) -and
        $roleId -ne [string]$GuildId -and
        -not $savedRoleIds.Contains($roleId) -and
        -not [bool]$_.managed
    } | Sort-Object { if ([string]$_.id -eq [string]$GuildId) { 1 } else { 0 } }, position, id)

    foreach ($role in $extraRoles) {
        $roleId = [string]$role.id
        if ($roleId -eq [string]$GuildId) {
            continue
        }

        $roleName = if ($role.PSObject.Properties.Name -contains "name") { [string]$role.name } else { $roleId }
        $deleteResult = & $script:Guild.RemoveDiscordGuildRole -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -ServerId $ServerId -RoleId $roleId -Headers $RequestContext.Headers -TimeoutSeconds $RequestContext.TimeoutSeconds -JoinApiUri $RequestContext.JoinApiUri -InvokeNetworkRequest $RequestContext.InvokeNetworkRequest
        if (-not $deleteResult.Success) {
            $statusText = Get-DiscordBackupRequestStatusText -Result $deleteResult
            if (Test-DiscordBackupPermissionDenied -Result $deleteResult -or ([int]$deleteResult.Result.StatusCode -eq 400) -or ([int]$deleteResult.Result.StatusCode -eq 404)) {
                [void]$warnings.Add("Skipping delete for extra role '$roleName' because the bot received $statusText.")
                continue
            }
            throw "Role delete failed for extra role '$roleName': $statusText"
        }

        [void]$actions.Add("Deleted extra role '$roleName'.")
    }

    return @{
        Actions = @($actions.ToArray())
        Warnings = @($warnings.ToArray())
    }
}

function Get-DiscordBackupRenderedMessageConfig {
    param(
        [string]$MessagesRootPath,
        [string]$RelativePath,
        [hashtable]$Fallback,
        [hashtable]$TemplateValues
    )

    $config = & $script:Commands.GetDiscordMessageConfig -MessagesRootPath $MessagesRootPath -RelativePath $RelativePath -Fallback $Fallback
    $resolved = & $script:Commands.ResolveDiscordTemplateObject -InputObject $config -Values $TemplateValues
    if ($resolved -isnot [hashtable]) {
        return $Fallback
    }

    return $resolved
}

function Test-DiscordBackupPermissionDenied {
    param(
        $Result
    )

    if ($null -eq $Result -or $null -eq $Result.Result -or $null -eq $Result.Result.StatusCode) {
        return $false
    }

    return ([int]$Result.Result.StatusCode -eq 403)
}

function Get-DiscordBackupRequestStatusText {
    param(
        $Result
    )

    if ($null -ne $Result -and $null -ne $Result.Result -and $null -ne $Result.Result.StatusCode) {
        return "$($Result.Result.StatusCode) $($Result.Result.StatusDescription)"
    }

    return "request failure"
}

function Send-DiscordMemberRemovalDm {
    param(
        [hashtable]$RequestContext,
        [string]$UserId,
        [hashtable]$MessageConfig
    )

    $channelResult = & $script:Guild.NewDiscordDmChannel -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -RecipientId $UserId -Headers $RequestContext.Headers -TimeoutSeconds $RequestContext.TimeoutSeconds -JoinApiUri $RequestContext.JoinApiUri -InvokeNetworkRequest $RequestContext.InvokeNetworkRequest
    if (-not $channelResult.Success -or $null -eq $channelResult.Channel -or $channelResult.Channel.PSObject.Properties.Name -notcontains "id") {
        return @{
            Success = $false
            Error = "Failed to create DM channel."
        }
    }

    $messageResult = & $script:Commands.SendDiscordChannelMessage -BaseUrl $RequestContext.BaseUrl -ApiVersion $RequestContext.ApiVersion -ChannelId ([string]$channelResult.Channel.id) -MessageConfig $MessageConfig -Headers $RequestContext.Headers -TimeoutSeconds $RequestContext.TimeoutSeconds
    if (-not $messageResult.Success) {
        return @{
            Success = $false
            Error = "Failed to send DM."
        }
    }

    return @{
        Success = $true
        Error = $null
    }
}

function Invoke-DiscordBackupRestore {
    param(
        [hashtable]$Config,
        [string]$BaseUrl,
        [string]$ApiVersion,
        [hashtable]$Headers,
        [int]$TimeoutSeconds,
        [string]$MessagesRootPath,
        [string]$ServerReference,
        [string]$BackupId,
        [object[]]$IncludeValues = @(),
        [object[]]$ExcludeValues = @(),
        [switch]$ForceMembers,
        [hashtable]$ServerMap = @{},
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $backup = Get-DiscordBackupData -Config $Config -BackupId $BackupId
    $selection = Resolve-DiscordBackupSelection -Mode "restore" -IncludeValues $IncludeValues -ExcludeValues $ExcludeValues -AvailableSections @($backup.Manifest.includedSections)
    if ($selection -contains "members" -and -not $ForceMembers.IsPresent) {
        throw "Member restore is destructive. Re-run with --force-members to restore members and remove out-of-scope joiners."
    }

    $serverResolution = Resolve-DiscordBackupServer -ServerReference $ServerReference -ServerMap $ServerMap
    $requestContext = New-DiscordRequestContext -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Headers $Headers -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    $targetGuild = Get-DiscordGuildForBackup -RequestContext $requestContext -ServerId $serverResolution.ServerId
    $settings = Get-DiscordBackupSettings -Config $Config
    $report = [ordered]@{
        backupId = $BackupId
        targetGuildId = [string]$targetGuild.id
        targetGuildName = [string]$targetGuild.name
        selectedSections = @($selection)
        steps = New-Object System.Collections.ArrayList
    }

    $roleState = @{ RoleIdMap = @{} }
    $channelState = @{ ChannelIdMap = @{} }
    $cleanupActions = New-Object System.Collections.ArrayList
    $cleanupWarnings = New-Object System.Collections.ArrayList

    if ($selection -contains "channels") {
        $channelCleanup = Remove-DiscordExtraChannels -RequestContext $requestContext -ServerId $serverResolution.ServerId -SavedChannels @($backup.Channels) -ExtraChannelDeleteMode ([string]$settings.ExtraChannelDeleteMode)
        foreach ($action in @($channelCleanup.Actions)) {
            [void]$cleanupActions.Add($action)
        }
        foreach ($warning in @($channelCleanup.Warnings)) {
            [void]$cleanupWarnings.Add($warning)
        }
    }

    if ($selection -contains "roles") {
        $roleCleanup = Remove-DiscordExtraRoles -RequestContext $requestContext -ServerId $serverResolution.ServerId -GuildId ([string]$targetGuild.id) -SavedRoles @($backup.Roles)
        foreach ($action in @($roleCleanup.Actions)) {
            [void]$cleanupActions.Add($action)
        }
        foreach ($warning in @($roleCleanup.Warnings)) {
            [void]$cleanupWarnings.Add($warning)
        }
    }

    if ($cleanupActions.Count -gt 0 -or $cleanupWarnings.Count -gt 0) {
        [void]$report.steps.Add([ordered]@{
            section = "cleanup"
            result = [ordered]@{
                Actions = @($cleanupActions.ToArray())
                Warnings = @($cleanupWarnings.ToArray())
            }
        })
    }

    if ($selection -contains "guild") {
        $stepResult = Restore-DiscordGuildSettings -RequestContext $requestContext -ServerId $serverResolution.ServerId -GuildSnapshot $backup.Guild
        [void]$report.steps.Add([ordered]@{ section = "guild"; result = $stepResult })
    }

    if ($selection -contains "roles") {
        $roleState = Restore-DiscordRoles -RequestContext $requestContext -ServerId $serverResolution.ServerId -GuildId ([string]$targetGuild.id) -SavedRoles @($backup.Roles)
        [void]$report.steps.Add([ordered]@{ section = "roles"; result = $roleState })
    }
    else {
        $currentRoles = Get-DiscordRoleListForBackup -RequestContext $requestContext -ServerId $serverResolution.ServerId
        foreach ($role in $currentRoles) {
            $roleState.RoleIdMap[[string]$role.id] = [string]$role.id
        }
    }

    if ($selection -contains "channels") {
        $channelState = Restore-DiscordChannels -RequestContext $requestContext -ServerId $serverResolution.ServerId -SavedChannels @($backup.Channels) -RoleIdMap $roleState.RoleIdMap
        [void]$report.steps.Add([ordered]@{ section = "channels"; result = $channelState })
    }

    if ($selection -contains "members") {
        $memberState = Restore-DiscordMembers -RequestContext $requestContext -ServerId $serverResolution.ServerId -GuildName ([string]$targetGuild.name) -BackupId $BackupId -SavedMembers @($backup.Members) -RoleIdMap $roleState.RoleIdMap -Settings $settings -MessagesRootPath $MessagesRootPath
        [void]$report.steps.Add([ordered]@{ section = "members"; result = $memberState })
    }

    return $report
}

Export-ModuleMember -Function Parse-DiscordBackupArguments, Resolve-DiscordBackupSelection, Get-DiscordBackupList, Get-DiscordBackupInfo, Test-DiscordBackup, Remove-DiscordBackup, Invoke-DiscordBackupCreate, Invoke-DiscordBackupRestore
