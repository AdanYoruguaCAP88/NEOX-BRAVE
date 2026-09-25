package com.neox.brave

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class CompanionState(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var action: CompanionAction = CompanionAction.GUARD,
    var attackCooldown: Float = 0f
)

class CompanionController {

    val state = CompanionState()

    fun update(
        dt: Float,
        groundY: Float,
        player: PlayerState,
        enemies: MutableList<EnemyState>,
        projectiles: MutableList<Projectile>,
        profile: CompanionProfile,
        action: CompanionAction,
        worldWidth: Float
    ) {
        state.action = action
        state.attackCooldown = max(0f, state.attackCooldown - dt)

        val anchorY = groundY - 120f
        val nearestEnemy = enemies
            .filter { it.energy > 0f }
            .minByOrNull { abs(it.x - player.x) }

        val nearestProjectile = projectiles
            .filter { it.hostile }
            .minByOrNull { abs(it.x - state.x) }

        when (action) {
            CompanionAction.ATTACK -> attack(
                dt, anchorY, nearestEnemy, enemies, profile
            )

            CompanionAction.INTERCEPT -> intercept(
                dt, anchorY, nearestProjectile, projectiles
            )

            CompanionAction.GUARD -> guard(
                dt, anchorY, player
            )

            CompanionAction.REPOSITION -> reposition(
                dt, anchorY, player, nearestEnemy, profile
            )

            CompanionAction.SUPPRESS -> suppress(
                dt, anchorY, enemies, profile
            )
        }

        state.x = state.x.coerceIn(24f, max(24f, worldWidth - 24f))
        state.y = anchorY
    }

    private fun attack(
        dt: Float,
        targetY: Float,
        target: EnemyState?,
        enemies: MutableList<EnemyState>,
        profile: CompanionProfile
    ) {
        if (target == null) {
            state.vx = 0f
            return
        }

        val speed = 180f + profile.mobility * 120f
        moveToward(target.x, speed, dt)

        if (abs(target.x - state.x) <= 62f && state.attackCooldown <= 0f) {
            val damage = 5f + profile.attack * 10f
            target.energy = max(0f, target.energy - damage)
            state.attackCooldown = max(0.28f, 0.75f - profile.attack * 0.3f)
        }
    }

    private fun intercept(
        dt: Float,
        targetY: Float,
        projectile: Projectile?,
        projectiles: MutableList<Projectile>
    ) {
        if (projectile == null) {
            state.vx = 0f
            return
        }

        moveToward(projectile.x, 300f, dt)

        if (abs(projectile.x - state.x) <= 30f) {
            projectiles.remove(projectile)
            state.attackCooldown = 0.15f
        }
    }

    private fun guard(
        dt: Float,
        targetY: Float,
        player: PlayerState
    ) {
        val desiredX = player.x + if (player.facing >= 0) 72f else -72f
        moveToward(desiredX, 220f, dt)
    }

    private fun reposition(
        dt: Float,
        targetY: Float,
        player: PlayerState,
        enemy: EnemyState?,
        profile: CompanionProfile
    ) {
        val desiredX = when {
            enemy == null -> player.x + 90f
            abs(enemy.x - player.x) > 300f -> player.x + if (player.facing >= 0) 120f else -120f
            else -> (player.x + enemy.x) * 0.5f
        }

        moveToward(desiredX, 220f + profile.mobility * 120f, dt)
    }

    private fun suppress(
        dt: Float,
        targetY: Float,
        enemies: MutableList<EnemyState>,
        profile: CompanionProfile
    ) {
        val active = enemies.filter { it.energy > 0f }
        if (active.isEmpty()) {
            state.vx = 0f
            return
        }

        val center = active.map { it.x }.average().toFloat()
        moveToward(center, 200f + profile.mobility * 100f, dt)

        if (state.attackCooldown <= 0f) {
            val radius = 120f + profile.range * 90f
            val damage = 3f + profile.control * 7f

            active.forEach { enemy ->
                if (abs(enemy.x - state.x) <= radius) {
                    enemy.energy = max(0f, enemy.energy - damage)
                }
            }

            state.attackCooldown = max(0.45f, 0.9f - profile.control * 0.25f)
        }
    }

    private fun moveToward(targetX: Float, speed: Float, dt: Float) {
        val delta = targetX - state.x
        val direction = when {
            delta > 4f -> 1f
            delta < -4f -> -1f
            else -> 0f
        }

        state.vx = direction * speed
        state.x += state.vx * dt

        if (direction == 0f) {
            state.vx = 0f
        }
    }
}
