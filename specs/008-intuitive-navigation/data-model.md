# Data Model: Intuitive navigation (spec 008)

**Feature**: `008-intuitive-navigation`
**Branch**: `008-intuitive-navigation`

Nothing is added to storage. The data this feature is about is the shape of
the navigation stack and the state the add-food flow carries while it is open.

## The flow's state (unchanged in content, exact in its lifetime)

| Field | Meaning | Lifetime |
| --- | --- | --- |
| `sessionId` | one opening of the flow; the nested graph's own entry id | the opening |
| `source` | search / library / scan | the opening |
| `query` | what the search box contains | the opening |
| `mealType` | the meal the food will be logged into | the opening |
| `product` | the chosen food | survives the flow's steps |
| `quantityGrams` | the amount, once a product is chosen | the quantity step |
| `preview` | the nutrition of the amount | the quantity step |
| `isMealSheetOpen` | the meal sheet | the quantity step |
| `isResolvingProduct` | the code's lookup in flight | the opening |

The **rule** the fix makes exact: a new session id resets the state; the same
session id keeps it and fills only what is still missing. A detail opened from
a flow row must therefore never produce a new session id, or the user's
search, results and meal are gone on the way back.

## The routes (unchanged from ADR-0019, with the new rule named)

| Route | What it is | Its back control means |
| --- | --- | --- |
| `today` | the diary | the system's own back (the app's first screen) |
| `library` | the personal library | the system's own back |
| `more` | goals, coach, premium, privacy | the system's own back |
| `addfood/source` | the food is looked for: search, library, scan | the screen the flow came from |
| `addfood/quantity` | the amount and the meal are confirmed | the flow's source step |
| `product/{code}` | one food's detail | the screen (or the flow's step) it was opened from |
| coach, weekly | the assistant's dialog and the week screen | the screen it was opened from |

## The new facts the rule adds

| Fact | Where it lives |
| --- | --- |
| **A confirm lands on the flow's source step**, with the meal re-selected | the navigation layer's landing rule (ADR-0021) |
| **A detail opened from a flow row carries the flow further**, instead of opening a second flow | the detail's "Add to diary" action, using the flow that is already open |
| **A back press is one step** | the shell's pop, already the case; locked by tests |
| **A repeated tab tap is a no-op** | the shell's destination switch: one rule, one decision |

## The relationships

- The flow's session is the graph entry's id, so the state's lifetime and the
  stack's shape cannot disagree: a reopen makes a new entry (a reset), and a
  step carried out inside one opening reuses the same entry (no reset).
- The landing rule uses the flow's own *source* route, which is a route the
  graph declares, so the same rule can be carried out by any shell that reads
  the shared navigation model.
- The detail's "Add to diary" is the one action whose *target* depends on
  whether a flow is already open; the destination layer owns the answer.
