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

function Get-ModrinthDisplayName {
    param(
        $Data
    )

    if ($null -eq $Data) {
        return $null
    }

    if ($Data.PSObject.Properties.Name -contains "username") {
        return [string]$Data.username
    }

    if ($Data.PSObject.Properties.Name -contains "id") {
        return [string]$Data.id
    }

    return $null
}

Export-ModuleMember -Function Get-ModrinthAuthErrorMessage, Get-ModrinthDisplayName
