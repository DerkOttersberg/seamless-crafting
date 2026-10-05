# Minecraft 1.21.1 acceptance — in progress

Active suite: SeamlessLib, Meteors, Workbench, Crafting, Throw Weapons.
Retired Block Animations and experimental Comfort are excluded.
Fabric / Forge / NeoForge, Java 21 game runtime, Java 25 Gradle host.

## Passed local gates — 5 October 2026

All five clean builds passed across three loaders. 90 unit tests and 86 required
loader GameTests passed with discovery guards. All 15 runtime jars passed
metadata/version/bytecode/license/isolation and exact CurseForge PNG checks.
Client-hook/metadata follow-up changes are rebuilt and retested separately.

| Product | Unit | Fabric GameTests | Forge GameTests | NeoForge GameTests |
| --- | ---: | ---: | ---: | ---: |
| SeamlessLib | 4 | — | — | — |
| Meteors | 25 | 2 | 2 | 2 |
| Workbench | 17 | 8 | 8 | 8 |
| Crafting | 16 | 11 | 12 | 12 |
| Throw Weapons | 28 | 7 | 7 | 7 |

Workspace logs: `qa-artifacts/mc1.21.1-*.log`. Failed attempts are retained,
not counted as accepted evidence.

## Runtime progress and outstanding gates

All five products were selected in the real Fabric, Forge and NeoForge Mods
menus; the bundled 400x400 icons match their CurseForge provenance. All three
genuine packaged dedicated servers passed two boots, exact Sword/Workbench
persistence checks and backup-preserving Workbench config migration (attempt r3,
corrected candidate r2 runtime jars).
All three combined integrated-world tests passed partial throwing, local sleeve
poses, item identity, book-slot tooltip, workbench processing and nearby crafting
autofill/return conservation. Meteor state synchronization passed on all three;
clear-sky screenshots demonstrate visible trails on Fabric standalone r3, Forge
combined r3 and NeoForge combined r1. The original fixed-camera capture
had no visible trails and is not counted as visual proof.

Clean individual clients passed all four gameplay products on Fabric and Meteors
on Forge (r3). Fabric installed JEI 19.57.0.451 passed actual runtime exclusion
registration and visually inspected inventory/crafting screens (jei/r1).
Actual Forge settings captures exposed a 1.21.1 background blur drawn after our
labels; all four shared screen copies were fixed and all three-loader check/build
matrices passed. Candidate r1 is superseded by built-20261005-r2. Corrected settings
captures remain pending; earlier gameplay evidence records its original input jars.

Remaining: other independent clean installs, corrected settings at multiple GUI
scales, Forge/NeoForge installed JEI, broader copied-world upgrade and
two-actual-client multiplayer. The test-only
`seamless-api/.github/mc1211-client-qa` uses private WSL/Xvfb/software OpenGL.
Its source/compile is not a passed scenario; Forge/NeoForge dev remapping is
not a packaged-launcher test. Record exact final hashes after all changes.

## Release state

Not production accepted. No 1.21.1 CurseForge submission/publication, GitHub
push, branch deletion or repository rename has been performed by this port.
Historical suite-lock/release-artifact manifests are not current acceptance.
