# Repository workflow

This is one multi-loader project: `common`, `fabric`, `forge`, and `neoforge`.
Each Minecraft version branch contains all three loaders.

## Version branches

- `26.2`: the preserved Minecraft 26.2 release-hardening source line.
- `26.3`: the separately ported Minecraft 26.3 source line.
- `main` is left unchanged by this version-branch migration.
- Use short-lived `feat/<name>` and `fix/<name>` branches based on the
  relevant version branch. Do not create permanent per-loader branches.
- Keep published tags and releases. Never force-push or delete a version line
  as a substitute for a proper port or an explicit retirement decision.

## Updating safely

Save your own uncommitted work first, then run:

```bash
git fetch origin
git switch 26.3
git pull --ff-only
```

For Minecraft 26.2, switch to `26.2` instead. Do not mix 26.2 and 26.3 sources,
runtime jars, or API sibling checkouts. Dependents using a composite build need
the matching Minecraft version of `seamless-api` in the sibling directory.
The API's `suite-lock.json` pins exact suite commits.

Build with Java 25 and `./gradlew clean check build`. Game/toolchain versions
live in `gradle/libs.versions.toml`; a new Minecraft version requires code and
runtime verification, not merely broader metadata. CI status must be checked
separately from local acceptance; an unavailable or blocked CI run is not a pass.

Publish only verified loader-specific runtime jars. Never distribute QA helpers,
Minecraft libraries, test worlds, or development profiles. Keep existing licensing
and compatibility/registry IDs unchanged.
