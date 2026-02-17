package com.noxvision.app.hunting.calendar

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.noxvision.app.hunting.calendar.HuntingSeasonData.HuntingSeason

class HuntingSeasonDataTest {

    // Test for a season completely within a single year (e.g., May 1 to October 15)
    @Test
    fun isInSeason_standardSeason_returnsTrueForInSeasonDates() {
        val season = HuntingSeason(
            wildlifeType = "Test",
            gender = null,
            startMonth = 5,
            startDay = 1,
            endMonth = 10,
            endDay = 15
        )

        // Test start date
        assertTrue("May 1st should be in season", season.isInSeason(5, 1))

        // Test middle date
        assertTrue("July 15th should be in season", season.isInSeason(7, 15))

        // Test end date
        assertTrue("October 15th should be in season", season.isInSeason(10, 15))
    }

    @Test
    fun isInSeason_standardSeason_returnsFalseForOutOfSeasonDates() {
        val season = HuntingSeason(
            wildlifeType = "Test",
            gender = null,
            startMonth = 5,
            startDay = 1,
            endMonth = 10,
            endDay = 15
        )

        // Test before start
        assertFalse("April 30th should not be in season", season.isInSeason(4, 30))

        // Test after end
        assertFalse("October 16th should not be in season", season.isInSeason(10, 16))
    }

    // Test for a season spanning across the year boundary (e.g., September 1 to January 31)
    @Test
    fun isInSeason_crossYearSeason_returnsTrueForInSeasonDates() {
        val season = HuntingSeason(
            wildlifeType = "Test",
            gender = null,
            startMonth = 9,
            startDay = 1,
            endMonth = 1,
            endDay = 31
        )

        // Test start date
        assertTrue("September 1st should be in season", season.isInSeason(9, 1))

        // Test late in the year
        assertTrue("December 31st should be in season", season.isInSeason(12, 31))

        // Test early in the year
        assertTrue("January 1st should be in season", season.isInSeason(1, 1))

        // Test end date
        assertTrue("January 31st should be in season", season.isInSeason(1, 31))
    }

    @Test
    fun isInSeason_crossYearSeason_returnsFalseForOutOfSeasonDates() {
        val season = HuntingSeason(
            wildlifeType = "Test",
            gender = null,
            startMonth = 9,
            startDay = 1,
            endMonth = 1,
            endDay = 31
        )

        // Test before start (middle of year)
        assertFalse("August 31st should not be in season", season.isInSeason(8, 31))

        // Test after end (middle of year)
        assertFalse("February 1st should not be in season", season.isInSeason(2, 1))
    }

    // Edge Cases
    @Test
    fun isInSeason_singleDaySeason_handlesCorrectly() {
        val season = HuntingSeason(
            wildlifeType = "Test",
            gender = null,
            startMonth = 5,
            startDay = 1,
            endMonth = 5,
            endDay = 1
        )

        assertTrue("May 1st should be in season", season.isInSeason(5, 1))
        assertFalse("May 2nd should not be in season", season.isInSeason(5, 2))
        assertFalse("April 30th should not be in season", season.isInSeason(4, 30))
    }

    @Test
    fun isInSeason_fullYearSeason_handlesCorrectly() {
        val season = HuntingSeason(
            wildlifeType = "Test",
            gender = null,
            startMonth = 1,
            startDay = 1,
            endMonth = 12,
            endDay = 31
        )

        assertTrue("January 1st should be in season", season.isInSeason(1, 1))
        assertTrue("December 31st should be in season", season.isInSeason(12, 31))
        assertTrue("July 15th should be in season", season.isInSeason(7, 15))
    }
}
