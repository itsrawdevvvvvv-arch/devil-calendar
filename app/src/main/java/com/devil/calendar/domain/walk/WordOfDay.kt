package com.devil.calendar.domain.walk

import java.time.LocalDate

/**
 * The word that fills up with your steps.
 *
 * 61 cores x 6 endings = 366 distinct phrases, so every day of a leap year gets
 * its own and none repeats. The pick is deterministic from the date, so the word
 * never changes mid-day even across a reinstall.
 */
object WordOfDay {

    private val cores = listOf(
        "KEEP GOING", "CHALTE RAHO", "NO EXCUSES", "ONE MORE LAP", "SHOW UP",
        "MOVE IT", "DON'T STOP", "EARN IT", "PUSH ON", "STAY HARD",
        "AAGE BADH", "LAGA REH", "ONE MORE", "OWN TODAY", "BUILD IT",
        "GO AGAIN", "STAND UP", "WALK IT OFF", "HOLD THE LINE", "FINISH IT",
        "START NOW", "BE RELENTLESS", "NO ZERO DAYS", "MAKE IT COUNT", "KEEP THE PACE",
        "RUN YOUR RACE", "DAYS ADD UP", "SMALL STEPS", "BIG CHANGE", "DO THE WORK",
        "RISE EARLY", "STAY HUNGRY", "BREATHE DEEP", "LEGS MOVING", "HEART STRONG",
        "TERRACE LAPS", "ONE HUNDRED", "TEN THOUSAND", "STEP BY STEP", "OVER THE LINE",
        "QUIET GRIND", "OWN IT", "GOOD TIRED", "SHOW THE WORK", "DAILY GRIND",
        "WALK TALL", "STAY THE COURSE", "PICK IT UP", "DON'T RUSH", "STEADY ON",
        "NEVER SKIP", "ALWAYS ON", "MAKE TIME", "BE THE ONE", "PROVE IT",
        "GO THE LAP", "HOLD ON", "FIND A WAY", "KEEP COUNT", "MORE TODAY",
        "BETTER TOMORROW",
    )

    private val endings = listOf("", " TODAY", " NOW", " AGAIN", " ONCE MORE", " FOR YOU")

    /** 366 phrases, one per day of the year. */
    val all: List<String> = buildList {
        cores.forEach { core -> endings.forEach { ending -> add(core + ending) } }
    }

    /** Deterministic by local date, so it cannot change mid-day. */
    fun forDate(date: LocalDate): String {
        val index = (date.dayOfYear - 1).coerceIn(0, all.lastIndex)
        return all[index % all.size]
    }

    fun isGoalReached(steps: Int, goal: Int): Boolean = goal > 0 && steps >= goal

    /** 0..1 fill for the liquid word. */
    fun progress(steps: Int, goal: Int): Float =
        if (goal <= 0) 0f else (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
}
