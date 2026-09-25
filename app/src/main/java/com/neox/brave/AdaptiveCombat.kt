package com.neox.brave

enum class CompanionAction { ATTACK, INTERCEPT, GUARD, REPOSITION, SUPPRESS }

data class CombatContext(
    val playerX: Float,
    val playerEnergy: Float,
    val nearestEnemyDistance: Float?,
    val hostileProjectileDistance: Float?,
    val enemyCount: Int
)

class AdaptiveCombat {
    fun decide(profile: CompanionProfile, context: CombatContext): CompanionAction {
        val projectileThreat = context.hostileProjectileDistance?.let { it < 180f } ?: false
        val enemyThreat = context.nearestEnemyDistance?.let { it < 260f } ?: false

        if (projectileThreat && profile.defense + profile.control >= 1.0f) {
            return CompanionAction.INTERCEPT
        }
        if (context.playerEnergy < 30f && profile.defense >= profile.attack) {
            return CompanionAction.GUARD
        }
        if (context.enemyCount >= 2 && profile.control >= 0.65f) {
            return CompanionAction.SUPPRESS
        }
        if (enemyThreat && profile.attack >= 0.5f) {
            return CompanionAction.ATTACK
        }
        return if (profile.mobility >= 0.65f) CompanionAction.REPOSITION else CompanionAction.GUARD
    }
}