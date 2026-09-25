package com.neox.brave

import kotlin.math.abs
import kotlin.math.max

/**
 * Headless deterministic combat harness.
 *
 * No Android/UI dependencies: the same Observation -> AdaptiveCombat ->
 * CompanionController loop can be executed repeatedly for experiments.
 */
data class SimulationWorld(
    val player: PlayerState = PlayerState(x = 180f, y = 0f),
    val enemies: MutableList<EnemyState> = mutableListOf(
        EnemyState(900f, 0f),
        EnemyState(1180f, 0f)
    ),
    val projectiles: MutableList<Projectile> = mutableListOf(),
    var time: Float = 0f
) {
    fun observe(): Observation = Observation(
        playerX = player.x,
        playerEnergy = player.energy,
        nearestEnemyDistance = enemies
            .filter { it.energy > 0f }
            .minOfOrNull { abs(it.x - player.x) },
        hostileProjectileDistance = projectiles
            .filter { it.hostile }
            .minOfOrNull { abs(it.x - player.x) },
        enemyCount = enemies.count { it.energy > 0f }
    )
}

data class CombatMetrics(
    var damageReceived: Float = 0f,
    var projectilesIntercepted: Int = 0,
    var enemiesDefeated: Int = 0,
    var survivalTime: Float = 0f,
    var energyRemaining: Float = 0f,
    var damageDealt: Float = 0f,
    var actions: MutableMap<CompanionAction, Int> = CompanionAction.values()
        .associateWith { 0 }
        .toMutableMap()
}

data class SimulationResult(
    val profile: CompanionProfile,
    val metrics: CombatMetrics
)

class CombatSimulation(
    private val groundY: Float = 500f,
    private val worldWidth: Float = 1600f,
    private val stepSeconds: Float = 1f / 30f
) {
    fun run(
        profile: CompanionProfile,
        durationSeconds: Float = 60f,
        seed: Int = 0
    ): SimulationResult {
        val world = SimulationWorld(
            player = PlayerState(x = 180f, y = groundY - 72f),
            enemies = seededEnemies(seed).toMutableList()
        )
        val combat = AdaptiveCombat()
        val controller = CompanionController()
        controller.state.x = world.player.x + 72f

        val metrics = CombatMetrics()
        var previousEnergy = world.player.energy

        val steps = (durationSeconds / stepSeconds).toInt()

        for (step in 0 until steps) {
            world.time += stepSeconds
            updateEnemies(world, stepSeconds)
            updateProjectiles(world, stepSeconds)

            val observation = world.observe()
            val action = combat.decide(profile, observation.toCombatContext())
            metrics.actions[action] = (metrics.actions[action] ?: 0) + 1

            val previousEnemyEnergy = world.enemies.sumOf { it.energy.toDouble() }.toFloat()
            val previousProjectileCount = world.projectiles.count { it.hostile }

            controller.update(
                dt = stepSeconds,
                groundY = groundY,
                player = world.player,
                enemies = world.enemies,
                projectiles = world.projectiles,
                profile = profile,
                action = action,
                worldWidth = worldWidth
            )

            val currentEnemyEnergy = world.enemies.sumOf { it.energy.toDouble() }.toFloat()
            val defeatedBefore = metrics.enemiesDefeated
            metrics.enemiesDefeated = world.enemies.count { it.energy <= 0f }

            val energyDelta = previousEnergy - world.player.energy
            if (energyDelta > 0f) metrics.damageReceived += energyDelta
            previousEnergy = world.player.energy

            val dealt = previousEnemyEnergy - currentEnemyEnergy
            if (dealt > 0f) metrics.damageDealt += dealt

            if (action == CompanionAction.INTERCEPT) {
                val remainingProjectiles = world.projectiles.count { it.hostile }
                metrics.projectilesIntercepted +=
                    (previousProjectileCount - remainingProjectiles).coerceAtLeast(0)
            }

            if (world.player.energy <= 0f) break
        }

        metrics.survivalTime = world.time
        metrics.energyRemaining = world.player.energy

        return SimulationResult(profile, metrics)
    }

    private fun updateEnemies(world: SimulationWorld, dt: Float) {
        world.enemies
            .filter { it.energy > 0f }
            .forEach { enemy ->
                enemy.projectileCooldown -= dt
                if (enemy.projectileCooldown <= 0f) {
                    val direction = if (world.player.x < enemy.x) -1f else 1f
                    world.projectiles += Projectile(
                        x = enemy.x,
                        y = groundY - 42f,
                        vx = direction * 260f,
                        hostile = true
                    )
                    enemy.projectileCooldown = 1.8f
                }
            }
    }

    private fun updateProjectiles(world: SimulationWorld, dt: Float) {
        val before = world.projectiles.size
        world.projectiles.forEach { it.x += it.vx * dt }

        val playerLeft = world.player.x
        val playerRight = world.player.x + 42f
        val playerTop = world.player.y
        val playerBottom = world.player.y + 72f

        val hits = world.projectiles.filter {
            it.hostile &&
                it.x >= playerLeft &&
                it.x <= playerRight &&
                it.y >= playerTop &&
                it.y <= playerBottom
        }

        if (hits.isNotEmpty()) {
            world.player.energy = max(
                0f,
                world.player.energy - hits.size * 8f
            )
            world.projectiles.removeAll(hits)
        }

        world.projectiles.removeAll { it.x < -100f || it.x > worldWidth + 100f }

        // Controller removes intercepted projectiles before this frame's final state.
        // The harness measures this at the call site in run().
        @Suppress("UNUSED_VARIABLE")
        val removedByWorldRules = before - world.projectiles.size
    }

    private fun seededEnemies(seed: Int): List<EnemyState> {
        val offset = ((seed % 5) + 5) % 5
        return listOf(
            EnemyState(900f + offset * 24f, 0f),
            EnemyState(1180f - offset * 18f, 0f),
            EnemyState(1420f + offset * 12f, 0f)
        )
    }
}
