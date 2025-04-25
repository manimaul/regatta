package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.RaceSchedule
import components.*
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import utils.*
import viewmodel.*

@Composable
fun Rc() {
    val state = loginViewModel.flow.collectAsState()
    if (state.value.loginStatus == LoginStatus.LoggedIn) {
        RcLoggedIn()
    } else {
        H1 { Text("Unauthorized - Login Required") }
    }
}

@Composable
fun RcLoggedIn(viewModel: RcViewModel = remember { RcViewModel() }) {
    val state = viewModel.flow.collectAsState()
    //race name and date
    state.value.selectedRace?.let { selectedRace ->
        H1 { Text("RC Race Day - ${selectedRace.startTime.dateStr()}") }
    } ?: run {
        H1 { Text("RC Race Day") }
    }
    state.value.races.complete(viewModel) { races ->
        RgDropdown(races, state.value.selectedRace ?: races.first(), { it.race.name }, { viewModel.selectRace(it) })
    }
    RgDiv(customizer = { set(space = RgSpace.m, size = RgSz.s4, side = RgSide.y) }) {
        Ul(attrs = { classes("nav", "nav-pills") }) {
            RcTab.entries.forEach { tab ->
                Li {
                    A(href = "#", attrs = {
                        onClick { viewModel.selectTab(tab) }
                        classes("nav-link", if (tab == state.value.tab) "active" else "inactive")
                    }) {
                        Text(tab.title)
                    }
                }
            }
        }
    }
    state.value.selectedRace?.let { selectedRace ->
        when (state.value.tab) {
            RcTab.RaceConfig -> RcRaceConfig(viewModel, selectedRace, state.value.syncState)
            RcTab.Checkin -> RcCheckin(viewModel)
            RcTab.FinishLine -> RcFinish(viewModel)
        }
    }

}

@Composable
fun RcRaceConfig(viewModel: RcViewModel, selectedRace: RaceSchedule, syncState: SyncState) {
    when (syncState) {
        SyncState.Dirty -> P { Text("Status: Unsaved") }
        SyncState.Working -> P { Text("Status: Saving...") }
        SyncState.Synced -> P { Text("Status: Saved") }
    }
    selectedRace.schedule.forEach { classSchedule ->
        H6 { Text(classSchedule.raceClass.name) }
        RgDiv(customizer = { set(space = RgSpace.m, size = RgSz.s3, side = RgSide.b) }) {
            RgTime("Start time", classSchedule.startDate) {
                viewModel.classStart(classSchedule, it)
            }
        }
    }
    RgInput("Correction Factor", "${selectedRace.race.correctionFactor}") {
        viewModel.setCf(it.toIntOrNull() ?: 0)
    }
}


@Composable
fun RcCheckin(viewModel: RcViewModel) {
    val state = viewModel.flow.collectAsState()
    var filter by remember { mutableStateOf("") }
    Div(attrs = {
        classes("input-group", "input-group-sm")
    }) {
        RgButton(label = "X", style = RgButtonStyle.SecondaryOutline) {
            filter = ""
        }
        Input(InputType.Text) {
            classes("form-control")
            placeholder("Filter")
            value(filter)
            onInput {
                filter = it.value
            }
        }
    }
    Br { }
    state.value.boats.complete(viewModel) { boats ->
        RgTable {
            RgThead {
                RgTr {
                    RgTh { Text("Boat Name") }
                    RgTh { Text("Checked In") }
                }
            }
            RgTbody {
                boats.filter { itemNameContainsFilter(it.bs.dropLabel(), filter) }.forEach { ea ->
                    RgTr {
                        RgTd {
                            if (ea.checkedIn) {
                                B { Text(ea.bs.dropLabel()) }
                            } else {
                                Text(ea.bs.dropLabel())
                            }
                        }
                        RgTd {
                            if (ea.checkedIn) {
                                RgButton(
                                    label = "Checkout",
                                    style = RgButtonStyle.Danger,
                                    disabled = ea.result != null
                                ) {
                                    viewModel.checkOut(ea.bs)
                                }
                            } else {
                                RgButton(label = "Checkin", style = RgButtonStyle.Success) {
                                    viewModel.checkIn(ea.bs)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RcFinish(viewModel: RcViewModel) {
    H4 { Text("Race Finish Line") }
    val state = viewModel.flow.collectAsState()
    var confirmDeleteResult by remember {  mutableStateOf<CheckIn?>(null) }

    RgModalBody(id = "rc-finish-time", modalTitle = { state.value.focus?.bs?.shortLabel() ?: "" }, content = {
        state.value.focus?.let { focus ->
            RcTimeRow(viewModel)
        }
    }, footer = {
        Button(attrs = {
            classes(*RgButtonStyle.PrimaryOutline.classes)
            attr("data-bs-dismiss", "modal")
        }) {
            Text("Cancel")
        }
        Button(attrs = {
            classes(*RgButtonStyle.Success.classes)
            attr("data-bs-dismiss", "modal")
            if (state.value.focus?.isValid() == false) {
                disabled()
            }
            onClick {
                viewModel.saveFocus()
            }
        }) {
            Text("Save")
        }
    })

    state.value.boats.complete(viewModel) { boats ->
        RgTable {
            RgThead {
                RgTr {
                    RgTh { Text("Boat Name") }
                    RgTh { Text("Finish Time") }
                    RgTh { Text("Action") }
                }
            }
            RgTbody {
                boats.filter { ea -> ea.checkedIn }.forEach { ea ->
                    RgTr {
                        RgTd { Text(ea.bs.boat?.name ?: "") }
                        RgTd {
                            val txt = ea.result?.finishText(ea.startTime)
                            val style = txt?.let { RgButtonStyle.PrimaryOutline } ?: RgButtonStyle.SuccessOutline
                            RgModalButton(
                                id = "rc-finish-time",
                                style = style,
                                buttonLabel = { ea.result?.finishText(ea.startTime) ?: "Finish" }) {
                                viewModel.focus(ea.bs, ea.result)
                            }
                        }
                        RgTd {
                            ea.result?.let {
                                RgModalButton(
                                    id = "finish-time-delete-confirm",
                                    style = RgButtonStyle.Danger,
                                    buttonLabel = { "Delete" }) {
                                    confirmDeleteResult = ea
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    RgModalBody(
        id = "finish-time-delete-confirm",
        modalTitle = { "Are you sure?" },
        content = {
            confirmDeleteResult?.let {
                P { Text("Delete result for ${it.bs.boat?.name}?") }
                P { Text("${it.result?.finish?.display()}") }
            }

        }, footer = {
            Button(attrs = {
                classes(*RgButtonStyle.PrimaryOutline.classes)
                attr("data-bs-dismiss", "modal")
            }) {
                Text("Cancel")
            }
            Button(attrs = {
                classes(*RgButtonStyle.Danger.classes)
                attr("data-bs-dismiss", "modal")
                onClick {
                    confirmDeleteResult?.result?.let { viewModel.delete(it) }
                }
            }) {
                Text("Yes - Delete it!")
            }

        })
}
