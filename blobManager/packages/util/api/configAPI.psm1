function ConvertTo-Hashtable {
    param(
        $InputObject
    )

    if ($null -eq $InputObject) {
        return $null
    }

    if ($InputObject -is [hashtable]) {
        return $InputObject
    }

    if ($InputObject -is [System.Collections.IDictionary]) {
        $dictionary = @{}
        foreach ($key in $InputObject.Keys) {
            $dictionary[$key] = ConvertTo-Hashtable -InputObject $InputObject[$key]
        }
        return $dictionary
    }

    if ($InputObject -is [System.Collections.IEnumerable] -and $InputObject -isnot [string]) {
        $items = New-Object System.Collections.ArrayList
        foreach ($item in $InputObject) {
            [void]$items.Add((ConvertTo-Hashtable -InputObject $item))
        }
        return ,$items.ToArray()
    }

    if ($InputObject -is [psobject]) {
        $properties = $InputObject.PSObject.Properties
        if ($properties.Count -gt 0) {
            $result = @{}
            foreach ($property in $properties) {
                $result[$property.Name] = ConvertTo-Hashtable -InputObject $property.Value
            }
            return $result
        }
    }

    return $InputObject
}

function Get-JsonConfig {
    param(
        [string]$Path,
        [hashtable]$Fallback = @{}
    )

    if (!(Test-Path $Path -PathType Leaf)) {
        return $Fallback
    }

    try {
        $raw = Get-Content -Raw $Path
        if ([string]::IsNullOrWhiteSpace($raw)) {
            return $Fallback
        }

        $parsed = $raw | ConvertFrom-Json
        if ($null -eq $parsed) {
            return $Fallback
        }

        return ConvertTo-Hashtable -InputObject $parsed
    }
    catch {
        return $Fallback
    }
}

function Get-ConfigValue {
    param(
        [hashtable]$Config,
        [string]$Key,
        $DefaultValue
    )

    if ($null -ne $Config -and $Config.ContainsKey($Key) -and $null -ne $Config[$Key]) {
        return $Config[$Key]
    }

    return $DefaultValue
}

function Test-ConfigVersion {
    param(
        [string]$PackageName,
        [string]$ScriptVersion,
        [hashtable]$Config,
        [hashtable]$State
    )

    $configVersion = Get-ConfigValue -Config $Config -Key "version" -DefaultValue $null
    $configVersionText = if ([string]::IsNullOrWhiteSpace([string]$configVersion)) { $null } else { [string]$configVersion }
    $warningMessage = $null
    $matches = $false

    if ($null -eq $configVersionText) {
        $warningMessage = "Warning: $PackageName config version is missing. Script version: $ScriptVersion"
    }
    elseif ($configVersionText -ne $ScriptVersion) {
        $warningMessage = "Warning: $PackageName script version $ScriptVersion does not match config version $configVersionText"
    }
    else {
        $matches = $true
    }

    if ($warningMessage -and $null -ne $State) {
        if ($State.ContainsKey("Warnings") -and $null -ne $State.Warnings -and -not ($State.Warnings -contains $warningMessage)) {
            [void]$State.Warnings.Add($warningMessage)
        }
    }

    return @{
        ConfigVersion = if ($null -ne $configVersionText) { $configVersionText } else { "<missing>" }
        Matches = $matches
        WarningMessage = $warningMessage
    }
}

Export-ModuleMember -Function Get-JsonConfig, Get-ConfigValue, Test-ConfigVersion
