# ADR-0021: The add-food flow lands on its own search, and a detail carries it further

**Status**: Proposed
**Date**: 2026-10-22
**Related**: ADR-0015 (navigation stays Android-Compose-only), ADR-0019 (the
Tracker Shell's navigation model), spec 008.

## Context

ADR-0019 put the app's destinations in order: three roots, a nested add-food
graph, the detail as a route, back that means one step. Two behaviours survived
that work and still confuse the user, both on the one path the app's work
actually follows:

1. **A confirm lands on a screen that was already left.** The detail open from
   a flow row pushes a new opening of the flow ("Add to diary" navigates a
   second time to an add-food route with the product's code), so confirming the
   amount pops one entry and presents the detail again: the product the user
   just logged is presented as if they had just opened it.
2. **A detail's round trip clones the flow.** The second opening the detail's
   action causes creates a second `addfood` graph entry with a new session id,
   which resets the flow's state: the search the user had typed, the results
   and the meal are gone when they come back.

Both are one cause: the flow's confirm control and the detail's "Add to diary"
both decide where to land from *their own* point of view, instead of one rule
about where the work continues.

## Decision

The landing is **navigation's decision**, in the navigation layer, and the
screens ask for it instead of deciding it themselves:

- **A confirm lands on the flow's own source step.** The flow's graph is
  popped to its own start, keeping the flow's state (the meal reselected), so
  the work continues where it was: the same meal's next food, one search away.
  No entry above the flow's steps survives the confirm, and the screen the flow
  came from stays where it is until the user leaves the flow itself.
- **A detail opened from a flow row carries the flow further.** The detail's
  "Add to diary" action does not navigate to a second add-food route: it
  returns to the flow that is already open, with the product's code, which
  keeps one session, one stack of steps, and one walk back. A detail opened
  outside a flow opens the flow from the beginning, as today.
- **Back keeps its 0019 meaning**: one step, to the entry that precedes, with
  no state saved or restored, and a repeated tab's tap is a no-op.

## Consequences

- The main work (look for a food, log it, go on) is one continuous flow: the
  search is where every confirm returns, and the meal is never lost.
- The stack cannot grow without the user's action: a confirm pops the flow's
  own steps, and a detail's round trip adds none, so the "detail resurrects"
  class of defect has no path back into the app.
- The flow's state model needs its session rule spelled exactly: the round
  trip must neither reset the flow (a new session) nor clone it (a second
  entry), and the covered unit tests are the reason the rule is safe.
- The screens stay stateless: the landing is the navigator's, so a future
  native SwiftUI shell can carry out the same rule from the same shared model.
