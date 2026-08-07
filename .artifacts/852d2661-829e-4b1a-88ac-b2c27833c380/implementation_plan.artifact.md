# Refactor Hardcoded Colors for Home Screen

This plan outlines the steps to refactor hardcoded colors in the Home Screen and organize the theme files (`Color.kt`, `Theme.kt`, `Type.kt`).

## Proposed Changes

### Theme Organization

#### [MODIFY] [Color.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/theme/Color.kt)
- Add missing `LimeGreen` declaration.
- Add specific semantic colors for Shop Cards and 3D Buttons to avoid hardcoding.

#### [MODIFY] [Theme.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/theme/Theme.kt)
- Move `LightColorScheme` from `Type.kt` to `Theme.kt`.
- Properly apply `LightColorScheme` in `LovelyPrintsTheme`.
- Implement a custom color scheme extension (optional but recommended for non-Material colors like 3D buttons) or just use the semantic names from `Color.kt`.

#### [MODIFY] [Type.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/theme/Type.kt)
- Remove `LightColorScheme` (moved to `Theme.kt`).
- Ensure typography uses theme colors where possible.

### Home Screen Refactoring

#### [MODIFY] [HomeScreen.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/ui/home/HomeScreen.kt)
- Replace all `Color(0xFF...)` hardcoded values with references from `MaterialTheme.colorScheme` or the newly defined semantic colors in `Color.kt`.
- Fix the `LimeGreen` import issue.

## Specific Color Mapping
| Original Color | Semantic Name | Usage |
| :--- | :--- | :--- |
| `0xFFBECC4D` | `LimeGreen` | Pull-to-refresh, Button highlights |
| `0xFF9CCC65` | `ActiveCardStart` | Shop Card Gradient (Active) |
| `0xFF7CB342` | `ActiveCardEnd` | Shop Card Gradient (Active) |
| `0xFFE0E0E0` | `InactiveCardStart` | Shop Card Gradient (Inactive) |
| `0xFFBDBDBD` | `InactiveCardEnd` | Shop Card Gradient (Inactive) |
| `0xFF616161` | `TextDisabled` | Shop name (Inactive) |
| `0xFF9E9E9E` | `TextDisabledSecondary` | Shop Level (Inactive) |
| `0xFF2E7D32` | `SuccessGreen` | Time Banner Text |
| `0xFF4A7C20` | `ButtonShadowEnabled` | 3D Button Depth |
| `0xFF1B5E20` | `ButtonTextEnabled` | 3D Button Text |

## Verification Plan

### Automated Tests
- Run the build to ensure no compilation errors after refactoring.
- Check if all screens still look the same visually.

### Manual Verification
- Deploy the app and navigate to the Home Screen.
- Verify that the Shop Cards, Search Bar, and 3D Buttons have the correct colors.
- Toggle between active and inactive shops to verify gradient and text color changes.
