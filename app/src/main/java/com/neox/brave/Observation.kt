package com.neox.brave

data class Observation(
    val playerX: Float,
    val playerEnergy: Float,
    val nearestEnemyDistance: Float?,
    val hostileProjectileDistance: Float?,
    val enemyCount: Int
) {
    fun toCombatContext(): CombatContext =
        CombatContext(
            playerX = playerX,
            playerEnergy = playerEnergy,
            nearestEnemyDistance = nearestEnemyDistance,
            hostileProjectileDistance = hostileProjectileDistance,
            enemyCount = enemyCount
        )
}
