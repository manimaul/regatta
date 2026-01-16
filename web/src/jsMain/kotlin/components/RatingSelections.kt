package components

import OrcCertificate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.RatingType
import com.mxmariner.regatta.ratingDefault
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import styles.AppStyle
import utils.Complete
import utils.Error
import utils.Loading
import utils.Uninitialized
import utils.display
import viewmodel.OrcViewModel

enum class Action {
    Add,
    Delete
}

@Composable
fun RatingSelections(
    boat: Boat,
    onOrc: ((Action, OrcCertificate) -> Unit)? = null,
    typeChange: (RatingType, Int) -> Unit,
) {
    RatingSelections(boat.ratingType, boat.phrfRating, boat.orcCerts, onOrc, typeChange)
}


@Composable
fun RatingSelections(
    boatType: RatingType,
    phrfRating: Int?,
    certs: List<OrcCertificate> = emptyList(),
    onOrc: ((Action, OrcCertificate) -> Unit)? = null,
    typeChange: (RatingType, Int) -> Unit,
) {
    P {
        Label(attrs = {
            classes(AppStyle.marginEnd)
        }) { B { Text("Rating Type") } }
        RgDropdown(RatingType.entries, boatType, { it.label }) {
            typeChange(it, phrfRating ?: ratingDefault.toInt())
        }
    }
    when (boatType) {
        RatingType.ORC -> {
            P { OrcInfo(certs = certs) { c, a -> onOrc?.invoke(c, a) } }
        }

        RatingType.ORC_PHRF -> {
            P {
                RgNumberInput(
                    label = "PHRF Rating",
                    value = phrfRating,
                ) {
                    typeChange(boatType, it.toInt())
                }
            }
            P { OrcInfo(certs = certs) { c, a -> onOrc?.invoke(c, a) } }
        }

        RatingType.PHRF -> {
            P {
                RgNumberInput(
                    label = "PHRF Rating",
                    value = phrfRating,
                ) {
                    typeChange(boatType, it.toInt())
                }
            }
        }

        else -> {}
    }
}

@Composable
fun OrcInfo(
    viewModel: OrcViewModel = remember { OrcViewModel() },
    certs: List<OrcCertificate> = emptyList(),
    onOrc: (Action, OrcCertificate) -> Unit,
) {
    viewModel.setCerts(certs)
    val state by viewModel.flow.collectAsState()
    B {
        Text("ORC Certificates")
    }
    state.certs.forEach {
        OrcDisplay(it, onOrc)
    }
    when (val event = state.cert) {
        is Complete<OrcCertificate> -> {
            OrcFetch(onOrc, viewModel)
        }

        is Error<OrcCertificate> -> ErrorDisplay(event) { viewModel.reload() }
        is Loading<OrcCertificate> -> RgSpinner()
        Uninitialized -> OrcFetch(onOrc, viewModel)
    }
}

@Composable
fun OrcFetch(
    onOrc: (Action, OrcCertificate) -> Unit,
    viewModel: OrcViewModel = remember { OrcViewModel() }
) {
    val state by viewModel.flow.collectAsState()
    RgInputWithButton(
        label = "ORC Reference Number",
        btnLabel = "Add Cert",
        value = state.refNumber,
        btnDisabled = !state.readyToAdd,
    ) { change, clicked ->
        if (clicked) {
            viewModel.confirmRef(onOrc)
        } else {
            viewModel.refNumber(change)
        }
    }
}

@Composable
fun OrcDisplay(
    certificate: OrcCertificate,
    onOrc: (Action, OrcCertificate) -> Unit,
) {
    P {
        Text("Reference Number: ${certificate.refNo} ")
        RgButton(
            label = "Remove",
            style = RgButtonStyle.SecondaryOutline
        ) {
            onOrc(Action.Delete, certificate)
        }
        Br { }
        Text("${certificate.yachtName} ${certificate.sailNo}")
        Br { }
        Text("Certificate Number: ${certificate.certNo}")
        Br { }
        Text("Issue Date: ${certificate.issueDate.display()}")
        Br { }
        Text("Type: ${certificate.cType} Division: ${certificate.division}")
    }
}