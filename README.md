# True Tile Renderer

[![CI](https://github.com/ChI2is/true-tile-renderer/actions/workflows/ci.yml/badge.svg)](https://github.com/ChI2is/true-tile-renderer/actions/workflows/ci.yml)
[![Quality](https://github.com/ChI2is/true-tile-renderer/actions/workflows/quality.yml/badge.svg)](https://github.com/ChI2is/true-tile-renderer/actions/workflows/quality.yml)
[![Security](https://github.com/ChI2is/true-tile-renderer/actions/workflows/security.yml/badge.svg)](https://github.com/ChI2is/true-tile-renderer/actions/workflows/security.yml)
![Java](https://img.shields.io/badge/Java-11-ff9a3c)
![RuneLite](https://img.shields.io/badge/RuneLite-Plugin-00d4ff)
![Gradle](https://img.shields.io/badge/build-Gradle-02303a)
![Checks](https://img.shields.io/badge/checks-format%20%7C%20lint%20%7C%20spotbugs-blue)
![Security](https://img.shields.io/badge/security-CodeQL%20%7C%20Trivy-8a2be2)
![Status](https://img.shields.io/badge/status-active-3fb950)

True Tile Renderer is a RuneLite plugin that hides selected actors at their client-rendered position and redraws their animated outline, mirrored overlays, and combat information on the server-true tile instead.

![True Tile Renderer overview](docs/assets/readme-hero.png)

## Icons

<p>
<img src="src/main/resources/com/truetilerenderer/pluginhub_icon.png" alt="Plugin Hub icon" width="96" />
<img src="src/main/resources/com/truetilerenderer/sidebar_icon.png" alt="Sidebar icon" width="32" />
</p>

- Plugin Hub placeholder icon is wired at [`icon.png`](/Users/chris/GitHub/true-tile-renderer/icon.png)
- Sidebar placeholder icon is stored at [`sidebar_icon.png`](/Users/chris/GitHub/true-tile-renderer/src/main/resources/com/truetilerenderer/sidebar_icon.png) for future nav-panel wiring

## What it does

- Hides your local player model and redraws its animated outline on the true tile.
- Hides configured NPCs and redraws them on their true tile.
- Supports either your current combat target or a configurable NPC name list.
- Mirrors health bars, head icons, overhead text, names, and prototype hitsplats at the true tile location.
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

- Health bars
- Head icons
- Overhead text
- Actor names
- Prototype mirrored hitsplats

## Configuration

The plugin currently exposes controls for:

- Rendering the local player
- Rendering NPCs
- Choosing NPC target mode
- Configuring the NPC name list
- Toggling mirrored overhead text
- Toggling mirrored actor names
- Toggling mirrored health bars
- Toggling mirrored head icons
- Toggling mirrored hitsplats
- Adjusting outline color and style

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

## Status notes

- Mirrored health bars and head icons are custom overlay recreations.
- Mirrored hitsplats are implemented as a first-pass prototype using exact hitsplat events and timing, but they are not yet pixel-identical to native Jagex hitsplats.
- The plugin is being built iteratively with RuneLite Plugin Hub readiness in mind.
