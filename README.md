# Seamless Crafting

Seamless Crafting lets the crafting table and player inventory use items from
nearby containers. Its recipe-book integration remains server-authoritative,
returns withdrawn items when crafting is cancelled, and can locate/highlight
the container holding an ingredient.

Version `2.0.0+mc26.2` supports Minecraft Java 26.2 on Fabric, Forge, and
NeoForge with Java 25.

## Compatibility

- Fabric retains the established mod ID `seamless_crafting`.
- Forge and NeoForge retain `derk_easy_inventory_crafter` so existing
  installations are not silently treated as a different mod.
- Packet identifiers retain the established Forge/NeoForge namespace.
- Legacy “Bluethooth Chest” configuration names are migrated; see
  [MIGRATION.md](MIGRATION.md).

## Architecture

- `common` contains scanning, recipe availability, withdrawals/returns, UI,
  configuration, payload contracts, rendering, mixins, and unit tests.
- `fabric`, `forge`, and `neoforge` contain entrypoints, networking, lifecycle,
  configuration paths, and configuration-screen registration.
- Loader services are passed explicitly to common bootstraps. There is no
  reflective or `ServiceLoader` discovery.
- Architectury Loom is build tooling only; Architectury API is not required at
  runtime.

Nearby scans use vanilla container access rules: unloaded chunks are skipped,
locked or blocked chests are excluded, and both halves of a double chest are
counted once as one 54-slot inventory.

## Build

Use Java 25 and run:

```text
gradlew.bat clean check build
```

Loader jars are written to each loader module's `build/libs` directory as:

```text
seamless-crafting-2.0.0+mc26.2-fabric.jar
seamless-crafting-2.0.0+mc26.2-forge.jar
seamless-crafting-2.0.0+mc26.2-neoforge.jar
```

See [PORTING.md](PORTING.md) for version-port boundaries and
[FEATURE_PARITY_CHECKLIST.md](FEATURE_PARITY_CHECKLIST.md) for current
verification coverage.
