import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class OrcCertificate(
    @SerialName("NatAuth") val natAuth: String,
    @SerialName("BIN") val bin: String,
    @SerialName("CertNo") val certNo: String,
    @SerialName("RefNo") val refNo: String,
    @SerialName("SailNo") val sailNo: String,
    @SerialName("YachtName") val yachtName: String,
    @SerialName("Class") val yachtClass: String,
    @SerialName("Builder") val builder: String,
    @SerialName("Designer") val designer: String,
    @SerialName("C_Type") val cType: String,
    @SerialName("Division") val division: String,
    @SerialName("IssueDate") val issueDate: String,
    @SerialName("ILCWA") val wlSingleNumberTod: Double,
    @SerialName("TMF_Inshore") val wlSingleNumberTot: Double,
    @SerialName("APHD") val allPurposeTod: Double,
    @SerialName("APHT") val allPurposeTot: Double,
    @SerialName("US_WL6040_L_TOD") val usWl6040LTod: Double,
    @SerialName("US_WL6040_L_TOT") val usWl6040LTot: Double,
    @SerialName("US_WL6040_LM_TOD") val usWl6040LmTod: Double,
    @SerialName("US_WL6040_LM_TOT") val usWl6040LmTot: Double,
    @SerialName("US_WL6040_M_TOD") val usWl6040MTod: Double,
    @SerialName("US_WL6040_M_TOT") val usWl6040MTot: Double,
    @SerialName("US_WL6040_MH_TOD") val usWl6040MhTod: Double,
    @SerialName("US_WL6040_MH_TOT") val usWl6040MhTot: Double,
    @SerialName("US_WL6040_H_TOD") val usWl6040HTod: Double,
    @SerialName("US_WL6040_H_TOT") val usWl6040HTot: Double,
)

