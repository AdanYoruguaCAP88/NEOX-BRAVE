# NEOX-BRAVE — Architecture v0.3

## Runtime layers

### Presentation
Android Activity and custom Canvas renderer.

### Simulation
Player, enemies, projectiles, stage geometry and collision state.

### Configuration
AdaptiveSystem transforms three collected cores into an original capability vector.

### Decision
AdaptiveCombat evaluates capability plus live context and returns an action.

### Execution
The future CompanionController will translate the action into movement, attacks, interception and positioning.

## Key invariant

**Configuration != behavior.**

A configuration describes what the companion is capable of doing. The decision layer chooses what it should do based on current system state.

This invariant is central to the NEOX-BRAVE adaptive model.

## Future portability

The simulation and decision layers intentionally avoid Android UI dependencies. They can later be hosted by another 2D engine or test harness without changing the core adaptive model.