package com.neox.brave

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CombatSimulationTest {

    @Test
    fun sameSeedProducesIdenticalSnapshot() {
        val simulation = CombatSimulation()
        val first = simulation.run(
            profile = profile("AAA"),
            scenario = SimulationScenario.PROJECTILE_THREAT,
            seed = 42L
        )
        val second = simulation.run(
            profile = profile("AAA"),
            scenario = SimulationScenario.PROJECTILE_THREAT,
            seed = 42L
        )

        assertEquals(first, second)
    }

    @Test
    fun differentSeedsCanProduceDifferentSnapshots() {
        val simulation = CombatSimulation()
        val first = simulation.run(
            profile = profile("AAA"),
            scenario = SimulationScenario.CLOSE_ASSAULT,
            seed = 42L
        )
        val second = simulation.run(
            profile = profile("AAA"),
            scenario = SimulationScenario.CLOSE_ASSAULT,
            seed = 43L
        )

        assertNotEquals(first, second)
    }

    @Test
    fun fullExperimentProducesExactlyTwoThousandCombats() {
        val seeds = 1L..100L
        val summary = ExperimentRunner().run(seeds)

        assertEquals(2000, summary.combats)
    }

    private fun profile(signature: String): CompanionProfile {
        val system = AdaptiveSystem()
        var result: CompanionProfile? = null
        signature.forEach { core ->
            result = system.collect(Core.valueOf(core.toString()))
        }
        return requireNotNull(result)
    }
}
