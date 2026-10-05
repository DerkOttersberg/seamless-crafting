# Repository workflow

Each Minecraft version has one branch containing its supported loader modules.
`1.21.1` contains `common`, `fabric`, `forge` and `neoforge`.
The 1.20.1 Fabric/Forge exception remains limited to that historical line. `26.2` and `26.3` retain their own three-loader
projects. `main` is unchanged.

Use `feat/<name>` and `fix/<name>` based on the relevant version branch; do not
create permanent loader branches, force-push, or delete published version lines.

## Updating safely

Save your own uncommitted work first:

```bash
git fetch origin
git switch 1.21.1
git pull --ff-only
```

Use the matching `1.21.1` checkout of `seamless-api` as a sibling for dependent
composite builds. The suite manifest must pin commits from this game line.

Gradle 9.6 runs on Java 25. Compilation, tests, GameTests, and Minecraft use the
Java 21 toolchain (automatically resolved by Foojay). Run
`./gradlew clean check build`; do not attempt to run Gradle 9.6 on Java 21.
Versions live in `gradle/libs.versions.toml`.

Publish only canonical remapped jars from loader `build/libs`, never
`build/devlibs`, common jars, QA helpers, Minecraft libraries, or test worlds.
Preserve licenses, public API packages, registry IDs, and legacy config backups.
A newer game's world cannot be safely downgraded merely by using matching mod IDs.

Tests launch clients only in a private Linux Xvfb display with bounded resources.
Do not use the historical 26.x desktop launchers for 1.21.1 QA.
Remote CI status is separate from local test evidence: unavailable CI is not a pass.
