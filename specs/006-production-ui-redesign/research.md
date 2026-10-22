# Research: Production-Grade UI Redesign

**Feature**: `006-production-ui-redesign`  
**Branch**: `006-production-ui-redesign`  
**Date**: 2026-10-21

## Problem Statement
The current UI still looks like a child game. Need a complete redesign to look like a production app ready to sell to a client, and fix navigation behavior issues (e.g., when navigating from Saved to Search, it should behave correctly/intuitively).

## Current State Assessment
- Using Material 3 but with playful/rounded elements
- Bottom nav, cards, icons are present but feel generic/childish
- Navigation behavior needs correction
- Need cleaner, more mature aesthetic

## Production App Design Principles
| Element | Current | Target |
|---|---|---|
| **Typography** | Friendly/rounded feel | Stronger hierarchy, cleaner weights, professional type scale |
| **Color Palette** | Green/amber playful palette | More sophisticated, restrained palette (could use cleaner neutrals + accent) |
| **Spacing** | Decent but not systematic | Tight, consistent 4pt grid, generous whitespace |
| **Cards/Surfaces** | Rounded, elevated | Cleaner cards, subtle borders or elevation, less bubbly |
| **Icons** | Mixed filled/outlined | More refined icon style, purposeful usage |
| **Navigation** | Standard bottom nav | Cleaner, more professional treatment |
| **Overall Feel** | Game/appetizer-like | Clean, mature, like a real product app (think modern food tracking apps like Cronometer, Lose It! pro style) |

## Navigation Behavior Issues to Fix
- When navigating from Saved tab to Search (e.g., via empty state CTA), need to ensure it lands correctly
- Product detail navigation should behave like a proper product page
- Back stack behavior needs to feel natural
- Tab switching should be clean

## Design Direction
- **Style**: Clean, minimal, professional (like modern fintech/health apps)
- **Typography**: Inter/SF-like system fonts, stronger weights for headings
- **Colors**: More neutral base, single strong accent color
- **Shapes**: Slightly less rounded (8-12dp) vs very round
- **Elevation**: Subtle, used purposefully
- **Layout**: More breathing room, cleaner sections

## References
- Modern production apps: Cronometer, MyFitnessPal (pro feel), Notion, Linear
- Material 3 but more restrained

## Constraints
- All existing functionality must work
- All tests must pass
- Accessibility must be maintained
- KMP architecture unchanged
