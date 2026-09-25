# NEOX-BRAVE — Architecture v0.4

## Runtime layers

### Presentation
Android Activity and custom Canvas renderer.

### World model
Player, enemies, projectiles, stage geometry and mutable combat state.

### Observation
`Observation` is an immutable projection of world state. The UI does not construct combat context directly.

**WORLD STATE -> OBSERVATION -> COMBAT CONTEXT**

### Configuration
`AdaptiveSystem` transforms three collected cores into an original capability vector and instantiates a `CompanionProfile`.

### Decision
`AdaptiveCombat` evaluates capability plus live observation and returns an action.

### Execution
`CompanionController` translates the selected action into movement, attacks, interception and positioning.

### Experimentation
`CombatSimulation` runs the same observation/decision/execution loop without Android UI dependencies.

This creates a deterministic headless surface for repeated experiments and future batch evaluation.

## Adaptive loop

```text
OBSERVE
   ↓
CONFIGURE
   ↓
EVALUATE
   ↓
DECIDE
   ↓
ACT
   ↓
OBSERVE
```

The loop is closed: actions mutate the world, and the next observation is generated from the resulting state.

## Key invariant

**Configuration != behavior.**

A configuration describes what the companion is capable of doing. The decision layer chooses what it should do based on current system state.

This invariant is central to the NEOX-BRAVE adaptive model.

## Experimental surface

`CombatSimulation.run(profile, durationSeconds, seed)` provides a reproducible single-combat execution.

Current metrics include:

- damage received
- damage dealt
- projectiles intercepted
- enemies defeated
- survival time
- remaining energy
- action distribution

The next experimental layer can execute many seeded combats and aggregate these metrics without touching the Android renderer.

## Portability

The adaptive decision and execution layers are independent of Android UI APIs. The headless simulation makes that boundary explicit and provides a foundation for automated experiments, regression tests and large-sample behavioral analysis.
