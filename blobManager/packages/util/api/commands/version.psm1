function Command-Version {
    param(
        [string]$PackageName,
        [string]$Version,
        [string]$ConfigVersion,
        [string]$WarningMessage
    )

    Write-Host "$PackageName $Version" -ForegroundColor Cyan
    Write-Host "Config version: $ConfigVersion" -ForegroundColor Cyan

    if ($WarningMessage) {
        Write-Host $WarningMessage -ForegroundColor Yellow
    }
}

Export-ModuleMember -Function Command-Version
