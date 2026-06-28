function Get-ModrinthProjectErrorMessage {
    param(
        $Result,
        [string]$ProjectRef
    )

    if ($null -eq $Result) {
        return "Project request failed."
    }

    if ($Result.StatusCode -eq 404) {
        return "Modrinth project not found: $ProjectRef"
    }

    if ($null -eq $Result.StatusCode) {
        if (-not [string]::IsNullOrWhiteSpace([string]$Result.ErrorMessage)) {
            return "Modrinth API/network failure: $($Result.ErrorMessage)"
        }

        return "Modrinth API/network failure."
    }

    $detail = $null
    if ($null -ne $Result.Data) {
        if ($Result.Data.PSObject.Properties.Name -contains "description") {
            $detail = [string]$Result.Data.description
        }
        elseif ($Result.Data.PSObject.Properties.Name -contains "error") {
            $detail = [string]$Result.Data.error
        }
    }

    if (-not [string]::IsNullOrWhiteSpace([string]$detail)) {
        return "Modrinth API failure: $($Result.StatusCode) $($Result.StatusDescription) - $detail"
    }

    return "Modrinth API failure: $($Result.StatusCode) $($Result.StatusDescription)"
}

function Write-ModrinthProjectSummary {
    param(
        $Project
    )

    foreach ($line in (Get-ModrinthProjectSummaryLines -Project $Project)) {
        if (-not [string]::IsNullOrWhiteSpace([string]$line)) {
            Write-Host $line
        }
    }
}

function Get-ModrinthProjectSummaryLines {
    param(
        $Project
    )

    if ($null -eq $Project) {
        return @()
    }

    $lines = New-Object System.Collections.ArrayList
    [void]$lines.Add("Title: $($Project.title)")
    [void]$lines.Add("Slug: $($Project.slug)")
    [void]$lines.Add("ID: $($Project.id)")
    [void]$lines.Add("Type: $($Project.project_type)")

    if (-not [string]::IsNullOrWhiteSpace([string]$Project.description)) {
        [void]$lines.Add("Description: $($Project.description)")
    }

    [void]$lines.Add("Client Side: $($Project.client_side)")
    [void]$lines.Add("Server Side: $($Project.server_side)")
    [void]$lines.Add("Downloads: $($Project.downloads)")
    [void]$lines.Add("Status: $($Project.status)")

    $labels = @{
        source_url = "Source URL"
        issues_url = "Issues URL"
        wiki_url = "Wiki URL"
        discord_url = "Discord URL"
    }

    foreach ($field in @("source_url", "issues_url", "wiki_url", "discord_url")) {
        $value = [string]$Project.$field
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            [void]$lines.Add("$($labels[$field]): $value")
        }
    }

    return $lines.ToArray()
}

function Write-ModrinthProjectList {
    param(
        $Projects
    )

    if ($null -eq $Projects) {
        Write-Host "No projects returned."
        return
    }

    $items = @()
    if ($Projects -is [System.Collections.IEnumerable] -and $Projects -isnot [string]) {
        $items = @($Projects)
    }
    else {
        $items = @($Projects)
    }

    if ($items.Count -eq 0) {
        Write-Host "No projects returned."
        return
    }

    foreach ($project in $items) {
        Write-Host "- $($project.title)"
        Write-Host "  slug: $($project.slug)"
        Write-Host "  id: $($project.id)"
        if (-not [string]::IsNullOrWhiteSpace([string]$project.status)) {
            Write-Host "  status: $($project.status)"
        }
        if (-not [string]::IsNullOrWhiteSpace([string]$project.project_type)) {
            Write-Host "  type: $($project.project_type)"
        }
    }
}

Export-ModuleMember -Function Get-ModrinthProjectErrorMessage, Write-ModrinthProjectSummary, Get-ModrinthProjectSummaryLines, Write-ModrinthProjectList
