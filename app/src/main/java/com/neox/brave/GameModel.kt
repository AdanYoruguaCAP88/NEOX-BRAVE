package com.neox.brave

import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max

data class PlayerState(
    var x: Float = 180f,
    var y: Float = 0f,
    var energy: Float = 100f,
    var facing: Int = 1
)

data class EnemyState(
    var x: Float,
    var y: Float,
    var energy: Float = 30f,
    var projectileCooldown: Float = 0f
)

data class Projectile(
    var x: Float,
    var y: Float,
    val vx: Float,
    val hostile: Boolean
)

class GameModel {
    val player = PlayerState()
    val enemies = mutableListOf(EnemyState(900f, 0f), EnemyState(1180f, 0f))
    val projectiles = mutableListOf<Projectile>()

    fun update(dt: Float, groundY: Float) {
        player.y = groundY - 72f

        enemies.filter { it.energy > 0f }.forEach { enemy ->
            enemy.projectileCooldown -= dt

            if (enemy.projectileCooldown <= 0f) {
                val direction = if (player.x < enemy.x) -1f else 1f
                projectiles += Projectile(
                    enemy.x,
                    groundY - 42f,
                    direction * 260f,
                    hostile = true
                )
                enemy.projectileCooldown = 1.8f
            }
        }

        projectiles.forEach { it.x += it.vx * dt }

        val playerHitbox = hitbox()
        val hits = projectiles.filter {
            it.hostile && playerHitbox.contains(it.x, it.y)
        }

        if (hits.isNotEmpty()) {
            player.energy = max(0f, player.energy - hits.size * 8f)
            projectiles.removeAll(hits)
        }

        projectiles.removeAll { it.x < -100f || it.x > 3000f }
    }

    fun observe(): Observation = Observation(
        playerX = player.x,
        playerEnergy = player.energy,
        nearestEnemyDistance = nearestEnemy()?.let { abs(it.x - player.x) },
        hostileProjectileDistance = nearestHostileProjectile()?.let { abs(it.x - player.x) },
        enemyCount = enemies.count { it.energy > 0f }
    )

    fun nearestEnemy(): EnemyState? =
        enemies
            .filter { it.energy > 0f }
            .minByOrNull { abs(it.x - player.x) }

    fun nearestHostileProjectile(originX: Float = player.x): Projectile? =
        projectiles
            .filter { it.hostile }
            .minByOrNull { abs(it.x - originX) }

    fun hitbox(): RectF =
        RectF(player.x, player.y, player.x + 42f, player.y + 72f)
}
