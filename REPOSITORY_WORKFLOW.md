# Repository workflow

This repository is one multi-loader project: `common`, `fabric`, `forge`, and
`neoforge`. Do not create permanent per-loader branches or repositories.

## Branches and releases

- `main` is the only permanent active branch and the normal pull target.
- Use short-lived `feat/<name>`, `fix/<name>`, `port/mc-<version>`, or
  `release/<version>` branches; merge through a PR after required checks pass.
- Create `support/mc-<version>` only when that older line is genuinely maintained.
- Keep historical code in annotated `archive/...` tags. Before removing any
  unmerged branch, record its exact SHA and original name, push its archive tag,
  and verify the remote peeled tag resolves to that SHA.
- Keep published version tags/releases. Deleting obsolete branch heads must not
  erase history, existing download files, or release tags.

## Updating safely

Run `git fetch origin --prune --tags`, switch to a clean `main` checkout, then
`git pull --ff-only`. Preserve your own uncommitted work before switching.
During the October 2026 release cutover, the latest tested changes are in
`fix/26.2-release-hardening`, not yet in `main`: GitHub Actions is blocked by
an account billing lock. Do not bypass branch protection to finish that merge.

Build with Java 25 and `./gradlew clean check build`. Toolchain versions live in
`gradle/libs.versions.toml`; a Minecraft-version change is a port requiring
loader and gameplay verification, not an automatic dependency-only bump.
Dependabot maintenance is monthly and grouped, with game-line dependencies
updated explicitly. Existing license and compatibility/registry IDs are retained.

Publish only verified loader-specific runtime jars. Sources jars are developer
artifacts, not installable mods. CurseForge review approval is not a substitute
for release acceptance; publish the matching library first, then its dependents.
