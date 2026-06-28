function Test-ImageSize {
    param(
        [string]$Path,
        [int]$Width,
        [int]$Height
    )

    try {
        Add-Type -AssemblyName System.Drawing

        $img = [System.Drawing.Image]::FromFile($Path)
        $result = ($img.Width -eq $Width -and $img.Height -eq $Height)
        $img.Dispose()

        return $result
    }
    catch {
        return $false
    }
}

function New-TransparentImage {
    param(
        [string]$Path,
        [int]$Width,
        [int]$Height
    )

    Add-Type -AssemblyName System.Drawing

    $folder = Split-Path $Path
    if (!(Test-Path $folder -PathType Container)) {
        New-Item -ItemType Directory -Path $folder -Force | Out-Null
    }

    $bitmap = New-Object System.Drawing.Bitmap $Width, $Height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)

    $graphics.Clear([System.Drawing.Color]::Transparent)
    $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)

    $graphics.Dispose()
    $bitmap.Dispose()
}

Export-ModuleMember -Function Test-ImageSize, New-TransparentImage
