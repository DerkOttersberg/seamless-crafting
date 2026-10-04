# Seamless Crafting

This is the `1.20.1` source branch: **Fabric and Forge only**, with Java 17
for Minecraft. The `26.2` and `26.3` branches remain separate; never mix their
jars, worlds, or dependency checkouts with this line. See
[REPOSITORY_WORKFLOW.md](REPOSITORY_WORKFLOW.md).

Seamless Crafting lets the crafting table and player inventory use items from
nearby containers. Its recipe-book integration remains server-authoritative,
returns withdrawn items when crafting is cancelled, and can locate/highlight
the container holding an ingredient.

Version `2.1.1+mc1.20.1` supports Minecraft Java 1.20.1 on Fabric and Forge with Java 17.

## Compatibility

- Fabric retains the established mod ID `seamless_crafting`.
- Forge retains `derk_easy_inventory_crafter` so existing
  installations are not silently treated as a different mod.
- Packet identifiers retain the established Forge namespace.
- Fabric metadata supports Fabric API `>=0.92.12 <0.93.0`; the build and CI
  use `0.92.12+1.20.1`.
- Legacy “Bluethooth Chest” configuration names are migrated; see
  [MIGRATION.md](MIGRATION.md).

## Architecture

- `common` contains scanning, recipe availability, withdrawals/returns, UI,
  configuration, payload contracts, rendering, mixins, and unit tests.
- `fabric` and `forge` contain entrypoints, networking, lifecycle,
  configuration paths, and configuration-screen registration.
- Loader services are passed explicitly to common bootstraps. There is no
  reflective or `ServiceLoader` discovery.
- Architectury Loom is build tooling only; Architectury API is not required at
  runtime.

Nearby scans use vanilla container access rules plus each loader's standard
item-storage capability. Unloaded chunks are skipped, locked or blocked chests
are excluded, and both halves of a double chest are counted once as one 54-slot
inventory. Exact item NBT (including enchantments and custom data) are
preserved during display, recipe placement, cancellation, and menu close.

The nearby panel adapts to the free side of the crafting UI and collapses when
neither side fits. Optional JEI 15 adapters on both Fabric and Forge register
GUI exclusions and ingredient lookup. JEI is not bundled or required by this mod.
The tested JEI 15.62 runtime also needs MezzConfig 0.6.8; install JEI's own
dependencies when enabling the viewer.

## Build

Run Gradle on Java 25; source and Minecraft use the Java 17 toolchain:


```text
gradlew.bat clean check build
```

Clone matching `seamless-api` as a sibling. To build the pinned Fabric API lane:

```text
gradlew.bat :common:check :fabric:build -PfabricApiVersion=0.92.12+1.20.1
```

`check` runs unit tests, both loader GameTest servers, test-discovery
guards, common-source isolation, and exact processed/packaged metadata checks.

Loader jars are written to each loader module's `build/libs` directory as:

```text
seamless-crafting-2.1.1+mc1.20.1-fabric.jar
seamless-crafting-2.1.1+mc1.20.1-forge.jar
```

See [PORTING.md](PORTING.md) for version-port boundaries and
[FEATURE_PARITY_CHECKLIST.md](FEATURE_PARITY_CHECKLIST.md) for current
verification coverage.

## License

Seamless Crafting is released under `CC0-1.0`. The complete official CC0 1.0
Universal legal code is included in [LICENSE.txt](LICENSE.txt) and in every
packaged loader jar.
