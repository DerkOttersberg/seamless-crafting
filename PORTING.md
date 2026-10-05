# Minecraft 1.21.1 porting guide

One version branch holds `common`, `fabric`, `forge` and `neoforge`.
Pins live only in `gradle/libs.versions.toml`. Java 25 hosts Gradle; Java 21
compiles/runs Minecraft. Use regular Loom, official Mojang mappings and
`remapJar`; named development jars are not distributable.

Keep common code free of loader/JEI imports. Inject platform services explicitly;
no reflective discovery, runtime Architectury API or shaded SeamlessLib.
Preserve compatibility/registry IDs, public library packages and licensing.

## Version boundaries

1.21.1 uses item data components and registry-aware persistence,
`RecipeHolder`/`CraftingInput`, typed `CustomPacketPayload` networking,
`DeltaTracker` rendering and vanilla vertex APIs. Use singular data paths:
`recipe`, `loot_table`, `tags/item`, `structure`. Never downgrade a newer world.

## Verification

Run `clean check build` and inspect all three remapped jars. Forge 52 filters
GameTest batch namespaces and uses `GameTestDontPrefix`; NeoForge 21 has its
own template-prefix rules. Test-only source staging must never enter releases.
Keep test-discovery and required-pass guards.

Test independent installs plus dependencies, combined profiles, genuine
packaged servers, multiplayer, save/restart, migration backups and actual
optional integrations. Use the private WSL/Xvfb wrapper for GUI checks only;
never steal desktop focus or inject OS mouse/keyboard input. Software OpenGL
does not prove physical-GPU coverage; 1.21.1 has no vanilla Vulkan backend.

Icons and all-loader artifact guards are under `gradle/`.
Historical 1.20.1/26.x helpers and acceptance are not current results. See
[.github/RELEASE_ACCEPTANCE.md](.github/RELEASE_ACCEPTANCE.md).
