# Prod vs. local scoring, 2026-10-14

Every race report and series standings on production (`https://regatta.mxmariner.com`, the code before the
scoring fixes) compared with the same database served locally by the code after the fixes in
[`test_audit_and_plan.md`](test_audit_and_plan.md). Only boats whose place changed are listed.

**Bold rows** are boats whose *order* against another boat actually flipped: they were strictly ahead of a
boat on prod and are strictly behind it locally, or the reverse. The other rows moved only because a tie formed
or was broken, or because every boat shifted together.

Notes name the decision from the plan:

- **D1** RC volunteer average counted in the total, fractional and unrounded.
- **D2** an unsailed race scores nothing; the throw-out is chosen per bracket and per class.
- **D3** NSC and DNS score starters + 1, and an RC volunteer is not a starter.
- **D4** a penalty adds points to the boat's own place, others unchanged, capped at DNF.
- **D5** a tie on the last race goes back race by race; full ties share a place.
- **D8** one standings row per boat after a skipper change.
- *own total* means the boat's own total changed; *others* means it moved because other boats' totals did.

## Race results

| Race | Class / bracket | Boat | Prod | Local | Note |
|---|---|---|---|---|---|
| [2024 Early Spring Windseekers #1](https://regatta.mxmariner.com/races/results/view/18) | Windseekers FS / All | Waymaker | 3 | 4 | D4 |
|  | Windseekers FS / All | Blue Jay | 2 | 3 | D4 |
|  | Windseekers FS | Waymaker (class) | 3 | 4 | D4 |
|  | Windseekers FS | Blue Jay (class) | 2 | 3 | D4 |
| [2024 Late Spring Windseekers #4](https://regatta.mxmariner.com/races/results/view/26) | Fast (72-) / I | Kahuna | 4 | 3 | D4 |
|  | Fast (72-) | Kahuna (class) | 6 | 5 | D4 |
| [2024 Summer Series Windseekers #4](https://regatta.mxmariner.com/races/results/view/31) | Windseekers NFS / All | Gardyloo | 6 | 5 | D4 |
|  | Windseekers NFS | Gardyloo (class) | 6 | 5 | D4 |
| [2025 Early Spring Windseekers #3](https://regatta.mxmariner.com/races/results/view/55) | Half Fast (73+) / III | Obsession | 7 | 8 | D3 |
| [2025 Late Spring Windseekers #1](https://regatta.mxmariner.com/races/results/view/58) | Half Fast (73+) / IV | Bella Rose | 6 | 7 | D3 |
|  | Windseekers NFS / All | Thane | 4 | 5 | D3 |
|  | Windseekers NFS | Thane (class) | 4 | 5 | D3 |
| [2025 Late Spring Windseekers #2](https://regatta.mxmariner.com/races/results/view/59) | Half Fast (73+) / III | Sidewinder | 6 | 7 | D3 |
|  | Half Fast (73+) | Sidewinder (class) | 13 | 14 | D3 |
| [2025 Awards Race](https://regatta.mxmariner.com/races/results/view/79) | Half Fast (73+) / III | Keet | 5 | 6 | D3 |
|  | Half Fast (73+) | Keet (class) | 8 | 9 | D3 |
| [2026 Late Spring Windseekers #2](https://regatta.mxmariner.com/races/results/view/96) | Half Fast (73+) / III | Athena | 6 | 7 | D3 |
|  | Half Fast (73+) | Athena (class) | 12 | 13 | D3 |

## Series standings

| Series | Class / bracket | Boat | Prod place | Local place | Prod total | Local total | Note |
|---|---|---|---|---|---|---|---|
| [2024 Harbor Series](https://regatta.mxmariner.com/series/standings/view/1/2024) | Half Fast (73+) / III/IV | Work Release | 8 | 7 | 18 | 18 | D5 |
|  | Half Fast (73+) (class) | Work Release | 8 | 7 | 18 | 18 | D5 |
| [2025 Harbor Series](https://regatta.mxmariner.com/series/standings/view/1/2025) | Half Fast (73+) / III/IV | **Les Chevaux Blancs** | **7** | **8** | 17 | 17 | others: D1, D3; order flips with Blue Max |
|  | Half Fast (73+) / III/IV | **Boginka** | **6** | **7** | 17 | 17 | others: D1, D3; order flips with Blue Max |
|  | Half Fast (73+) / III/IV | **Blue Max** | **8** | **6** | 18 | 17 | own total: D3; order flips with Boginka, Les Chevaux Blancs |
|  | Half Fast (73+) (class) | **Les Chevaux Blancs** | **7** | **8** | 17 | 17 | others: D1, D3; order flips with Blue Max |
|  | Half Fast (73+) (class) | **Boginka** | **6** | **7** | 17 | 17 | others: D1, D3; order flips with Blue Max |
|  | Half Fast (73+) (class) | **Blue Max** | **8** | **6** | 18 | 17 | own total: D3; order flips with Boginka, Les Chevaux Blancs |
| [2024 Point Series](https://regatta.mxmariner.com/series/standings/view/18/2024) | Half Fast (73+) (class) | Strider | 14 | 13 | 39 | 39 | D5 |
| [2024 Early Spring Windseekers](https://regatta.mxmariner.com/series/standings/view/13/2024) | Half Fast (73+) (class) | **Work Release** | **8** | **9** | 29 | 29 | others: D2; order flips with Slick |
|  | Half Fast (73+) (class) | **Slick** | **9** | **8** | 30 | 29 | own total: D2; order flips with Work Release |
| [2025 Early Spring Windseekers](https://regatta.mxmariner.com/series/standings/view/13/2025) | Half Fast (73+) (class) | **Work Release** | **5** | **7** | 22 | 22 | others: D1, D3; order flips with Out of the Blue, Slick |
|  | Half Fast (73+) (class) | **Slick** | **6** | **5** | 24 | 21.33 | own total: D1, D3; order flips with Work Release |
|  | Half Fast (73+) (class) | **Out of the Blue** | **7** | **6** | 25 | 21.33 | own total: D1, D3; order flips with Work Release |
|  | Half Fast (73+) (class) | **Aleshanee** | **11** | **13** | 33 | 33 | others: D1, D3; order flips with Scoundrel, Waymaker |
|  | Half Fast (73+) (class) | **Scoundrel** | **13** | **11** | 34 | 32 | own total: D3; order flips with Aleshanee, Waymaker |
| [2026 Early Spring Windseekers](https://regatta.mxmariner.com/series/standings/view/13/2026) | Half Fast (73+) / III | **Bucket List** | **5** | **6** | 14 | 20 | own total: D2; order flips with Amidala |
|  | Half Fast (73+) / III | **Amidala** | **6** | **5** | 15 | 20 | own total: D2; order flips with Bucket List |
|  | Half Fast (73+) / IV | **Waymaker** | **6** | **5** | 12 | 16 | own total: D2, D3; order flips with Boginka |
|  | Half Fast (73+) / IV | **Work Release** | **2** | **3** | 5 | 10 | own total: D2; order flips with Zephyr |
|  | Half Fast (73+) / IV | **Boginka** | **5** | **6** | 10 | 16 | own total: D2; order flips with Waymaker |
|  | Half Fast (73+) / IV | **Zephyr** | **3** | **2** | 6 | 9 | own total: D2; order flips with Work Release |
|  | Half Fast (73+) (class) | **Waymaker** | **11** | **9** | 24 | 33 | own total: D2, D3; order flips with Bucket List, DYNO |
|  | Half Fast (73+) (class) | **Short Circuit** | **2** | **3** | 9 | 13 | own total: D2; order flips with Out of the Blue |
|  | Half Fast (73+) (class) | **Out of the Blue** | **3** | **2** | 10 | 12 | own total: D2; order flips with Short Circuit |
|  | Half Fast (73+) (class) | **Strider** | **5** | **4** | 12 | 15 | own total: D2; order flips with Zephyr |
|  | Half Fast (73+) (class) | **Zephyr** | **4** | **5** | 12 | 18 | own total: D2; order flips with Strider |
|  | Half Fast (73+) (class) | **Bucket List** | **10** | **12** | 23 | 34 | own total: D2, D3; order flips with Amidala, Waymaker |
|  | Half Fast (73+) (class) | **DYNO** | **9** | **10** | 23 | 34 | own total: D2; order flips with Waymaker |
|  | Half Fast (73+) (class) | **Amidala** | **12** | **11** | 24 | 34 | own total: D2; order flips with Bucket List |
| [2024 Late Spring Windseekers](https://regatta.mxmariner.com/series/standings/view/14/2024) | Half Fast (73+) (class) | **Obsession** | **10** | **9** | 34 | 33 | own total: D2; order flips with Sidewinder |
|  | Half Fast (73+) (class) | **Sidewinder** | **9** | **10** | 34 | 34 | others: D2; order flips with Obsession |
|  | Windseekers NFS / All | **Saoirse** | **5** | **4** | 12 | 12 | D5; order flips with Bella |
|  | Windseekers NFS / All | **Bella** | **4** | **5** | 12 | 12 | D5; order flips with Saoirse |
|  | Windseekers NFS (class) | **Saoirse** | **5** | **4** | 12 | 12 | D5; order flips with Bella |
|  | Windseekers NFS (class) | **Bella** | **4** | **5** | 12 | 12 | D5; order flips with Saoirse |
| [2025 Late Spring Windseekers](https://regatta.mxmariner.com/series/standings/view/14/2025) | Half Fast (73+) / III | **Keet** | **5** | **4** | 17 | 14.67 | own total: D1; order flips with Scoundrel |
|  | Half Fast (73+) / III | **Scoundrel** | **4** | **5** | 15 | 15 | others: D1; order flips with Keet |
|  | Half Fast (73+) (class) | **Waymaker** | **11** | **13** | 42 | 42 | others: D1, D2, D3; order flips with Boginka, Work Release |
|  | Half Fast (73+) (class) | **Boginka** | **13** | **11** | 43 | 41 | own total: D2; order flips with Waymaker, Work Release |
|  | Half Fast (73+) (class) | **Bella Rose** | **9** | **10** | 32 | 32 | others: D1, D2, D3; order flips with Keet |
|  | Half Fast (73+) (class) | **Strider** | **6** | **7** | 27 | 27 | others: D1, D2, D3; order flips with Keet |
|  | Half Fast (73+) (class) | **Keet** | **10** | **6** | 32 | 25.33 | own total: D1; order flips with Bella Rose, DYNO, Scoundrel, Strider |
|  | Half Fast (73+) (class) | **DYNO** | **7** | **8** | 28 | 28 | others: D1, D2, D3; order flips with Keet |
|  | Half Fast (73+) (class) | **Scoundrel** | **8** | **9** | 30 | 30 | others: D1, D2, D3; order flips with Keet |
|  | Windseekers NFS / All | **Thane** | **3** | **4** | 11 | 12 | own total: D3; order flips with Windswept |
|  | Windseekers NFS / All | **Windswept** | **4** | **3** | 12 | 12 | others: D1, D3; order flips with Thane |
|  | Windseekers NFS (class) | **Thane** | **3** | **4** | 11 | 12 | own total: D3; order flips with Windswept |
|  | Windseekers NFS (class) | **Windswept** | **4** | **3** | 12 | 12 | others: D1, D3; order flips with Thane |
| [2026 Late Spring Windseekers](https://regatta.mxmariner.com/series/standings/view/14/2026) | Half Fast (73+) (class) | **Work Release** | **7** | **6** | 25 | 22.67 | own total: D1; order flips with Zephyr |
|  | Half Fast (73+) (class) | **Zephyr** | **6** | **7** | 24 | 23 | own total: D2; order flips with Work Release |
| [2024 Summer Series Windseekers](https://regatta.mxmariner.com/series/standings/view/15/2024) | Fast (72-) (class) | **Flicka** | **3** | **4** | 14 | 14 | others: D2; order flips with Zulu Tango |
|  | Fast (72-) (class) | **Zulu Tango** | **4** | **3** | 15 | 13 | own total: D2; order flips with Flicka |
| [2026 Summer Series Windseekers](https://regatta.mxmariner.com/series/standings/view/15/2026) | Fast (93-) / II | **Flicka** | **2** | **3** | 9 | 9 | others: D1, D8; order flips with Goes to 11 |
|  | Fast (93-) / II | **Goes to 11** | **3** | **2** | 9 | 6.67 | own total: D1; order flips with Flicka |
|  | Fast (93-) / II | **Short Circuit** | **5** | **4** | 18 | 14 | D8 row merge; order flips with Bucket List |
|  | Fast (93-) / II | **Bucket List** | **4** | **5** | 15 | 15 | others: D1, D8; order flips with Short Circuit |
|  | Fast (93-) / II | Athena | 8 | 7 | 24 | 24 | others: D1, D8 |
|  | Fast (93-) / II | DYNO | 9 | 8 | 25 | 25 | others: D1, D8 |
|  | Fast (93-) / II | Amidala | 7 | 6 | 23 | 23 | others: D1, D8 |
|  | Fast (93-) (class) | **Kahuna** | **3** | **2** | 12 | 10 | own total: D2; order flips with Grace E |
|  | Fast (93-) (class) | **Short Circuit** | **9** | **8** | 31 | 25 | D8 row merge; order flips with Jeopardy |
|  | Fast (93-) (class) | **Jeopardy** | **8** | **9** | 26 | 26 | others: D1, D2, D8; order flips with Short Circuit |
|  | Fast (93-) (class) | **Grace E** | **2** | **3** | 10 | 10 | others: D1, D2, D8; order flips with Kahuna |
|  | Fast (93-) (class) | Athena | 13 | 12 | 41 | 41 | others: D1, D2, D8 |
|  | Fast (93-) (class) | DYNO | 14 | 13 | 42 | 42 | others: D1, D2, D8 |
|  | Fast (93-) (class) | Amidala | 12 | 11 | 39 | 39 | others: D1, D2, D8 |
|  | Half Fast (94+) (class) | **Slick** | **2** | **1** | 10 | 9 | own total: D2; order flips with Sidewinder |
|  | Half Fast (94+) (class) | **Klatawa** | **5** | **6** | 19 | 19 | others: D1, D2, D3; order flips with Zephyr |
|  | Half Fast (94+) (class) | **Zephyr** | **6** | **5** | 19 | 17 | own total: D2; order flips with Klatawa |
|  | Half Fast (94+) (class) | **Sidewinder** | **1** | **2** | 9 | 9 | others: D1, D2, D3; order flips with Slick |
| [2025 Three Hour Tour](https://regatta.mxmariner.com/series/standings/view/16/2025) | Half Fast (73+) / IV | **Solitaire** | **4** | **3** | 10 | 9 | own total: D3; order flips with Slick |
|  | Half Fast (73+) / IV | **Slick** | **3** | **4** | 10 | 10.50 | own total: D1; order flips with Solitaire |
|  | Half Fast (73+) (class) | **Solitaire** | **9** | **8** | 21 | 20 | own total: D3; order flips with Slick |
|  | Half Fast (73+) (class) | **Slick** | **8** | **9** | 21 | 21 | others: D3; order flips with Solitaire |
| [2024 Late Summer Windseekers](https://regatta.mxmariner.com/series/standings/view/17/2024) | Fast (72-) (class) | **Zulu Tango** | **3** | **4** | 13 | 13 | others: D2; order flips with Gardyloo |
|  | Fast (72-) (class) | **Gardyloo** | **4** | **3** | 13 | 12 | own total: D2; order flips with Zulu Tango |
|  | Half Fast (73+) (class) | **Skosh** | **13** | **12** | 36 | 36 | D5; order flips with Aleshanee |
|  | Half Fast (73+) (class) | **Aleshanee** | **12** | **13** | 36 | 36 | D5; order flips with Skosh |
| [2025 Late Summer Windseekers](https://regatta.mxmariner.com/series/standings/view/17/2025) | Fast (72-) / I | **Flash** | **3** | **4** | 10 | 10 | D5; order flips with Zulu Tango |
|  | Fast (72-) / I | **Zulu Tango** | **4** | **3** | 10 | 10 | D5; order flips with Flash |
|  | Half Fast (73+) / IV | **Slick** | **3** | **1** | 9 | 5.33 | own total: D1; order flips with Strider, Zephyr |
|  | Half Fast (73+) / IV | **Strider** | **1** | **2** | 8 | 8 | others: D1, D3; order flips with Slick |
|  | Half Fast (73+) / IV | **Zephyr** | **2** | **3** | 8 | 8 | others: D1, D3; order flips with Slick |
|  | Half Fast (73+) (class) | **Goes to 11** | **2** | **3** | 12 | 10.67 | own total: D1; order flips with Slick |
|  | Half Fast (73+) (class) | **Slick** | **3** | **2** | 16 | 8 | own total: D1; order flips with Goes to 11 |
| [2026 Late Summer Windseekers](https://regatta.mxmariner.com/series/standings/view/17/2026) | Fast (93-) / II | **Goes to 11** | **5** | **3** | 14 | 12 | own total: D1; order flips with Bucket List, Short Circuit |
|  | Fast (93-) / II | **Short Circuit** | **3** | **4** | 12 | 12 | others: D1, D3; order flips with Goes to 11 |
|  | Fast (93-) / II | **Bucket List** | **4** | **5** | 13 | 13 | others: D1, D3; order flips with Goes to 11 |

8 race reports and 15 series standings differ; the order of boats flips in 0 race reports and 13 series standings.

Generated by comparing the two APIs boat by boat; notes come from `database/audit_scoring.py` run on the
outputs of the original code. The local database is a clone of production taken 2026-10-14, so every
difference is a scoring change.
