package com.neox.brave

import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

enum class SimulationScenario {
    OPEN_FIELD,
    CLOSE_ASSAULT,
    PROJECTILE_THREAT,
    LOW_ENERGY,
    CROWD_CONTROL
}

data class SimulationWorld(
    val player: PlayerState,
    val enemies: MutableList<EnemyState>,
    val projectiles: MutableList<Projectile>,
    val width: Float = 1600f,
    private val random: Random
) {
    var time: Float = 0f
        private set
    var damageReceived: Float = 0f
        private set
    var damageDealt: Float = 0f
        private set
    var projectilesIntercepted: Int = 0
        private set
    var enemiesDefeated: Int = 0
        private set

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

    fun advance(dt: Float) {
        time += dt

        enemies.filter { it.energy > 0f }.forEach { enemy ->
            enemy.projectileCooldown -= dt
            if (enemy.projectileCooldown <= 0f) {
                val direction = if (player.x < enemy.x) -1f else 1f
                projectiles += Projectile(
                    x = enemy.x,
                    y = 0f,
                    vx = direction * (220f + random.nextFloat() * 80f),
                    hostile = true
                )
                enemy.projectileCooldown = 1.4f + random.nextFloat() * 0.8f
            }
        }

        projectiles.forEach { it.x += it.vx * dt }

        val hits = projectiles.filter { it.hostile && abs(it.x - player.x) <= 28f }
        if (hits.isNotEmpty()) {
            val damage = hits.size * 8f
            player.energy = max(0f, player.energy - damage)
            damageReceived += damage
            projectiles.removeAll(hits)
        }

        projectiles.removeAll { it.x < -100f || it.x > width + 100f }
    }

    fun recordEnemyChanges(before: Map<EnemyState, Float>) {
        before.forEach { (enemy, previous) ->
            val delta = previous - enemy.energy
            if (delta > 0f) {
                damageDealt += delta
                if (previous > 0f && enemy.energy <= 0f) {
                    enemiesDefeated += 1
                }
            }
        }
    }

    fun recordInterceptions(count: Int) {
        projectilesIntercepted += count
    }
}

data class CombatMetrics(
    val seed: Long,
    val scenario: SimulationScenario,
    val profileSignature: String,
    val durationSeconds: Float,
    val damageDealt: Float,
    val damageReceived: Float,
    val projectilesIntercepted: Int,
    val enemiesDefeated: Int,
    val survivalTimeSeconds: Float,
    val remainingEnergy: Float,
    val actionCounts: Map<CompanionAction, Int>
) {
    val totalActions: Int
        get() = actionCounts.values.sum()

    fun actionShare(action: CompanionAction): Float =
        if (totalActions == 0) 0f else (actionCounts[action] ?: 0) / totalActions.toFloat()
}

/**
 * Headless deterministic combat harness.
 *
 * The Android renderer is absent here. The decision path remains:
 * Observation -> AdaptiveCombat -> CompanionController -> world mutation.
 */
class CombatSimulation(
    private val stepSeconds: Float = 1f / 30f,
    private val maxDurationSeconds: Float = 30f
) {
    fun run(
        profile: CompanionProfile,
        scenario: SimulationScenario,
        seed: Long
    ): CombatMetrics {
        val world = createWorld(scenario, seed)
        val combat = AdaptiveCombat()
        val controller = CompanionController()
        controller.state.x = world.player.x + 72f

        val actions = CompanionAction.values().associateWith { 0 }.toMutableMap()
        var elapsed = 0f

        while (elapsed < maxDurationSeconds && world.player.energy > 0f) {
            val observation = world.observe()
            val action = combat.decide(profile, observation.toCombatContext())
            actions[action] = (actions[action] ?: 0) + 1

            val beforeEnemies = world.enemies.associateWith { it.energy }
            val beforeProjectiles = world.projectiles.size

            controller.update(
                dt = stepSeconds,
                groundY = 0f,
                player = world.player,
                enemies = world.enemies,
                projectiles = world.projectiles,
                profile = profile,
                action = action,
                worldWidth = world.width
            )

            world.recordInterceptions(
                (beforeProjectiles - world.projectiles.size).coerceAtLeast(0)
            )
            world.recordEnemyChanges(beforeEnemies)
            world.advance(stepSeconds)
            elapsed += stepSeconds
        }

        return CombatMetrics(
            seed = seed,
            scenario = scenario,
            profileSignature = profile.signature,
            durationSeconds = elapsed,
            damageDealt = world.damageDealt,
            damageReceived = world.damageReceived,
            projectilesIntercepted = world.projectilesIntercepted,
            enemiesDefeated = world.enemiesDefeated,
            survivalTimeSeconds = elapsed,
            remainingEnergy = world.player.energy,
            actionCounts = actions.toMap()
        )
    }

    private fun createWorld(
        scenario: SimulationScenario,
        seed: Long
    ): SimulationWorld {
        val random = Random(seed)
        val playerEnergy: Float
        val enemyPositions: List<Float>
        val projectilePositions: List<Float>

        when (scenario) {
            SimulationScenario.OPEN_FIELD -> {
                playerEnergy = 100f
                enemyPositions = listOf(760f)
                projectilePositions = emptyList()
            }
            SimulationScenario.CLOSE_ASSAULT -> {
                playerEnergy = 100f
                enemyPositions = listOf(260f, 340f)
                projectilePositions = emptyList()
            }
            SimulationScenario.PROJECTILE_THREAT -> {
                playerEnergy = 100f
                enemyPositions = listOf(900f)
                projectilePositions = listOf(390f)
            }
            SimulationScenario.LOW_ENERGY -> {
                playerEnergy = 22f
                enemyPositions = listOf(500f, 760f)
                projectilePositions = emptyList()
            }
            SimulationScenario.CROWD_CONTROL -> {
                playerEnergy = 100f
                enemyPositions = listOf(420f, 500f, 580f)
                projectilePositions = emptyList()
            }
        }

        return SimulationWorld(
            player = PlayerState(x = 180f, y = 0f, energy = playerEnergy),
            enemies = enemyPositions.map { EnemyState(it, 0f) }.toMutableList(),
            projectiles = projectilePositions.map {
                Projectile(
                    x = it,
                    y = 0f,
                    vx = if (it < 180f) 260f else -260f,
                    hostile = true
                )
            }.toMutableList(),
            random = random
        )
    }
}
