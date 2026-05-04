$ErrorActionPreference = "Stop"
$Version = "1.0.0"
$PackageName = "modrinth"

$errorApiModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\errorAPI.psm1") -Force -DisableNameChecking -PassThru
$configApiModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\configAPI.psm1") -Force -DisableNameChecking -PassThru
$networkApiModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\networkAPI.psm1") -Force -DisableNameChecking -PassThru
$keyApiModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\keyAPI.psm1") -Force -DisableNameChecking -PassThru
$versionModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\commands\version.psm1") -Force -DisableNameChecking -PassThru
$helpModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\commands\help.psm1") -Force -DisableNameChecking -PassThru
$pingModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\commands\ping.psm1") -Force -DisableNameChecking -PassThru
$commonModule = Import-Module (Join-Path $PSScriptRoot "..\util\modrinth\common.psm1") -Force -DisableNameChecking -PassThru
$authModule = Import-Module (Join-Path $PSScriptRoot "..\util\modrinth\auth.psm1") -Force -DisableNameChecking -PassThru
$projectModule = Import-Module (Join-Path $PSScriptRoot "..\util\modrinth\project.psm1") -Force -DisableNameChecking -PassThru
$releaseModule = Import-Module (Join-Path $PSScriptRoot "..\util\modrinth\release.psm1") -Force -DisableNameChecking -PassThru
$anytypeCommonModule = Import-Module (Join-Path $PSScriptRoot "..\util\anytype\common.psm1") -Force -DisableNameChecking -PassThru
$anytypeReleaseModule = Import-Module (Join-Path $PSScriptRoot "..\util\anytype\release.psm1") -Force -DisableNameChecking -PassThru

$Cmd = @{
    NewMessageState    = $errorApiModule.ExportedCommands["New-MessageState"]
    AddErrorMessage    = $errorApiModule.ExportedCommands["Add-ErrorMessage"]
    ThrowIfErrors      = $errorApiModule.ExportedCommands["Throw-IfErrors"]
    WriteWarnings      = $errorApiModule.ExportedCommands["Write-Warnings"]
    ExitBadUsage       = $errorApiModule.ExportedCommands["Exit-BadUsage"]
    GetJsonConfig      = $configApiModule.ExportedCommands["Get-JsonConfig"]
    GetConfigValue     = $configApiModule.ExportedCommands["Get-ConfigValue"]
    TestConfigVersion  = $configApiModule.ExportedCommands["Test-ConfigVersion"]
    InvokeNetworkRequest = $networkApiModule.ExportedCommands["Invoke-NetworkRequest"]
    MergeAuthorizationHeader = $networkApiModule.ExportedCommands["Merge-AuthorizationHeader"]
    JoinApiUri         = $networkApiModule.ExportedCommands["Join-ApiUri"]
    ResolveKeyValue    = $keyApiModule.ExportedCommands["Resolve-KeyValue"]
    CommandGetKey      = $keyApiModule.ExportedCommands["Command-GetKey"]
    CommandVersion     = $versionModule.ExportedCommands["Command-Version"]
    CommandHelp        = $helpModule.ExportedCommands["Command-Help"]
    CommandPing        = $pingModule.ExportedCommands["Command-Ping"]
    ResolveLocalPath   = $commonModule.ExportedCommands["Resolve-LocalPath"]
    AddRequiredConfigError = $commonModule.ExportedCommands["Add-RequiredConfigError"]
    GetAuthErrorMessage = $authModule.ExportedCommands["Get-ModrinthAuthErrorMessage"]
    GetDisplayName     = $authModule.ExportedCommands["Get-ModrinthDisplayName"]
    GetProjectErrorMessage = $projectModule.ExportedCommands["Get-ModrinthProjectErrorMessage"]
    WriteProjectSummary = $projectModule.ExportedCommands["Write-ModrinthProjectSummary"]
    WriteProjectList   = $projectModule.ExportedCommands["Write-ModrinthProjectList"]
    GetModrinthAuthorizationHeaders = $releaseModule.ExportedCommands["Get-ModrinthAuthorizationHeaders"]
    GetModMetadata     = $releaseModule.ExportedCommands["Get-ModMetadata"]
    GetModrinthDependencyPayload = $releaseModule.ExportedCommands["Get-ModrinthDependencyPayload"]
    GetModrinthReleaseArtifacts = $releaseModule.ExportedCommands["Get-ModrinthReleaseArtifacts"]
    WriteModrinthReleasePreview = $releaseModule.ExportedCommands["Write-ModrinthReleasePreview"]
    ReadModrinthReleaseConfirmation = $releaseModule.ExportedCommands["Read-ModrinthReleaseConfirmation"]
    NewModrinthVersionMultipartRequest = $releaseModule.ExportedCommands["New-ModrinthVersionMultipartRequest"]
    ResolveAnytypeLocalPath = $anytypeCommonModule.ExportedCommands["Resolve-LocalPath"]
    GetAnytypeAuthorizationHeaders = $anytypeReleaseModule.ExportedCommands["Get-AnytypeAuthorizationHeaders"]
    FindAnytypeSpaceById = $anytypeReleaseModule.ExportedCommands["Find-AnytypeSpaceById"]
    FindAnytypeTypeByName = $anytypeReleaseModule.ExportedCommands["Find-AnytypeTypeByName"]
    FindAnytypeObjectByName = $anytypeReleaseModule.ExportedCommands["Find-AnytypeObjectByName"]
    GetAnytypeObjectDetails = $anytypeReleaseModule.ExportedCommands["Get-AnytypeObjectDetails"]
    FindAnytypeChildObjectByName = $anytypeReleaseModule.ExportedCommands["Find-AnytypeChildObjectByName"]
    ResolveAnytypeLinkedObjectByName = $anytypeReleaseModule.ExportedCommands["Resolve-AnytypeLinkedObjectByName"]
    GetAnytypeObjectMarkdownBody = $anytypeReleaseModule.ExportedCommands["Get-AnytypeObjectMarkdownBody"]
}

foreach ($entry in $Cmd.GetEnumerator()) {
    if ($null -eq $entry.Value) {
        throw "Required command handle missing: $($entry.Key)"
    }
}

$configPath = Join-Path $PSScriptRoot "..\config\modrinth.json"
$config = & $Cmd.GetJsonConfig -Path $configPath -Fallback @{}
$networkConfig = & $Cmd.GetConfigValue -Config $config -Key "network" -DefaultValue @{}
$headers = & $Cmd.GetConfigValue -Config $networkConfig -Key "headers" -DefaultValue @{}
$baseUrl = & $Cmd.GetConfigValue -Config $networkConfig -Key "baseUrl" -DefaultValue "https://api.modrinth.com"
$apiVersion = & $Cmd.GetConfigValue -Config $networkConfig -Key "apiVersion" -DefaultValue "v2"
$connectionTestUrl = & $Cmd.GetConfigValue -Config $networkConfig -Key "connectionTestUrl" -DefaultValue "https://api.modrinth.com/"
$testEndpoint = & $Cmd.GetConfigValue -Config $networkConfig -Key "testEndpoint" -DefaultValue "/tag/game_version"
$authenticatedTestEndpoint = & $Cmd.GetConfigValue -Config $networkConfig -Key "authenticatedTestEndpoint" -DefaultValue "/user"
$timeoutSeconds = & $Cmd.GetConfigValue -Config $networkConfig -Key "timeoutSeconds" -DefaultValue 30
$authConfig = & $Cmd.GetConfigValue -Config $config -Key "auth" -DefaultValue @{}
$authKeyFile = & $Cmd.GetConfigValue -Config $authConfig -Key "keyFile" -DefaultValue "blobManager/config/keys/modrinth.txt"
$authKeyName = & $Cmd.GetConfigValue -Config $authConfig -Key "keyName" -DefaultValue "ModrinthPATKey"
$authEnvVar = & $Cmd.GetConfigValue -Config $authConfig -Key "envVar" -DefaultValue "MODRINTH_PAT"
$releaseConfig = & $Cmd.GetConfigValue -Config $config -Key "release" -DefaultValue @{}
$artifactRootValue = & $Cmd.GetConfigValue -Config $releaseConfig -Key "artifactRoot" -DefaultValue "blobManager/packages/data/modrinth/version"
$defaultLoaders = & $Cmd.GetConfigValue -Config $releaseConfig -Key "defaultLoaders" -DefaultValue @("fabric")
$dependencyMap = & $Cmd.GetConfigValue -Config $releaseConfig -Key "dependencyMap" -DefaultValue @{}
$defaultStatus = & $Cmd.GetConfigValue -Config $releaseConfig -Key "status" -DefaultValue $null
$defaultFeatured = & $Cmd.GetConfigValue -Config $releaseConfig -Key "featured" -DefaultValue $null

$anytypeConfigPath = Join-Path $PSScriptRoot "..\config\anytype.json"
$anytypeConfig = & $Cmd.GetJsonConfig -Path $anytypeConfigPath -Fallback @{}
$anytypeNetworkConfig = & $Cmd.GetConfigValue -Config $anytypeConfig -Key "network" -DefaultValue @{}
$anytypeHeaders = & $Cmd.GetConfigValue -Config $anytypeNetworkConfig -Key "headers" -DefaultValue @{}
$anytypeBaseUrl = & $Cmd.GetConfigValue -Config $anytypeNetworkConfig -Key "baseUrl" -DefaultValue "http://localhost:31009"
$anytypeApiVersion = & $Cmd.GetConfigValue -Config $anytypeNetworkConfig -Key "apiVersion" -DefaultValue "v1"
$anytypeTimeoutSeconds = & $Cmd.GetConfigValue -Config $anytypeNetworkConfig -Key "timeoutSeconds" -DefaultValue 30
$anytypeAuthConfig = & $Cmd.GetConfigValue -Config $anytypeConfig -Key "auth" -DefaultValue @{}
$anytypeAuthKeyFile = & $Cmd.GetConfigValue -Config $anytypeAuthConfig -Key "keyFile" -DefaultValue "blobManager/packages/config/keys/personal.txt"
$anytypeAuthKeyName = & $Cmd.GetConfigValue -Config $anytypeAuthConfig -Key "keyName" -DefaultValue "AnytypeApiKey"
$anytypeAuthEnvVar = & $Cmd.GetConfigValue -Config $anytypeAuthConfig -Key "envVar" -DefaultValue "ANYTYPE_API_KEY"
$anytypeReleaseConfig = & $Cmd.GetConfigValue -Config $anytypeConfig -Key "release" -DefaultValue @{}
$anytypeChannels = & $Cmd.GetConfigValue -Config $anytypeReleaseConfig -Key "channels" -DefaultValue @{}
$updateTypeName = & $Cmd.GetConfigValue -Config $anytypeReleaseConfig -Key "updateTypeName" -DefaultValue "Updates"
$updateMatchStrategy = & $Cmd.GetConfigValue -Config $anytypeReleaseConfig -Key "updateMatchStrategy" -DefaultValue "exact-name"
$changelogChildName = & $Cmd.GetConfigValue -Config $anytypeReleaseConfig -Key "changelogChildName" -DefaultValue "Changelogs"

$state = & $Cmd.NewMessageState
$versionStatus = & $Cmd.TestConfigVersion -PackageName $PackageName -ScriptVersion $Version -Config $config -State $state

$Command = if ($args.Count -gt 0) { $args[0] } else { $null }
$Rest = @()
if ($args.Count -gt 1) { $Rest += $args[1..($args.Count - 1)] }

switch ($Command) {
    "-p" {
        & $Cmd.AddRequiredConfigError -Value $baseUrl -ConfigKey "network.baseUrl" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $apiVersion -ConfigKey "network.apiVersion" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $connectionTestUrl -ConfigKey "network.connectionTestUrl" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $testEndpoint -ConfigKey "network.testEndpoint" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state

        & $Cmd.ThrowIfErrors -State $state

        $requestUri = & $Cmd.JoinApiUri -BaseUrl $baseUrl -ApiVersion $apiVersion -Endpoint $testEndpoint
        Write-Host "Resolved API route: $requestUri"
        $result = & $Cmd.CommandPing -PackageName $PackageName -ApiName "Modrinth API Root" -Uri $connectionTestUrl -Headers $headers -TimeoutSeconds $timeoutSeconds
        & $Cmd.WriteWarnings -State $state

        if ($result.Success) {
            exit 0
        }

        exit 1
    }

    "-getAuth" {
        $keyFilePath = & $Cmd.ResolveLocalPath -PathValue $authKeyFile
        $result = & $Cmd.CommandGetKey -PackageName $PackageName -KeyFilePath $keyFilePath -KeyName $authKeyName -EnvVarName $authEnvVar
        if ($result.Success) {
            exit 0
        }

        exit 1
    }

    "-auth" {
        & $Cmd.AddRequiredConfigError -Value $baseUrl -ConfigKey "network.baseUrl" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $apiVersion -ConfigKey "network.apiVersion" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $authenticatedTestEndpoint -ConfigKey "network.authenticatedTestEndpoint" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $authKeyFile -ConfigKey "auth.keyFile" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $authKeyName -ConfigKey "auth.keyName" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $authEnvVar -ConfigKey "auth.envVar" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.ThrowIfErrors -State $state

        $keyFilePath = & $Cmd.ResolveLocalPath -PathValue $authKeyFile
        $tokenResolution = & $Cmd.ResolveKeyValue -KeyFilePath $keyFilePath -KeyName $authKeyName -EnvVarName $authEnvVar
        if (-not $tokenResolution.Success) {
            Write-Host "Missing Modrinth PAT. No local key or env var was found." -ForegroundColor Red
            Write-Host "Expected local file: $keyFilePath" -ForegroundColor Red
            Write-Host "Expected file format: $authKeyName=mrp_..." -ForegroundColor DarkYellow
            Write-Host "Fallback env var: $authEnvVar" -ForegroundColor DarkYellow
            exit 1
        }

        $requestUri = & $Cmd.JoinApiUri -BaseUrl $baseUrl -ApiVersion $apiVersion -Endpoint $authenticatedTestEndpoint
        $authHeaders = & $Cmd.MergeAuthorizationHeader -Headers $headers -Token $tokenResolution.Value
        Write-Host "Resolved API route: $requestUri"
        Write-Host "Auth source: $($tokenResolution.Source)"

        $result = & $Cmd.InvokeNetworkRequest -Uri $requestUri -Method "GET" -Headers $authHeaders -TimeoutSeconds $timeoutSeconds
        if ($result.Success) {
            $displayName = & $Cmd.GetDisplayName -Data $result.Data

            Write-Host "Authenticated request successful." -ForegroundColor Green
            Write-Host "Status: $($result.StatusCode) $($result.StatusDescription)"
            if (-not [string]::IsNullOrWhiteSpace([string]$displayName)) {
                Write-Host "Identity: $displayName"
            }
            & $Cmd.WriteWarnings -State $state
            exit 0
        }

        Write-Host "Authenticated request failed." -ForegroundColor Red
        if ($null -ne $result.StatusCode) {
            Write-Host "Status: $($result.StatusCode) $($result.StatusDescription)" -ForegroundColor Red
        }

        $authError = & $Cmd.GetAuthErrorMessage -Result $result
        if (-not [string]::IsNullOrWhiteSpace([string]$authError)) {
            Write-Host $authError -ForegroundColor Red
        }

        if (-not [string]::IsNullOrWhiteSpace([string]$result.RawContent)) {
            Write-Host "Response: $($result.RawContent)" -ForegroundColor DarkYellow
        }

        exit 1
    }
    "getInfo" {
        & $Cmd.AddRequiredConfigError -Value $baseUrl -ConfigKey "network.baseUrl" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $apiVersion -ConfigKey "network.apiVersion" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.ThrowIfErrors -State $state

        if ($Rest.Count -lt 1) {
            Write-Host "Usage: .\blob.ps1 modrinth getInfo <slug-or-id>"
            exit 1
        }

        $projectRef = $Rest[0]
        $requestUri = & $Cmd.JoinApiUri -BaseUrl $baseUrl -ApiVersion $apiVersion -Endpoint "/project/$projectRef"
        $result = & $Cmd.InvokeNetworkRequest -Uri $requestUri -Method "GET" -Headers $headers -TimeoutSeconds $timeoutSeconds

        if ($result.Success -and $null -ne $result.Data) {
            & $Cmd.WriteProjectSummary -Project $result.Data
            & $Cmd.WriteWarnings -State $state
            exit 0
        }

        $errorMessage = & $Cmd.GetProjectErrorMessage -Result $result -ProjectRef $projectRef
        Write-Host $errorMessage -ForegroundColor Red

        if (-not [string]::IsNullOrWhiteSpace([string]$result.RawContent)) {
            Write-Host "Response: $($result.RawContent)" -ForegroundColor DarkYellow
        }

        exit 1
    }
    "getMyProjects" {
        & $Cmd.AddRequiredConfigError -Value $baseUrl -ConfigKey "network.baseUrl" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $apiVersion -ConfigKey "network.apiVersion" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $authKeyFile -ConfigKey "auth.keyFile" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $authKeyName -ConfigKey "auth.keyName" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $authEnvVar -ConfigKey "auth.envVar" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.ThrowIfErrors -State $state

        $keyFilePath = & $Cmd.ResolveLocalPath -PathValue $authKeyFile
        $tokenResolution = & $Cmd.ResolveKeyValue -KeyFilePath $keyFilePath -KeyName $authKeyName -EnvVarName $authEnvVar
        if (-not $tokenResolution.Success) {
            Write-Host "Missing Modrinth PAT. No local key or env var was found." -ForegroundColor Red
            Write-Host "Expected local file: $keyFilePath" -ForegroundColor Red
            Write-Host "Expected file format: $authKeyName=mrp_..." -ForegroundColor DarkYellow
            Write-Host "Fallback env var: $authEnvVar" -ForegroundColor DarkYellow
            exit 1
        }

        $authHeaders = & $Cmd.MergeAuthorizationHeader -Headers $headers -Token $tokenResolution.Value

        $userRequestUri = & $Cmd.JoinApiUri -BaseUrl $baseUrl -ApiVersion $apiVersion -Endpoint "/user"
        $userResult = & $Cmd.InvokeNetworkRequest -Uri $userRequestUri -Method "GET" -Headers $authHeaders -TimeoutSeconds $timeoutSeconds
        if (-not $userResult.Success -or $null -eq $userResult.Data) {
            Write-Host "Failed to resolve the authenticated Modrinth user." -ForegroundColor Red
            if ($null -ne $userResult.StatusCode) {
                Write-Host "Status: $($userResult.StatusCode) $($userResult.StatusDescription)" -ForegroundColor Red
            }

            $authError = & $Cmd.GetAuthErrorMessage -Result $userResult
            if (-not [string]::IsNullOrWhiteSpace([string]$authError)) {
                Write-Host $authError -ForegroundColor Red
            }

            if (-not [string]::IsNullOrWhiteSpace([string]$userResult.RawContent)) {
                Write-Host "Response: $($userResult.RawContent)" -ForegroundColor DarkYellow
            }

            exit 1
        }

        $userId = [string]$userResult.Data.id
        if ([string]::IsNullOrWhiteSpace($userId)) {
            Write-Host "Authenticated Modrinth user did not include an id." -ForegroundColor Red
            exit 1
        }

        $requestUri = & $Cmd.JoinApiUri -BaseUrl $baseUrl -ApiVersion $apiVersion -Endpoint "/user/$userId/projects"
        $result = & $Cmd.InvokeNetworkRequest -Uri $requestUri -Method "GET" -Headers $authHeaders -TimeoutSeconds $timeoutSeconds

        if ($result.Success -and $null -ne $result.Data) {
            & $Cmd.WriteProjectList -Projects $result.Data
            exit 0
        }

        Write-Host "Failed to list Modrinth projects for the authenticated user." -ForegroundColor Red
        if ($null -ne $result.StatusCode) {
            Write-Host "Status: $($result.StatusCode) $($result.StatusDescription)" -ForegroundColor Red
        }

        $authError = & $Cmd.GetAuthErrorMessage -Result $result
        if (-not [string]::IsNullOrWhiteSpace([string]$authError)) {
            Write-Host $authError -ForegroundColor Red
        }

        if (-not [string]::IsNullOrWhiteSpace([string]$result.RawContent)) {
            Write-Host "Response: $($result.RawContent)" -ForegroundColor DarkYellow
        }

        exit 1
    }
    "release" {
        if ($Rest.Count -lt 2) {
            & $Cmd.ExitBadUsage -Usage ".\blob.ps1 modrinth release <projectModrinth> <anytypeChannel>" -ArgsLabel "release"
        }

        if ($updateMatchStrategy -ne "exact-name") {
            Write-Host "Unsupported Anytype updateMatchStrategy: $updateMatchStrategy" -ForegroundColor Red
            exit 1
        }

        $projectRef = [string]$Rest[0]
        $anytypeChannel = [string]$Rest[1]

        & $Cmd.AddRequiredConfigError -Value $baseUrl -ConfigKey "network.baseUrl" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $apiVersion -ConfigKey "network.apiVersion" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $authKeyFile -ConfigKey "auth.keyFile" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $authKeyName -ConfigKey "auth.keyName" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $authEnvVar -ConfigKey "auth.envVar" -PackageName $PackageName -ConfigPath "blobManager\\packages\\config\\modrinth.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $anytypeBaseUrl -ConfigKey "network.baseUrl" -PackageName "anytype" -ConfigPath "blobManager\\packages\\config\\anytype.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $anytypeApiVersion -ConfigKey "network.apiVersion" -PackageName "anytype" -ConfigPath "blobManager\\packages\\config\\anytype.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $anytypeAuthKeyFile -ConfigKey "auth.keyFile" -PackageName "anytype" -ConfigPath "blobManager\\packages\\config\\anytype.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $anytypeAuthKeyName -ConfigKey "auth.keyName" -PackageName "anytype" -ConfigPath "blobManager\\packages\\config\\anytype.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.AddRequiredConfigError -Value $anytypeAuthEnvVar -ConfigKey "auth.envVar" -PackageName "anytype" -ConfigPath "blobManager\\packages\\config\\anytype.json" -AddErrorMessage $Cmd.AddErrorMessage -State $state
        & $Cmd.ThrowIfErrors -State $state

        $channelConfig = $null
        if ($anytypeChannels -is [hashtable]) {
            if ($anytypeChannels.ContainsKey($anytypeChannel)) {
                $channelConfig = $anytypeChannels[$anytypeChannel]
            }
        }
        elseif ($null -ne $anytypeChannels -and $anytypeChannels.PSObject.Properties.Name -contains $anytypeChannel) {
            $channelConfig = $anytypeChannels.$anytypeChannel
        }

        if ($null -eq $channelConfig) {
            Write-Host "Unknown Anytype channel key: $anytypeChannel" -ForegroundColor Red
            exit 1
        }
        $spaceId = $null
        if ($channelConfig -is [hashtable]) {
            $spaceId = [string](& $Cmd.GetConfigValue -Config $channelConfig -Key "spaceId" -DefaultValue $null)
        }
        elseif ($null -ne $channelConfig -and $channelConfig.PSObject.Properties.Name -contains "spaceId") {
            $spaceId = [string]$channelConfig.spaceId
        }
        if ([string]::IsNullOrWhiteSpace($spaceId)) {
            Write-Host "Anytype channel '$anytypeChannel' is missing spaceId." -ForegroundColor Red
            exit 1
        }

        $fabricModJsonPath = & $Cmd.ResolveLocalPath -PathValue "src/main/resources/fabric.mod.json"
        try {
            $modMetadata = & $Cmd.GetModMetadata -FabricModJsonPath $fabricModJsonPath
        }
        catch {
            Write-Host $_.Exception.Message -ForegroundColor Red
            exit 1
        }

        $artifactRoot = & $Cmd.ResolveLocalPath -PathValue $artifactRootValue
        try {
            $artifacts = & $Cmd.GetModrinthReleaseArtifacts -ArtifactRoot $artifactRoot -Version $modMetadata.Version -ReleaseConfig $releaseConfig
        }
        catch {
            Write-Host $_.Exception.Message -ForegroundColor Red
            exit 1
        }

        $dependencies = & $Cmd.GetModrinthDependencyPayload -FabricDependencies $modMetadata.Depends -DependencyMap $dependencyMap

        $modrinthKeyFilePath = & $Cmd.ResolveLocalPath -PathValue $authKeyFile
        $modrinthAuth = & $Cmd.GetModrinthAuthorizationHeaders -Headers $headers -KeyFilePath $modrinthKeyFilePath -KeyName $authKeyName -EnvVarName $authEnvVar -ResolveKeyValue $Cmd.ResolveKeyValue -MergeAuthorizationHeader $Cmd.MergeAuthorizationHeader
        if (-not $modrinthAuth.Success) {
            Write-Host $modrinthAuth.Error -ForegroundColor Red
            Write-Host "Expected local file: $modrinthKeyFilePath" -ForegroundColor Red
            Write-Host "Expected file format: $authKeyName=mrp_..." -ForegroundColor DarkYellow
            Write-Host "Fallback env var: $authEnvVar" -ForegroundColor DarkYellow
            exit 1
        }

        $projectLookupUri = & $Cmd.JoinApiUri -BaseUrl $baseUrl -ApiVersion $apiVersion -Endpoint "/project/$projectRef"
        $projectLookupResult = & $Cmd.InvokeNetworkRequest -Uri $projectLookupUri -Method "GET" -Headers $headers -TimeoutSeconds $timeoutSeconds
        if (-not $projectLookupResult.Success -or $null -eq $projectLookupResult.Data) {
            $errorMessage = & $Cmd.GetProjectErrorMessage -Result $projectLookupResult -ProjectRef $projectRef
            Write-Host $errorMessage -ForegroundColor Red
            if (-not [string]::IsNullOrWhiteSpace([string]$projectLookupResult.RawContent)) {
                Write-Host "Response: $($projectLookupResult.RawContent)" -ForegroundColor DarkYellow
            }
            exit 1
        }

        $resolvedProjectId = [string]$projectLookupResult.Data.id
        if ([string]::IsNullOrWhiteSpace($resolvedProjectId)) {
            Write-Host "Resolved Modrinth project did not include an id." -ForegroundColor Red
            exit 1
        }

        $anytypeKeyFilePath = & $Cmd.ResolveAnytypeLocalPath -PathValue $anytypeAuthKeyFile
        $anytypeAuth = & $Cmd.GetAnytypeAuthorizationHeaders -Headers $anytypeHeaders -KeyFilePath $anytypeKeyFilePath -KeyName $anytypeAuthKeyName -EnvVarName $anytypeAuthEnvVar -ResolveKeyValue $Cmd.ResolveKeyValue -MergeAuthorizationHeader $Cmd.MergeAuthorizationHeader
        if (-not $anytypeAuth.Success) {
            Write-Host $anytypeAuth.Error -ForegroundColor Red
            Write-Host "Expected local file: $anytypeKeyFilePath" -ForegroundColor Red
            Write-Host "Expected file format: $anytypeAuthKeyName=<your_api_key>" -ForegroundColor DarkYellow
            Write-Host "Fallback env var: $anytypeAuthEnvVar" -ForegroundColor DarkYellow
            exit 1
        }

        $spaceLookup = & $Cmd.FindAnytypeSpaceById -BaseUrl $anytypeBaseUrl -ApiVersion $anytypeApiVersion -Headers $anytypeAuth.Headers -SpaceId $spaceId -TimeoutSeconds $anytypeTimeoutSeconds -JoinApiUri $Cmd.JoinApiUri -InvokeNetworkRequest $Cmd.InvokeNetworkRequest
        if (-not $spaceLookup.Success) {
            Write-Host $spaceLookup.Error -ForegroundColor Red
            exit 1
        }

        $typeLookup = & $Cmd.FindAnytypeTypeByName -BaseUrl $anytypeBaseUrl -ApiVersion $anytypeApiVersion -Headers $anytypeAuth.Headers -SpaceId $spaceId -TypeName $updateTypeName -TimeoutSeconds $anytypeTimeoutSeconds -JoinApiUri $Cmd.JoinApiUri -InvokeNetworkRequest $Cmd.InvokeNetworkRequest
        if (-not $typeLookup.Success) {
            Write-Host $typeLookup.Error -ForegroundColor Red
            exit 1
        }

        $updateLookup = & $Cmd.FindAnytypeObjectByName -BaseUrl $anytypeBaseUrl -ApiVersion $anytypeApiVersion -Headers $anytypeAuth.Headers -SpaceId $spaceId -ObjectName $modMetadata.Version -TypeName $updateTypeName -TimeoutSeconds $anytypeTimeoutSeconds -JoinApiUri $Cmd.JoinApiUri -InvokeNetworkRequest $Cmd.InvokeNetworkRequest
        if (-not $updateLookup.Success) {
            Write-Host $updateLookup.Error -ForegroundColor Red
            exit 1
        }

        $updateId = [string]$updateLookup.Object.id
        if ([string]::IsNullOrWhiteSpace($updateId)) {
            $updateId = [string]$updateLookup.Object.object_id
        }
        if ([string]::IsNullOrWhiteSpace($updateId)) {
            Write-Host "Matched Anytype update object did not include an id." -ForegroundColor Red
            exit 1
        }

        $updateDetails = & $Cmd.GetAnytypeObjectDetails -BaseUrl $anytypeBaseUrl -ApiVersion $anytypeApiVersion -Headers $anytypeAuth.Headers -SpaceId $spaceId -ObjectId $updateId -TimeoutSeconds $anytypeTimeoutSeconds -JoinApiUri $Cmd.JoinApiUri -InvokeNetworkRequest $Cmd.InvokeNetworkRequest
        if (-not $updateDetails.Success) {
            Write-Host $updateDetails.Error -ForegroundColor Red
            exit 1
        }

        $changelogObject = $null
        $linkedChangelog = & $Cmd.ResolveAnytypeLinkedObjectByName -ParentObject $updateDetails.Object -LinkedName $changelogChildName -BaseUrl $anytypeBaseUrl -ApiVersion $anytypeApiVersion -Headers $anytypeAuth.Headers -SpaceId $spaceId -TimeoutSeconds $anytypeTimeoutSeconds -JoinApiUri $Cmd.JoinApiUri -InvokeNetworkRequest $Cmd.InvokeNetworkRequest
        if ($linkedChangelog.Success) {
            $changelogObject = $linkedChangelog.Object
        }
        else {
            $changelogChild = & $Cmd.FindAnytypeChildObjectByName -ParentObject $updateDetails.Object -ChildName $changelogChildName
            if (-not $changelogChild.Success) {
                Write-Host $linkedChangelog.Error -ForegroundColor Red
                exit 1
            }

            $changelogChildId = [string]$changelogChild.Child.id
            if ([string]::IsNullOrWhiteSpace($changelogChildId)) {
                $changelogChildId = [string]$changelogChild.Child.object_id
            }

            $changelogObject = $changelogChild.Child
            if (-not [string]::IsNullOrWhiteSpace($changelogChildId)) {
                $changelogDetails = & $Cmd.GetAnytypeObjectDetails -BaseUrl $anytypeBaseUrl -ApiVersion $anytypeApiVersion -Headers $anytypeAuth.Headers -SpaceId $spaceId -ObjectId $changelogChildId -TimeoutSeconds $anytypeTimeoutSeconds -JoinApiUri $Cmd.JoinApiUri -InvokeNetworkRequest $Cmd.InvokeNetworkRequest
                if (-not $changelogDetails.Success) {
                    Write-Host $changelogDetails.Error -ForegroundColor Red
                    exit 1
                }
                $changelogObject = $changelogDetails.Object
            }
        }

        $changelogBody = & $Cmd.GetAnytypeObjectMarkdownBody -Object $changelogObject
        if ([string]::IsNullOrWhiteSpace([string]$changelogBody)) {
            Write-Host "Anytype changelog body is empty." -ForegroundColor Red
            exit 1
        }

        $artifactPaths = @($artifacts | ForEach-Object { $_.FullName })
        $fileParts = @()
        for ($index = 0; $index -lt $artifactPaths.Count; $index++) {
            $fileParts += "file$index"
        }

        $payload = @{
            project_id = $resolvedProjectId
            version_title = $modMetadata.Version
            version_number = $modMetadata.Version
            version_type = $modMetadata.ReleaseType
            changelog = $changelogBody
            game_versions = @($modMetadata.GameVersions)
            loaders = @($defaultLoaders)
            dependencies = @($dependencies)
            file_parts = @($fileParts)
            featured = if ($null -ne $defaultFeatured) { [bool]$defaultFeatured } else { $false }
        }

        if ($fileParts.Count -gt 0) {
            $payload.primary_file = $fileParts[0]
        }

        if (-not [string]::IsNullOrWhiteSpace([string]$defaultStatus)) {
            $payload.status = [string]$defaultStatus
        }

        $releaseData = @{
            ProjectRef = $projectRef
            ProjectId = $resolvedProjectId
            AnytypeChannel = $anytypeChannel
            SpaceId = $spaceId
            ReleaseType = $modMetadata.ReleaseType
            VersionNumber = $modMetadata.Version
            GameVersions = @($modMetadata.GameVersions)
            Loaders = @($defaultLoaders)
            Dependencies = @($dependencies)
            FileParts = @($fileParts)
            PrimaryFile = if ($fileParts.Count -gt 0) { $fileParts[0] } else { $null }
            Artifacts = @($artifactPaths)
            Changelog = $changelogBody
        }

        & $Cmd.WriteModrinthReleasePreview -ReleaseData $releaseData
        $confirmed = & $Cmd.ReadModrinthReleaseConfirmation
        if (-not $confirmed) {
            Write-Host "Release canceled." -ForegroundColor Yellow
            exit 0
        }

        $requestUri = & $Cmd.JoinApiUri -BaseUrl $baseUrl -ApiVersion $apiVersion -Endpoint "/version"
        $uploadResult = & $Cmd.NewModrinthVersionMultipartRequest -Uri $requestUri -Headers $modrinthAuth.Headers -Payload $payload -FilePaths $artifactPaths -TimeoutSeconds $timeoutSeconds
        if ($uploadResult.Success) {
            Write-Host "Modrinth version created successfully." -ForegroundColor Green
            if ($null -ne $uploadResult.Data) {
                if ($uploadResult.Data.PSObject.Properties.Name -contains "id") {
                    Write-Host "Version ID: $($uploadResult.Data.id)"
                }
                if ($uploadResult.Data.PSObject.Properties.Name -contains "version_number") {
                    Write-Host "Version number: $($uploadResult.Data.version_number)"
                }
            }
            exit 0
        }

        Write-Host "Modrinth version create failed." -ForegroundColor Red
        if ($null -ne $uploadResult.StatusCode) {
            Write-Host "Status: $($uploadResult.StatusCode) $($uploadResult.StatusDescription)" -ForegroundColor Red
        }
        if (-not [string]::IsNullOrWhiteSpace([string]$uploadResult.RawContent)) {
            Write-Host "Response: $($uploadResult.RawContent)" -ForegroundColor DarkYellow
        }
        exit 1
    }

    "-v" {
        & $Cmd.CommandVersion -PackageName $PackageName -Version $Version -ConfigVersion $versionStatus.ConfigVersion -WarningMessage $versionStatus.WarningMessage
        exit 0
    }

    "-?" {
        & $Cmd.WriteWarnings -State $state
        & $Cmd.CommandHelp -PackageName $PackageName -Commands @("modrinth -p", "modrinth -auth", "modrinth -getAuth", "modrinth getInfo <slug-or-id>", "modrinth getMyProjects", "modrinth release <projectModrinth> <anytypeChannel>", "modrinth -v", "modrinth -?")
        Write-Host "Auth key file: $authKeyFile"
        Write-Host "Auth key format: $authKeyName=mrp_..."
        Write-Host "Automation env var: $authEnvVar"
        exit 0
    }

    default {
        & $Cmd.WriteWarnings -State $state
        Write-Host "Unknown modrinth command: $Command"
        Write-Host "Run: .\blob.ps1 modrinth -?"
        exit 1
    }
}
