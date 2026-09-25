package com.neox.brave

enum class Core { A, B }

data class CompanionProfile(
    val signature: String,
    val attack: Float,
    val defense: Float,
    val control: Float,
    val mobility: Float
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
        val attack = signature.count { it == 'A' }.toFloat()
        val defense = signature.count { it == 'B' }.toFloat()

        return when {
            attack == 3f -> CompanionProfile(signature, 1.0f, 0.2f, 0.2f, 0.4f)
            defense == 3f -> CompanionProfile(signature, 0.25f, 1.0f, 0.35f, 0.35f)
            signature == "ABA" -> CompanionProfile(signature, 0.7f, 0.4f, 0.9f, 0.5f)
            signature == "BAB" -> CompanionProfile(signature, 0.45f, 0.85f, 0.75f, 0.6f)
            else -> CompanionProfile(signature, 0.6f, 0.6f, 0.6f, 0.6f)
        }
    }
}