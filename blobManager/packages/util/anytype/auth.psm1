function Get-AnytypeAuthErrorMessage {
    param(
        $Result,
        [string]$AuthKeyName,
        [string]$AuthEnvVar
    )

    if ($null -eq $Result) {
        return "Authenticated request failed."
    }

    if ($null -eq $Result.StatusCode) {
        if (-not [string]::IsNullOrWhiteSpace([string]$Result.ErrorMessage)) {
            return "Anytype API/network failure: $($Result.ErrorMessage)"
        }

        return "Anytype API/network failure."
    }

    if ($Result.StatusCode -eq 401) {
        return "Unauthorized. Check that $AuthKeyName is set in the key file or $AuthEnvVar and that the key is still valid."
    }

    $detail = $null
    if (-not [string]::IsNullOrWhiteSpace([string]$Result.RawContent)) {
        $detail = [string]$Result.RawContent
    }

    if (-not [string]::IsNullOrWhiteSpace([string]$detail)) {
        return "Anytype API failure: $($Result.StatusCode) $($Result.StatusDescription) - $detail"
    }

    return "Anytype API failure: $($Result.StatusCode) $($Result.StatusDescription)"
}

function Get-AnytypeDisplayName {
    param(
        $Data
    )

    if ($null -eq $Data) {
        return $null
    }

    if ($Data -is [System.Collections.IEnumerable] -and $Data -isnot [string]) {
        foreach ($item in $Data) {
            $candidate = Get-AnytypeDisplayName -Data $item
            if (-not [string]::IsNullOrWhiteSpace([string]$candidate)) {
                return $candidate
            }
        }

        return $null
    }

    foreach ($propertyName in @("name", "spaceName", "id", "targetSpaceId")) {
        if ($Data.PSObject.Properties.Name -contains $propertyName) {
            $value = [string]$Data.$propertyName
            if (-not [string]::IsNullOrWhiteSpace($value)) {
                return $value
            }
        }
    }

    foreach ($nestedName in @("spaces", "data", "list")) {
        if ($Data.PSObject.Properties.Name -contains $nestedName) {
            $candidate = Get-AnytypeDisplayName -Data $Data.$nestedName
            if (-not [string]::IsNullOrWhiteSpace([string]$candidate)) {
                return $candidate
            }
        }
    }

    return $null
}

Export-ModuleMember -Function Get-AnytypeAuthErrorMessage, Get-AnytypeDisplayName
