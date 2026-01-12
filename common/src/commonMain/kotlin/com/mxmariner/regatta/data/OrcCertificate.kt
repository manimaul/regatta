import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

enum class Orc3Band(
    val label: String,
    val desc: String,
) {
    Low("Low", "9 kts or less (8.9 kts avg)"),
    Medium("Medium", "9 to 14 kts (13.5 kts avg)"),
    High("High", "14 kts or more (17 kts avg"),
}

enum class Orc5Band(
    val label: String,
    val desc: String,
) {
    Low("Low", "7 kts or less"),
    LowMedium("Low/Medium", "7 to 10 kts"),
    Medium("Medium", "10 to 13 kts"),
    MediumHigh("Medium/High", "13 to 17 kts"),
    High("High", "17 kts or more"),
}

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
    FiveBandAllPurpose("5-Band All Purpose")
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
    @SerialName("IssueDate") val issueDate: String = "",

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
    @SerialName("US_DREDUP_TOD") val singleNumberPredominantUpwindTod: Double = 0.0,

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
)

