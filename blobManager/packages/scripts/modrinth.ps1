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

$Cmd = @{
    NewMessageState    = $errorApiModule.ExportedCommands["New-MessageState"]
    AddErrorMessage    = $errorApiModule.ExportedCommands["Add-ErrorMessage"]
    ThrowIfErrors      = $errorApiModule.ExportedCommands["Throw-IfErrors"]
    WriteWarnings      = $errorApiModule.ExportedCommands["Write-Warnings"]
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

$state = & $Cmd.NewMessageState
$versionStatus = & $Cmd.TestConfigVersion -PackageName $PackageName -ScriptVersion $Version -Config $config -State $state

function Resolve-LocalPath {
    param(
        [string]$PathValue
    )

    if ([string]::IsNullOrWhiteSpace([string]$PathValue)) {
        return $null
    }

    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }

    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $PathValue))
}

function Add-RequiredConfigError {
    param(
        [string]$Value,
        [string]$ConfigKey
    )

    if ([string]::IsNullOrWhiteSpace([string]$Value)) {
        & $Cmd.AddErrorMessage -State $state -Message "modrinth $ConfigKey is missing in blobManager\\packages\\config\\modrinth.json"
    }
}

function Get-ModrinthAuthErrorMessage {
    param(
        $Result
    )

    if ($null -eq $Result) {
        return "Authenticated request failed."
    }

    if ($null -eq $Result.StatusCode) {
        if (-not [string]::IsNullOrWhiteSpace([string]$Result.ErrorMessage)) {
            return "Modrinth API/network failure: $($Result.ErrorMessage)"
        }

        return "Modrinth API/network failure."
    }

    if ($Result.StatusCode -eq 401) {
        return "Invalid Modrinth PAT. Check the token stored in the key file or env var."
    }

    if ($Result.StatusCode -eq 403) {
        return "Modrinth PAT was accepted but does not have sufficient scope for /user."
    }

    $detail = $null
    if ($null -ne $Result.Data) {
        if ($Result.Data.PSObject.Properties.Name -contains "description") {
            $detail = [string]$Result.Data.description
        }
        elseif ($Result.Data.PSObject.Properties.Name -contains "error") {
            $detail = [string]$Result.Data.error
        }
    }

    if (-not [string]::IsNullOrWhiteSpace([string]$detail)) {
        return "Modrinth API failure: $($Result.StatusCode) $($Result.StatusDescription) - $detail"
    }

    return "Modrinth API failure: $($Result.StatusCode) $($Result.StatusDescription)"
}

$Command = if ($args.Count -gt 0) { $args[0] } else { $null }

switch ($Command) {
    "-p" {
        Add-RequiredConfigError -Value $baseUrl -ConfigKey "network.baseUrl"
        Add-RequiredConfigError -Value $apiVersion -ConfigKey "network.apiVersion"
        Add-RequiredConfigError -Value $connectionTestUrl -ConfigKey "network.connectionTestUrl"
        Add-RequiredConfigError -Value $testEndpoint -ConfigKey "network.testEndpoint"

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

    "-gk" {
        $keyFilePath = Resolve-LocalPath -PathValue $authKeyFile
        $result = & $Cmd.CommandGetKey -PackageName $PackageName -KeyFilePath $keyFilePath -KeyName $authKeyName -EnvVarName $authEnvVar
        if ($result.Success) {
            exit 0
        }

        exit 1
    }

    "-getAuth" {
        $keyFilePath = Resolve-LocalPath -PathValue $authKeyFile
        $result = & $Cmd.CommandGetKey -PackageName $PackageName -KeyFilePath $keyFilePath -KeyName $authKeyName -EnvVarName $authEnvVar
        if ($result.Success) {
            exit 0
        }

        exit 1
    }

    "-auth" {
        Add-RequiredConfigError -Value $baseUrl -ConfigKey "network.baseUrl"
        Add-RequiredConfigError -Value $apiVersion -ConfigKey "network.apiVersion"
        Add-RequiredConfigError -Value $authenticatedTestEndpoint -ConfigKey "network.authenticatedTestEndpoint"
        Add-RequiredConfigError -Value $authKeyFile -ConfigKey "auth.keyFile"
        Add-RequiredConfigError -Value $authKeyName -ConfigKey "auth.keyName"
        Add-RequiredConfigError -Value $authEnvVar -ConfigKey "auth.envVar"
        & $Cmd.ThrowIfErrors -State $state

        $keyFilePath = Resolve-LocalPath -PathValue $authKeyFile
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
            $displayName = $null
            if ($null -ne $result.Data) {
                if ($result.Data.PSObject.Properties.Name -contains "username") {
                    $displayName = [string]$result.Data.username
                }
                elseif ($result.Data.PSObject.Properties.Name -contains "id") {
                    $displayName = [string]$result.Data.id
                }
            }

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

        $authError = Get-ModrinthAuthErrorMessage -Result $result
        if (-not [string]::IsNullOrWhiteSpace([string]$authError)) {
            Write-Host $authError -ForegroundColor Red
        }

        if (-not [string]::IsNullOrWhiteSpace([string]$result.RawContent)) {
            Write-Host "Response: $($result.RawContent)" -ForegroundColor DarkYellow
        }

        exit 1
    }

    "-v" {
        & $Cmd.CommandVersion -PackageName $PackageName -Version $Version -ConfigVersion $versionStatus.ConfigVersion -WarningMessage $versionStatus.WarningMessage
        exit 0
    }

    "-?" {
        & $Cmd.WriteWarnings -State $state
        & $Cmd.CommandHelp -PackageName $PackageName -Commands @("modrinth -p", "modrinth -auth", "modrinth -getAuth", "modrinth -v", "modrinth -?")
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
