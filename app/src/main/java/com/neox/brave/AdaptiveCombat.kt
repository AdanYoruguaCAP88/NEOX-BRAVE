package com.neox.brave

enum class CompanionAction { ATTACK, INTERCEPT, GUARD, REPOSITION }

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
        val enemyThreat = context.nearestEnemyDistance?.let { it < 240f } ?: false

        if (projectileThreat && profile.defense >= profile.attack) return CompanionAction.INTERCEPT
        if (context.playerEnergy < 30f && profile.defense > 0.5f) return CompanionAction.GUARD
        if (enemyThreat && profile.attack >= profile.defense) return CompanionAction.ATTACK
        return if (profile.mobility >= 0.7f) CompanionAction.REPOSITION else CompanionAction.GUARD
    }
}