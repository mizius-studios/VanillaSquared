function Get-McResolvedPath {
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

function Invoke-McBuildAndStageArtifacts {
    param(
        [string]$GradleWrapperPath = ".\gradlew.bat",
        [string]$FabricModJsonPath = "src/main/resources/fabric.mod.json",
        [string]$BuildLibsPath = "build/libs",
        [string]$ArtifactRoot = "blobManager/packages/data/modrinth/version"
    )

    $resolvedGradleWrapperPath = Get-McResolvedPath -PathValue $GradleWrapperPath
    $resolvedFabricModJsonPath = Get-McResolvedPath -PathValue $FabricModJsonPath
    $resolvedBuildLibsPath = Get-McResolvedPath -PathValue $BuildLibsPath
    $resolvedArtifactRoot = Get-McResolvedPath -PathValue $ArtifactRoot

    if (-not (Test-Path $resolvedGradleWrapperPath -PathType Leaf)) {
        throw "Gradle wrapper not found at $resolvedGradleWrapperPath"
    }

    if (-not (Test-Path $resolvedFabricModJsonPath -PathType Leaf)) {
        throw "fabric.mod.json not found at $resolvedFabricModJsonPath"
    }

    Write-Host "Building project with Gradle..."
    & $resolvedGradleWrapperPath build
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE"
    }

    if (-not (Test-Path $resolvedBuildLibsPath -PathType Container)) {
        throw "Build libs folder not found at $resolvedBuildLibsPath"
    }

    $modJson = Get-Content -Raw $resolvedFabricModJsonPath | ConvertFrom-Json
    $version = [string]$modJson.version
    if ([string]::IsNullOrWhiteSpace($version)) {
        throw "fabric.mod.json version is missing."
    }

    $builtArtifacts = @(Get-ChildItem -Path $resolvedBuildLibsPath -File -Filter *.jar | Where-Object {
        $_.Name -like "*$version*"
    })
    if ($builtArtifacts.Count -eq 0) {
        throw "No built jars matched version '$version' in $resolvedBuildLibsPath"
    }

    New-Item -ItemType Directory -Path $resolvedArtifactRoot -Force | Out-Null

    $stagedArtifacts = New-Object System.Collections.ArrayList
    foreach ($artifact in $builtArtifacts) {
        $targetPath = Join-Path $resolvedArtifactRoot $artifact.Name
        Copy-Item -LiteralPath $artifact.FullName -Destination $targetPath -Force
        [void]$stagedArtifacts.Add($targetPath)
    }

    return @{
        Version = $version
        BuildLibsPath = $resolvedBuildLibsPath
        ArtifactRoot = $resolvedArtifactRoot
        Artifacts = @($stagedArtifacts)
    }
}

Export-ModuleMember -Function Invoke-McBuildAndStageArtifacts
