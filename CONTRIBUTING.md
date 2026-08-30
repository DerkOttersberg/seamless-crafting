# Contributing

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
