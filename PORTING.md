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

World highlights are submitted through Minecraft's render-state collector and
vanilla render pipelines. Do not reintroduce immediate buffers or raw OpenGL:
the same common renderer must work under OpenGL and Vulkan.

## Port checklist

1. Update only `gradle/libs.versions.toml` and the pack format.
2. Compile and test common accounting/configuration logic.
3. Adapt mappings and render-state APIs in common without loader imports.
4. Adapt Fabric, Forge, and NeoForge networking/lifecycle entrypoints.
5. Run `clean check build` and inspect every loader jar's metadata.
6. Boot a client and dedicated server for every loader.
7. Verify item conservation, double-chest deduplication, menu close,
   disconnect, save/reload, and resource reload in copied worlds.
