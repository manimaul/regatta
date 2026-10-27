//! = `routes/Classes.kt` (+ `ClassesViewModel.kt` state).

use crate::app_style;
use crate::components::join_classes;
use crate::components::modal::{RgModalBody, RgModalButton, RgModalFooterRow};
use crate::components::rg_button::{RgButton, RgButtonStyle};
use crate::components::rg_dropdown::RgDropdown;
use crate::components::rg_form::{NumberType, RgForm, RgInput, RgNumberInput};
use crate::components::rg_sortable::RgSortable;
use crate::components::rg_table::{RgTable, RgTbody, RgTd, RgTh, RgThead, RgTr};
use crate::routes::async_view;
use crate::server_fns;
use crate::utils::async_state::{call, Loaded};
use leptos::either::Either;
use leptos::prelude::*;
use regatta_common::{Bracket, RaceClass, RaceClassBrackets, RatingType};

pub const ADD_EDIT_CLASS_MODAL_ID: &str = "add-edit-class";

/// = `ClassesState`.
#[derive(Clone, Copy)]
pub struct ClassesState {
    pub edit_class: RwSignal<RaceClassBrackets>,
    pub class_list: Loaded<Vec<RaceClassBrackets>>,
    pub sort_mode: RwSignal<bool>,
}

fn new_class() -> RaceClassBrackets {
    RaceClassBrackets {
        race_class: RaceClass::default(),
        brackets: vec![Bracket::default()],
    }
}

impl ClassesState {
    pub fn new() -> ClassesState {
        ClassesState {
            edit_class: RwSignal::new(new_class()),
            class_list: Loaded::new(server_fns::all_classes),
            sort_mode: RwSignal::new(false),
        }
    }

    pub fn reload(&self) {
        self.class_list.reload();
    }

    /// `nextSort()`: one past the last class's sort value.
    pub fn next_sort(&self) -> i32 {
        self.class_list
            .value()
            .and_then(|list| list.last().map(|c| c.race_class.sort + 1))
            .unwrap_or(0)
    }

    pub fn edit_class(&self, class: RaceClassBrackets) {
        self.edit_class.set(class);
    }

    pub fn add_bracket(&self) {
        self.edit_class
            .update(|c| c.brackets.push(Bracket::default()));
    }

    pub fn remove_bracket(&self, index: usize) {
        self.edit_class.update(|c| {
            if index < c.brackets.len() {
                c.brackets.remove(index);
            }
        });
    }

    pub fn update_bracket(&self, index: usize, bracket: Bracket) {
        self.edit_class.update(|c| {
            if let Some(b) = c.brackets.get_mut(index) {
                *b = bracket;
            }
        });
    }

    /// `isUpsertClassBracketsValid`: a name, at least one bracket, every bracket named.
    pub fn is_upsert_class_brackets_valid(&self, class: &RaceClassBrackets) -> bool {
        !class.race_class.name.trim().is_empty()
            && !class.brackets.is_empty()
            && class.brackets.iter().all(|b| !b.name.trim().is_empty())
    }

    /// `upsertClassBrackets`: a new class takes the next sort value; the editor resets; then reload.
    pub fn upsert_class_brackets(&self, class: RaceClassBrackets) {
        let class = if class.race_class.id == 0 {
            RaceClassBrackets {
                race_class: RaceClass {
                    sort: self.next_sort(),
                    ..class.race_class
                },
                ..class
            }
        } else {
            class
        };
        self.edit_class.set(RaceClassBrackets {
            race_class: RaceClass {
                sort: self.next_sort(),
                ..RaceClass::default()
            },
            brackets: vec![Bracket::default()],
        });
        let list = self.class_list;
        call(server_fns::post_class(class), move |r| {
            list.after(r.map(drop))
        });
    }

    /// `delete(raceClassBrackets)`: every bracket, then the class (a bracket still in use stops
    /// the deletion with the server's text), then reload.
    pub fn delete(&self, class: RaceClassBrackets) {
        let list = self.class_list;
        call(
            async move {
                for bracket in &class.brackets {
                    server_fns::delete_bracket(bracket.id).await?;
                }
                server_fns::delete_class(class.race_class.id).await
            },
            move |r| list.after(r),
        );
    }

    /// `saveClassOrder`: posts the list in its new order, then leaves sort mode and reloads.
    pub fn save_class_order(&self, order: Vec<RaceClass>) {
        self.sort_mode.set(false);
        let list = self.class_list;
        call(server_fns::post_class_list(order), move |r| {
            list.after(r.map(drop))
        });
    }
}

impl Default for ClassesState {
    fn default() -> Self {
        ClassesState::new()
    }
}

#[component]
pub fn Classes() -> impl IntoView {
    let state = ClassesState::new();
    async_view(
        state.class_list.signal(),
        move || state.reload(),
        move |list| {
            if state.sort_mode.get() {
                Either::Left(
                    view! { <SortClasses list=list.into_iter().map(|c| c.race_class).collect() state=state/> },
                )
            } else {
                Either::Right(view! { <ClassList state=state list=list/> })
            }
        },
    )
}

#[component]
pub fn SortClasses(list: Vec<RaceClass>, state: ClassesState) -> impl IntoView {
    let order = RwSignal::new(list.clone());
    let classes = join_classes(&[app_style::MARGIN_START, app_style::MARGIN_TOP]);
    view! {
        <h1>"Series Sort Order"</h1>
        <RgSortable
            items=list
            name=|c: &RaceClass| c.name.clone()
            on_sort={move |sorted: Vec<RaceClass>| order.set(sorted.into_iter().enumerate().map(|(i, mut c)| { c.sort = i as i32; c }).collect())}
        />
        <RgButton label="Cancel" style=RgButtonStyle::PrimaryOutline custom_classes=classes.clone() click=move |()| state.sort_mode.set(false)/>
        <RgButton label="Save" style=RgButtonStyle::Success custom_classes=classes click=move |()| state.save_class_order(order.get_untracked())/>
    }
}

#[component]
pub fn ClassList(state: ClassesState, list: Vec<RaceClassBrackets>) -> impl IntoView {
    view! {
        <h1>"Race Classes"</h1>
        <div class=app_style::MARGIN_BOT>
            <RgModalButton
                id=ADD_EDIT_CLASS_MODAL_ID
                style=RgButtonStyle::SuccessOutline
                button_label="Add Class"
                open_action=move |()| state.edit_class(new_class())
            />
            <AddEditClassModal state=state/>
        </div>
        <RgTable>
            <RgThead>
                <RgTr>
                    <RgTh>"Name"</RgTh>
                    <RgTh>"Rating"</RgTh>
                    <RgTh>"Brackets"</RgTh>
                    <RgTh>"Number Of Races"</RgTh>
                    <RgTh>"Action"</RgTh>
                </RgTr>
            </RgThead>
            <RgTbody>
                {list.into_iter().map(|each| {
                    let edit = each.clone();
                    let delete = each.clone();
                    let deletable = each.race_class.number_of_races == 0;
                    view! {
                        <RgTr>
                            <RgTd>{each.race_class.name.clone()}</RgTd>
                            <RgTd>{each.race_class.rating_type.label()}</RgTd>
                            <RgTd>
                                {each.brackets.iter().map(|b| view! { <div>{b.label()}<br/></div> }).collect_view()}
                            </RgTd>
                            <RgTd>{each.race_class.number_of_races.to_string()}</RgTd>
                            <RgTd>
                                <RgModalButton
                                    id=ADD_EDIT_CLASS_MODAL_ID
                                    style=RgButtonStyle::PrimaryOutline
                                    button_label="Edit Class"
                                    open_action=move |()| state.edit_class(edit.clone())
                                />
                                {deletable.then(|| view! {
                                    <RgButton label="Delete Class" style=RgButtonStyle::Danger custom_classes=app_style::MARGIN_ALL click=move |()| state.delete(delete.clone())/>
                                })}
                            </RgTd>
                        </RgTr>
                    }
                }).collect_view()}
            </RgTbody>
        </RgTable>
        <div class="flex-wrap">
            <RgButton label="Change Sort Order" click=move |()| state.sort_mode.set(true)/>
        </div>
    }
}

#[component]
pub fn AddEditClassModal(state: ClassesState) -> impl IntoView {
    let edit = state.edit_class;
    let title = move || {
        if edit.with(|c| c.race_class.id == 0) {
            "Add Race Class"
        } else {
            "Edit Race Class"
        }
    };
    let name = Signal::derive(move || edit.with(|c| c.race_class.name.clone()));
    let rating = Signal::derive(move || edit.with(|c| c.race_class.rating_type));
    let fresh = move || edit.with(|c| c.race_class.number_of_races == 0);
    view! {
        <RgModalBody
            id=ADD_EDIT_CLASS_MODAL_ID
            modal_title=title
            footer=Box::new(move || view! {
                <RgModalFooterRow>
                    <RgButton
                        label="Save"
                        style=RgButtonStyle::Success
                        disabled=Signal::derive(move || !edit.with(|c| state.is_upsert_class_brackets_valid(c)))
                        closes_modal=true
                        click=move |()| state.upsert_class_brackets(edit.get_untracked())
                    />
                </RgModalFooterRow>
            }.into_any())
        >
            <RgForm>
                <fieldset>
                    <p><RgInput label="Name" value=name place_holder=true listener=move |v: String| edit.update(|c| c.race_class.name = v)/></p>
                    <div class="mb-4">
                        {move || if fresh() {
                            Either::Left(view! {
                                <RgDropdown
                                    items=RatingType::ENTRIES.to_vec()
                                    selected_item=rating
                                    name=|t: &RatingType| t.label().to_string()
                                    handler=move |t: RatingType| edit.update(|c| c.race_class.rating_type = t)
                                />
                            })
                        } else {
                            Either::Right(edit.with(|c| format!("({} {} race(s) / cannot be edited)", c.race_class.rating_type.label(), c.race_class.number_of_races)))
                        }}
                    </div>
                    <h6>"Brackets"</h6>
                    <hr/>
                    {move || edit.get().brackets.into_iter().enumerate().map(|(i, bracket)| view! {
                        {if fresh() {
                            Either::Left(view! { <div class="mb-4"><EditBracket index=i state=state/></div> })
                        } else {
                            Either::Right(view! { <p><ViewBracket bracket=bracket/></p> })
                        }}
                        <hr/>
                    }).collect_view()}
                    {move || fresh().then(|| view! {
                        <p><RgButton label="+" click=move |()| state.add_bracket()/></p>
                    })}
                </fieldset>
            </RgForm>
        </RgModalBody>
    }
}

#[component]
pub fn ViewBracket(bracket: Bracket) -> impl IntoView {
    view! { <p>{format!("{} ({} race(s) / cannot be edited)", bracket.name, bracket.number_of_races)}</p> }
}

#[component]
pub fn EditBracket(index: usize, state: ClassesState) -> impl IntoView {
    let bracket = Signal::derive(move || {
        state
            .edit_class
            .with(|c| c.brackets.get(index).cloned().unwrap_or_default())
    });
    let name = Signal::derive(move || bracket.with(|b| b.name.clone()));
    let description =
        Signal::derive(move || bracket.with(|b| b.description.clone().unwrap_or_default()));
    let min_rating = Signal::derive(move || Some(f64::from(bracket.with(|b| b.min_rating))));
    let max_rating = Signal::derive(move || Some(f64::from(bracket.with(|b| b.max_rating))));
    let update = move |f: &dyn Fn(&mut Bracket)| {
        let mut b = bracket.get_untracked();
        f(&mut b);
        state.update_bracket(index, b);
    };
    view! {
        <div>
            <RgButton label="-" custom_classes=app_style::MARGIN_END click=move |()| state.remove_bracket(index)/>
            <RgInput label="Bracket Name" value=name custom_classes=app_style::MARGIN_BOT listener=move |v: String| update(&|b| b.name = v.clone())/>
        </div>
        <div>
            <RgInput label="Bracket Description" value=description custom_classes=app_style::MARGIN_BOT listener=move |v: String| update(&|b| b.description = Some(v.clone()))/>
        </div>
        <div class="flex-auto flex justify-between">
            <RgNumberInput label="Min Rating" value=min_rating number_type=NumberType::NumberInt listener=move |n: f64| update(&|b| b.min_rating = n as f32)/>
            <RgNumberInput label="Max Rating" value=max_rating number_type=NumberType::NumberInt listener=move |n: f64| update(&|b| b.max_rating = n as f32)/>
        </div>
    }
}
