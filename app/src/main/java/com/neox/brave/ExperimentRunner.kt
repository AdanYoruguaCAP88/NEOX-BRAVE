package com.neox.brave

class ExperimentRunner(
    private val simulation: CombatSimulation = CombatSimulation()
) {
    private val defaultProfiles = listOf("AAA", "BBB", "CCC", "DDD")

    fun run(
        seeds: Iterable<Long>,
        signatures: List<String> = defaultProfiles,
        scenarios: List<SimulationScenario> = SimulationScenario.entries
    ): ExperimentSummary {
        val results = buildList {
            for (signature in signatures) {
                val profile = profileFromSignature(signature)
                for (scenario in scenarios) {
                    for (seed in seeds) {
                        add(simulation.run(profile, scenario, seed))
                    }
                }
            }
        }
        return ExperimentSummary.from(results)
    }

    fun runSingle(
        signature: String,
        scenario: SimulationScenario,
        seed: Long
    ): CombatMetrics =
        simulation.run(profileFromSignature(signature), scenario, seed)

    private fun profileFromSignature(signature: String): CompanionProfile {
        require(signature.length == 3 && signature.all { it in "ABCD" }) {
            "Signature must contain exactly three cores from A-D."
        }

        val system = AdaptiveSystem()
        var profile: CompanionProfile? = null
        signature.forEach { core ->
            profile = system.collect(Core.valueOf(core.toString()))
        }
        return requireNotNull(profile)
    }
}
