function Get-KeyEntries {
    param(
        [string]$Path
    )

    $entries = @{}

    if ([string]::IsNullOrWhiteSpace([string]$Path) -or -not (Test-Path $Path -PathType Leaf)) {
        return $entries
    }

    foreach ($rawLine in Get-Content $Path) {
        $line = [string]$rawLine
        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }

        $trimmedLine = $line.Trim()
        if ($trimmedLine.StartsWith('#')) {
            continue
        }

        $separatorIndex = $trimmedLine.IndexOf('=')
        if ($separatorIndex -lt 1) {
            continue
        }

        $keyName = $trimmedLine.Substring(0, $separatorIndex).Trim()
        $keyValue = $trimmedLine.Substring($separatorIndex + 1).Trim()

        if (-not [string]::IsNullOrWhiteSpace($keyName)) {
            $entries[$keyName] = $keyValue
        }
    }

    return $entries
}

function Get-KeyValueFromFile {
    param(
        [string]$Path,
        [string]$KeyName
    )

    if ([string]::IsNullOrWhiteSpace([string]$Path) -or [string]::IsNullOrWhiteSpace([string]$KeyName)) {
        return $null
    }

    $entries = Get-KeyEntries -Path $Path
    if ($entries.ContainsKey($KeyName)) {
        return [string]$entries[$KeyName]
    }

    return $null
}

function Get-KeyValueFromEnv {
    param(
        [string]$EnvVarName
    )

    if ([string]::IsNullOrWhiteSpace([string]$EnvVarName)) {
        return $null
    }

    $value = [Environment]::GetEnvironmentVariable($EnvVarName)
    if ([string]::IsNullOrWhiteSpace([string]$value)) {
        return $null
    }

    return [string]$value
}

function Resolve-KeyValue {
    param(
        [string]$KeyFilePath,
        [string]$KeyName,
        [string]$EnvVarName
    )

    $fileValue = Get-KeyValueFromFile -Path $KeyFilePath -KeyName $KeyName
    if (-not [string]::IsNullOrWhiteSpace([string]$fileValue)) {
        return @{
            Success = $true
            Source = "file"
            Value = $fileValue
        }
    }

    $envValue = Get-KeyValueFromEnv -EnvVarName $EnvVarName
    if (-not [string]::IsNullOrWhiteSpace([string]$envValue)) {
        return @{
            Success = $true
            Source = "env"
            Value = $envValue
        }
    }

    return @{
        Success = $false
        Source = $null
        Value = $null
    }
}

function Test-InteractiveSession {
    if (-not [Environment]::UserInteractive) {
        return $false
    }

    try {
        return $null -ne $Host.UI -and $null -ne $Host.UI.RawUI
    }
    catch {
        return $false
    }
}

function Command-GetKey {
    param(
        [string]$PackageName,
        [string]$KeyFilePath,
        [string]$KeyName,
        [string]$EnvVarName
    )

    if (-not (Test-InteractiveSession)) {
        Write-Host "Interactive confirmation is unavailable. Use env var '$EnvVarName' for non-interactive automation." -ForegroundColor Yellow
        return @{
            Success = $false
            Cancelled = $false
            Error = "Interactive confirmation unavailable."
            Value = $null
        }
    }

    $value = Get-KeyValueFromFile -Path $KeyFilePath -KeyName $KeyName
    if ([string]::IsNullOrWhiteSpace([string]$value)) {
        Write-Host "No local key named '$KeyName' was found for package '$PackageName'." -ForegroundColor Red
        Write-Host "Expected file: $KeyFilePath" -ForegroundColor Red
        Write-Host "Expected format: $KeyName=mrp_..." -ForegroundColor DarkYellow
        Write-Host "Fallback env var for automation: $EnvVarName" -ForegroundColor DarkYellow
        return @{
            Success = $false
            Cancelled = $false
            Error = "Key not found."
            Value = $null
        }
    }

    Write-Host "Warning: this will reveal the stored key '$KeyName' for package '$PackageName'." -ForegroundColor Yellow
    Write-Host "Source file: $KeyFilePath" -ForegroundColor Yellow
    $selection = Read-Host "Type REVEAL to continue"
    if ($selection -cne "REVEAL") {
        Write-Host "Key reveal cancelled." -ForegroundColor Yellow
        return @{
            Success = $false
            Cancelled = $true
            Error = "Cancelled."
            Value = $null
        }
    }

    Write-Host $value
    return @{
        Success = $true
        Cancelled = $false
        Error = $null
        Value = $value
    }
}

Export-ModuleMember -Function Get-KeyEntries, Get-KeyValueFromFile, Get-KeyValueFromEnv, Resolve-KeyValue, Test-InteractiveSession, Command-GetKey
