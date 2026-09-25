# NEOX-BRAVE

**NEOX-BRAVE** is an original spiritual successor to the 8-bit action-platformer experience that inspired this project.

It is **not a port, remake, decompilation, or asset conversion** of *Tokkyū Shirei Solbrain*, *Shatterhand*, or any other existing game. Characters, world, names, art, audio, code, story, and assets are original.

## Vision

A fast 2D action-platformer for Android built around one central idea:

> **The right companion is the one appropriate to the current state of the system.**

The player collects adaptive cores, combines three signals, and instantiates an autonomous companion whose behavior is determined by the resulting configuration and battlefield context.

## Current prototype

The first vertical slice targets:

- Android
- Kotlin
- Custom 2D renderer
- Touch controls
- Keyboard/gamepad-friendly input architecture
- Breakable blocks
- A/B core collection
- Three-core combination
- Companion instantiation
- Basic adaptive behavior
- Original placeholder visuals

## Repository structure

```
app/
  src/main/java/com/neox/brave/
    MainActivity.kt
    BraveGameView.kt
    AdaptiveSystem.kt

docs/
  GAME_DESIGN.md
  ARCHITECTURE.md
```

## Development principle

Build the **system first**, then increase visual fidelity.

The prototype deliberately uses simple procedural shapes so gameplay and adaptive logic can be tested before original art production.

## Roadmap

1. Playable vertical slice
2. Combat model
3. Adaptive companion behaviors
4. First complete stage
5. Boss prototype
6. Original art direction
7. Audio
8. Android polish
9. Controller support
10. Release build

## IP boundary

NEOX-BRAVE may be inspired by general gameplay concepts from classic action games, but it does not reuse protected characters, names, visual designs, music, dialogue, maps, sprites, code, or other expressive assets from those works.

© 2026 Adán Ramalho / NEOX-BRAVE. Original project.