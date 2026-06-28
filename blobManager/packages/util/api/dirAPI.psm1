function Get-UniqueFolderName {
    param(
        [string]$BasePath,
        [string]$Name
    )

    $baseName = $Name
    $i = 1
    $renamed = $false
    $fullPath = Join-Path $BasePath $Name

    while (Test-Path $fullPath -PathType Container) {
        $Name = "$baseName-$i"
        $fullPath = Join-Path $BasePath $Name
        $i++
        $renamed = $true
    }

    return @{
        Name = $Name
        Renamed = $renamed
    }
}

Export-ModuleMember -Function Get-UniqueFolderName
