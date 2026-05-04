function Get-HttpErrorResponse {
    param(
        [System.Net.WebException]$Exception
    )

    if ($null -eq $Exception.Response) {
        return $null
    }

    try {
        $stream = $Exception.Response.GetResponseStream()
        if ($null -eq $stream) {
            return $null
        }

        $reader = New-Object System.IO.StreamReader($stream)
        try {
            return $reader.ReadToEnd()
        }
        finally {
            $reader.Dispose()
            $stream.Dispose()
        }
    }
    catch {
        return $null
    }
}

function ConvertFrom-JsonSafe {
    param(
        [string]$Content
    )

    if ([string]::IsNullOrWhiteSpace($Content)) {
        return $null
    }

    try {
        return $Content | ConvertFrom-Json
    }
    catch {
        return $null
    }
}

function Invoke-NetworkRequest {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Uri,
        [ValidateSet("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD")]
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        $Body = $null,
        [string]$ContentType = "application/json",
        [int]$TimeoutSeconds = 30
    )

    $request = [System.Net.HttpWebRequest]::Create($Uri)
    $request.Method = $Method
    $request.Timeout = $TimeoutSeconds * 1000
    $request.ReadWriteTimeout = $TimeoutSeconds * 1000
    $request.Accept = "application/json"

    foreach ($header in $Headers.GetEnumerator()) {
        switch -Regex ($header.Key) {
            "^Accept$" {
                $request.Accept = [string]$header.Value
            }
            "^Content-Type$" {
                $ContentType = [string]$header.Value
            }
            "^User-Agent$" {
                $request.UserAgent = [string]$header.Value
            }
            default {
                $request.Headers[$header.Key] = [string]$header.Value
            }
        }
    }

    if ($null -ne $Body -and $Method -ne "GET" -and $Method -ne "HEAD") {
        $request.ContentType = $ContentType

        $payload = if ($Body -is [string]) {
            $Body
        }
        else {
            $Body | ConvertTo-Json -Depth 20
        }

        $bytes = [System.Text.Encoding]::UTF8.GetBytes($payload)
        $request.ContentLength = $bytes.Length
        $requestStream = $request.GetRequestStream()
        try {
            $requestStream.Write($bytes, 0, $bytes.Length)
        }
        finally {
            $requestStream.Dispose()
        }
    }

    try {
        $response = [System.Net.HttpWebResponse]$request.GetResponse()
        try {
            $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
            try {
                $rawContent = $reader.ReadToEnd()
            }
            finally {
                $reader.Dispose()
            }

            return @{
                Success = $true
                StatusCode = [int]$response.StatusCode
                StatusDescription = [string]$response.StatusDescription
                ContentType = [string]$response.ContentType
                RawContent = $rawContent
                Data = ConvertFrom-JsonSafe -Content $rawContent
            }
        }
        finally {
            $response.Dispose()
        }
    }
    catch [System.Net.WebException] {
        $statusCode = $null
        $statusDescription = $null

        if ($null -ne $_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            $statusDescription = [string]$_.Exception.Response.StatusDescription
        }

        return @{
            Success = $false
            StatusCode = $statusCode
            StatusDescription = $statusDescription
            ContentType = $null
            RawContent = Get-HttpErrorResponse -Exception $_.Exception
            Data = $null
            ErrorMessage = $_.Exception.Message
        }
    }
}

function Test-NetworkConnection {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Uri,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30,
        [string]$Method = "GET"
    )

    return Invoke-NetworkRequest -Uri $Uri -Method $Method -Headers $Headers -TimeoutSeconds $TimeoutSeconds
}

function Merge-RequestHeaders {
    param(
        [hashtable]$Headers = @{},
        [hashtable]$AdditionalHeaders = @{}
    )

    $mergedHeaders = @{}

    if ($null -ne $Headers) {
        foreach ($header in $Headers.GetEnumerator()) {
            $mergedHeaders[[string]$header.Key] = $header.Value
        }
    }

    if ($null -ne $AdditionalHeaders) {
        foreach ($header in $AdditionalHeaders.GetEnumerator()) {
            $mergedHeaders[[string]$header.Key] = $header.Value
        }
    }

    return $mergedHeaders
}

function Merge-AuthorizationHeader {
    param(
        [hashtable]$Headers = @{},
        [string]$Token
    )

    if ([string]::IsNullOrWhiteSpace([string]$Token)) {
        return Merge-RequestHeaders -Headers $Headers
    }

    return Merge-RequestHeaders -Headers $Headers -AdditionalHeaders @{
        Authorization = $Token
    }
}

function Join-ApiUri {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,
        [string]$ApiVersion,
        [Parameter(Mandatory = $true)]
        [string]$Endpoint
    )

    $normalizedBaseUrl = $BaseUrl.TrimEnd('/')
    $normalizedEndpoint = if ($Endpoint.StartsWith('/')) { $Endpoint } else { "/$Endpoint" }

    if ([string]::IsNullOrWhiteSpace([string]$ApiVersion)) {
        return "$normalizedBaseUrl$normalizedEndpoint"
    }

    $normalizedApiVersion = $ApiVersion.Trim('/')
    return "$normalizedBaseUrl/$normalizedApiVersion$normalizedEndpoint"
}

Export-ModuleMember -Function Invoke-NetworkRequest, Test-NetworkConnection, Merge-RequestHeaders, Merge-AuthorizationHeader, Join-ApiUri
