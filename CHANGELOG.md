# Changelog

## 2.0.0+mc26.2

- Ported Seamless Crafting to Minecraft Java 26.2 and Java 25.
- Unified Fabric, Forge, and NeoForge in one `common`/loader-module project.
- Retained the Fabric `seamless_crafting` ID and the established Forge and
  NeoForge `derk_easy_inventory_crafter` ID.
- Merged nearby-container panels, recipe-book integration, quick placement,
  container locating, and world-space highlighting across all loaders.
- Made nearby crafting server-authoritative and validate the active menu,
  reachability, locks, quantities, and packet bounds.
- Deduplicated double chests using vanilla's merged 54-slot container rules and
  stopped scans from loading chunks.
- Added withdrawal rollback on menu close and disconnect so nearby items are
  not lost or duplicated.
- Replaced the old immediate renderer with Minecraft 26.2 render-state
  submission, compatible with both rendering backends.
- Migrated legacy and misspelled configuration filenames to
  `seamless-crafting.json`, retaining backups.
- Added accounting and configuration migration tests, loader metadata
  isolation checks, and live GameTests for double-chest deduplication and
  server-authoritative withdrawal/cancellation conservation.
