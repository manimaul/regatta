import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class OrcTest {

    @Test
    fun testEncode() {
        val withUnknownKeys = Json { ignoreUnknownKeys = true }
        val response = withUnknownKeys.decodeFromString(OrcResponse.serializer(), certResponseJson)
        assertEquals(response.rms.first().refNo, "04560003WR9")
    }
}

const val certResponseJson = """
{
  "rms": [
    {
      "NatAuth": "USA",
      "BIN": "US59793",
      "CertNo": "US59793",
      "RefNo": "04560003WR9",
      "SailNo": "USA 59793",
      "YachtName": "WAYMAKER",
      "Class": "TARTAN 37 DK",
      "Builder": "TARTAN MARINE",
      "Designer": "S&S",
      "Address3": "1PXTUHS",
      "C_Type": "CLUB",
      "Family": "ORC",
      "Division": "C",
      "IssueDate": "2025-08-18T12:19:45.802Z",
      "Dspl_Sailing": 9001,
      "WSS": 29.23,
      "Area_Main": 29.18,
      "Area_Jib": 51,
      "Area_Sym": null,
      "Area_Asym": 86.13,
      "Age_Year": 1976,
      "CrewWT": 326,
      "LOA": 11.308,
      "IMSL": 9.186,
      "Draft": 2.082,
      "MB": 3.554,
      "Dspl_Measurement": 8447,
      "Dynamic_Allowance": 0.64,
      "CDL": 8.231,
      "metric": true,
      "ILCWA": 746.5,
      "TMF_Inshore": 0.8037,
      "APHD": 604.7,
      "APHT": 0.9923,
      "OSN": 657.7,
      "TMF_Offshore": 0.9122,
      "GPH": 677.3,
      "TND_Offshore_Low": 771.8,
      "TN_Offshore_Low": 0.7774,
      "TND_Offshore_Medium": 600.5,
      "TN_Offshore_Medium": 0.9992,
      "TND_Offshore_High": 542.1,
      "TN_Offshore_High": 1.1068,
      "TND_Inshore_Low": 986.8,
      "TN_Inshore_Low": 0.608,
      "TND_Inshore_Medium": 741.6,
      "TN_Inshore_Medium": 0.8091,
      "TND_Inshore_High": 652.7,
      "TN_Inshore_High": 0.9193,
      "Pred_Up_TOD": 651.9,
      "Pred_Up_TOT": 0.9204,
      "Pred_Down_TOD": 616.5,
      "Pred_Down_TOT": 0.9732,
      "US_TND_Inshore_Low": 1029.9,
      "US_TN_Inshore_Low": 0.5826,
      "US_PREDUP_TOD": 676.8,
      "US_PREDUP_TOT": 0.8865,
      "US_PREDRC_TOD": 602,
      "US_PREDRC_TOT": 0.9967,
      "US_PREDDN_TOD": 643.6,
      "US_PREDDN_TOT": 0.9323,
      "US_PREDUP_L_TOD": 920.4,
      "US_PREDUP_L_TOT": 0.6519,
      "US_PREDUP_LM_TOD": 711.8,
      "US_PREDUP_LM_TOT": 0.843,
      "US_PREDUP_M_TOD": 631.4,
      "US_PREDUP_M_TOT": 0.9503,
      "US_PREDUP_MH_TOD": 592.9,
      "US_PREDUP_MH_TOT": 1.012,
      "US_PREDUP_H_TOD": 578.4,
      "US_PREDUP_H_TOT": 1.0374,
      "US_PREDDN_L_TOD": 933.4,
      "US_PREDDN_L_TOT": 0.6428,
      "US_PREDDN_LM_TOD": 694.7,
      "US_PREDDN_LM_TOT": 0.8637,
      "US_PREDDN_M_TOD": 594.5,
      "US_PREDDN_M_TOT": 1.0092,
      "US_PREDDN_MH_TOD": 536.5,
      "US_PREDDN_MH_TOT": 1.1183,
      "US_PREDDN_H_TOD": 505,
      "US_PREDDN_H_TOT": 1.1882,
      "US_PREDRC_L_TOD": 830,
      "US_PREDRC_L_TOT": 0.7229,
      "US_PREDRC_LM_TOD": 638.2,
      "US_PREDRC_LM_TOT": 0.9401,
      "US_PREDRC_M_TOD": 561.7,
      "US_PREDRC_M_TOT": 1.0682,
      "US_PREDRC_MH_TOD": 521.3,
      "US_PREDRC_MH_TOT": 1.151,
      "US_PREDRC_H_TOD": 500.2,
      "US_PREDRC_H_TOT": 1.1995,
      "US_CHIMAC_UP_TOT": 0.8603,
      "US_CHIMAC_AP_TOT": 0.8827,
      "US_CHIMAC_DN_TOT": 0.8999,
      "US_BAYMAC_CV_TOT": 0.6521,
      "US_BAYMAC_SH_TOT": 0.6569,
      "US_HARVMOON_TOD": 528.4,
      "US_HARVMOON_TOT": 1.1356,
      "US_VICMAUI_TOT": 1.0168,
      "US_5B_L_TOD": 1147.1,
      "US_5B_L_TOT": 0.5231,
      "US_5B_LM_TOD": 851.6,
      "US_5B_LM_TOT": 0.7046,
      "US_5B_M_TOD": 729.5,
      "US_5B_M_TOT": 0.8225,
      "US_5B_MH_TOD": 661.8,
      "US_5B_MH_TOT": 0.9066,
      "US_5B_H_TOD": 631.4,
      "US_5B_H_TOT": 0.9502,
      "US_AP_L_TOD": 852.6,
      "US_AP_L_TOT": 0.7037,
      "US_AP_LM_TOD": 676.4,
      "US_AP_LM_TOT": 0.887,
      "US_AP_M_TOD": 592.5,
      "US_AP_M_TOT": 1.0126,
      "US_AP_MH_TOD": 548,
      "US_AP_MH_TOT": 1.0949,
      "US_AP_H_TOD": 527.9,
      "US_AP_H_TOT": 1.1366,
      "US_TNAP_L_TOD": 771.8,
      "US_TNAP_L_TOT": 0.7774,
      "US_TNAP_M_TOD": 600.5,
      "US_TNAP_M_TOT": 0.9991,
      "US_TNAP_H_TOD": 542.1,
      "US_TNAP_H_TOT": 1.1068,
      "US_SFBay_L_TOD": 991.1,
      "US_SFBay_L_TOT": 0.6054,
      "US_SFBay_LM_TOD": 813.8,
      "US_SFBay_LM_TOT": 0.7373,
      "US_SFBay_M_TOD": 702.5,
      "US_SFBay_M_TOT": 0.8541,
      "US_SFBay_MH_TOD": 643.1,
      "US_SFBay_MH_TOT": 0.933,
      "US_SFBay_H_TOD": 617.4,
      "US_SFBay_H_TOT": 0.9717,
      "US_WL6040_L_TOD": 1148.7,
      "US_WL6040_L_TOT": 0.5223,
      "US_WL6040_LM_TOD": 859.7,
      "US_WL6040_LM_TOT": 0.6979,
      "US_WL6040_M_TOD": 742.6,
      "US_WL6040_M_TOT": 0.808,
      "US_WL6040_MH_TOD": 680.2,
      "US_WL6040_MH_TOT": 0.882,
      "US_WL6040_H_TOD": 654.3,
      "US_WL6040_H_TOT": 0.917,
      "KR_PREDR_TOD": 590,
      "RSA_CD_INS_TOD": 620.4,
      "RSA_CD_INS_TOT": 0.9671,
      "RSA_CD_OFF_TOD": 519.4,
      "RSA_CD_OFF_TOT": 1.1552,
      "BRA_ALL_UP_TOT": 0.714,
      "BRA_ALL_DN_TOT": 0.8425,
      "BRA_7030_TOT": 0.7482,
      "BRA_3070_TOT": 0.7993,
      "FIN_FinRating_TOD": 657.7,
      "FIN_FinRating_TOT": 0.9122,
      "FIN_FinRating_L_TOD": 1127.9,
      "FIN_FinRating_L_TOT": 0.8866,
      "FIN_FinRating_H_TOD": 532.1,
      "FIN_FinRating_H_TOT": 0.9396,
      "Allowances": {
        "WindSpeeds": [
          4,
          6,
          8,
          10,
          12,
          14,
          16,
          20,
          24
        ],
        "WindAngles": [
          52,
          60,
          75,
          90,
          110,
          120,
          135,
          150
        ],
        "R52": [
          967.9,
          728,
          623.2,
          569.5,
          545,
          533.2,
          527.1,
          523.6,
          523.8
        ],
        "R60": [
          920.5,
          696.6,
          601,
          553.5,
          531.5,
          519.7,
          512.8,
          508.4,
          506.7
        ],
        "R75": [
          896.4,
          679.1,
          586.9,
          541.9,
          519.7,
          506.6,
          497.5,
          486.3,
          482.4
        ],
        "R90": [
          919.3,
          693.3,
          593.8,
          538.2,
          512.6,
          500.6,
          490.5,
          474.3,
          462.7
        ],
        "R110": [
          956.2,
          696.9,
          579,
          525.7,
          499.6,
          482,
          468.3,
          451.3,
          440.5
        ],
        "R120": [
          984.5,
          716.5,
          592.8,
          532.2,
          502.7,
          482.4,
          465.9,
          440,
          422.6
        ],
        "R135": [
          1106.4,
          794.6,
          644.7,
          561.1,
          519.3,
          494.5,
          475,
          443.1,
          415.9
        ],
        "R150": [
          1340.2,
          934.3,
          737.1,
          629.1,
          562.5,
          525.4,
          502.7,
          469.3,
          441.9
        ],
        "Beat": [
          1502.3,
          1100.7,
          916.9,
          826.4,
          782.9,
          762.3,
          750.7,
          742.8,
          752.8
        ],
        "Run": [
          1547.5,
          1078.9,
          851.1,
          726.5,
          649.3,
          600.8,
          565.1,
          513.7,
          481.4
        ],
        "BeatAngle": [
          43,
          43,
          40.7,
          39.9,
          39.9,
          39.5,
          39.3,
          39.3,
          40
        ],
        "GybeAngle": [
          145.1,
          145.1,
          147.9,
          148.5,
          150.5,
          155.3,
          160.1,
          174.8,
          175.2
        ],
        "DW180": [
          2040.9,
          1362.6,
          1026.4,
          834.8,
          716.5,
          636.7,
          579,
          516.1,
          483.4
        ],
        "DW165": [
          1818.4,
          1220.9,
          926.5,
          763.4,
          663.9,
          595.2,
          549.6,
          502.3,
          472.9
        ],
        "DW150": [
          1360,
          942,
          739.2,
          630.4,
          563.1,
          526,
          503.4,
          470,
          442.6
        ],
        "WL": [
          1523.4,
          1089.8,
          884,
          776.4,
          716.1,
          681.5,
          657.9,
          628.2,
          617.1
        ],
        "CR": [
          1160.3,
          845.1,
          698.6,
          622.9,
          583.3,
          560.8,
          545.4,
          526.3,
          516.4
        ],
        "OC": [
          1614.4,
          1088.7,
          833.5,
          700.9,
          623,
          582.4,
          551.4,
          504.5,
          492.5
        ]
      }
    }
  ],
  "Countries": [
    {
      "CountryId": "ORC",
      "Name": "Offshore Racing Congress"
    },
    {
      "CountryId": "AUS",
      "Name": "Australia"
    },
    {
      "CountryId": "AUT",
      "Name": "Austria"
    },
    {
      "CountryId": "BRA",
      "Name": "Brazil"
    },
    {
      "CountryId": "BUL",
      "Name": "Bulgaria"
    },
    {
      "CountryId": "CAN",
      "Name": "Canada"
    },
    {
      "CountryId": "CYP",
      "Name": "Cyprus"
    },
    {
      "CountryId": "DEN",
      "Name": "Denmark"
    },
    {
      "CountryId": "ESP",
      "Name": "Spain"
    },
    {
      "CountryId": "EST",
      "Name": "Estonia"
    },
    {
      "CountryId": "FIN",
      "Name": "Finland"
    },
    {
      "CountryId": "FRA",
      "Name": "France"
    },
    {
      "CountryId": "GER",
      "Name": "Germany"
    },
    {
      "CountryId": "GRE",
      "Name": "Greece"
    },
    {
      "CountryId": "HUN",
      "Name": "Hungary"
    },
    {
      "CountryId": "ISR",
      "Name": "Israel"
    },
    {
      "CountryId": "ITA",
      "Name": "Italy"
    },
    {
      "CountryId": "JPN",
      "Name": "Japan"
    },
    {
      "CountryId": "KOR",
      "Name": "Korea"
    },
    {
      "CountryId": "LTU",
      "Name": "Lithuania"
    },
    {
      "CountryId": "NED",
      "Name": "Netherlands"
    },
    {
      "CountryId": "NOR",
      "Name": "Norway"
    },
    {
      "CountryId": "POR",
      "Name": "Portugal"
    },
    {
      "CountryId": "RUS",
      "Name": "Russia"
    },
    {
      "CountryId": "RSA",
      "Name": "South Africa"
    },
    {
      "CountryId": "SLO",
      "Name": "Slovenia"
    },
    {
      "CountryId": "SUI",
      "Name": "Switzerland"
    },
    {
      "CountryId": "SWE",
      "Name": "Sweden"
    },
    {
      "CountryId": "UKR",
      "Name": "Ukraine"
    },
    {
      "CountryId": "USA",
      "Name": "United States of America"
    }
  ],
  "ScoringOptions": [
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "Mu",
        "NS",
        "LI"
      ],
      "CountryId": "ORC",
      "Kind": "TOD",
      "Fieldname": "APHD",
      "Name": "All Purpose"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "Mu",
        "NS",
        "LI"
      ],
      "CountryId": "ORC",
      "Kind": "TOT",
      "Fieldname": "APHT",
      "Name": "All Purpose"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "Mu",
        "NS",
        "LI"
      ],
      "CountryId": "ORC",
      "Kind": "PCS",
      "Fieldname": "CR",
      "Name": "All Purpose"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "Mu",
        "NS",
        "LI"
      ],
      "CountryId": "ORC",
      "Kind": "TOD",
      "Fieldname": "ILCWA",
      "Name": "Windward/Leeward"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "Mu",
        "NS",
        "LI"
      ],
      "CountryId": "ORC",
      "Kind": "TOT",
      "Fieldname": "TMF_Inshore",
      "Name": "Windward/Leeward"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "Mu",
        "NS",
        "LI"
      ],
      "CountryId": "ORC",
      "Kind": "PCS",
      "Fieldname": "WL",
      "Name": "Windward/Leeward"
    },
    {
      "Families": [
        "Mu"
      ],
      "CountryId": "ORC",
      "Kind": "TOD",
      "Fieldname": "MHRD",
      "Name": "Multihull Rating"
    },
    {
      "Families": [
        "Mu"
      ],
      "CountryId": "ORC",
      "Kind": "TOT",
      "Fieldname": "MHRT",
      "Name": "Multihull Rating"
    },
    {
      "Families": [
        "SY"
      ],
      "CountryId": "ORC",
      "Kind": "TOD",
      "Fieldname": "SY_DL",
      "Name": "Light"
    },
    {
      "Families": [
        "SY"
      ],
      "CountryId": "ORC",
      "Kind": "TOT",
      "Fieldname": "SY_TL",
      "Name": "Light"
    },
    {
      "Families": [
        "SY"
      ],
      "CountryId": "ORC",
      "Kind": "TOD",
      "Fieldname": "SY_DLM",
      "Name": "Light-Moderate"
    },
    {
      "Families": [
        "SY"
      ],
      "CountryId": "ORC",
      "Kind": "TOT",
      "Fieldname": "SY_TLM",
      "Name": "Light-Moderate"
    },
    {
      "Families": [
        "SY"
      ],
      "CountryId": "ORC",
      "Kind": "TOD",
      "Fieldname": "SY_DM",
      "Name": "Moderate"
    },
    {
      "Families": [
        "SY"
      ],
      "CountryId": "ORC",
      "Kind": "TOT",
      "Fieldname": "SY_TM",
      "Name": "Moderate"
    },
    {
      "Families": [
        "SY"
      ],
      "CountryId": "ORC",
      "Kind": "TOD",
      "Fieldname": "SY_DMS",
      "Name": "Moderate-Strong"
    },
    {
      "Families": [
        "SY"
      ],
      "CountryId": "ORC",
      "Kind": "TOT",
      "Fieldname": "SY_TMS",
      "Name": "Moderate-Strong"
    },
    {
      "Families": [
        "SY"
      ],
      "CountryId": "ORC",
      "Kind": "TOD",
      "Fieldname": "SY_DS",
      "Name": "Strong"
    },
    {
      "Families": [
        "SY"
      ],
      "CountryId": "ORC",
      "Kind": "TOT",
      "Fieldname": "SY_TS",
      "Name": "Strong"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUS",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUS",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUS",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUS",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUS",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUS",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUS",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUS",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUS",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUS",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUS",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUS",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUT",
      "Kind": "TOD",
      "Fieldname": "OSN",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUT",
      "Kind": "TOT",
      "Fieldname": "TMF_Offshore",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUT",
      "Kind": "PCS",
      "Fieldname": "OC",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUT",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUT",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUT",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUT",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUT",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUT",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUT",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUT",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUT",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUT",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUT",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "AUT",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BRA",
      "Kind": "TOT",
      "Fieldname": "BRA_ALL_UP_TOT",
      "Name": "Upwind \"bolina\""
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BRA",
      "Kind": "TOT",
      "Fieldname": "BRA_ALL_DN_TOT",
      "Name": "Downwind \"popa\""
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BRA",
      "Kind": "TOT",
      "Fieldname": "BRA_7030_TOT",
      "Name": "70% up, 30% down \"Ilhabela\""
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BRA",
      "Kind": "TOT",
      "Fieldname": "BRA_3070_TOT",
      "Name": "30% up, 70% down \"Ilhabela-inverso\""
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BUL",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BUL",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BUL",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BUL",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BUL",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BUL",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BUL",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BUL",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BUL",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BUL",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BUL",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BUL",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BUL",
      "Kind": "TOD",
      "Fieldname": "Pred_Up_TOD",
      "Name": "Predominantly Upwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BUL",
      "Kind": "TOT",
      "Fieldname": "Pred_Up_TOT",
      "Name": "Predominantly Upwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BUL",
      "Kind": "TOD",
      "Fieldname": "Pred_Down_TOD",
      "Name": "Predominantly Downwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "BUL",
      "Kind": "TOT",
      "Fieldname": "Pred_Down_TOT",
      "Name": "Predominantly Downwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_TNAP_L_TOD",
      "Name": "Triple Number AP Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_TNAP_L_TOT",
      "Name": "Triple Number AP Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_TNAP_M_TOD",
      "Name": "Triple Number AP Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_TNAP_M_TOT",
      "Name": "Triple Number AP Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_TNAP_H_TOD",
      "Name": "Triple Number AP High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_TNAP_H_TOT",
      "Name": "Triple Number AP High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDUP_TOD",
      "Name": "Single Number Predominant Upwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDUP_TOT",
      "Name": "Single Number Predominant Upwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDRC_TOD",
      "Name": "Single Number Predominant Reaching"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDRC_TOT",
      "Name": "Single Number Predominant Reaching"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDDN_TOD",
      "Name": "Single Number Predominant Downwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDDN_TOT",
      "Name": "Single Number Predominant Downwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDUP_L_TOD",
      "Name": "Predominant Upwind - Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDUP_L_TOT",
      "Name": "Predominant Upwind - Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDUP_LM_TOD",
      "Name": "Predominant Upwind - Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDUP_LM_TOT",
      "Name": "Predominant Upwind - Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDUP_M_TOD",
      "Name": "Predominant Upwind - Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDUP_M_TOT",
      "Name": "Predominant Upwind - Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDUP_MH_TOD",
      "Name": "Predominant Upwind - Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDUP_MH_TOT",
      "Name": "Predominant Upwind - Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDUP_H_TOD",
      "Name": "Predominant Upwind - High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDUP_H_TOT",
      "Name": "Predominant Upwind - High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDRC_L_TOD",
      "Name": "Predominant Reaching Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDRC_L_TOT",
      "Name": "Predominant Reaching Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDRC_LM_TOD",
      "Name": "Predominant Reaching Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDRC_LM_TOT",
      "Name": "Predominant Reaching Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDRC_M_TOD",
      "Name": "Predominant Reaching Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDRC_M_TOT",
      "Name": "Predominant Reaching Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDRC_MH_TOD",
      "Name": "Predominant Reaching Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDRC_MH_TOT",
      "Name": "Predominant Reaching Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDRC_H_TOD",
      "Name": "Predominant Reaching High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDRC_H_TOT",
      "Name": "Predominant Reaching High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDDN_L_TOD",
      "Name": "Predominant Downwind Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDDN_L_TOT",
      "Name": "Predominant Downwind Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDDN_LM_TOD",
      "Name": "Predominant Downwind Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDDN_LM_TOT",
      "Name": "Predominant Downwind Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDDN_M_TOD",
      "Name": "Predominant Downwind Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDDN_M_TOT",
      "Name": "Predominant Downwind Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDDN_MH_TOD",
      "Name": "Predominant Downwind Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDDN_MH_TOT",
      "Name": "Predominant Downwind Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_PREDDN_H_TOD",
      "Name": "Predominant Downwind High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_PREDDN_H_TOT",
      "Name": "Predominant Downwind High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_CHIMAC_UP_TOT",
      "Name": "Chicago-Mac Upwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_CHIMAC_AP_TOT",
      "Name": "Chicago-Mac All Purpose"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_CHIMAC_DN_TOT",
      "Name": "Chicago-Mac Downwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_BAYMAC_CV_TOT",
      "Name": "Bayview-Mac Cove Island"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_BAYMAC_SH_TOT",
      "Name": "Bayview-Mac Shore"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_HARVMOON_TOD",
      "Name": "Harvest Moon Regatta"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_HARVMOON_TOT",
      "Name": "Harvest Moon Regatta"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_VICMAUI_TOT",
      "Name": "Victoria-Maui"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_SFBay_L_TOD",
      "Name": "5-Band SF Bay Tour - Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_SFBay_L_TOT",
      "Name": "5-Band SF Bay Tour - Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_SFBay_LM_TOD",
      "Name": "5-Band SF Bay Tour - Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_SFBay_LM_TOT",
      "Name": "5-Band SF Bay Tour - Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_SFBay_M_TOD",
      "Name": "5-Band SF Bay Tour - Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_SFBay_M_TOT",
      "Name": "5-Band SF Bay Tour - Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_SFBay_MH_TOD",
      "Name": "5-Band SF Bay Tour - Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_SFBay_MH_TOT",
      "Name": "5-Band SF Bay Tour - Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_SFBay_H_TOD",
      "Name": "5-Band SF Bay Tour - High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_SFBay_H_TOT",
      "Name": "5-Band SF Bay Tour - High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_AP_L_TOD",
      "Name": "5-Band AP Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_AP_L_TOT",
      "Name": "5-Band AP Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_AP_LM_TOD",
      "Name": "5-Band AP Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_AP_LM_TOT",
      "Name": "5-Band AP Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_AP_M_TOD",
      "Name": "5-Band AP Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_AP_M_TOT",
      "Name": "5-Band AP Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_AP_MH_TOD",
      "Name": "5-Band AP Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_AP_MH_TOT",
      "Name": "5-Band AP Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_AP_H_TOD",
      "Name": "5-Band AP High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_AP_H_TOT",
      "Name": "5-Band AP High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_5B_L_TOD",
      "Name": "5-Band Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_5B_L_TOT",
      "Name": "5-Band Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_5B_LM_TOD",
      "Name": "5-Band Windward/Leeward Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_5B_LM_TOT",
      "Name": "5-Band Windward/Leeward Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_5B_M_TOD",
      "Name": "5-Band Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_5B_M_TOT",
      "Name": "5-Band Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_5B_MH_TOD",
      "Name": "5-Band Windward/Leeward Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_5B_MH_TOT",
      "Name": "5-Band Windward/Leeward Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_5B_H_TOD",
      "Name": "5-Band Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_5B_H_TOT",
      "Name": "5-Band Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_WL6040_L_TOD",
      "Name": "Windward/Leeward 60-40 Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_WL6040_L_TOT",
      "Name": "Windward/Leeward 60-40 Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_WL6040_LM_TOD",
      "Name": "Windward/Leeward 60-40 Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_WL6040_LM_TOT",
      "Name": "Windward/Leeward 60-40 Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_WL6040_M_TOD",
      "Name": "Windward/Leeward 60-40 Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_WL6040_M_TOT",
      "Name": "Windward/Leeward 60-40 Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_WL6040_MH_TOD",
      "Name": "Windward/Leeward 60-40 Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_WL6040_MH_TOT",
      "Name": "Windward/Leeward 60-40 Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOD",
      "Fieldname": "US_WL6040_H_TOD",
      "Name": "Windward/Leeward 60-40 High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CAN",
      "Kind": "TOT",
      "Fieldname": "US_WL6040_H_TOT",
      "Name": "Windward/Leeward 60-40 High"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CYP",
      "Kind": "TOD",
      "Fieldname": "OSN",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CYP",
      "Kind": "TOT",
      "Fieldname": "TMF_Offshore",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CYP",
      "Kind": "PCS",
      "Fieldname": "OC",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CYP",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CYP",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CYP",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CYP",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CYP",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CYP",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CYP",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CYP",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CYP",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CYP",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CYP",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "CYP",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "DEN",
      "Kind": "TOD",
      "Fieldname": "OSN",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "DEN",
      "Kind": "TOT",
      "Fieldname": "TMF_Offshore",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "DEN",
      "Kind": "PCS",
      "Fieldname": "OC",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "DEN",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "DEN",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "DEN",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "DEN",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "DEN",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "DEN",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "DEN",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "DEN",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "DEN",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "DEN",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "DEN",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "DEN",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ESP",
      "Kind": "TOD",
      "Fieldname": "OSN",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ESP",
      "Kind": "PCS",
      "Fieldname": "OC",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ESP",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ESP",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ESP",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ESP",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ESP",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ESP",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "EST",
      "Kind": "TOD",
      "Fieldname": "OSN",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "EST",
      "Kind": "TOT",
      "Fieldname": "TMF_Offshore",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "EST",
      "Kind": "PCS",
      "Fieldname": "OC",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "EST",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "EST",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "EST",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "EST",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "EST",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "EST",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "EST",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "EST",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "EST",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "EST",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "EST",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "EST",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOD",
      "Fieldname": "FIN_FinRating_TOD",
      "Name": "FinRating"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOT",
      "Fieldname": "FIN_FinRating_TOT",
      "Name": "FinRating"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOD",
      "Fieldname": "FIN_FinRating_L_TOD",
      "Name": "FinRating, kevyt tuuli"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOT",
      "Fieldname": "FIN_FinRating_L_TOT",
      "Name": "FinRating, kevyt tuuli"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOD",
      "Fieldname": "FIN_FinRating_H_TOD",
      "Name": "FinRating, kova tuuli"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOT",
      "Fieldname": "FIN_FinRating_H_TOT",
      "Name": "FinRating, kova tuuli"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FIN",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FRA",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FRA",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FRA",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FRA",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FRA",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FRA",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FRA",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FRA",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FRA",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FRA",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FRA",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "FRA",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GER",
      "Kind": "TOT",
      "Fieldname": "TMF_Offshore",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GER",
      "Kind": "PCS",
      "Fieldname": "OC",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GER",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GER",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GER",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GER",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GER",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GER",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GRE",
      "Kind": "TOD",
      "Fieldname": "OSN",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GRE",
      "Kind": "TOT",
      "Fieldname": "TMF_Offshore",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GRE",
      "Kind": "PCS",
      "Fieldname": "OC",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GRE",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GRE",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GRE",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GRE",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GRE",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GRE",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GRE",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GRE",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GRE",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GRE",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GRE",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "GRE",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "HUN",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "HUN",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "HUN",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "HUN",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "HUN",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "HUN",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "HUN",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "HUN",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "HUN",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "HUN",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "HUN",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "HUN",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ISR",
      "Kind": "TOD",
      "Fieldname": "OSN",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ISR",
      "Kind": "TOT",
      "Fieldname": "TMF_Offshore",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ISR",
      "Kind": "PCS",
      "Fieldname": "OC",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ISR",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ISR",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ISR",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ISR",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ISR",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ISR",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ISR",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ISR",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ISR",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ISR",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ISR",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "ISR",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "JPN",
      "Kind": "TOD",
      "Fieldname": "OSN",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "JPN",
      "Kind": "TOT",
      "Fieldname": "TMF_Offshore",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "JPN",
      "Kind": "PCS",
      "Fieldname": "OC",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "JPN",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "JPN",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "JPN",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "JPN",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "JPN",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "JPN",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "JPN",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "JPN",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "JPN",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "JPN",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "JPN",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "JPN",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "KOR",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "KOR",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "KOR",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "KOR",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "KOR",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "KOR",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "KOR",
      "Kind": "TOD",
      "Fieldname": "Pred_Up_TOD",
      "Name": "Predominantly Upwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "KOR",
      "Kind": "TOD",
      "Fieldname": "Pred_Down_TOD",
      "Name": "Predominantly Downwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "KOR",
      "Kind": "TOD",
      "Fieldname": "KR_PREDR_TOD",
      "Name": "Predominantly Reaching"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "LTU",
      "Kind": "TOD",
      "Fieldname": "OSN",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "LTU",
      "Kind": "TOT",
      "Fieldname": "TMF_Offshore",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "LTU",
      "Kind": "PCS",
      "Fieldname": "OC",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "LTU",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "LTU",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "LTU",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "LTU",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "LTU",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "LTU",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "LTU",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "LTU",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "LTU",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "LTU",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "LTU",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "LTU",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NED",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NED",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NED",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NED",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NED",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NED",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NED",
      "Kind": "TOT",
      "Fieldname": "TMF_Offshore",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NED",
      "Kind": "TOT",
      "Fieldname": "Pred_Up_TOT",
      "Name": "Predominantly Upwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NED",
      "Kind": "TOT",
      "Fieldname": "Pred_Down_TOT",
      "Name": "Predominantly Downwind"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "Mu",
        "NS",
        "LI"
      ],
      "CountryId": "NOR",
      "Kind": "TOT",
      "Fieldname": "APHT",
      "Name": "Distanseseilas Singeltall  (APH)"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NOR",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Distanseseilas Trippeltall svak vind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NOR",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Distanseseilas Trippeltall mellomvind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NOR",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Distanseseilas Trippeltall sterk vind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NOR",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Pølsebane Trippeltall svak vind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NOR",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Pølsebane Trippeltall mellomvind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NOR",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Pølsebane Trippeltall sterk vind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NOR",
      "Kind": "TOT",
      "Fieldname": "Pred_Up_TOT",
      "Name": "Motvind Singeltall"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "NOR",
      "Kind": "TOT",
      "Fieldname": "Pred_Down_TOT",
      "Name": "Medvind Singeltall"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "POR",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "POR",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "POR",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "POR",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "POR",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "POR",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "POR",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "POR",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "POR",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "POR",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "POR",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "POR",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "POR",
      "Kind": "TOD",
      "Fieldname": "OSN",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "POR",
      "Kind": "TOT",
      "Fieldname": "TMF_Offshore",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RSA",
      "Kind": "TOD",
      "Fieldname": "RSA_CD_INS_TOD",
      "Name": "Cape Doctor Inshore"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RSA",
      "Kind": "TOT",
      "Fieldname": "RSA_CD_INS_TOT",
      "Name": "Cape Doctor Inshore"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RSA",
      "Kind": "TOD",
      "Fieldname": "RSA_CD_OFF_TOD",
      "Name": "Cape Doctor Coastal"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RSA",
      "Kind": "TOT",
      "Fieldname": "RSA_CD_OFF_TOT",
      "Name": "Cape Doctor Coastal"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RSA",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RSA",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RSA",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RSA",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RSA",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RSA",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RSA",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RSA",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RSA",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RSA",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RSA",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RSA",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RUS",
      "Kind": "TOD",
      "Fieldname": "Pred_Up_TOD",
      "Name": "Predominantly Upwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RUS",
      "Kind": "TOT",
      "Fieldname": "Pred_Up_TOT",
      "Name": "Predominantly Upwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RUS",
      "Kind": "TOD",
      "Fieldname": "Pred_Down_TOD",
      "Name": "Predominantly Downwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "RUS",
      "Kind": "TOT",
      "Fieldname": "Pred_Down_TOT",
      "Name": "Predominantly Downwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SLO",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SLO",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SLO",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SLO",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SLO",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SLO",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SUI",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SUI",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SUI",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SUI",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SUI",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SUI",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOD",
      "Fieldname": "OSN",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOT",
      "Fieldname": "TMF_Offshore",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "PCS",
      "Fieldname": "OC",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOD",
      "Fieldname": "Pred_Up_TOD",
      "Name": "Predominantly Upwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOT",
      "Fieldname": "Pred_Up_TOT",
      "Name": "Predominantly Upwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOD",
      "Fieldname": "Pred_Down_TOD",
      "Name": "Predominantly Downwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "SWE",
      "Kind": "TOT",
      "Fieldname": "Pred_Down_TOT",
      "Name": "Predominantly Downwind"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "UKR",
      "Kind": "TOD",
      "Fieldname": "OSN",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "UKR",
      "Kind": "TOT",
      "Fieldname": "TMF_Offshore",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "SY",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "UKR",
      "Kind": "PCS",
      "Fieldname": "OC",
      "Name": "Coastal/Long Distance"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "UKR",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "UKR",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Low",
      "Name": "Triple Number Coastal/Long Distance Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "UKR",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "UKR",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_Medium",
      "Name": "Triple Number Coastal/Long Distance Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "UKR",
      "Kind": "TOD",
      "Fieldname": "TND_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "UKR",
      "Kind": "TOT",
      "Fieldname": "TN_Offshore_High",
      "Name": "Triple Number Coastal/Long Distance High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "UKR",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "UKR",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "UKR",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "UKR",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "UKR",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "UKR",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_TNAP_L_TOD",
      "Name": "Triple Number AP Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_TNAP_L_TOT",
      "Name": "Triple Number AP Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_TNAP_M_TOD",
      "Name": "Triple Number AP Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_TNAP_M_TOT",
      "Name": "Triple Number AP Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_TNAP_H_TOD",
      "Name": "Triple Number AP High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_TNAP_H_TOT",
      "Name": "Triple Number AP High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Low",
      "Name": "Triple Number Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_Medium",
      "Name": "Triple Number Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "TND_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "TN_Inshore_High",
      "Name": "Triple Number Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDUP_TOD",
      "Name": "Single Number Predominant Upwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDUP_TOT",
      "Name": "Single Number Predominant Upwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDRC_TOD",
      "Name": "Single Number Predominant Reaching"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDRC_TOT",
      "Name": "Single Number Predominant Reaching"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDDN_TOD",
      "Name": "Single Number Predominant Downwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDDN_TOT",
      "Name": "Single Number Predominant Downwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDUP_L_TOD",
      "Name": "Predominant Upwind - Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDUP_L_TOT",
      "Name": "Predominant Upwind - Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDUP_LM_TOD",
      "Name": "Predominant Upwind - Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDUP_LM_TOT",
      "Name": "Predominant Upwind - Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDUP_M_TOD",
      "Name": "Predominant Upwind - Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDUP_M_TOT",
      "Name": "Predominant Upwind - Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDUP_MH_TOD",
      "Name": "Predominant Upwind - Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDUP_MH_TOT",
      "Name": "Predominant Upwind - Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDUP_H_TOD",
      "Name": "Predominant Upwind - High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDUP_H_TOT",
      "Name": "Predominant Upwind - High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDRC_L_TOD",
      "Name": "Predominant Reaching Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDRC_L_TOT",
      "Name": "Predominant Reaching Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDRC_LM_TOD",
      "Name": "Predominant Reaching Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDRC_LM_TOT",
      "Name": "Predominant Reaching Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDRC_M_TOD",
      "Name": "Predominant Reaching Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDRC_M_TOT",
      "Name": "Predominant Reaching Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDRC_MH_TOD",
      "Name": "Predominant Reaching Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDRC_MH_TOT",
      "Name": "Predominant Reaching Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDRC_H_TOD",
      "Name": "Predominant Reaching High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDRC_H_TOT",
      "Name": "Predominant Reaching High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDDN_L_TOD",
      "Name": "Predominant Downwind Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDDN_L_TOT",
      "Name": "Predominant Downwind Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDDN_LM_TOD",
      "Name": "Predominant Downwind Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDDN_LM_TOT",
      "Name": "Predominant Downwind Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDDN_M_TOD",
      "Name": "Predominant Downwind Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDDN_M_TOT",
      "Name": "Predominant Downwind Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDDN_MH_TOD",
      "Name": "Predominant Downwind Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDDN_MH_TOT",
      "Name": "Predominant Downwind Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_PREDDN_H_TOD",
      "Name": "Predominant Downwind High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_PREDDN_H_TOT",
      "Name": "Predominant Downwind High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_CHIMAC_UP_TOT",
      "Name": "Chicago-Mac Upwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_CHIMAC_AP_TOT",
      "Name": "Chicago-Mac All Purpose"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_CHIMAC_DN_TOT",
      "Name": "Chicago-Mac Downwind"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_BAYMAC_CV_TOT",
      "Name": "Bayview-Mac Cove Island"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_BAYMAC_SH_TOT",
      "Name": "Bayview-Mac Shore"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_HARVMOON_TOD",
      "Name": "Harvest Moon Regatta"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_HARVMOON_TOT",
      "Name": "Harvest Moon Regatta"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_VICMAUI_TOT",
      "Name": "Victoria-Maui"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_SFBay_L_TOD",
      "Name": "5-Band SF Bay Tour - Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_SFBay_L_TOT",
      "Name": "5-Band SF Bay Tour - Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_SFBay_LM_TOD",
      "Name": "5-Band SF Bay Tour - Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_SFBay_LM_TOT",
      "Name": "5-Band SF Bay Tour - Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_SFBay_M_TOD",
      "Name": "5-Band SF Bay Tour - Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_SFBay_M_TOT",
      "Name": "5-Band SF Bay Tour - Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_SFBay_MH_TOD",
      "Name": "5-Band SF Bay Tour - Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_SFBay_MH_TOT",
      "Name": "5-Band SF Bay Tour - Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_SFBay_H_TOD",
      "Name": "5-Band SF Bay Tour - High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_SFBay_H_TOT",
      "Name": "5-Band SF Bay Tour - High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_AP_L_TOD",
      "Name": "5-Band AP Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_AP_L_TOT",
      "Name": "5-Band AP Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_AP_LM_TOD",
      "Name": "5-Band AP Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_AP_LM_TOT",
      "Name": "5-Band AP Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_AP_M_TOD",
      "Name": "5-Band AP Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_AP_M_TOT",
      "Name": "5-Band AP Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_AP_MH_TOD",
      "Name": "5-Band AP Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_AP_MH_TOT",
      "Name": "5-Band AP Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_AP_H_TOD",
      "Name": "5-Band AP High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_AP_H_TOT",
      "Name": "5-Band AP High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_5B_L_TOD",
      "Name": "5-Band Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_5B_L_TOT",
      "Name": "5-Band Windward/Leeward Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_5B_LM_TOD",
      "Name": "5-Band Windward/Leeward Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_5B_LM_TOT",
      "Name": "5-Band Windward/Leeward Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_5B_M_TOD",
      "Name": "5-Band Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_5B_M_TOT",
      "Name": "5-Band Windward/Leeward Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_5B_MH_TOD",
      "Name": "5-Band Windward/Leeward Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_5B_MH_TOT",
      "Name": "5-Band Windward/Leeward Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_5B_H_TOD",
      "Name": "5-Band Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_5B_H_TOT",
      "Name": "5-Band Windward/Leeward High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_WL6040_L_TOD",
      "Name": "Windward/Leeward 60-40 Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_WL6040_L_TOT",
      "Name": "Windward/Leeward 60-40 Low"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_WL6040_LM_TOD",
      "Name": "Windward/Leeward 60-40 Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_WL6040_LM_TOT",
      "Name": "Windward/Leeward 60-40 Low/Med"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_WL6040_M_TOD",
      "Name": "Windward/Leeward 60-40 Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_WL6040_M_TOT",
      "Name": "Windward/Leeward 60-40 Medium"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_WL6040_MH_TOD",
      "Name": "Windward/Leeward 60-40 Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_WL6040_MH_TOT",
      "Name": "Windward/Leeward 60-40 Med/High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOD",
      "Fieldname": "US_WL6040_H_TOD",
      "Name": "Windward/Leeward 60-40 High"
    },
    {
      "Families": [
        "ORC",
        "DH",
        "NS",
        "LI"
      ],
      "CountryId": "USA",
      "Kind": "TOT",
      "Fieldname": "US_WL6040_H_TOT",
      "Name": "Windward/Leeward 60-40 High"
    }
  ]
}
"""