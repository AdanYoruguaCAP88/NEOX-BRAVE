package com.neox.brave

import kotlin.math.abs
import kotlin.math.max

enum class SimulationScenario(
    val initialPlayerEnergy: Float,
    val enemyPositions: List<Float>,
    val initialProjectilePositions: List<Float>
) {
    OPEN_FIELD(100f, listOf(700f), emptyList()),
    CLOSE_ASSAULT(100f, listOf(260f, 340f), emptyList()),
    PROJECTILE_THREAT(100f, listOf(900f), listOf(390f)),
    LOW_ENERGY(22f, listOf(500f, 760f), emptyList()),
    CROWD_CONTROL(100f, listOf(420f, 500f, 580f), emptyList())
}

class SimulationWorld(
    val scenario: SimulationScenario,
    seed: Long,
    val width: Float = 1600f
) {
    private val rng = java.util.Random(seed)

    val player = PlayerState(x = 180f, y = 0f, energy = scenario.initialPlayerEnergy)
    val enemies = scenario.enemyPositions.map { EnemyState(it, 0f) }.toMutableList()
    val projectiles = scenario.initialProjectilePositions
        .map { Projectile(it, 0f, if (it < player.x) 260f else -260f, hostile = true) }
        .toMutableList()

    var timeSeconds: Float = 0f
        private set

    var damageDealt: Float = 0f
        private set

    var damageReceived: Float = 0f
        private set

    var projectilesIntercepted: Int = 0
        private set

    var enemiesDefeated: Int = 0
        private set

    fun observe(): Observation = Observation(
        playerX = player.x,
        playerEnergy = player.energy,
        nearestEnemyDistance = nearestEnemy()?.let { abs(it.x - player.x) },
        hostileProjectileDistance = nearestHostileProjectile()?.let { abs(it.x - player.x) },
        enemyCount = enemies.count { it.energy > 0f }
    )

    fun advance(dt: Float) {
        timeSeconds += dt

        enemies.filter { it.energy > 0f }.forEach { enemy ->
            enemy.projectileCooldown -= dt
            if (enemy.projectileCooldown <= 0f) {
                val direction = if (player.x < enemy.x) -1f else 1f
                projectiles += Projectile(
                    x = enemy.x,
                    y = 0f,
                    vx = direction * (220f + rng.nextFloat() * 80f),
                    hostile = true
                )
                enemy.projectileCooldown = 1.4f + rng.nextFloat() * 0.8f
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

    fun recordEnemyEnergyBeforeAfter(before: Map<EnemyState, Float>) {
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

    fun nearestEnemy(): EnemyState? =
        enemies.filter { it.energy > 0f }.minByOrNull { abs(it.x - player.x) }

    fun nearestHostileProjectile(): Projectile? =
        projectiles.filter { it.hostile }.minByOrNull { abs(it.x - player.x) }

    fun markIntercepted(count: Int) {
        projectilesIntercepted += count
    }
}
