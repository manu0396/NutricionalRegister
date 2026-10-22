# Quickstart: spec 008

**Feature**: `008-intuitive-navigation`

## What is being done

The landing rules of the app's main work, so that adding food is one
continuous flow: a confirm returns to the flow's own search with the meal
still chosen, a detail opened from a flow row carries the flow further instead
of opening a second one, and no screen the user did not navigate to ever
appears.

## Build and run

```
./gradlew :androidApp:installDebug
adb shell am start -n com.example.nutricionalregister/.MainActivity
```

The Coach's assistant is not part of this feature; its development gateway is
started with `./gateway/start-dev.sh` and is a separate concern.

## What to test, by hand

1. Open a meal section's "Add" on the Today screen, pick a food from the
   search, open its detail, and confirm the amount: the flow's **search** is
   shown again, with the meal still chosen, and no detail is in sight.
2. Press back there: the Today screen, with the entry among the meal's
   sections.
3. Do it once more from the Library: the same landing, and the Library one
   back press away.
4. Tap this screen's tab of the bottom bar again: nothing changes.
5. Open the Coach over the flow and close it: the flow is exactly as it was.
6. Walk the flow with back at every step: each press is one step, with the
   choices kept.

## The gates

```
./gradlew :core:domain:testAndroidHostTest :feature:diary:testAndroidHostTest :coverageVerify
./gradlew :androidApp:ktlintCheck :kotlinLint :architectureTest :plainTextCheck
./gradlew :androidApp:connectedDebugAndroidTest
```
