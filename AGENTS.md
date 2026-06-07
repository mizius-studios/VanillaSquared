# **The following prompts are for all Tasks**
- Always format JSONs in a good-looking way, make sure they look like vscodes pretty printed JSONs.
 ---
# **The following prompts are for Minecraft Modding tasks only:**
- Make sure to use and expand API features for the things you make if that's needed. Make sure to tell this the one who prompts and when they tell you to not modify the API, then don't modify it.
- Make sure to check Minecrafts Source Code for injecting etc. to make sure you're not targeting non-existent functions or variables.
- Make sure to not use deprecated features from Minecraft or Fabric API.
- Make sure that Vanilla features still work!
- You can use `./gradlew runClient` to test if Minecraft starts, which you should also do btw and `./gradlew runClient --warning-mode all` is a more detailed. After the game started, you obviously need to close it after it finished starting.
- If you add new tags, then translate them in en\_us to avoid a fabric api warning.