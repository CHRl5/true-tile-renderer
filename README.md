# True Tile Renderer

True Tile Renderer is a RuneLite plugin that hides selected actors at their client-rendered position and redraws their animated outline and mirrored combat overlays on the server-true tile instead.

![True Tile Renderer overview](docs/assets/readme-hero.png)

## What it does

- Hides your local player model and redraws its animated outline on the true tile.
- Hides matching NPC models and redraws them on their true tile.
- Supports either your current combat target or a configurable NPC name list.
- Mirrors names, health bars, overheads, and hitsplats at the true tile location.
- Keeps the visual language focused on server position instead of interpolated client position.

## Why use it

This plugin is built for situations where exact server positioning matters more than the default rendered position. It is especially useful for movement-heavy PvM, learning encounter spacing, reading melee/range/mage proximity on bosses, and understanding when a model has visually drifted away from its real tile.

## Features

- Local player true-tile outline rendering
- NPC true-tile outline rendering
- Exact live model animation mirroring on the true tile
- Current-target mode
- Configured-list mode using NPC names
- Actor names
- Health bars
- Overheads
- Mirrored hitsplats
- Configurable outline colors, width, and feathering

## Configuration

- Hide the local player model and render a true-tile outline instead
- Hide matching NPC models and render true-tile outlines instead
- Choose between current-target mode and configured-list mode
- Configure an NPC name list
- Toggle mirrored actor names, health bars, overheads, and hitsplats
- Adjust outline colors, width, and feathering

## Status notes

- Mirrored health bars and head icons are custom overlay recreations.
- Mirrored hitsplats are implemented as a first-pass prototype using exact hitsplat events and timing, but they are not yet pixel-identical to native Jagex hitsplats.
