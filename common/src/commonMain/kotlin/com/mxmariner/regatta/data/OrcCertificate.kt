package com.mxmariner.regatta.data

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
enum class PhrfBFactor(
    val label: String,
    val desc: String,
    val cf: Int,
) {
    Calm("Calm / Drifting 1 to 5 knots", "Very light air", 650),
    Light("Light 5 to 10 knots", "Light air or all windward work", 600),
    Avg("Average 10 to 15 knots", "Medium racing conditions", 550),
    Hvy("Heavy 15 to 20+ knots", "Heavy air or all off-the-wind sailing", 480);


    companion object {

        fun fromBFactor(cf: Int): PhrfBFactor {
            if (cf <= Hvy.cf) {
                return Hvy
            } else if (cf >= 650) {
                return Calm
            }
            return entries.toTypedArray().let { array ->
                var min: Int
                var max: Int
                var selected = Light
                array.forEachIndexed { index, factor ->
                    if (index == 0) {
                        min = factor.cf - (factor.cf - array[index + 1].cf) / 2
                        max = factor.cf
                    } else if (index == array.size - 1) {
                        min = factor.cf
                        max = factor.cf + (array[index - 1].cf - factor.cf) / 2
                    } else {
                        min = factor.cf - (factor.cf - array[index + 1].cf) / 2
                        max = factor.cf + (array[index - 1].cf - factor.cf) / 2
                    }
                    if (cf <= max && cf >= min) {
                        selected = factor
                    }
                }
                selected
            }
        }
    }
}

@Serializable
enum class Orc3Band(
    val label: String,
    val desc: String,
    val phrfBFactor: Int,
) {
    Low("Low", "9 kts or less", 600),
    Medium("Medium", "9 to 14 kts", 550),
    High("High", "14 kts or more", 480);


    companion object {
        fun fromPhrfBFactor(cf: Int): Orc3Band {
            return when {
                cf >= Low.phrfBFactor -> Low
                cf in Medium.phrfBFactor..Low.phrfBFactor -> Medium
                else -> High
            }
        }

    }
}

@Serializable
enum class Orc5Band(
    val label: String,
    val desc: String,
    val phrfBFactor: Int,
) {
    Low("Low", "7 kts or less", 650),
    LowMedium("Low/Medium", "7 to 10 kts", 600),
    Medium("Medium", "10 to 13 kts", 550),
    MediumHigh("Medium/High", "13 to 17 kts", 510),
    High("High", "17 kts or more", 480);

    companion object {
        fun fromPhrfBFactor(cf: Int): Orc5Band {
            return when {
                cf >= Low.phrfBFactor -> Low
                cf in LowMedium.phrfBFactor..Low.phrfBFactor -> LowMedium
                cf in Medium.phrfBFactor..LowMedium.phrfBFactor -> Medium
                cf in MediumHigh.phrfBFactor..Medium.phrfBFactor -> MediumHigh
                else -> High
            }
        }
    }
}

@Serializable
enum class OrcScoringOption(val label: String) {
    SingleNumberAllPurpose("Single Number All Purpose"),
    SingleNumberWindwardLeeward("Single Number Windward/Leeward"),
    TripleNumberAllPurpose("Triple Number All Purpose"),
    TripleNumberWindwardLeeward("Triple Number Windward/Leeward"),
    SingleNumberPredominantUpwind("Single Number Predominant Upwind"),
    SingleNumberPredominantReaching("Single Number Predominant Reaching"),
    SingleNumberPredominantDownwind("Single Number Predominant Downwind"),
    PredominantUpwind("Predominant Upwind "),
    PredominantDownwind("Predominant Downwind"),
    PredominantReaching("Predominant Reaching"),
    FiveBandWindwardLeeward("5-Band Windward/Leeward"),
    WindwardLeeward60_40("Windward/Leeward 60-40"),
    FiveBandAllPurpose("5-Band All Purpose");

    val uses3Band: Boolean
        get() = when (this) {
            TripleNumberAllPurpose,
            TripleNumberWindwardLeeward -> true

            else -> false
        }

    val uses5Band: Boolean
        get() = when (this) {
            PredominantUpwind,
            PredominantDownwind,
            PredominantReaching,
            FiveBandWindwardLeeward,
            WindwardLeeward60_40,
            FiveBandAllPurpose -> true

            else -> false
        }

    fun orcBandLabel(orc5Band: Orc5Band, orc3Band: Orc3Band): String? {
        return when (this) {
            SingleNumberAllPurpose,
            SingleNumberWindwardLeeward,
            SingleNumberPredominantUpwind,
            SingleNumberPredominantReaching,
            SingleNumberPredominantDownwind -> null

            TripleNumberAllPurpose,
            TripleNumberWindwardLeeward -> "${orc3Band.label} - ${orc3Band.desc}"

            PredominantUpwind,
            PredominantDownwind,
            PredominantReaching,
            FiveBandWindwardLeeward,
            WindwardLeeward60_40,
            FiveBandAllPurpose -> "${orc5Band.label} - ${orc5Band.desc}"
        }
    }

    companion object {

    }

}

@Serializable
data class OrcResponse(
    @SerialName("rms") val rms: List<OrcCertificate> = emptyList(),
)

fun List<OrcCertificate>.findPreferred(): OrcCertificate? {
    return sortedWith { lhs, rhs ->
        if (lhs.cType != rhs.cType) {
            if (lhs.cType.equals("CLUB", false)) {
                -1
            } else if (rhs.cType.equals("CLUB", false)) {
                1
            } else {
                0
            }
        } else {
            lhs.issueDate.compareTo(rhs.issueDate)
        }
    }.firstOrNull()
}

@Serializable
data class OrcCertificate(
    @SerialName("NatAuth") val natAuth: String = "",
    @SerialName("BIN") val bin: String = "",
    @SerialName("CertNo") val certNo: String = "",
    @SerialName("RefNo") val refNo: String = "",
    @SerialName("SailNo") val sailNo: String = "",
    @SerialName("YachtName") val yachtName: String = "",
    @SerialName("Class") val yachtClass: String = "",
    @SerialName("Builder") val builder: String = "",
    @SerialName("Designer") val designer: String = "",
    @SerialName("C_Type") val cType: String = "",
    @SerialName("Division") val division: String = "",
    @SerialName("IssueDate") val issueDate: Instant = Instant.DISTANT_FUTURE,

    // Single Number All Purpose
    @SerialName("APHD") val allPurposeTod: Double = 0.0,
    @SerialName("APHT") val allPurposeTot: Double = 0.0,

    // Single Number Windward/Leeward
    @SerialName("ILCWA") val wlSingleNumberTod: Double = 0.0,
    @SerialName("TMF_Inshore") val wlSingleNumberTot: Double = 0.0,

    // Triple Number All Purpose (Low, Med, High)
    @SerialName("TND_Offshore_Low") val tripleNumberAllPurposeLowTod: Double = 0.0,
    @SerialName("TN_Offshore_Low") val tripleNumberAllPurposeLowTot: Double = 0.0,
    @SerialName("TND_Offshore_Medium") val tripleNumberAllPurposeMedTod: Double = 0.0,
    @SerialName("TN_Offshore_Medium") val tripleNumberAllPurposeMedTot: Double = 0.0,
    @SerialName("TND_Offshore_High") val tripleNumberAllPurposeHiTod: Double = 0.0,
    @SerialName("TN_Offshore_High") val tripleNumberAllPurposeHiTot: Double = 0.0,

    // Triple Number Windward/Leeward (Low, Med, High)
    @SerialName("US_TN_Inshore_Low") val tripleNumberWlLowTot: Double = 0.0,
    @SerialName("TN_Inshore_Medium") val tripleNumberWlMedTot: Double = 0.0,
    @SerialName("TN_Inshore_High") val tripleNumberWlHiTot: Double = 0.0,


    // Single Number Predominant Upwind
    @SerialName("US_PREDUP_TOT") val singleNumberPredominantUpwindTot: Double = 0.0,
    @SerialName("US_PREDUP_TOD") val singleNumberPredominantUpwindTod: Double = 0.0,

    // Single Number Predominant Reaching
    @SerialName("US_PREDRC_TOT") val singleNumberPredominantReachingTot: Double = 0.0,
    @SerialName("US_PREDRC_TOD") val singleNumberPredominantReachingTod: Double = 0.0,

    // Single Number Predominant Downwind
    @SerialName("US_PREDDN_TOT") val singleNumberPredominantDownwindTot: Double = 0.0,
    @SerialName("US_PREDDN_TOD") val singleNumberPredominantDownwindTod: Double = 0.0,

    // Predominant Upwind (Low, Low/Med, Medium, Med/High, High)
    @SerialName("US_PREDUP_L_TOT") val predominantUpwindLowTot: Double = 0.0,
    @SerialName("US_PREDUP_L_TOD") val predominantUpwindLowTod: Double = 0.0,
    @SerialName("US_PREDUP_LM_TOT") val predominantUpwindLowMedTot: Double = 0.0,
    @SerialName("US_PREDUP_LM_TOD") val predominantUpwindLowMedTod: Double = 0.0,
    @SerialName("US_PREDUP_M_TOT") val predominantUpwindMedTot: Double = 0.0,
    @SerialName("US_PREDUP_M_TOD") val predominantUpwindMedTod: Double = 0.0,
    @SerialName("US_PREDUP_MH_TOT") val predominantUpwindMedHiTot: Double = 0.0,
    @SerialName("US_PREDUP_MH_TOD") val predominantUpwindMedHiTod: Double = 0.0,
    @SerialName("US_PREDUP_H_TOT") val predominantUpwindHiTot: Double = 0.0,
    @SerialName("US_PREDUP_H_TOD") val predominantUpwindHiTod: Double = 0.0,

    // Predominant Downwind (Low, Low/Med, Medium, Med/High, High)
    @SerialName("US_PREDDN_L_TOT") val predominantDownwindLowTot: Double = 0.0,
    @SerialName("US_PREDDN_L_TOD") val predominantDownwindLowTod: Double = 0.0,
    @SerialName("US_PREDDN_LM_TOT") val predominantDownwindLowMedTot: Double = 0.0,
    @SerialName("US_PREDDN_LM_TOD") val predominantDownwindLowMedTod: Double = 0.0,
    @SerialName("US_PREDDN_M_TOT") val predominantDownwindMedTot: Double = 0.0,
    @SerialName("US_PREDDN_M_TOD") val predominantDownwindMedTod: Double = 0.0,
    @SerialName("US_PREDDN_MH_TOT") val predominantDownwindMedHiTot: Double = 0.0,
    @SerialName("US_PREDDN_MH_TOD") val predominantDownwindMedHiTod: Double = 0.0,
    @SerialName("US_PREDDN_H_TOT") val predominantDownwindHiTot: Double = 0.0,
    @SerialName("US_PREDDN_H_TOD") val predominantDownwindHiTod: Double = 0.0,

    // Predominant Reaching (Low, Low/Med, Medium, Med/High, High)
    @SerialName("US_PREDRC_L_TOT") val predominantReachingLowTot: Double = 0.0,
    @SerialName("US_PREDRC_L_TOD") val predominantReachingLowTod: Double = 0.0,
    @SerialName("US_PREDRC_LM_TOT") val predominantReachingLowMedTot: Double = 0.0,
    @SerialName("US_PREDRC_LM_TOD") val predominantReachingLowMedTod: Double = 0.0,
    @SerialName("US_PREDRC_M_TOT") val predominantReachingMedTot: Double = 0.0,
    @SerialName("US_PREDRC_M_TOD") val predominantReachingMedTod: Double = 0.0,
    @SerialName("US_PREDRC_MH_TOT") val predominantReachingMedHiTot: Double = 0.0,
    @SerialName("US_PREDRC_MH_TOD") val predominantReachingMedHiTod: Double = 0.0,
    @SerialName("US_PREDRC_H_TOT") val predominantReachingHiTot: Double = 0.0,
    @SerialName("US_PREDRC_H_TOD") val predominantReachingHiTod: Double = 0.0,

    // 5-Band Windward/Leeward (Low, Low/Med, Medium, Med/High, High)
    @SerialName("US_5B_L_TOT") val fiveBandWlLowTot: Double = 0.0,
    @SerialName("US_5B_L_TOD") val fiveBandWlLowTod: Double = 0.0,
    @SerialName("US_5B_LM_TOT") val fiveBandWlLowMedTot: Double = 0.0,
    @SerialName("US_5B_LM_TOD") val fiveBandWlLowMedTod: Double = 0.0,
    @SerialName("US_5B_M_TOT") val fiveBandWlMedTot: Double = 0.0,
    @SerialName("US_5B_M_TOD") val fiveBandWlMedTod: Double = 0.0,
    @SerialName("US_5B_MH_TOT") val fiveBandWlMedHiTot: Double = 0.0,
    @SerialName("US_5B_MH_TOD") val fiveBandWlMedHiTod: Double = 0.0,
    @SerialName("US_5B_H_TOT") val fiveBandWlHiTot: Double = 0.0,
    @SerialName("US_5B_H_TOD") val fiveBandWlHiTod: Double = 0.0,

    // Windward/Leeward 60-40 (Low, Low/Med, Medium, Med/High, High)
    @SerialName("US_WL6040_L_TOD") val usWl6040LTod: Double = 0.0,
    @SerialName("US_WL6040_L_TOT") val usWl6040LTot: Double = 0.0,
    @SerialName("US_WL6040_LM_TOD") val usWl6040LmTod: Double = 0.0,
    @SerialName("US_WL6040_LM_TOT") val usWl6040LmTot: Double = 0.0,
    @SerialName("US_WL6040_M_TOD") val usWl6040MTod: Double = 0.0,
    @SerialName("US_WL6040_M_TOT") val usWl6040MTot: Double = 0.0,
    @SerialName("US_WL6040_MH_TOD") val usWl6040MhTod: Double = 0.0,
    @SerialName("US_WL6040_MH_TOT") val usWl6040MhTot: Double = 0.0,
    @SerialName("US_WL6040_H_TOD") val usWl6040HTod: Double = 0.0,
    @SerialName("US_WL6040_H_TOT") val usWl6040HTot: Double = 0.0,

    // 5-Band All Purpose (Low, Low/Med, Medium, Med/High, High)
    @SerialName("US_AP_L_TOT") val fiveBandAllPurposeLowTot: Double = 0.0,
    @SerialName("US_AP_L_TOD") val fiveBandAllPurposeLowTod: Double = 0.0,
    @SerialName("US_AP_LM_TOT") val fiveBandAllPurposeLowMedTot: Double = 0.0,
    @SerialName("US_AP_LM_TOD") val fiveBandAllPurposeLowMedTod: Double = 0.0,
    @SerialName("US_AP_M_TOT") val fiveBandAllPurposeMedTot: Double = 0.0,
    @SerialName("US_AP_M_TOD") val fiveBandAllPurposeMedTod: Double = 0.0,
    @SerialName("US_AP_MH_TOT") val fiveBandAllPurposeMedHiTot: Double = 0.0,
    @SerialName("US_AP_MH_TOD") val fiveBandAllPurposeMedHiTod: Double = 0.0,
    @SerialName("US_AP_H_TOT") val fiveBandAllPurposeHiTot: Double = 0.0,
    @SerialName("US_AP_H_TOD") val fiveBandAllPurposeHiTod: Double = 0.0,
) {

    fun label(): String {
        return "$yachtName $refNo $cType $division"
    }

    fun virtualPhrf(): Int {
        /*
        Coefficient A (The Numerator)
        Purpose: A is a constant used to scale the resulting TCF so that a "middle-of-the-fleet" boat has a multiplier near 1.000.
        Impact: Changing the A coefficient does not change the finishing order of the race; it only affects the margins between boats.
        Standard Value: The most commonly used value for A is 650.
         */
        val a = 500.0

        /*
        Coefficient B (The Denominator)
        480: Heavy air (15+ knots) or all off-the-wind sailing.
        550: Average/Moderate conditions (10–15 knots).
        600: Light air (5–10 knots) or all windward work.
        650: Very light air/Calm (1–5 knots).
         */
        val b = 550.0

        /*
        Low("Low", "7 kts or less"),
        LowMedium("Low/Medium", "7 to 10 kts"),
        Medium("Medium", "10 to 13 kts"),
        MediumHigh("Medium/High", "13 to 17 kts"),
        High("High", "17 kts or more");
         */
        val tcf = fiveBandWlLowMedTot

        /*
        PHRF formula "tcf = a / ( b + phrf)"
         */
        val phrf = (a / tcf) - b

        return phrf.roundToInt()
    }
}

