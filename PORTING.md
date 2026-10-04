# Porting Seamless Crafting

Minecraft and tool versions live only in `gradle/libs.versions.toml`. A normal
Minecraft port starts by updating that catalog and compiling `common` against
official Minecraft names before changing loader adapters.

## Stable common contracts

- `PlatformServices` owns config paths and server networking.
- `ClientPlatformServices` owns client networking and loader UI integration.
- `SeamlessCraftingMod` and `SeamlessCraftingClientBootstrap` receive those
  implementations explicitly.
- Common payload records own validation bounds and common handlers own all
  server-authoritative checks.

Do not add Fabric, Forge, or NeoForge imports to `common`; the root
`verifyCommonIsolation` task rejects them. Keep loader event APIs and channel
registration inside the corresponding loader module.

## Rendering boundary

World highlights use vanilla `RenderType`, `MultiBufferSource`, and the 1.20.1
`LevelRenderer.renderLevel` hook. Never use raw OpenGL calls. This line has no
26.x render-state collector or vanilla Vulkan backend.

## Port checklist

1. Update only `gradle/libs.versions.toml` and the pack format.
2. Compile and test common accounting/configuration logic.
3. Adapt mappings and render-state APIs in common without loader imports.
4. Adapt Fabric and Forge networking/lifecycle entrypoints.
5. Run `clean check build` and inspect every loader jar's metadata.
6. Boot a client and dedicated server for every loader.
7. Verify item conservation, double-chest deduplication, menu close,
   disconnect, save/reload, and resource reload in copied worlds.

## Legacy build boundary

This branch uses regular `dev.architectury.loom` and official Mojang mappings.
Compile shared sources into each loader module; do not put a remapped common jar
on a named development runtime classpath. Both loaders need legacy mixin refmaps.
Only loader remapped `build/libs` jars are distributable. Java 25 hosts Gradle;
Java 17 is used for compilation and Minecraft. Keep plural 1.20.1 data directories
and NBT item persistence; newer data components are not interchangeable.
