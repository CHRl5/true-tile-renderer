# True Tile Renderer

[![CI](https://github.com/CHRl5/true-tile-renderer/actions/workflows/ci.yml/badge.svg)](https://github.com/CHRl5/true-tile-renderer/actions/workflows/ci.yml)
[![Quality](https://github.com/CHRl5/true-tile-renderer/actions/workflows/quality.yml/badge.svg)](https://github.com/CHRl5/true-tile-renderer/actions/workflows/quality.yml)
[![Security](https://github.com/CHRl5/true-tile-renderer/actions/workflows/security.yml/badge.svg)](https://github.com/CHRl5/true-tile-renderer/actions/workflows/security.yml)
[![Release](https://img.shields.io/github/v/release/CHRl5/true-tile-renderer?display_name=tag)](https://github.com/CHRl5/true-tile-renderer/releases)
![Java](https://img.shields.io/badge/Java-11-ff9a3c)
![RuneLite](https://img.shields.io/badge/RuneLite-Plugin-00d4ff)
![Gradle](https://img.shields.io/badge/build-Gradle-02303a)
![Checks](https://img.shields.io/badge/checks-format%20%7C%20lint%20%7C%20spotbugs-blue)
![Security](https://img.shields.io/badge/security-CodeQL%20%7C%20Gitleaks%20%7C%20Trivy-8a2be2)
![Status](https://img.shields.io/badge/status-alpha-ffb000)

True Tile Renderer is a RuneLite plugin that hides selected actors at their client-rendered position and redraws their animated outline and mirrored combat overlays on the server-true tile instead.

![True Tile Renderer overview](docs/assets/readme-hero.png)

## Icons

<p>
<img src="src/main/resources/com/truetilerenderer/pluginhub_icon.png" alt="Plugin Hub icon" width="96" />
</p>

Current distribution icon:

- [`icon.png`](icon.png) for Plugin Hub packaging

## What it does

- Hides your local player model and redraws its animated outline on the true tile.
- Hides matching NPC models and redraws them on their true tile.
- Supports either your current combat target or a configurable NPC name list.
- Mirrors names, health bars, overheads, and hitsplats at the true tile location.
- Keeps the visual language focused on server position instead of interpolated client position.

## Why use it

This plugin is built for situations where exact server positioning matters more than the default rendered position. It is especially useful for movement-heavy PvM, learning encounter spacing, reading melee/range/mage proximity on bosses, and understanding when a model has visually drifted away from its real tile.

## Current feature set

### Rendering

- Local player true-tile outline rendering
- NPC true-tile outline rendering
- Exact live model animation mirroring on the true tile
- Configurable outline colors, width, and feathering

### Targeting

- Local player toggle
- NPC toggle
- Current-target mode
- Configured-list mode using NPC names

### Mirrored overlays

- Actor names
- Health bars
- Overheads
- Mirrored hitsplats

## Configuration

The plugin exposes controls for:

- Hiding the local model and rendering a true-tile outline
- Hiding matching NPC models and rendering true-tile outlines
- Choosing NPC target mode
- Configuring the NPC name list
- Toggling mirrored actor names
- Toggling mirrored health bars
- Toggling mirrored overheads
- Toggling mirrored hitsplats
- Adjusting outline color and style

## Release readiness

- Automated CI, linting, spellcheck, static analysis, and security workflows gate pull requests
- Version labels drive semantic prerelease and release bumps
- Merges to `main` automatically bump the project version, create a matching Git tag, and publish a GitHub release
- `CODEOWNERS`, Dependabot, CodeQL, Gitleaks, and Trivy are configured in-repo

## Project structure

- `src/main/java/com/truetilerenderer`: plugin implementation
- `src/main/resources/com/truetilerenderer`: icon assets
- `.github/workflows/ci.yml`: continuous integration
- `.github/CODEOWNERS`: ownership rules

## Development

### Requirements

- Java 11
- Gradle wrapper included in repo

### Commands

```bash
./gradlew test
./gradlew run
```

## Versioning

- The canonical project version lives in [`gradle.properties`](gradle.properties).
- The current prerelease version is `0.0.1-alpha.18`.
- PRs targeting `main` must carry exactly one release label:
- `release:major` bumps to the next major release.
- `release:minor` bumps to the next minor release.
- `release:patch` bumps to the next patch release.
- `release:prerelease` increments the current `-alpha.N` version, or starts the next patch as `-alpha.1` if the current version is stable.
- [`.github/workflows/version-label.yml`](.github/workflows/version-label.yml) enforces that rule on PRs to `main`.
- [`.github/workflows/version-bump.yml`](.github/workflows/version-bump.yml) applies the bump after merge to `main`, creates the matching `v<version>` tag, and publishes the GitHub release. It also supports manual `workflow_dispatch` runs for branch testing.

## Status notes

- Mirrored health bars and head icons are custom overlay recreations.
- Mirrored hitsplats are implemented as a first-pass prototype using exact hitsplat events and timing, but they are not yet pixel-identical to native Jagex hitsplats.
- The project is release-ready from a repository and automation perspective, with gameplay polish continuing through alpha releases.
