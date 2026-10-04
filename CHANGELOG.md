# Changelog

## 2.1.1+mc1.20.1

- Backport the current shared gameplay architecture to Minecraft 1.20.1, Java 17,
  Fabric and Forge. NeoForge is intentionally excluded from this line.
- Restore remapped loader jars, mixin refmaps, legacy NBT/data formats and bounded
  networking without changing public compatibility or registry namespaces.
- Preserve current config migration, UI clarity and item-conservation safeguards.
- Add optional JEI 15 exclusion/ingredient adapters on both loaders.


## 2.1.1+mc26.3

- Adapt inventory-return prediction and pose rotations; update optional JEI integration. Preserve authoritative crafting, item conservation, nearby/double-chest accounting, menu cleanup, and established mod IDs.
- Minecraft 26.3 only, Java 25; Fabric, Forge, and NeoForge.
- Forge 66.0.9 and NeoForge 26.3.0.48-beta are upstream beta loaders.
- Existing 26.2 releases remain separate; no blanket 26.* compatibility.

## 2.1.0+mc26.2

- Added a dedicated Mods-menu icon on all loaders.
- Reorganized settings into nearby items, container highlights, and locate
  effects with responsive pages, visible ARGB labels, units, limits, and help.
- Preserved unsaved edits across the color picker, page changes, and resizing;
  rejected non-finite numbers before applying any configuration changes.
- Reworked the color picker with a hex field, named RGB sliders, and preview.
- Preserved existing Fabric/NeoForge JEI exclusion and ingredient integration;
  verified the live exclusions with open/collapsed panels and the recipe book.
- Preserved complete item components, including enchantments and custom data,
  in nearby counts, tooltips, highlighting, recipe placement, and rollback.
- Added whole-grid, prevalidated exact-stack autofill with bounded component
  backtracking, source revalidation, atomic commit, and conservation-safe
  rollback for nearby and player-held enchanted inputs.
- Added each loader's standard item-storage adapter for modded containers,
  including exact-component extraction, fallback-slot restoration, exact
  simulated rollback admission, and physical-storage view deduplication.
- Bounded nearby packet entries and VarLong counts, disclosed truncated results
  to the client, rejected oversized codecs, and upgraded the network protocol.
- Replaced duplicated inventory/crafting panel logic with one adaptive controller
  that exposes stable loading, empty, filtered-empty, and partial-result states.
- Added overlay exclusion and clickable exact-ingredient regions to optional JEI
  30.29.0.199 on Fabric and NeoForge; EMI remains unlinked on 26.2.
- Added live Fabric, Forge, and NeoForge GameTests with discovery-count guards,
  capability-only storage discovery, locked/unloaded scans, maximum crafting,
  post-mutation rollback, and component-sensitive transaction tests.
- Declared Fabric API `>=0.158.0 <0.160.0`, CI-tests both supported releases,
  validates packaged metadata, and made every loader's license CC0-1.0.

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
