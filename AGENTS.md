# Project Guidelines

## General Guidelines

- Format JSON files cleanly, matching VS Code's pretty-printed style.

## Minecraft Modding Guidelines

- Use and extend existing API features when appropriate. If an API change is needed, explain it to the requester first. If the requester says not to modify the API, do not modify it.
- Check Minecraft source code before adding injections or mixins to ensure targets, methods, and fields exist.
- Do not use deprecated Minecraft or Fabric API features.
- Preserve vanilla Minecraft behavior unless a requested change explicitly requires otherwise.
- Test that Minecraft starts with `./gradlew runClient` when making modding changes. Use `./gradlew runClient --warning-mode all` for more detailed warnings. Close the game after it finishes starting.
- If you add new tags, add matching translations in `en_us` to avoid Fabric API warnings.
- Do not change the mod version in `gradle.properties` or `fabric.mod.json` (`mod_version` in `gradle.properties` and `"version"` in `fabric.mod.json`).
- Keep language files in `main/resources/assets`. Keep textures, models, and other client assets in `client/resources/assets`.
