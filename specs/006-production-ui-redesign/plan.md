# Implementation Plan: Production-Grade UI Redesign

**Feature**: `006-production-ui-redesign`  
**Branch**: `006-production-ui-redesign`  
**Date**: 2026-10-21

## Summary
Complete redesign to production-grade, sellable appearance. Remove all child/game aesthetic, use mature design language, fix navigation behaviors.

## Approach

### Phase 1: Design System Overhaul
- **Theme colors**: More sophisticated palette - cleaner neutrals, stronger accent
- **Typography**: Refined type scale with better weights
- **Shapes**: Less rounded, more professional (8-12dp vs 16-24dp)
- **Spacing**: Enforce strict 4pt grid
- **Elevation/Surfaces**: More subtle, use borders where appropriate

### Phase 2: Navigation Behavior Fixes
- Fix navigation from Saved -> Search
- Ensure proper back stack behavior
- Make tab navigation feel natural and correct
- Fix any navigation edge cases

### Phase 3: Screen Redesigns (Complete Overhaul)
- **Shell/Bottom Nav**: Cleaner, more professional
- **Search**: Production-grade search interface
- **Product Detail**: Like a real product page
- **Saved**: Clean, professional list
- **Diary**: Polished, intentional
- **Purchase**: Trustworthy, clean

### Phase 4: Testing & Polish
- All tests must pass
- Manual QA
- Dark mode verification

## Key Files
- Theme: `ui/theme/*`
- Navigation: `AppShell.kt`, `NutriLogBottomBar.kt`
- All screens as before
