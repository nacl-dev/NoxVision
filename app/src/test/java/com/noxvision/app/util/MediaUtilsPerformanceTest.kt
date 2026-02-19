package com.noxvision.app.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaUtilsPerformanceTest {

    // Helper to simulate network check
    private suspend fun mockCheckUrl(url: String, delays: Map<String, Long>, working: Set<String>): Boolean {
        val d = delays[url] ?: 100L
        delay(d)
        return working.contains(url)
    }

    // Sequential implementation (simulating current code)
    private suspend fun findWorkingUrlSequential(
        urls: List<String>,
        check: suspend (String) -> Boolean
    ): String? {
        for (url in urls) {
            if (check(url)) {
                return url
            }
        }
        return null
    }

    @Test
    fun testPerformanceImprovement() = runBlocking {
        val urls = listOf("url1", "url2", "url3", "url4", "url5")
        // Scenario: url1 and url2 are slow and fail (timeout). url3 is fast and works.
        val delays = mapOf(
            "url1" to 2000L,
            "url2" to 2000L,
            "url3" to 500L,
            "url4" to 100L,
            "url5" to 100L
        )
        val working = setOf("url3", "url4", "url5")

        val check: suspend (String) -> Boolean = { url ->
            mockCheckUrl(url, delays, working)
        }

        // Measure Sequential (Baseline)
        val startSeq = System.currentTimeMillis()
        val resultSeq = findWorkingUrlSequential(urls, check)
        val timeSeq = System.currentTimeMillis() - startSeq

        println("Sequential Time: ${timeSeq}ms")
        assertTrue("Sequential should verify url1(2s) + url2(2s) + url3(0.5s) ~ 4.5s", timeSeq >= 4500)
        assertTrue("Sequential result should be url3", resultSeq == "url3")

        // Measure Parallel (Using actual implementation)
        val startPar = System.currentTimeMillis()
        val resultPar = findWorkingUrl(urls, check)
        val timePar = System.currentTimeMillis() - startPar

        println("Parallel Time: ${timePar}ms")
        // Parallel picks the fastest winner, which is url4 or url5 (100ms)
        assertTrue("Parallel should be around 100ms", timePar < 1000)
        assertTrue("Parallel result should be one of the working urls", working.contains(resultPar))
    }
}
