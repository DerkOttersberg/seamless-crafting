# Migration to 2.0.0

Back up a world and its configuration directory before testing any mod update.
Only test copied worlds until the complete matching-loader suite has passed.

## Mod IDs

No compatibility ID is renamed in 2.0.0:

- Fabric: `seamless_crafting`
- Forge and NeoForge: `derk_easy_inventory_crafter`

Packet IDs continue to use `derk_easy_inventory_crafter`. This is deliberate
and prevents an identifier-only break for established installations.

## Configuration

The canonical configuration file is `seamless-crafting.json`. On first launch,
the first existing legacy file is copied to the canonical name and retained as
`<old-name>.pre-2.0.bak`:

- `seamless_crafting.json`
- `derk_easy_inventory_crafter.json`
- `bluethooth_chest.json`
- `bluethooth-chest.json`

An invalid canonical file is retained as
`seamless-crafting.json.invalid-<timestamp>.bak` before safe defaults are
written. Old `showSmokeTrail` values are read as the corrected
`showLocateTrail` setting.

## Expected behavior changes

- Double chests now follow vanilla merging, obstruction, and lock rules.
- Nearby scans do not force-load chunks and clamp the configured radius to
  1–64 blocks.
- The server revalidates crafting menus and returns pending withdrawals when a
  menu closes or a player disconnects.
