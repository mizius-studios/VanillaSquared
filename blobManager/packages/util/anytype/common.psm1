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
        [string]$ConfigKey,
        [string]$PackageName,
        [string]$ConfigPath,
        $AddErrorMessage,
        [hashtable]$State
    )

    if ([string]::IsNullOrWhiteSpace([string]$Value)) {
        & $AddErrorMessage -State $State -Message "$PackageName $ConfigKey is missing in $ConfigPath"
    }
}

Export-ModuleMember -Function Resolve-LocalPath, Add-RequiredConfigError
