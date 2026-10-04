# Minecraft 1.20.1 feature and verification checklist

Fabric and Forge only. Historical 26.x checkmarks are not acceptance evidence
for this branch; see those version branches for their reports.

## Implemented

- [x] Inventory/table nearby panels, search, sorting, scrolling and locate actions
- [x] Server-authoritative recipe placement, withdrawal/return and close cleanup
- [x] Exact NBT/enchantment identity, bounded counts and packet validation
- [x] Grid simulation, variant backtracking, partial-commit rollback and conservation
- [x] Double-chest deduplication, lock/obstruction checks and unloaded-chunk avoidance
- [x] Fabric Transfer API and Forge item-handler adapters
- [x] Optional JEI 15 exclusions and ingredient lookup on both loaders
- [x] Vanilla RenderType highlights, distance labels and trails without raw OpenGL
- [x] Clear configuration screens and legacy migration with backups

## Verification

- [x] Unit tests for accounting, migration, UI layout and mixin targets
- [x] Eleven Fabric and twelve Forge required native GameTests
- [x] Discovery-count guards, loader metadata isolation, license and Java 17 bytecode checks
- [x] No QA classes, shaded Seamless API or bundled JEI classes in release jars
- [x] Real isolated clients with JEI: nearby synchronization, autofill and exact NBT return
- [x] Untouched production jars load in combined dedicated servers and survive restart
- [x] Final combined real-client checks on Fabric and Forge (see API suite QA report)
- [ ] Remote GitHub CI independently passes (account billing lock is not a pass)
- [ ] Native packaged Forge-launcher client acceptance
- [ ] Two-client multiplayer disconnect/reconnect acceptance for this backport

No vanilla Vulkan backend exists for 1.20.1. Physical-GPU/resource-pack breadth
and every third-party mod combination are not implied by headless QA.
