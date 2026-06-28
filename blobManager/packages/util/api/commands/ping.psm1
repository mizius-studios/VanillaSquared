$networkApiModule = Import-Module (Join-Path $PSScriptRoot "..\networkAPI.psm1") -Force -DisableNameChecking -PassThru
$script:TestNetworkConnectionCommand = $networkApiModule.ExportedCommands["Test-NetworkConnection"]

function Command-Ping {
    param(
        [string]$PackageName,
        [string]$ApiName,
        [string]$Uri,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30,
        [string]$Method = "GET"
    )

    if ($null -eq $script:TestNetworkConnectionCommand) {
        throw "Required command handle missing: Test-NetworkConnection"
    }

    Write-Host "Testing $ApiName for package '$PackageName'..."

    $result = & $script:TestNetworkConnectionCommand -Uri $Uri -Headers $Headers -TimeoutSeconds $TimeoutSeconds -Method $Method

    if ($result.Success) {
        Write-Host "Connection successful." -ForegroundColor Green
        Write-Host "Status: $($result.StatusCode) $($result.StatusDescription)"
    }
    else {
        Write-Host "Connection failed." -ForegroundColor Red

        if ($null -ne $result.StatusCode) {
            Write-Host "Status: $($result.StatusCode) $($result.StatusDescription)" -ForegroundColor Red
        }

        if (-not [string]::IsNullOrWhiteSpace([string]$result.ErrorMessage)) {
            Write-Host "Error: $($result.ErrorMessage)" -ForegroundColor Red
        }

        if (-not [string]::IsNullOrWhiteSpace([string]$result.RawContent)) {
            Write-Host "Response: $($result.RawContent)" -ForegroundColor DarkYellow
        }
    }

    return $result
}

Export-ModuleMember -Function Command-Ping
