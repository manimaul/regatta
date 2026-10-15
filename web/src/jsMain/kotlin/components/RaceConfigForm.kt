package components

import androidx.compose.runtime.Composable
import com.mxmariner.regatta.data.ClassSchedule
import com.mxmariner.regatta.data.Orc3Band
import com.mxmariner.regatta.data.Orc5Band
import com.mxmariner.regatta.data.OrcScoringOption
import com.mxmariner.regatta.data.PhrfBFactor
import com.mxmariner.regatta.data.RaceSchedule
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
import kotlin.time.Instant

/**
 * The race day config: one start time per class, the PHRF B factor, and the ORC scoring option
 * with its wind band. Shared by the RC page and the results editor's Config modal (R4a). The
 * callers apply the edits with the helpers in utils/RaceConfig.kt.
 */
@Composable
fun RaceConfigForm(
    schedule: RaceSchedule,
    onClassStart: (ClassSchedule, Instant) -> Unit,
    onPhrfBFactor: (Int?) -> Unit,
    onOrcOption: (OrcScoringOption, Orc3Band, Orc5Band) -> Unit,
) {
    schedule.schedule.forEach { classSchedule ->
        H6 { Text(classSchedule.raceClass.name) }
        RgDiv(customizer = { set(space = RgSpace.m, size = RgSz.s3, side = RgSide.b) }) {
            RgTime("Start time", classSchedule.startDate) {
                onClassStart(classSchedule, it)
            }
        }
    }

    RgInput("PHRF B Factor", "${schedule.race.phrfBFactor}") {
        onPhrfBFactor(it.toIntOrNull() ?: 0)
    }

    PhrfBFactor.fromBFactor(schedule.race.phrfBFactor).let { factor ->
        P {
            Text("${factor.label} - ${factor.desc}")
        }
    }

    RgDiv(customizer = { set(space = RgSpace.m, size = RgSz.s3, side = RgSide.t) }) {
        OrcScoringOptionSelect(
            schedule.race.orcScoringOption,
            schedule.race.orc3Band,
            schedule.race.orc5Band,
            onOrcOption
        )
    }
}

@Composable
fun OrcScoringOptionSelect(
    selected: OrcScoringOption,
    selected3Band: Orc3Band,
    selected5Band: Orc5Band,
    onSelect: (OrcScoringOption, Orc3Band, Orc5Band) -> Unit
) {
    P {
        Label("orc_option", attrs = {
            classes(AppStyle.marginEnd)
        }) { B { Text("ORC Scoring Option") } }
        RgIdDropdown("orc_option", OrcScoringOption.entries, selected, { it.label }) {
            onSelect(it, selected3Band, selected5Band)
        }
    }
    if (selected.uses3Band) {
        P {
            Label("orc_wind", attrs = {
                classes(AppStyle.marginEnd)
            }) { B { Text("ORC Wind / Conditions") } }
            RgIdDropdown("orc_wind", Orc3Band.entries, selected3Band, { "${it.label} - ${it.desc}" }) {
                onSelect(selected, it, selected5Band)
            }
        }
    } else if (selected.uses5Band) {
        P {
            Label("orc_wind", attrs = {
                classes(AppStyle.marginEnd)
            }) { B { Text("ORC Wind / Conditions") } }
            RgIdDropdown("orc_wind", Orc5Band.entries, selected5Band, { "${it.label} - ${it.desc}" }) {
                onSelect(selected, selected3Band, it)
            }
        }
    }
}
