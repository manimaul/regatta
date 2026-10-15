package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.RaceClassBrackets
import com.mxmariner.regatta.data.RaceSchedule
import components.*
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
import utils.display
import viewmodel.ClassScheduleDraft
import viewmodel.RacesEditViewModel
import viewmodel.RacesViewModel
import viewmodel.alertsViewModel
import viewmodel.complete

private const val raceModalId = "add-edit-race"

@Composable
fun Races(
    viewModel: RacesViewModel = remember { RacesViewModel() }
) {
    val state = viewModel.flow.collectAsState()
    H1 { Text("Races") }
    // year picker and the create button side by side under the title, like the other list pages
    Div(attrs = { classes("flex", "flex-wrap", "items-end", "gap-4") }) {
        Div {
            B { Text("Year") }
            RgRaceYearSelector { viewModel.selectYear(it) }
        }
        RgModalButton(
            id = raceModalId,
            style = RgButtonStyle.SuccessOutline,
            customClasses = listOf(AppStyle.marginBot),
            buttonLabel = { "Create Race" },
            openAction = { viewModel.newRace() }
        )
    }
    AddEditRaceModal(state.value.edit)
    state.value.races.complete(viewModel) { RaceList(it, viewModel) }
}

@Composable
fun RaceList(schedules: List<RaceSchedule>, viewModel: RacesViewModel) {
    RgTable {
        RgThead {
            RgTr {
                RgTh { Text("Series") }
                RgTh { Text("Name") }
                RgTh { Text("Race start") }
                RgTh { Text("Race finish") }
                RgTh { Text("Race committee") }
                RgTh { Text("Action") }
            }
        }
        RgTbody {
            schedules.forEach { sch ->
                RgTr {
                    RgTd { Text(sch.series?.name ?: "-") }
                    RgTd { Text(sch.race.name) }
                    RgTd { Text(sch.startTime.display()) }
                    RgTd { Text(sch.endTime.display()) }
                    RgTd { Text(sch.rc?.let { "${it.first} ${it.last}" } ?: "-") }
                    RgTd {
                        RgModalButton(
                            id = raceModalId,
                            style = RgButtonStyle.PrimaryOutline,
                            customClasses = listOf("float-start"),
                            buttonLabel = { "Edit" },
                            openAction = { viewModel.editRace(sch.race) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Adds or edits a race: name, race committee, series, correction factor, then the class start times edited in place
 * with +/- buttons like the brackets of the Add Race Class modal. [viewModel] is null until the modal is first opened.
 */
@Composable
fun AddEditRaceModal(viewModel: RacesEditViewModel?) {
    val state = viewModel?.let {
        val state by it.flow.collectAsState()
        state
    }
    val schedule = state?.draftedSchedule()
    val editing = viewModel != null && viewModel.raceId != 0L
    // results reference the race (a foreign key), so a race with results cannot be deleted
    val resultCount = state?.race?.value?.resultCount ?: 0L
    RgModalBody(
        id = raceModalId,
        modalTitle = { if (editing) "Edit Race" else "Create Race" },
        content = {
            if (viewModel == null) {
                RgSpinner()
            } else {
                RaceFields(viewModel)
            }
        },
        footer = {
            RgModalFooterRow {
                if (editing) {
                    RgButton(
                        label = "Delete",
                        style = RgButtonStyle.Danger,
                        disabled = resultCount > 0L,
                        closesModal = true,
                    ) {
                        val name = viewModel.flow.value.race.value?.race?.name ?: "this race"
                        alertsViewModel.confirm("Are you sure?", "Delete $name?") { yes ->
                            if (yes) viewModel.delete()
                        }
                    }
                }
                RgButton(
                    label = "Save",
                    style = RgButtonStyle.Success,
                    disabled = viewModel == null || schedule == null,
                    closesModal = true,
                ) {
                    if (viewModel != null && schedule != null) viewModel.save(schedule)
                }
            }
        }
    )
}

@Composable
private fun RaceFields(viewModel: RacesEditViewModel) {
    val state by viewModel.flow.collectAsState()
    state.race.complete(viewModel) { schedule ->
        RgForm {
            Fieldset {
                P {
                    RgInput(label = "Race Name", placeHolder = true, value = schedule.race.name) {
                        viewModel.setRaceName(it)
                    }
                }
                P {
                    Label(attrs = { classes(AppStyle.marginEnd) }) { B { Text("Race Committee") } }
                    state.skippers.complete(viewModel) { people ->
                        RgDropdownNone(people.sortedBy { it.fullName() }, schedule.rc, { it.fullName() }) {
                            viewModel.setRC(it)
                        }
                    }
                }
                P {
                    Label(attrs = { classes(AppStyle.marginEnd) }) { B { Text("Series") } }
                    state.series.complete(viewModel) { series ->
                        RgSeriesDropdown(series, schedule.series) {
                            viewModel.setSeries(it)
                        }
                    }
                }
                P {
                    RgInput(label = "CF", placeHolder = true, value = schedule.race.phrfBFactor.toString()) {
                        viewModel.setCF(it.toIntOrNull())
                    }
                }

                if (schedule.resultCount > 0L) {
                    P(attrs = { classes("text-muted") }) {
                        Text("${schedule.resultCount} result(s) are recorded for this race, so it cannot be deleted.")
                    }
                }

                H6 { Text("Race Class Start Times") }
                Hr { }
                state.classes.complete(viewModel) { classes ->
                    state.drafts.forEach { draft ->
                        key(draft.key) {
                            P { EditClassSchedule(draft, classes, state.drafts, viewModel) }
                            Hr { }
                        }
                    }
                    P {
                        RgButton(label = "+", disabled = state.drafts.size >= classes.size) {
                            viewModel.addDraft()
                        }
                    }
                }
            }
        }
    }
}

/**
 * One class's row: remove button and class picker, start and end, then the class's brackets as switches. The picker
 * offers only classes no other draft has taken.
 */
@Composable
private fun EditClassSchedule(
    draft: ClassScheduleDraft,
    classes: List<RaceClassBrackets>,
    drafts: List<ClassScheduleDraft>,
    viewModel: RacesEditViewModel,
) {
    val taken = drafts.filter { it.key != draft.key }.mapNotNull { it.raceClass?.id }
    val available = classes.filter { it.raceClass.id !in taken }
    Div(attrs = { classes("flex", "items-center") }) {
        RgButton(label = "-", customClasses = listOf(AppStyle.marginEnd)) {
            viewModel.removeDraft(draft.key)
        }
        RgClassDropdown(available, draft.raceClass) {
            viewModel.selectDraftClass(draft.key, it?.raceClass)
        }
    }
    Div(attrs = { classes(AppStyle.marginTop) }) {
        RgTime(label = "Start", showDate = true, date = draft.startDate) { t ->
            viewModel.updateDraft(draft.key) { copy(startDate = t) }
        }
    }
    Div(attrs = { classes(AppStyle.marginTop) }) {
        RgTime(label = "End", showDate = true, date = draft.endDate) { t ->
            viewModel.updateDraft(draft.key) { copy(endDate = t) }
        }
    }
    if (draft.endDate <= draft.startDate) {
        P(attrs = { classes("text-danger-fg") }) { Text("End must be after start") }
    }
    classes.firstOrNull { it.raceClass.id == draft.raceClass?.id }?.brackets?.let { brackets ->
        P { B { Text("Brackets") } }
        RgSwitches(
            items = brackets,
            label = { it.label() },
            check = { b -> draft.brackets.any { it.id == b.id } },
        ) { b, on ->
            viewModel.updateDraft(draft.key) {
                copy(brackets = if (on) this.brackets + b else this.brackets.filterNot { it.id == b.id })
            }
        }
    }
}
