param(
    [string]$ConfigPath = ".\tools\special_enchantment_cases.json",
    [ValidatePattern("^[0-9]$")]
    [string]$GlobalHotkeyKey = "9",
    [int]$Cycles = 1,
    [int]$StartupDelaySeconds = 3,
    [switch]$NoHotkey,
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    $time = Get-Date -Format "HH:mm:ss"
    Write-Host "[$time] $Message"
}

function Resolve-KeyToken {
    param([string]$Token)

    $map = @{
        "ALT" = "%"
        "LALT" = "%"
        "RALT" = "%"
        "SPACE" = " "
        "TAB" = "{TAB}"
        "ENTER" = "{ENTER}"
        "ESC" = "{ESC}"
    }

    if ($Token -match "^[1-9]$") {
        return $Token
    }

    $upper = $Token.ToUpperInvariant()
    if ($map.ContainsKey($upper)) {
        return $map[$upper]
    }

    if ($Token.Length -eq 1) {
        return $Token
    }

    throw "Unsupported key token '$Token'."
}

function Send-Key {
    param(
        [string]$Token,
        [bool]$Dry
    )

    $resolved = Resolve-KeyToken -Token $Token
    if ($Dry) {
        Write-Step "DRYRUN send key '$Token' -> '$resolved'"
        return
    }

    [System.Windows.Forms.SendKeys]::SendWait($resolved)
}

function Resolve-HotbarSlotToken {
    param(
        [object]$Config,
        [string]$Slot
    )

    if ([string]::IsNullOrWhiteSpace($Slot)) {
        throw "hotbarSlot cannot be empty."
    }

    $token = $Slot
    if ($Config.controls -and $Config.controls.hotbarKeybinds) {
        $slotMap = $Config.controls.hotbarKeybinds.PSObject.Properties
        foreach ($prop in $slotMap) {
            if ($prop.Name -eq $Slot) {
                $token = [string]$prop.Value
                break
            }
        }
    }

    return $token
}

function Send-ChatCommand {
    param(
        [string]$Command,
        [object]$Config,
        [bool]$Dry
    )

    if ([string]::IsNullOrWhiteSpace($Command)) {
        return
    }

    if ($Dry) {
        Write-Step "DRYRUN chat command: $Command"
        return
    }

    Send-Key -Token $Config.controls.openChat -Dry:$false
    Start-Sleep -Milliseconds ([int]$Config.timing.chatOpenDelayMs)
    [System.Windows.Forms.SendKeys]::SendWait($Command)
    Start-Sleep -Milliseconds ([int]$Config.timing.chatTypingDelayMs)
    Send-Key -Token $Config.controls.confirmChat -Dry:$false
    Start-Sleep -Milliseconds ([int]$Config.timing.afterChatSubmitDelayMs)
}

function Invoke-ChatCommands {
    param(
        [object]$Commands,
        [object]$Config,
        [bool]$Dry
    )

    if (-not $Commands) {
        return
    }

    foreach ($cmd in $Commands) {
        Send-ChatCommand -Command ([string]$cmd) -Config $Config -Dry:$Dry
    }
}

function Get-OptionalPropertyValue {
    param(
        [object]$Object,
        [string]$Name
    )

    if (-not $Object) {
        return $null
    }

    $prop = $Object.PSObject.Properties[$Name]
    if (-not $prop) {
        return $null
    }

    return $prop.Value
}

function Focus-MinecraftWindow {
    param(
        [string]$WindowTitleContains,
        [bool]$Dry
    )

    if ($Dry) {
        Write-Step "DRYRUN focus window containing '$WindowTitleContains'"
        return
    }

    $shell = New-Object -ComObject WScript.Shell
    $ok = $shell.AppActivate($WindowTitleContains)
    if (-not $ok) {
        throw "Could not focus window containing '$WindowTitleContains'. Start Minecraft first."
    }
}

Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class VSQHotkey {
    [StructLayout(LayoutKind.Sequential)]
    public struct POINT {
        public int X;
        public int Y;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct MSG {
        public IntPtr hwnd;
        public uint message;
        public IntPtr wParam;
        public IntPtr lParam;
        public uint time;
        public POINT pt;
    }

    [DllImport("user32.dll", SetLastError = true)]
    public static extern bool RegisterHotKey(IntPtr hWnd, int id, uint fsModifiers, uint vk);

    [DllImport("user32.dll", SetLastError = true)]
    public static extern bool UnregisterHotKey(IntPtr hWnd, int id);

    [DllImport("user32.dll", SetLastError = true)]
    public static extern sbyte GetMessage(out MSG lpMsg, IntPtr hWnd, uint wMsgFilterMin, uint wMsgFilterMax);
}
"@

Add-Type -AssemblyName System.Windows.Forms

if (-not (Test-Path -LiteralPath $ConfigPath)) {
    throw "Config not found: $ConfigPath"
}

$configRaw = Get-Content -LiteralPath $ConfigPath -Raw
$config = $configRaw | ConvertFrom-Json

if ($Cycles -lt 1) {
    throw "Cycles must be >= 1."
}

Write-Step "Loaded config: $ConfigPath"
function Run-Cases {
    Focus-MinecraftWindow -WindowTitleContains $config.windowTitleContains -Dry:$DryRun.IsPresent

    if ($StartupDelaySeconds -gt 0) {
        Write-Step "Startup delay: ${StartupDelaySeconds}s"
        Start-Sleep -Seconds $StartupDelaySeconds
    }

    if ($config.preRunCommands) {
        Invoke-ChatCommands -Commands $config.preRunCommands -Config $config -Dry:$DryRun.IsPresent
    }

    for ($cycle = 1; $cycle -le $Cycles; $cycle++) {
        Write-Step "Cycle $cycle/$Cycles"

        foreach ($case in $config.cases) {
            Write-Step "Case '$($case.name)' start"

            $beforeCommands = Get-OptionalPropertyValue -Object $case -Name "beforeCommands"
            if ($beforeCommands) {
                Invoke-ChatCommands -Commands $beforeCommands -Config $config -Dry:$DryRun.IsPresent
            }

            $slotToken = Resolve-HotbarSlotToken -Config $config -Slot ([string]$case.hotbarSlot)
            Send-Key -Token $slotToken -Dry:$DryRun.IsPresent
            Start-Sleep -Milliseconds ([int]$config.timing.afterSlotSwitchDelayMs)

            $pressCount = [int]$case.hotkeyPresses
            for ($i = 1; $i -le $pressCount; $i++) {
                Send-Key -Token $config.controls.enchantmentHotkey -Dry:$DryRun.IsPresent
                Start-Sleep -Milliseconds ([int]$config.timing.betweenHotkeyPressDelayMs)
            }

            $waitSeconds = Get-OptionalPropertyValue -Object $case -Name "waitSecondsAfterCase"
            if ($waitSeconds) {
                $wait = [double]$waitSeconds
                if ($wait -gt 0) {
                    Write-Step "Case '$($case.name)' cooldown wait: ${wait}s"
                    Start-Sleep -Milliseconds ([int]($wait * 1000))
                }
            }

            $afterCommands = Get-OptionalPropertyValue -Object $case -Name "afterCommands"
            if ($afterCommands) {
                Invoke-ChatCommands -Commands $afterCommands -Config $config -Dry:$DryRun.IsPresent
            }
        }
    }

    if ($config.postRunCommands) {
        Invoke-ChatCommands -Commands $config.postRunCommands -Config $config -Dry:$DryRun.IsPresent
    }
}

if ($NoHotkey) {
    Run-Cases
    Write-Step "Special enchantment case runner finished."
    exit 0
}

$hotkeyId = 1
$vk = [byte][char]$GlobalHotkeyKey.ToUpperInvariant()
$modNone = 0x0000

$registered = [VSQHotkey]::RegisterHotKey([IntPtr]::Zero, $hotkeyId, $modNone, $vk)
if (-not $registered) {
    throw "Failed to register global hotkey '$GlobalHotkeyKey'. Close other tools that may already capture it."
}

Write-Step "Hotkey mode enabled."
Write-Step "Press keyboard '$GlobalHotkeyKey' to run test cycles. Press Ctrl+C in this terminal to stop."

try {
    while ($true) {
        $msg = New-Object VSQHotkey+MSG
        $result = [VSQHotkey]::GetMessage([ref]$msg, [IntPtr]::Zero, 0, 0)
        if ($result -eq -1) {
            throw "GetMessage failed while waiting for hotkey."
        }
        if ($result -eq 0) {
            break
        }

        if ($msg.message -eq 0x0312 -and $msg.wParam.ToInt32() -eq $hotkeyId) {
            Write-Step "Hotkey '$GlobalHotkeyKey' pressed."
            Run-Cases
            Write-Step "Waiting for next hotkey press..."
        }
    }
}
finally {
    [void][VSQHotkey]::UnregisterHotKey([IntPtr]::Zero, $hotkeyId)
}
