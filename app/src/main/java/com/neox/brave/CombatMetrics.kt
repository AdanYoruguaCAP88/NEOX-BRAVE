package com.neox.brave

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
