package com.devil.calendar.domain.workout

import com.devil.calendar.data.local.workout.WeightUnit

/**
 * Pure training-volume maths. Kept free of Android types so it is unit tested
 * (Phase 3 builds PR detection on top of it).
 */
object Volume {

    private const val LB_PER_KG = 2.2046226218

    /** reps x weight, in the unit the weight was entered in. */
    fun setVolume(reps: Int, weight: Double): Double =
        reps.coerceAtLeast(0) * weight.coerceAtLeast(0.0)

    fun totalVolume(sets: List<Pair<Int, Double>>): Double =
        sets.sumOf { (reps, weight) -> setVolume(reps, weight) }

    /** Normalises a weight to kilograms so mixed-unit sessions compare correctly. */
    fun toKilograms(weight: Double, unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> weight
        WeightUnit.LB -> weight / LB_PER_KG
    }

    fun fromKilograms(kg: Double, unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> kg
        WeightUnit.LB -> kg * LB_PER_KG
    }

    /** "1,250 kg" - grouped, no decimals unless the value has them. */
    fun format(weight: Double, unit: WeightUnit): String {
        val rounded = if (weight % 1.0 == 0.0) {
            weight.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.1f", weight)
        }
        val grouped = rounded.reversed().chunked(3).joinToString(",").reversed()
        return "$grouped ${unit.name.lowercase()}"
    }
}
