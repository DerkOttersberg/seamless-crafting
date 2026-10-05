# Seamless Crafting

Minecraft **1.21.1**, Java **21**; separate **Fabric, Forge and NeoForge** jars.
Version `2.1.1+mc1.21.1`. Never mix these with 1.20.1 or 26.x binaries.

Crafts from reachable nearby containers with authoritative recipe autofill, exact-item withdrawal/return and highlighting. Fabric retains `seamless_crafting`; Forge/NeoForge retain `derk_easy_inventory_crafter`. Optional JEI 19 adapters on all three loaders register exclusions and ingredient lookup. Runtime coexistence is still under test. Crafting has no code-level SeamlessLib dependency; the owner's suite distribution requires it.

## Build and architecture

`common` holds loader-neutral code, resources and tests; `fabric`, `forge`
and `neoforge` explicitly inject their platform services. Architectury Loom
is build tooling only, not a runtime API. Pins are in
`gradle/libs.versions.toml`. Gameplay composite builds use a sibling
`seamless-api` checkout for the matching Minecraft line; the library is not shaded.

Run Gradle with Java 25 installed; source/game tasks use Java 21:

```text
gradlew.bat clean check build
```

Distribute only remapped
`<loader>/build/libs/seamless-crafting-2.1.1+mc1.21.1-<loader>.jar`.
Dev/QA jars are not release files. `check` runs common tests/isolation,
applicable loader GameTests with discovery guards, and all-loader jar checks.

## Icons and settings

All loaders reference the current CurseForge project PNG, bundled locally.
Source URLs and SHA-256 are in `gradle/icon-provenance.json`; do not replace
this artwork by running historical SVG generators. Fabric gameplay settings
use optional Mod Menu 11.0.5; Forge/NeoForge use native Mods-menu adapters.
SeamlessLib is a library with no gameplay settings screen.

## Status and migration

Local clean builds pass across the suite: 90 unit tests and 86 loader GameTests.
Client/UI, multiplayer, packaged-server and optional-JEI acceptance is separate:
see [.github/RELEASE_ACCEPTANCE.md](.github/RELEASE_ACCEPTANCE.md).
Build success is not production readiness, a GitHub push or a CurseForge release.
See [PORTING.md](PORTING.md) and [MIGRATION.md](MIGRATION.md).
Upgrade only backup copies of worlds/configs.

## License

Existing CC0-1.0 licensing is unchanged; see [LICENSE.txt](LICENSE.txt).
