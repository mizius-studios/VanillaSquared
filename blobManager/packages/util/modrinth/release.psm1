function Get-ModrinthAuthorizationHeaders {
    param(
        [hashtable]$Headers = @{},
        [string]$KeyFilePath,
        [string]$KeyName,
        [string]$EnvVarName,
        $ResolveKeyValue,
        $MergeAuthorizationHeader
    )

    $tokenResolution = & $ResolveKeyValue -KeyFilePath $KeyFilePath -KeyName $KeyName -EnvVarName $EnvVarName
    if (-not $tokenResolution.Success) {
        return @{
            Success = $false
            Error = "Missing Modrinth PAT. No local key or env var was found."
            Resolution = $tokenResolution
        }
    }

    return @{
        Success = $true
        Headers = & $MergeAuthorizationHeader -Headers $Headers -Token $tokenResolution.Value
        Resolution = $tokenResolution
    }
}

function Get-ModMetadata {
    param(
        [string]$FabricModJsonPath
    )

    if (-not (Test-Path $FabricModJsonPath -PathType Leaf)) {
        throw "fabric.mod.json not found at $FabricModJsonPath"
    }

    $content = Get-Content -Raw $FabricModJsonPath | ConvertFrom-Json
    $version = [string]$content.version
    if ([string]::IsNullOrWhiteSpace($version)) {
        throw "fabric.mod.json version is missing."
    }

    $releaseType = "release"
    if ($version.StartsWith("-2")) {
        $releaseType = "alpha"
    }
    elseif ($version.StartsWith("-1")) {
        $releaseType = "beta"
    }

    $depends = @{}
    if ($null -ne $content.depends) {
        foreach ($property in $content.depends.PSObject.Properties) {
            $depends[$property.Name] = [string]$property.Value
        }
    }

    $gameVersions = @()
    if ($depends.ContainsKey("minecraft")) {
        $gameVersion = $depends["minecraft"].TrimStart("~", "=", ">", "<", "^")
        if (-not [string]::IsNullOrWhiteSpace($gameVersion)) {
            $gameVersions += $gameVersion
        }
    }

    return @{
        Id = [string]$content.id
        Name = [string]$content.name
        Version = $version
        ReleaseType = $releaseType
        Depends = $depends
        GameVersions = $gameVersions
    }
}

function Get-ModrinthDependencyPayload {
    param(
        [hashtable]$FabricDependencies = @{},
        [hashtable]$DependencyMap = @{}
    )

    $dependencies = New-Object System.Collections.ArrayList
    foreach ($entry in $FabricDependencies.GetEnumerator()) {
        if ($entry.Key -eq "minecraft" -or $entry.Key -eq "java" -or $entry.Key -eq "fabricloader") {
            continue
        }

        if (-not $DependencyMap.ContainsKey($entry.Key)) {
            continue
        }

        $mapping = $DependencyMap[$entry.Key]
        $projectId = $mapping.projectId
        if ([string]::IsNullOrWhiteSpace([string]$projectId)) {
            continue
        }

        $dependencyType = [string]$mapping.dependencyType
        if ([string]::IsNullOrWhiteSpace($dependencyType)) {
            $dependencyType = "required"
        }

        [void]$dependencies.Add(@{
            project_id = [string]$projectId
            dependency_type = $dependencyType
        })
    }

    return @($dependencies)
}

function Get-ModrinthReleaseArtifacts {
    param(
        [string]$ArtifactRoot,
        [string]$Version,
        [hashtable]$ReleaseConfig = @{}
    )

    if (-not (Test-Path $ArtifactRoot -PathType Container)) {
        throw "Artifact root not found: $ArtifactRoot"
    }

    $matches = @(Get-ChildItem -Path $ArtifactRoot -File -Filter *.jar | Where-Object {
        $_.Name -like "*$Version*" -and $_.Name -notlike "*sources*"
    })

    if ($matches.Count -eq 0) {
        throw "No release jar matched version '$Version' in $ArtifactRoot"
    }

    if ($matches.Count -gt 1) {
        $primaryPattern = [string]$ReleaseConfig.primaryArtifactPattern
        if (-not [string]::IsNullOrWhiteSpace($primaryPattern)) {
            $preferred = @($matches | Where-Object { $_.Name -like $primaryPattern })
            if ($preferred.Count -eq 1) {
                return @($preferred[0])
            }
        }

        throw "Multiple release jars matched version '$Version' in $ArtifactRoot"
    }

    return @($matches)
}

function Write-ModrinthReleasePreview {
    param(
        [hashtable]$ReleaseData
    )

    Write-Host ""
    Write-Host "Modrinth release preview" -ForegroundColor Cyan
    Write-Host "Project ref: $($ReleaseData.ProjectRef)"
    Write-Host "Anytype channel: $($ReleaseData.AnytypeChannel)"
    Write-Host "Anytype spaceId: $($ReleaseData.SpaceId)"
    Write-Host "Release type: $($ReleaseData.ReleaseType)"
    Write-Host "Version number: $($ReleaseData.VersionNumber)"
    Write-Host "Game versions: $(([string[]]$ReleaseData.GameVersions) -join ', ')"
    Write-Host "Loaders: $(([string[]]$ReleaseData.Loaders) -join ', ')"
    Write-Host "Dependencies: $((($ReleaseData.Dependencies | ConvertTo-Json -Compress -Depth 10)))"
    if ($ReleaseData.FileParts.Count -gt 0) {
        Write-Host "File parts: $(([string[]]$ReleaseData.FileParts) -join ', ')"
    }
    if (-not [string]::IsNullOrWhiteSpace([string]$ReleaseData.PrimaryFile)) {
        Write-Host "Primary file: $($ReleaseData.PrimaryFile)"
    }
    Write-Host "Artifacts:"
    foreach ($artifact in $ReleaseData.Artifacts) {
        Write-Host " - $artifact"
    }
    Write-Host "Changelog:"
    Write-Host $ReleaseData.Changelog
    Write-Host ""
}

function Read-ModrinthReleaseConfirmation {
    while ($true) {
        $response = Read-Host "Type 'confirm' to upload or 'cancel' to abort"
        switch ($response) {
            "confirm" { return $true }
            "cancel" { return $false }
            default { Write-Host "Expected 'confirm' or 'cancel'." -ForegroundColor Yellow }
        }
    }
}

function New-ModrinthVersionMultipartRequest {
    param(
        [string]$Uri,
        [hashtable]$Headers = @{},
        [hashtable]$Payload,
        [string[]]$FilePaths,
        [int]$TimeoutSeconds = 30
    )

    try {
        Add-Type -AssemblyName System.Net.Http -ErrorAction Stop | Out-Null
    }
    catch {
        return @{
            Success = $false
            StatusCode = $null
            StatusDescription = $null
            RawContent = $null
            Data = $null
            ErrorMessage = "System.Net.Http assembly is unavailable in this PowerShell runtime."
        }
    }

    $handler = $null
    $client = $null
    $multipart = $null

    $handler = [System.Net.Http.HttpClientHandler]::new()
    $client = [System.Net.Http.HttpClient]::new($handler)
    $client.Timeout = [TimeSpan]::FromSeconds($TimeoutSeconds)

    foreach ($header in $Headers.GetEnumerator()) {
        if ($header.Key -eq "User-Agent") {
            $null = $client.DefaultRequestHeaders.UserAgent.ParseAdd([string]$header.Value)
        }
        elseif ($header.Key -ne "Content-Type") {
            $null = $client.DefaultRequestHeaders.TryAddWithoutValidation([string]$header.Key, [string]$header.Value)
        }
    }

    $multipart = [System.Net.Http.MultipartFormDataContent]::new()
    try {
        $payloadJson = $Payload | ConvertTo-Json -Depth 20 -Compress
        $dataContent = [System.Net.Http.StringContent]::new($payloadJson, [System.Text.Encoding]::UTF8, "application/json")
        $multipart.Add($dataContent, "data")

        $streams = New-Object System.Collections.ArrayList
        for ($index = 0; $index -lt $FilePaths.Count; $index++) {
            $path = $FilePaths[$index]
            $stream = [System.IO.File]::OpenRead($path)
            [void]$streams.Add($stream)
            $fileContent = [System.Net.Http.StreamContent]::new($stream)
            $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("application/java-archive")
            $partName = "file$index"
            $multipart.Add($fileContent, $partName, [System.IO.Path]::GetFileName($path))
        }

        try {
            $response = $client.PostAsync($Uri, $multipart).GetAwaiter().GetResult()
            $rawContent = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            $data = $null
            if (-not [string]::IsNullOrWhiteSpace($rawContent)) {
                try {
                    $data = $rawContent | ConvertFrom-Json
                }
                catch {
                    $data = $null
                }
            }

            return @{
                Success = $response.IsSuccessStatusCode
                StatusCode = [int]$response.StatusCode
                StatusDescription = [string]$response.ReasonPhrase
                RawContent = $rawContent
                Data = $data
                ErrorMessage = if ($response.IsSuccessStatusCode) { $null } else { [string]$response.ReasonPhrase }
            }
        }
        finally {
            foreach ($stream in $streams) {
                $stream.Dispose()
            }
        }
    }
    finally {
        if ($null -ne $multipart) {
            $multipart.Dispose()
        }
        if ($null -ne $client) {
            $client.Dispose()
        }
        if ($null -ne $handler) {
            $handler.Dispose()
        }
    }
}

Export-ModuleMember -Function Get-ModrinthAuthorizationHeaders, Get-ModMetadata, Get-ModrinthDependencyPayload, Get-ModrinthReleaseArtifacts, Write-ModrinthReleasePreview, Read-ModrinthReleaseConfirmation, New-ModrinthVersionMultipartRequest
