//! The Classes screen (admin): the race classes with their brackets, their order, and the class editor.

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

/// The Classes screen state: the loaded classes, the class being edited and the sort mode.
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

    /// Returns one past the last class's sort value, for a new class.
    pub fn next_sort(&self) -> i32 {
        self.class_list
            .value()
            .and_then(|list| list.last().map(|c| c.race_class.sort + 1))
            .unwrap_or(0)
    }

    pub fn edit_class(&self, class: RaceClassBrackets) {
        self.edit_class.set(class);
    }

    /// Sets the edited class's rating type; a not rated type opens every bracket's rating range.
    pub fn set_rating_type(&self, rating_type: RatingType) {
        self.edit_class.update(|c| {
            c.race_class.rating_type = rating_type;
            if rating_type.is_cruising() {
                open_ranges(&mut c.brackets);
            }
        });
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

    /// Tells whether `class` can be saved.
    ///
    /// It needs a name, at least one bracket, every bracket named and, in a rated class, each bracket's max rating
    /// above its min rating.
    pub fn is_upsert_class_brackets_valid(&self, class: &RaceClassBrackets) -> bool {
        let rating_type = class.race_class.rating_type;
        !class.race_class.name.trim().is_empty()
            && !class.brackets.is_empty()
            && class
                .brackets
                .iter()
                .all(|b| !b.name.trim().is_empty() && is_range_valid(rating_type, b))
    }

    /// Saves `class` with its brackets, resets the editor, then reloads.
    ///
    /// A new class takes the next sort value; the brackets of an editable not rated class get the open rating range
    /// (the editor hides it).
    pub fn upsert_class_brackets(&self, mut class: RaceClassBrackets) {
        if class.race_class.rating_type.is_cruising() && class.race_class.number_of_races == 0 {
            open_ranges(&mut class.brackets);
        }
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

    /// Deletes every bracket of `class`, then the class, then reloads.
    ///
    /// A bracket still in use stops the deletion with the server's text.
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

    /// Saves the classes in their new order, then leaves sort mode and reloads.
    pub fn save_class_order(&self, order: Vec<RaceClass>) {
        self.sort_mode.set(false);
        let list = self.class_list;
        call(server_fns::post_class_list(order), move |r| {
            list.after(r.map(drop))
        });
    }
}

/// Sets every bracket to the open range, -1000 to 1000, that every rating falls in.
fn open_ranges(brackets: &mut [Bracket]) {
    let open = Bracket::default();
    for b in brackets {
        b.min_rating = open.min_rating;
        b.max_rating = open.max_rating;
    }
}

/// Tells whether a bracket's rating range is valid; a not rated class's range is hidden and opened on save, so it
/// passes.
fn is_range_valid(rating_type: RatingType, bracket: &Bracket) -> bool {
    rating_type.is_cruising() || bracket.max_rating > bracket.min_rating
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
    let fresh = Memo::new(move |_| edit.with(|c| c.race_class.number_of_races == 0));
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
                        {move || if fresh.get() {
                            Either::Left(view! {
                                <RgDropdown
                                    items=RatingType::ENTRIES.to_vec()
                                    selected_item=rating
                                    name=|t: &RatingType| t.label().to_string()
                                    handler=move |t: RatingType| state.set_rating_type(t)
                                />
                            })
                        } else {
                            Either::Right(edit.with(|c| format!("({} {} race(s) / cannot be edited)", c.race_class.rating_type.label(), c.race_class.number_of_races)))
                        }}
                    </div>
                    <h6>"Brackets"</h6>
                    <hr/>
                    // keyed by class, editability and index, so typing doesn't rebuild the inputs
                    <For
                        each=move || edit.with(|c| (0..c.brackets.len()).map(|i| (c.race_class.id, fresh.get(), i)).collect::<Vec<_>>())
                        key=|k| *k
                        children=move |(_, editable, i)| view! {
                            {if editable {
                                Either::Left(view! { <div class="mb-4"><EditBracket index=i state=state/></div> })
                            } else {
                                let bracket = edit.with_untracked(|c| c.brackets.get(i).cloned().unwrap_or_default());
                                Either::Right(view! { <div class="mb-4"><ViewBracket bracket=bracket/></div> })
                            }}
                            <hr/>
                        }
                    />
                    {move || fresh.get().then(|| view! {
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

/// The editor row of the bracket at `index` in the class being edited. Renders block elements: never inside `<p>`.
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
    let rated = Memo::new(move |_| {
        !state
            .edit_class
            .with(|c| c.race_class.rating_type.is_cruising())
    });
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
        {move || rated.get().then(|| view! {
            <div class="flex-auto flex justify-between">
                <RgNumberInput label="Min Rating" value=min_rating number_type=NumberType::NumberInt listener=move |n: f64| update(&|b| b.min_rating = n as f32)/>
                <RgNumberInput label="Max Rating" value=max_rating number_type=NumberType::NumberInt listener=move |n: f64| update(&|b| b.max_rating = n as f32)/>
            </div>
        })}
        {move || (!state.edit_class.with(|c| bracket.with(|b| is_range_valid(c.race_class.rating_type, b)))).then(|| view! {
            <p class="text-danger-fg">"Max rating must be greater than min rating"</p>
        })}
    }
}
