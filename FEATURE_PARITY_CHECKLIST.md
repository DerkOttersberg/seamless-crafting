# Minecraft 26.2 Feature and Verification Checklist

## Implemented

- [x] Nearby panels in crafting-table and player-inventory screens
- [x] Search, sorting, scrolling, counts, locate action, and auto-refresh
- [x] Nearby-assisted recipe-book availability and quick placement
- [x] Server-authoritative withdrawal and safe return tracking
- [x] Menu-close and disconnect cleanup
- [x] Vanilla double-chest merging, locks, obstruction, and loot handling
- [x] Bounded packets and unloaded-chunk avoidance
- [x] Filled world-space highlights, distance labels, and locate trail
- [x] Configuration and color-picker screens on all loaders
- [x] Canonical configuration migration with retained backups
- [x] Render-state/pipeline implementation without raw OpenGL

## Automated verification

- [x] Java 25 `clean check build`
- [x] Fabric, Forge, and NeoForge metadata isolation
- [x] Canonical double-chest key and merged-count accounting tests
- [x] Count overflow saturation test
- [x] Legacy and invalid configuration backup tests
- [x] Live double-chest deduplication and two-half highlighting GameTest
- [x] Live recipe-book withdrawal, cancellation, and item-conservation GameTest

## Runtime smoke tests

- [x] Fabric 26.2 client reaches title screen
- [x] Forge 26.2 client reaches title screen
- [x] NeoForge 26.2 client reaches title screen
- [x] Fabric, Forge, and NeoForge combined dedicated-server boots
- [x] In-world nearby item withdrawal and cancellation conservation
- [ ] Disconnect/reconnect and menu-close conservation matrix
- [x] Combined four-mod OpenGL and Vulkan profiles
