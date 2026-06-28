function Command-Help {
    param(
        [string]$PackageName,
        [string[]]$Commands
    )

    Write-Host "$PackageName"
    Write-Host "Commands:"
    foreach ($cmd in $Commands) {
        Write-Host "  .\blob.ps1 $cmd"
    }
}

Export-ModuleMember -Function Command-Help
