# Contracts: navigation flow (spec 008)

**Feature**: `008-intuitive-navigation`
**Branch**: `008-intuitive-navigation`

The contracts are the function signatures the layers agree on. Only the
signatures named here change; nothing else in the app's API moves.

## 1. The shared navigation model (`core:domain`)

Two new members on the shared model, so the landing rule is domain data and
not an Android detail (ADR-0019's promise):

```kotlin
/** The add-food session, as one opening of the flow. */
public fun addFoodFromFromLibrary(code: String): String  // unchanged from 007

/**
 * The landing the flow's confirm control asks for.
 */
public fun NutriLogRoutes.addFoodSource(): String  // already present

/**
 * Whether the flow is open, read from the shell's own navigation state.
 */
public fun isFlowOpen(currentRoute: String): Boolean
```

## 2. The navigator (`androidApp/ui`)

The navigator carries out the decisions above, in carrier functions; the
screens never pop themselves:

```kotlin
public class NutriLogNavigator internal constructor(
    private val navController: NavHostController,
) : NutriLogNavigatorApi {
    override fun openAddFood(meal: String?, productCode: String?)  // unchanged
    override fun openAddFoodQuantity()                            // unchanged
    /** Landing: pops the flow to its own source step and keeps the meal. */
    override fun landAfterAddFood(mealName: String?)
    override fun openProduct(code: String)                        // unchanged
    override fun back()                                           // unchanged
    override fun switchTo(destination: NutriLogDestination)       // unchanged
}
```

## 3. The screens' own actions, reworded to what they meant

The "Add to diary" action of the product detail does not open the flow from
the beginning: it carries the food back to the flow that is open, or opens a
new one when there is none, which is a decision of the destination layer and
not of the detail screen:

```kotlin
public fun addProductToFlow(product: Product?, navigator: NutriLogNavigator)
```

## 4. Unchanged, and locked by tests

- `NutriLogRoutes`: the route strings are what 007 committed and what the
  instrumented tests run against; no route is renamed.
- `AddFoodViewModel`: the state's content is unchanged; only the session's
  semantics are exact.
- The shell's slots: the day, the library, More, the flow's two steps, the
  product detail and the week screen are the slots they were.

## 5. The tests' contract

The instrumented suite must contain, and its failure must be able to say which
one failed:

| Test | Locks |
| --- | --- |
| a food logged on the confirm lands on the flow's search, meal kept | FR-001, story 1 |
| the diary's meal sections show the entry the user logged, once | FR-005 |
| the equality back from the quantity step returns to the search step | FR-002, story 2 |
| the detail opened from a flow row returns to that step; the push carries the flow | FR-003 |
| the flow's first step's back returns to the screen it came from | FR-004 |
| a repeated tab tap is a no-op | FR-006 |
| the bar's visibility on the three roots and inside a child | FR-007 |
| the coach's close leaves the screen below as it was | FR-008 |
