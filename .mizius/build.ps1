$ErrorActionPreference = 'Stop'

# $TARGET_DIR = "C:\Users\jan\AppData\Local\SquidServers\mizius\plugins"
$TARGET_DIR = "C:\Users\jan\AppData\Roaming\norisk\NoRiskClientV3\data\profiles\Fabric 1.21.11(2)\mods\nrc-1.21.11-fabric"

.\gradlew build

$JAR_FILE = Get-ChildItem build\libs\VSQ-2.7.4-STABLE.jar -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($null -eq $JAR_FILE) {
    Write-Host "No jar found in build/libs."
    exit 1
}

New-Item -ItemType Directory -Force -Path $TARGET_DIR | Out-Null
Copy-Item -Force $JAR_FILE.FullName $TARGET_DIR

Write-Host "Deployed $($JAR_FILE.Name) to $TARGET_DIR."
