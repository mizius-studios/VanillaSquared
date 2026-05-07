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

function Resolve-LocalPath {
    param(
        [string]$PathValue,
        [string]$BasePath = (Get-Location).Path
    )

    if ([string]::IsNullOrWhiteSpace([string]$PathValue)) {
        return $null
    }

    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return [System.IO.Path]::GetFullPath($PathValue)
    }

    return [System.IO.Path]::GetFullPath((Join-Path $BasePath $PathValue))
}

function Ensure-DirectoryPath {
    param(
        [string]$Path
    )

    if ([string]::IsNullOrWhiteSpace([string]$Path)) {
        return $null
    }

    if (-not (Test-Path $Path -PathType Container)) {
        [void](New-Item -ItemType Directory -Path $Path -Force)
    }

    return (Get-Item -LiteralPath $Path).FullName
}

function Ensure-ParentDirectoryForFile {
    param(
        [string]$Path
    )

    if ([string]::IsNullOrWhiteSpace([string]$Path)) {
        return $null
    }

    $parentPath = Split-Path -Parent $Path
    if ([string]::IsNullOrWhiteSpace([string]$parentPath)) {
        return $null
    }

    return Ensure-DirectoryPath -Path $parentPath
}

function Read-JsonFile {
    param(
        [string]$Path,
        [hashtable]$Fallback = @{}
    )

    if ([string]::IsNullOrWhiteSpace([string]$Path) -or -not (Test-Path $Path -PathType Leaf)) {
        return $Fallback
    }

    try {
        $rawContent = Get-Content -Raw -LiteralPath $Path
        if ([string]::IsNullOrWhiteSpace($rawContent)) {
            return $Fallback
        }

        $parsed = $rawContent | ConvertFrom-Json
        if ($null -eq $parsed) {
            return $Fallback
        }

        return ConvertTo-Hashtable -InputObject $parsed
    }
    catch {
        return $Fallback
    }
}

function Write-JsonFile {
    param(
        [string]$Path,
        $Data,
        [int]$Depth = 20
    )

    if ([string]::IsNullOrWhiteSpace([string]$Path)) {
        throw "Write-JsonFile requires a path."
    }

    [void](Ensure-ParentDirectoryForFile -Path $Path)

    $jsonContent = $Data | ConvertTo-Json -Depth $Depth
    [System.IO.File]::WriteAllText($Path, $jsonContent, [System.Text.Encoding]::UTF8)
}

function Remove-FileIfExists {
    param(
        [string]$Path
    )

    if (-not [string]::IsNullOrWhiteSpace([string]$Path) -and (Test-Path $Path -PathType Leaf)) {
        Remove-Item -LiteralPath $Path -Force
    }
}

Export-ModuleMember -Function Resolve-LocalPath, Ensure-DirectoryPath, Ensure-ParentDirectoryForFile, Read-JsonFile, Write-JsonFile, Remove-FileIfExists
