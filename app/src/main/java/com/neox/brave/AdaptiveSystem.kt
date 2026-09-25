package com.neox.brave

enum class Core { A, B, C, D }

data class CompanionProfile(
    val signature: String,
    val archetype: String,
    val attack: Float,
    val defense: Float,
    val control: Float,
    val mobility: Float,
    val range: Float
)

class AdaptiveSystem {
    private val cores = mutableListOf<Core>()

    fun collect(core: Core): CompanionProfile? {
        cores += core
        if (cores.size < 3) return null

        val signature = cores.joinToString("") { it.name }
        cores.clear()
        return profileFor(signature)
    }

    fun pending(): List<Core> = cores.toList()

    private fun profileFor(signature: String): CompanionProfile {
        val vector = signature.map { coreVector(it) }
        val attack = vector.map { it[0] }.average().toFloat()
        val defense = vector.map { it[1] }.average().toFloat()
        val control = vector.map { it[2] }.average().toFloat()
        val mobility = vector.map { it[3] }.average().toFloat()
        val range = vector.map { it[4] }.average().toFloat()

        val archetype = when {
            attack >= 0.82f && mobility >= 0.65f -> "HUNTER"
            defense >= 0.82f -> "GUARDIAN"
            control >= 0.82f -> "TACTICIAN"
            range >= 0.82f -> "SENTINEL"
            else -> "HYBRID"
        }

        return CompanionProfile(signature, archetype, attack, defense, control, mobility, range)
    }

    private fun coreVector(core: Char): FloatArray = when (core) {
        'A' -> floatArrayOf(1.0f, 0.2f, 0.15f, 0.55f, 0.35f)
        'B' -> floatArrayOf(0.2f, 1.0f, 0.35f, 0.30f, 0.35f)
        'C' -> floatArrayOf(0.45f, 0.35f, 1.0f, 0.60f, 0.55f)
        'D' -> floatArrayOf(0.40f, 0.30f, 0.45f, 1.0f, 0.75f)
        else -> floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
    }
}