package com.neox.brave

data class MetricSummary(
    val mean: Float,
    val median: Float,
    val variance: Float
)

data class ExperimentSummary(
    val combats: Int,
    val damageDealt: MetricSummary,
    val damageReceived: MetricSummary,
    val projectilesIntercepted: MetricSummary,
    val enemiesDefeated: MetricSummary,
    val survivalTimeSeconds: MetricSummary,
    val remainingEnergy: MetricSummary,
    val actionDistribution: Map<CompanionAction, Int>
) {
    companion object {
        fun from(results: List<CombatMetrics>): ExperimentSummary {
            require(results.isNotEmpty()) { "Experiment requires at least one combat." }

            fun summarize(values: List<Float>): MetricSummary {
                val sorted = values.sorted()
                val mean = sorted.average().toFloat()
                val median = if (sorted.size % 2 == 0) {
                    (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2f
                } else {
                    sorted[sorted.size / 2]
                }
                val variance = sorted
                    .map { (value - mean) * (value - mean) }
                    .average()
                    .toFloat()
                return MetricSummary(mean, median, variance)
            }

            val distribution = CompanionAction.values().associate { action ->
                action to results.sumOf { metrics ->
                    metrics.actionCounts[action] ?: 0
                }
            }

            return ExperimentSummary(
                combats = results.size,
                damageDealt = summarize(results.map { it.damageDealt }),
                damageReceived = summarize(results.map { it.damageReceived }),
                projectilesIntercepted = summarize(results.map { it.projectilesIntercepted.toFloat() }),
                enemiesDefeated = summarize(results.map { it.enemiesDefeated.toFloat() }),
                survivalTimeSeconds = summarize(results.map { it.survivalTimeSeconds }),
                remainingEnergy = summarize(results.map { it.remainingEnergy }),
                actionDistribution = distribution
            )
        }
    }
}
