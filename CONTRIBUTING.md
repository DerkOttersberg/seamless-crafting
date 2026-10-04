# Contributing

For this branch, base changes on `1.20.1`, not `main` or a 26.x branch.
Java 25 hosts Gradle; Java 17 compiles and runs Minecraft. Supported loaders
are Fabric and Forge only. Keep a matching `1.20.1` API sibling checkout.


Use Java 25 and keep changes loader-neutral unless they directly integrate a
loader API. New behavior belongs in `common`; loader modules should remain
small adapters.

Before submitting a change, run:

```text
gradlew.bat clean check build
```

Add tests for accounting, configuration migration, packet bounds, and item
conservation when changing those areas. Never test upgrades against a user's
only world copy, and do not change compatibility IDs or configuration migration
rules without documenting the impact in `MIGRATION.md` and `CHANGELOG.md`.
