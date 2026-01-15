package components

import OrcCertificate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.RatingType
import com.mxmariner.regatta.ratingDefault
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import styles.AppStyle
import utils.Complete
import utils.Error
import utils.Loading
import utils.Uninitialized
import viewmodel.OrcViewModel
import viewmodel.complete

@Composable
fun RatingSelections(
    boatType: RatingType,
    phrfRating: Int?,
    cert: OrcCertificate? = null,
    onOrc: ((OrcCertificate?) -> Unit)? = null,
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
            P { OrcInfo(cert = cert) { onOrc?.invoke(it) } }
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
            P { OrcInfo(cert = cert) { onOrc?.invoke(it) } }
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
    cert: OrcCertificate? = null,
    onOrc: (OrcCertificate?) -> Unit ,
) {
    viewModel.setCert(cert)
    val state by viewModel.flow.collectAsState()
    when (val event = state.cert) {
        is Complete<OrcCertificate> -> {
            onOrc(event.value)
            OrcDisplay(event.value)
        }
        is Error<OrcCertificate> -> ErrorDisplay(event) { viewModel.reload() }
        is Loading<OrcCertificate> -> RgSpinner()
        Uninitialized -> OrcFetch(viewModel)
    }
}

@Composable
fun OrcFetch(
    viewModel: OrcViewModel = remember { OrcViewModel() }
) {
    val state by viewModel.flow.collectAsState()
    RgInputWithButton(
        label = "ORC Reference Number",
        btnLabel = "Add",
        value = state.refNumber,
    ) { change, clicked ->
        if (clicked) {
            viewModel.confirmRef()
        } else {
            viewModel.refNumber(change)
        }
    }
}

@Composable
fun OrcDisplay(
    certificate: OrcCertificate
) {
    Text("Cert: ${certificate.refNo} ${certificate.certNo}")
}