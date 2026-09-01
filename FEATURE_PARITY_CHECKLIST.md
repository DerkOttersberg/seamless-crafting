# Minecraft 26.2 Feature and Verification Checklist

## Implemented

- [x] Nearby panels in crafting-table and player-inventory screens
- [x] Search, sorting, scrolling, counts, locate action, and auto-refresh
- [x] Nearby-assisted recipe-book availability and quick placement
- [x] Exact component-aware counts, tooltips, and highlighting
- [x] Whole-grid simulated recipe validation before live mutation
- [x] Server-authoritative transactional withdrawal and safe return tracking
- [x] Enchanted/component-bearing player and nearby autofill
- [x] Fabric Transfer API, Forge item-handler, and NeoForge resource-handler adapters
- [x] Conservative storage-contract admission and backing-handler deduplication
- [x] Shared adaptive panel controller with stable loading/empty/partial states
- [x] Optional JEI overlay/click handlers on Fabric and NeoForge; no EMI on 26.2
- [x] Menu-close and disconnect cleanup
- [x] Vanilla double-chest merging, locks, obstruction, and loot handling
- [x] Bounded VarLong packets with explicit truncation and unloaded-chunk avoidance
- [x] Filled world-space highlights, distance labels, and locate trail
- [x] Configuration and color-picker screens on all loaders
- [x] Canonical configuration migration with retained backups
- [x] Render-state/pipeline implementation without raw OpenGL

## Automated verification

- [x] Java 25 `clean check build`
- [x] Fabric, Forge, and NeoForge metadata isolation
- [x] Exact processed/packaged metadata, CC0 license, and test-class exclusion
- [x] Fabric API 0.158 and 0.159 CI lanes
- [x] Canonical double-chest key and merged-count accounting tests
- [x] Count overflow saturation test
- [x] Legacy and invalid configuration backup tests
- [x] Live double-chest deduplication and two-half highlighting GameTest
- [x] Live exact-component recipe placement, pre-commit variant backtracking,
  cancellation, rollback, and item-conservation GameTests on all loaders
- [x] Live maximum-craft exact-component placement and cancellation conservation
- [x] Live rollback after a storage mutates and then partially fails extraction
- [x] Deterministic grid/player/storage source-priority regression coverage
- [x] Live locked-container and unloaded-chunk scan coverage without mutation/loading
- [x] Live standard loader-storage adapter failure/fallback/conservation GameTests
- [x] Live capability-only block-entity discovery through every loader's world scan
- [x] Live ambiguous/read-only/wrong-identity/partial-extraction rejection and
  duplicate physical-storage view GameTests on all loaders
- [x] GameTest discovery-count guards on Fabric, Forge, and NeoForge

## Runtime smoke tests

- [x] Fabric 26.2 client reaches title screen
- [x] Forge 26.2 client reaches title screen
- [x] NeoForge 26.2 client reaches title screen
- [x] Fabric, Forge, and NeoForge combined dedicated-server boots
- [x] In-world nearby item withdrawal and cancellation conservation
- [ ] Disconnect/reconnect and menu-close conservation matrix
- [x] Combined four-mod OpenGL and Vulkan profiles
