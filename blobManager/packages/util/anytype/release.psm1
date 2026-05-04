function Get-AnytypeAuthorizationHeaders {
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
            Error = "Missing Anytype API key. No local key or env var was found."
            Resolution = $tokenResolution
        }
    }

    return @{
        Success = $true
        Headers = & $MergeAuthorizationHeader -Headers $Headers -Token "Bearer $($tokenResolution.Value)"
        Resolution = $tokenResolution
    }
}

function Get-AnytypeItemCollection {
    param(
        $Data
    )

    if ($null -eq $Data) {
        return @()
    }

    if ($Data -is [System.Collections.IEnumerable] -and $Data -isnot [string]) {
        return @($Data)
    }

    foreach ($name in @("items", "list", "data", "objects", "results", "spaces", "types")) {
        if ($Data.PSObject.Properties.Name -contains $name -and $null -ne $Data.$name) {
            $nested = Get-AnytypeItemCollection -Data $Data.$name
            if ($nested.Count -gt 0) {
                return $nested
            }
        }
    }

    return @($Data)
}

function Get-AnytypeFieldValue {
    param(
        $Object,
        [string[]]$Names
    )

    if ($null -eq $Object) {
        return $null
    }

    foreach ($name in $Names) {
        if ($Object -is [hashtable]) {
            if ($Object.ContainsKey($name) -and $null -ne $Object[$name]) {
                return $Object[$name]
            }
        }
        elseif ($Object.PSObject.Properties.Name -contains $name -and $null -ne $Object.$name) {
            return $Object.$name
        }
    }

    return $null
}

function Get-AnytypeObjectName {
    param(
        $Object
    )

    $nameValue = Get-AnytypeFieldValue -Object $Object -Names @("name", "title")
    if ($null -ne $nameValue) {
        return [string]$nameValue
    }

    $details = Get-AnytypeFieldValue -Object $Object -Names @("details")
    if ($null -ne $details) {
        $nestedValue = Get-AnytypeFieldValue -Object $details -Names @("name", "title")
        if ($null -ne $nestedValue) {
            return [string]$nestedValue
        }
    }

    return $null
}

function Get-AnytypeObjectId {
    param(
        $Object
    )

    $value = Get-AnytypeFieldValue -Object $Object -Names @("id", "object_id", "objectId")
    if ($null -ne $value) {
        return [string]$value
    }

    $details = Get-AnytypeFieldValue -Object $Object -Names @("details")
    if ($null -ne $details) {
        $nestedValue = Get-AnytypeFieldValue -Object $details -Names @("id", "object_id", "objectId")
        if ($null -ne $nestedValue) {
            return [string]$nestedValue
        }
    }

    return $null
}

function Get-AnytypeTypeName {
    param(
        $Object
    )

    foreach ($candidate in @(
        (Get-AnytypeFieldValue -Object $Object -Names @("name")),
        (Get-AnytypeFieldValue -Object $Object -Names @("title"))
    )) {
        if (-not [string]::IsNullOrWhiteSpace([string]$candidate)) {
            return [string]$candidate
        }
    }

    $details = Get-AnytypeFieldValue -Object $Object -Names @("details")
    if ($null -ne $details) {
        foreach ($candidate in @(
            (Get-AnytypeFieldValue -Object $details -Names @("name")),
            (Get-AnytypeFieldValue -Object $details -Names @("title"))
        )) {
            if (-not [string]::IsNullOrWhiteSpace([string]$candidate)) {
                return [string]$candidate
            }
        }
    }

    return $null
}

function Test-AnytypeObjectTypeMatch {
    param(
        $Object,
        [string]$TypeName
    )

    if ([string]::IsNullOrWhiteSpace([string]$TypeName)) {
        return $true
    }

    $typeObject = Get-AnytypeFieldValue -Object $Object -Names @("type")
    if ($null -eq $typeObject) {
        return $false
    }

    foreach ($candidate in @(
        (Get-AnytypeFieldValue -Object $typeObject -Names @("name")),
        (Get-AnytypeFieldValue -Object $typeObject -Names @("plural_name")),
        (Get-AnytypeFieldValue -Object $typeObject -Names @("key"))
    )) {
        if (-not [string]::IsNullOrWhiteSpace([string]$candidate) -and [string]$candidate -eq $TypeName) {
            return $true
        }
    }

    return $false
}

function Invoke-AnytypeApiRequest {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [string]$Endpoint,
        [ValidateSet("GET", "POST")]
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        $Body = $null,
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $requestUri = & $JoinApiUri -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint $Endpoint
    return & $InvokeNetworkRequest -Uri $requestUri -Method $Method -Headers $Headers -Body $Body -TimeoutSeconds $TimeoutSeconds
}

function Find-AnytypeSpaceById {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [hashtable]$Headers = @{},
        [string]$SpaceId,
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $result = Invoke-AnytypeApiRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/spaces" -Method "GET" -Headers $Headers -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    if (-not $result.Success) {
        return @{
            Success = $false
            Error = "Failed to list Anytype spaces."
            Result = $result
        }
    }

    $spaces = Get-AnytypeItemCollection -Data $result.Data
    foreach ($space in $spaces) {
        $candidateId = [string](Get-AnytypeFieldValue -Object $space -Names @("id", "spaceId", "targetSpaceId"))
        if ($candidateId -eq $SpaceId) {
            return @{
                Success = $true
                Space = $space
                Result = $result
            }
        }
    }

    return @{
        Success = $false
        Error = "Configured Anytype space was not found: $SpaceId"
        Result = $result
    }
}

function Find-AnytypeTypeByName {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [hashtable]$Headers = @{},
        [string]$SpaceId,
        [string]$TypeName,
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $result = Invoke-AnytypeApiRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/spaces/$SpaceId/types" -Method "GET" -Headers $Headers -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    if (-not $result.Success) {
        return @{
            Success = $false
            Error = "Failed to list Anytype types for space $SpaceId."
            Result = $result
        }
    }

    foreach ($type in (Get-AnytypeItemCollection -Data $result.Data)) {
        if ((Get-AnytypeTypeName -Object $type) -eq $TypeName) {
            return @{
                Success = $true
                Type = $type
                Result = $result
            }
        }
    }

    return @{
        Success = $false
        Error = "Anytype type not found in space '$SpaceId': $TypeName"
        Result = $result
    }
}

function Find-AnytypeObjectByName {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [hashtable]$Headers = @{},
        [string]$SpaceId,
        [string]$ObjectName,
        [string]$TypeName,
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $body = @{
        query = $ObjectName
    }

    $result = Invoke-AnytypeApiRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/spaces/$SpaceId/search" -Method "POST" -Headers $Headers -Body $body -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    if (-not $result.Success) {
        return @{
            Success = $false
            Error = "Failed to search Anytype objects in space $SpaceId."
            Result = $result
        }
    }

    $matches = @()
    foreach ($item in (Get-AnytypeItemCollection -Data $result.Data)) {
        if ((Get-AnytypeObjectName -Object $item) -eq $ObjectName -and (Test-AnytypeObjectTypeMatch -Object $item -TypeName $TypeName)) {
            $matches += $item
        }
    }

    if ($matches.Count -eq 1) {
        return @{
            Success = $true
            Object = $matches[0]
            Result = $result
        }
    }

    if ($matches.Count -gt 1) {
        return @{
            Success = $false
            Error = "Multiple Anytype objects matched '$ObjectName' in space '$SpaceId'."
            Result = $result
        }
    }

    return @{
        Success = $false
        Error = "Anytype object not found: $ObjectName"
        Result = $result
    }
}

function Get-AnytypeObjectDetails {
    param(
        [string]$BaseUrl,
        [string]$ApiVersion,
        [hashtable]$Headers = @{},
        [string]$SpaceId,
        [string]$ObjectId,
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $result = Invoke-AnytypeApiRequest -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Endpoint "/spaces/$SpaceId/objects/$ObjectId" -Method "GET" -Headers $Headers -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
    if (-not $result.Success -or $null -eq $result.Data) {
        return @{
            Success = $false
            Error = "Failed to fetch Anytype object '$ObjectId'."
            Result = $result
        }
    }

    $objectData = $result.Data
    if ($result.Data.PSObject.Properties.Name -contains "object" -and $null -ne $result.Data.object) {
        $objectData = $result.Data.object
    }

    return @{
        Success = $true
        Object = $objectData
        Result = $result
    }
}

function Find-AnytypeChildObjectByName {
    param(
        $ParentObject,
        [string]$ChildName
    )

    $children = Get-AnytypeFieldValue -Object $ParentObject -Names @("children", "blocks", "items")
    foreach ($child in (Get-AnytypeItemCollection -Data $children)) {
        if ((Get-AnytypeObjectName -Object $child) -eq $ChildName) {
            return @{
                Success = $true
                Child = $child
            }
        }
    }

    return @{
        Success = $false
        Error = "Child Anytype object not found: $ChildName"
    }
}

function Find-AnytypeLinkedObjectByName {
    param(
        $ParentObject,
        [string]$LinkedName
    )

    $properties = Get-AnytypeFieldValue -Object $ParentObject -Names @("properties")
    foreach ($property in (Get-AnytypeItemCollection -Data $properties)) {
        $format = [string](Get-AnytypeFieldValue -Object $property -Names @("format"))
        if ($format -ne "objects") {
            continue
        }

        $objectIds = Get-AnytypeFieldValue -Object $property -Names @("objects")
        if ($null -eq $objectIds) {
            continue
        }

        foreach ($objectId in (Get-AnytypeItemCollection -Data $objectIds)) {
            if ([string]::IsNullOrWhiteSpace([string]$objectId)) {
                continue
            }

            if ([string]$objectId -eq $LinkedName) {
                return @{
                    Success = $true
                    ObjectId = [string]$objectId
                    Property = $property
                }
            }
        }
    }

    return @{
        Success = $false
        Error = "Linked Anytype object not found by id: $LinkedName"
    }
}

function Resolve-AnytypeLinkedObjectByName {
    param(
        $ParentObject,
        [string]$LinkedName,
        [string]$BaseUrl,
        [string]$ApiVersion,
        [hashtable]$Headers = @{},
        [string]$SpaceId,
        [int]$TimeoutSeconds = 30,
        $JoinApiUri,
        $InvokeNetworkRequest
    )

    $properties = Get-AnytypeFieldValue -Object $ParentObject -Names @("properties")
    foreach ($property in (Get-AnytypeItemCollection -Data $properties)) {
        $format = [string](Get-AnytypeFieldValue -Object $property -Names @("format"))
        if ($format -ne "objects") {
            continue
        }

        $objectIds = Get-AnytypeFieldValue -Object $property -Names @("objects")
        foreach ($objectId in (Get-AnytypeItemCollection -Data $objectIds)) {
            if ([string]::IsNullOrWhiteSpace([string]$objectId)) {
                continue
            }

            $linkedObjectResult = Get-AnytypeObjectDetails -BaseUrl $BaseUrl -ApiVersion $ApiVersion -Headers $Headers -SpaceId $SpaceId -ObjectId ([string]$objectId) -TimeoutSeconds $TimeoutSeconds -JoinApiUri $JoinApiUri -InvokeNetworkRequest $InvokeNetworkRequest
            if (-not $linkedObjectResult.Success) {
                continue
            }

            if ((Get-AnytypeObjectName -Object $linkedObjectResult.Object) -eq $LinkedName) {
                return @{
                    Success = $true
                    Object = $linkedObjectResult.Object
                    Property = $property
                }
            }
        }
    }

    return @{
        Success = $false
        Error = "Linked Anytype object not found: $LinkedName"
    }
}

function Get-AnytypeTextFromBlocks {
    param(
        $InputObject
    )

    $segments = New-Object System.Collections.ArrayList

    function Add-BlockText {
        param($Node, $Segments)

        if ($null -eq $Node) {
            return
        }

        if ($Node -is [string]) {
            if (-not [string]::IsNullOrWhiteSpace($Node)) {
                [void]$Segments.Add($Node.Trim())
            }
            return
        }

        foreach ($key in @("text", "markdown", "body", "content", "snippet")) {
            $value = Get-AnytypeFieldValue -Object $Node -Names @($key)
            if ($value -is [string] -and -not [string]::IsNullOrWhiteSpace($value)) {
                [void]$Segments.Add($value.Trim())
            }
        }

        foreach ($collectionName in @("children", "blocks", "items", "content")) {
            $collection = Get-AnytypeFieldValue -Object $Node -Names @($collectionName)
            foreach ($child in (Get-AnytypeItemCollection -Data $collection)) {
                Add-BlockText -Node $child -Segments $Segments
            }
        }
    }

    Add-BlockText -Node $InputObject -Segments $segments
    return ($segments | Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) }) -join "`n`n"
}

function Get-AnytypeObjectMarkdownBody {
    param(
        $Object
    )

    foreach ($key in @("body", "markdown", "text", "content")) {
        $value = Get-AnytypeFieldValue -Object $Object -Names @($key)
        if ($value -is [string] -and -not [string]::IsNullOrWhiteSpace($value)) {
            return $value.Trim()
        }
    }

    $body = Get-AnytypeTextFromBlocks -InputObject $Object
    if (-not [string]::IsNullOrWhiteSpace($body)) {
        return $body.Trim()
    }

    return $null
}

Export-ModuleMember -Function Get-AnytypeAuthorizationHeaders, Find-AnytypeSpaceById, Find-AnytypeTypeByName, Find-AnytypeObjectByName, Get-AnytypeObjectDetails, Find-AnytypeChildObjectByName, Find-AnytypeLinkedObjectByName, Resolve-AnytypeLinkedObjectByName, Get-AnytypeObjectMarkdownBody
