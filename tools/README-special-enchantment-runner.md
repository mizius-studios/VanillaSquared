# Special Enchantment Case Runner

This runner is external to the mod. It sends keys to the Minecraft window so you can replay your A-E case actions quickly.

## Files

- `tools/special_enchantment_case_runner.ps1`
- `tools/special_enchantment_cases.json`
- `tools/predicate_enchantment_case_runner.ps1`
- `tools/predicate_enchantment_cases.json`
- `tools/enchantment_case_runner.ps1`

## Setup

1. Put your test items in hotbar slots `1..5`:
   - `1 = case A`
   - `2 = case B`
   - `3 = case C`
   - `4 = case D`
   - `5 = case E`
2. Make sure your enchantment hotkey is still Left Alt, or update `controls.enchantmentHotkey` in the JSON.
3. Start the world and stand in a safe place.

Hotbar keybind mapping is configurable in `controls.hotbarKeybinds`.
Current mapping is:
- `1 -> 1`
- `2 -> 3`
- `3 -> 2`
- `4 -> c`
- `5 -> 4`
- `6 -> z`
- `7 -> x`
- `8 -> r`
- `9 -> q`

## Run

Single command launcher:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\enchantment_case_runner.ps1
```

`enchantment_case_runner.ps1` modes:
- `-Suite special` (hotkey `9`)
- `-Suite predicate` (hotkey `8`)
- `-Suite both` (default)

For `-Suite both` with `-NoHotkey`, it runs special suite then predicate suite in sequence.

Default mode binds a global hotkey to keyboard `9`:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\special_enchantment_case_runner.ps1
```

Then press `9` to run one full A-E cycle.

Predicate suite runner (bound to keyboard `8`):

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\predicate_enchantment_case_runner.ps1
```

Immediate mode (no hotkey, runs immediately):

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\special_enchantment_case_runner.ps1 -NoHotkey -Cycles 3
```

Dry run (prints actions only):

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\special_enchantment_case_runner.ps1 -NoHotkey -DryRun
```

## Config Notes

- Edit `cases[*].hotbarSlot` if your layout differs.
- Edit `cases[*].hotkeyPresses` and `waitSecondsAfterCase` to match each enchantment behavior.
- Optional chat setup/cleanup commands can be added to:
  - `preRunCommands`
  - `postRunCommands`
- Optional per-case chat commands can be added to:
  - `cases[*].beforeCommands`
  - `cases[*].afterCommands`

Example chat command:

```json
"/time set day"
```
