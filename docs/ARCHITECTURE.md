# NEOX-BRAVE — Architecture v0.1

## Layers

### Presentation
Android Activity and custom 2D View.

### Simulation
Player, enemies, destructibles, projectiles, stages and collision rules.

### Decision
AdaptiveSystem and future behavior policies.

### Content
Original stage definitions, entity configuration and assets.

## Design rule

Game simulation must not depend on Android UI widgets.

This keeps the core portable and makes a future engine migration possible.

## Adaptive decision model

The companion system is intentionally data-driven.

A future profile can be represented as:

- attack priority
- defense priority
- control priority
- mobility priority
- threat response
- objective response

The runtime evaluates the current state and chooses the highest-priority valid action.

## Android strategy

The first prototype uses Kotlin and Canvas to minimize dependencies.

If the project grows beyond the prototype, the simulation layer can be migrated to a dedicated 2D engine while retaining the game-design and adaptive-system contracts.