$ErrorActionPreference = "Stop"

$errorApiModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\errorAPI.psm1") -Force -DisableNameChecking -PassThru
$dirApiModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\dirAPI.psm1") -Force -DisableNameChecking -PassThru
$imageApiModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\imageAPI.psm1") -Force -DisableNameChecking -PassThru
$configApiModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\configAPI.psm1") -Force -DisableNameChecking -PassThru
$versionModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\commands\version.psm1") -Force -DisableNameChecking -PassThru
$helpModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\commands\help.psm1") -Force -DisableNameChecking -PassThru
$intellijModule = Import-Module (Join-Path $PSScriptRoot "..\util\mc\intellij.psm1") -Force -DisableNameChecking -PassThru
$setnameModule = Import-Module (Join-Path $PSScriptRoot "..\util\mc\setname.psm1") -Force -DisableNameChecking -PassThru
$skinModule = Import-Module (Join-Path $PSScriptRoot "..\util\mc\skinmanager.psm1") -Force -DisableNameChecking -PassThru
$modversionModule = Import-Module (Join-Path $PSScriptRoot "..\util\mc\setmodversion.psm1") -Force -DisableNameChecking -PassThru
$buildModule = Import-Module (Join-Path $PSScriptRoot "..\util\mc\build.psm1") -Force -DisableNameChecking -PassThru
$modrinthConfigPath = Join-Path $PSScriptRoot "..\config\modrinth.json"

$Cmd = @{
    NewMessageState     = $errorApiModule.ExportedCommands["New-MessageState"]
    AddErrorMessage     = $errorApiModule.ExportedCommands["Add-ErrorMessage"]
    AddWarningMessage   = $errorApiModule.ExportedCommands["Add-WarningMessage"]
    WriteWarnings       = $errorApiModule.ExportedCommands["Write-Warnings"]
    ThrowIfErrors       = $errorApiModule.ExportedCommands["Throw-IfErrors"]
    GetUniqueFolderName = $dirApiModule.ExportedCommands["Get-UniqueFolderName"]
    TestImageSize       = $imageApiModule.ExportedCommands["Test-ImageSize"]
    GetJsonConfig       = $configApiModule.ExportedCommands["Get-JsonConfig"]
    GetConfigValue      = $configApiModule.ExportedCommands["Get-ConfigValue"]
    SetModVersion       = $modversionModule.ExportedCommands["Set-ModVersion"]
    SetUsername         = $setnameModule.ExportedCommands["Set-Username"]
    SetModel            = $setnameModule.ExportedCommands["Set-Model"]
    RestoreDefaultSkins = $skinModule.ExportedCommands["Restore-DefaultSkins"]
    ApplySkinProfile    = $skinModule.ExportedCommands["Apply-SkinProfile"]
    SyncIntelliJState   = $intellijModule.ExportedCommands["Sync-IntelliJState"]
    ExitBadUsage        = $errorApiModule.ExportedCommands["Exit-BadUsage"]
    TestConfigVersion   = $configApiModule.ExportedCommands["Test-ConfigVersion"]
    CommandVersion      = $versionModule.ExportedCommands["Command-Version"]
    CommandHelp         = $helpModule.ExportedCommands["Command-Help"]
    InvokeMcBuildAndStageArtifacts = $buildModule.ExportedCommands["Invoke-McBuildAndStageArtifacts"]
}

foreach ($entry in $Cmd.GetEnumerator()) {
    if ($null -eq $entry.Value) {
        throw "Required command handle missing: $($entry.Key)"
    }
}

$Version = "1.0.0"
$PackageName = "mc"

$configPath = Join-Path $PSScriptRoot "..\config\mc.json"
$config = & $Cmd.GetJsonConfig -Path $configPath -Fallback @{}
$globalConfigPath = Join-Path $PSScriptRoot "..\config\global.json"
$globalConfig = & $Cmd.GetJsonConfig -Path $globalConfigPath -Fallback @{}
$modrinthConfig = & $Cmd.GetJsonConfig -Path $modrinthConfigPath -Fallback @{}

$paths = & $Cmd.GetConfigValue -Config $config -Key "paths" -DefaultValue @{}
$markers = & $Cmd.GetConfigValue -Config $config -Key "markers" -DefaultValue @{}
$defaults = & $Cmd.GetConfigValue -Config $config -Key "defaults" -DefaultValue @{}
$skins = & $Cmd.GetConfigValue -Config $config -Key "skins" -DefaultValue @{}
$intellij = & $Cmd.GetConfigValue -Config $config -Key "intellij" -DefaultValue @{}
$modrinthReleaseConfig = & $Cmd.GetConfigValue -Config $modrinthConfig -Key "release" -DefaultValue @{}

$GradlePropertiesPath = & $Cmd.GetConfigValue -Config $paths -Key "gradleProperties" -DefaultValue "gradle.properties"
$BuildGradlePath = & $Cmd.GetConfigValue -Config $paths -Key "buildGradle" -DefaultValue "build.gradle"
$FabricModJsonPath = & $Cmd.GetConfigValue -Config $paths -Key "fabricModJson" -DefaultValue "src/main/resources/fabric.mod.json"
$GradleWrapperPath = & $Cmd.GetConfigValue -Config $paths -Key "gradleWrapper" -DefaultValue ".\gradlew.bat"
$BuildLibsPath = & $Cmd.GetConfigValue -Config $paths -Key "buildLibs" -DefaultValue "build/libs"
$TextureRoot = & $Cmd.GetConfigValue -Config $paths -Key "texturesRoot" -DefaultValue "blobManager/textures/skins"
$SkinPackRoot = & $Cmd.GetConfigValue -Config $paths -Key "skinPacksRoot" -DefaultValue "blobManager/skinPacks"
$DefaultSkinsRoot = & $Cmd.GetConfigValue -Config $paths -Key "defaultSkinsRoot" -DefaultValue "src/client/resources/assets/minecraft/textures/entity/player"
$RunConfigRoot = & $Cmd.GetConfigValue -Config $paths -Key "runConfig" -DefaultValue ".idea/runConfigurations"
$ModrinthArtifactRoot = & $Cmd.GetConfigValue -Config $modrinthReleaseConfig -Key "artifactRoot" -DefaultValue "blobManager/packages/data/modrinth/version"

$UsernameMarker = & $Cmd.GetConfigValue -Config $markers -Key "username" -DefaultValue "usernameVSQ"
$ModelMarker = & $Cmd.GetConfigValue -Config $markers -Key "model" -DefaultValue "modelVSQ"
$GradleVersionMarker = & $Cmd.GetConfigValue -Config $markers -Key "modVersion" -DefaultValue "mod_version"

$DefaultUsername = & $Cmd.GetConfigValue -Config $defaults -Key "username" -DefaultValue "TESTIFICATE"
$DefaultModel = & $Cmd.GetConfigValue -Config $defaults -Key "model" -DefaultValue "wide"

$SkinNames = & $Cmd.GetConfigValue -Config $skins -Key "supportedNames" -DefaultValue @("alex","steve","ari","efe","makena","kai","noor","sunny","zuri")
$SupportedModels = & $Cmd.GetConfigValue -Config $skins -Key "supportedModels" -DefaultValue @("slim","wide")
$IntelliJRunConfigurations = & $Cmd.GetConfigValue -Config $intellij -Key "runConfigurations" -DefaultValue @{
    client = @{
        path = (Join-Path $RunConfigRoot "Minecraft_Client.xml")
    }
    server = @{
        path = (Join-Path $RunConfigRoot "Minecraft_Server.xml")
        programParameters = "nogui"
    }
    dataGeneration = @{
        path = (Join-Path $RunConfigRoot "Data_Generation.xml")
        programParameters = ""
    }
}

$state = & $Cmd.NewMessageState
$versionStatus = & $Cmd.TestConfigVersion -PackageName $PackageName -ScriptVersion $Version -Config $config -State $state

$Command = if ($args.Count -gt 0) { $args[0] } else { $null }
$Rest = @()
if ($args.Count -gt 1) { $Rest += $args[1..($args.Count - 1)] }

switch ($Command) {
    "-v" {
        & $Cmd.CommandVersion -PackageName $PackageName -Version $Version -ConfigVersion $versionStatus.ConfigVersion -WarningMessage $versionStatus.WarningMessage
        exit 0
    }

    "setname" {
        if (!($Rest.Count -ge 1)) { Exit-BadUsage ".\blob.ps1 mc setname USERNAME" "setname" }
        $name = $Rest[0]
        if ($name.Length -lt 2 -or $name.Length -gt 16) { & $Cmd.AddErrorMessage -State $state -Message "Name must be between 2 and 16 characters long." }
        & $Cmd.ThrowIfErrors -State $state
        Write-Host "Setting name to: $name"
        & $Cmd.SetUsername -BuildGradlePath $BuildGradlePath -UsernameMarker $UsernameMarker -Username $name
        & $Cmd.SyncIntelliJState -GlobalConfig $globalConfig -RunConfigurations $IntelliJRunConfigurations -BuildGradlePath $BuildGradlePath -UsernameMarker $UsernameMarker -ModelMarker $ModelMarker -DefaultUsername $DefaultUsername -DefaultModel $DefaultModel -State $state
        & $Cmd.WriteWarnings -State $state
        exit 0
    }

    "setversion" {
        if (!($Rest.Count -ge 6 -and $Rest[0] -eq "--version" -and $Rest[2] -eq "--arg" -and $Rest[4] -eq "--release")) { Exit-BadUsage ".\blob.ps1 mc setversion --version VERSION_NUMBER --arg VERSION_COMMENT --release alpha|beta|release" "setversion" }
        $version = $Rest[1]
        $versionArg = $Rest[3]
        $releaseType = $Rest[5]
        if ($version -notmatch '^[^.]+\.[^.]+$') { & $Cmd.AddErrorMessage -State $state -Message "Version format is invalid, must be in the format X.Y" }
        if ($releaseType -notin @("alpha", "beta", "release")) { & $Cmd.AddErrorMessage -State $state -Message "Release type must be alpha, beta, or release." }
        & $Cmd.ThrowIfErrors -State $state
        $newVersion = & $Cmd.SetModVersion -GradlePropertiesPath $GradlePropertiesPath -FabricModJsonPath $FabricModJsonPath -GradleVersionMarker $GradleVersionMarker -Version $version -VersionArg $versionArg -ReleaseType $releaseType
        Write-Host "Setting version to: $newVersion"
        & $Cmd.WriteWarnings -State $state
        exit 0
    }

    "skinmanager" {
        $subCommand = if ($Rest.Count -gt 0) { $Rest[0] } else { $null }
        $McRest = @()
        if ($Rest.Count -gt 1) { $McRest += $Rest[1..($Rest.Count - 1)] }

        switch ($subCommand) {
            "create" {
                if (!($McRest.Count -ge 6 -and $McRest[0] -eq "--model" -and $McRest[2] -eq "--texture" -and $McRest[4] -eq "--name")) { Exit-BadUsage ".\blob.ps1 mc skinmanager create --model slim|wide --texture TEXTURE --name NAME [--username USERNAME]" "skinmanager" }

                $model = $McRest[1]
                $texture = $McRest[3]
                $name = $McRest[5]
                $username = $null

                for ($i = 6; $i -lt $McRest.Count; $i++) {
                    if ($McRest[$i] -eq "--username" -and ($i + 1) -lt $McRest.Count) {
                        $username = $McRest[$i + 1]
                    }
                }

                if ($model -notin $SupportedModels) { & $Cmd.AddErrorMessage -State $state -Message "Model must be slim or wide." }
                if ($name -match '[\\/:*?"<>|]' -or $texture -match '[\\/:*?"<>|]') { & $Cmd.AddErrorMessage -State $state -Message "Names shouldn't contain: \ / : * ? `" < > |" }
                & $Cmd.ThrowIfErrors -State $state

                $textureFile = Join-Path $TextureRoot "$texture.png"
                if (!(Test-Path $textureFile -PathType Leaf)) {
                    & $Cmd.AddErrorMessage -State $state -Message "Texture '$texture.png' does not exist in $TextureRoot."
                } elseif (!(& $Cmd.TestImageSize -Path $textureFile -Width 64 -Height 64)) {
                    & $Cmd.AddWarningMessage -State $state -Message "Texture size is not 64x64."
                }
                & $Cmd.ThrowIfErrors -State $state

                $unique = & $Cmd.GetUniqueFolderName -BasePath $SkinPackRoot -Name $name
                if ($unique.Renamed) {
                    $name = $unique.Name
                    & $Cmd.AddWarningMessage -State $state -Message "Skin pack already exists. Renaming to $name."
                }

                $profilePath = Join-Path $SkinPackRoot $name
                New-Item -ItemType Directory -Path $profilePath -Force | Out-Null
                Copy-Item -Force $textureFile (Join-Path $profilePath "skin.png")

                $profileText = @("model=$model")
                if ($username) {
                    $profileText += "username=$username"
                    & $Cmd.SetUsername -BuildGradlePath $BuildGradlePath -UsernameMarker $UsernameMarker -Username $username
                }

                Set-Content (Join-Path $profilePath "$name.txt") $profileText
                & $Cmd.SetModel -BuildGradlePath $BuildGradlePath -ModelMarker $ModelMarker -Model $model
                & $Cmd.SyncIntelliJState -GlobalConfig $globalConfig -RunConfigurations $IntelliJRunConfigurations -BuildGradlePath $BuildGradlePath -UsernameMarker $UsernameMarker -ModelMarker $ModelMarker -DefaultUsername $DefaultUsername -DefaultModel $DefaultModel -State $state

                & $Cmd.WriteWarnings -State $state
                Write-Host "Profile created."
                exit 0
            }

            "modify" {
                if ($McRest.Count -lt 1) { Exit-BadUsage ".\blob.ps1 mc skinmanager modify PROFILE --model slim|wide --username USERNAME --texture TEXTURE" "skinmanager" }

                $profileName = $McRest[0]
                $profilePath = Join-Path $SkinPackRoot $profileName
                $profileFile = Join-Path $profilePath "$profileName.txt"

                if (!(Test-Path $profilePath -PathType Container)) { & $Cmd.AddErrorMessage -State $state -Message "Profile does not exist: $profileName" }
                & $Cmd.ThrowIfErrors -State $state

                $model = $null
                $username = $null
                $texture = $null

                for ($i = 1; $i -lt $McRest.Count; $i++) {
                    switch ($McRest[$i]) {
                        "--model" { if (($i + 1) -lt $McRest.Count) { $model = $McRest[$i + 1] } }
                        "--username" { if (($i + 1) -lt $McRest.Count) { $username = $McRest[$i + 1] } }
                        "--texture" { if (($i + 1) -lt $McRest.Count) { $texture = $McRest[$i + 1] } }
                    }
                }

                if ($model -and $model -notin $SupportedModels) { & $Cmd.AddErrorMessage -State $state -Message "Model must be slim or wide." }
                & $Cmd.ThrowIfErrors -State $state

                $lines = if (Test-Path $profileFile -PathType Leaf) { @(Get-Content $profileFile) } else { @() }
                if ($model) { $lines = $lines | Where-Object { $_ -notmatch '^model=' }; $lines += "model=$model" }
                if ($username) { $lines = $lines | Where-Object { $_ -notmatch '^username=' }; $lines += "username=$username" }

                if ($texture) {
                    $textureFile = Join-Path $TextureRoot "$texture.png"
                    if (!(Test-Path $textureFile -PathType Leaf)) {
                        & $Cmd.AddErrorMessage -State $state -Message "Texture does not exist: $texture.png"
                    } elseif (!(& $Cmd.TestImageSize -Path $textureFile -Width 64 -Height 64)) {
                        & $Cmd.AddWarningMessage -State $state -Message "Texture size is not 64x64."
                    }
                    & $Cmd.ThrowIfErrors -State $state
                    Copy-Item -Force $textureFile (Join-Path $profilePath "skin.png")
                }

                Set-Content $profileFile $lines
                & $Cmd.WriteWarnings -State $state
                Write-Host "Profile modified."
                exit 0
            }

            "setProfile" {
                if ($McRest.Count -lt 1) { Exit-BadUsage ".\blob.ps1 mc skinmanager setProfile PROFILE" "skinmanager" }

                $profileName = $McRest[0]
                $profilePath = Join-Path $SkinPackRoot $profileName
                $profileFile = Join-Path $profilePath "$profileName.txt"

                if (!(Test-Path $profilePath -PathType Container)) { & $Cmd.AddErrorMessage -State $state -Message "Profile does not exist: $profileName" }
                if (!(Test-Path $profileFile -PathType Leaf)) { & $Cmd.AddErrorMessage -State $state -Message "Profile info file missing: $profileFile" }
                & $Cmd.ThrowIfErrors -State $state

                $profileData = Get-Content $profileFile
                $model = ($profileData | Where-Object { $_ -match '^model=' }) -replace '^model=', ''
                $username = ($profileData | Where-Object { $_ -match '^username=' }) -replace '^username=', ''
                if (!$model) { $model = $DefaultModel }
                if (!$username) { $username = $DefaultUsername }

                & $Cmd.ApplySkinProfile -ProfileName $profileName -Model $model -Username $username -SkinPacksRoot $SkinPackRoot -DefaultSkinsRoot $DefaultSkinsRoot -SkinNames $SkinNames -BuildGradlePath $BuildGradlePath -ModelMarker $ModelMarker -UsernameMarker $UsernameMarker -State $state
                & $Cmd.SyncIntelliJState -GlobalConfig $globalConfig -RunConfigurations $IntelliJRunConfigurations -BuildGradlePath $BuildGradlePath -UsernameMarker $UsernameMarker -ModelMarker $ModelMarker -DefaultUsername $DefaultUsername -DefaultModel $DefaultModel -State $state

                & $Cmd.WriteWarnings -State $state
                Write-Host "Profile applied."
                exit 0
            }

            "restoreDefaultSkins" {
                foreach ($model in $SupportedModels) {
                    & $Cmd.RestoreDefaultSkins -DefaultSkinsRoot $DefaultSkinsRoot -SkinNames $SkinNames -Model $model
                }
                Write-Host "Default skins restored."
                exit 0
            }

            "delete" {
                if ($McRest.Count -lt 1) { Exit-BadUsage ".\blob.ps1 mc skinmanager delete PROFILE" "skinmanager" }

                $profileName = $McRest[0]
                $profilePath = Join-Path $SkinPackRoot $profileName

                if (!(Test-Path $profilePath -PathType Container)) { & $Cmd.AddErrorMessage -State $state -Message "Profile does not exist: $profileName" }
                & $Cmd.ThrowIfErrors -State $state

                Remove-Item $profilePath -Recurse -Force
                & $Cmd.RestoreDefaultSkins -DefaultSkinsRoot $DefaultSkinsRoot -SkinNames $SkinNames -Model $DefaultModel
                & $Cmd.SetUsername -BuildGradlePath $BuildGradlePath -UsernameMarker $UsernameMarker -Username $DefaultUsername
                & $Cmd.SetModel -BuildGradlePath $BuildGradlePath -ModelMarker $ModelMarker -Model $DefaultModel
                & $Cmd.SyncIntelliJState -GlobalConfig $globalConfig -RunConfigurations $IntelliJRunConfigurations -BuildGradlePath $BuildGradlePath -UsernameMarker $UsernameMarker -ModelMarker $ModelMarker -DefaultUsername $DefaultUsername -DefaultModel $DefaultModel -State $state

                & $Cmd.WriteWarnings -State $state
                Write-Host "Deleted profile: $profileName"
                exit 0
            }

            "-?" {
                & $Cmd.WriteWarnings -State $state
                & $Cmd.CommandHelp -PackageName "$PackageName.skinmanager" -Commands @(
                    "mc skinmanager create --model slim|wide --texture TEXTURE --name NAME [--username USERNAME]",
                    "mc skinmanager modify PROFILE --model slim|wide --username USERNAME --texture TEXTURE",
                    "mc skinmanager setProfile PROFILE",
                    "mc skinmanager restoreDefaultSkins",
                    "mc skinmanager delete PROFILE",
                    "mc skinmanager -?"
                )
                exit 0
            }

            default {
                & $Cmd.WriteWarnings -State $state
                Write-Host "Unknown subcommand: $subCommand"
                Write-Host "Run: .\blob.ps1 mc skinmanager help"
                exit 1
            }
        }
    }

    "build" {
        try {
            $buildResult = & $Cmd.InvokeMcBuildAndStageArtifacts -GradleWrapperPath $GradleWrapperPath -FabricModJsonPath $FabricModJsonPath -BuildLibsPath $BuildLibsPath -ArtifactRoot $ModrinthArtifactRoot
        }
        catch {
            Write-Host $_.Exception.Message -ForegroundColor Red
            exit 1
        }

        Write-Host "Build complete for version $($buildResult.Version)."
        Write-Host "Staged artifacts:"
        foreach ($artifact in $buildResult.Artifacts) {
            Write-Host " - $artifact"
        }
        exit 0
    }

    "-?" {
        & $Cmd.WriteWarnings -State $state
        & $Cmd.CommandHelp -PackageName $PackageName -Commands @(
            "setname USERNAME", "setversion --version VERSION_NUMBER --arg VERSION_COMMENT --release alpha|beta|release", "skinmanager -?", "build", "-?"
        )
        exit 0
    }

    default {
        & $Cmd.WriteWarnings -State $state
        Write-Host "Unknown mc command: $Command"
        Write-Host "Run: .\blob.ps1 mc -?"
        exit 1
    }
}
