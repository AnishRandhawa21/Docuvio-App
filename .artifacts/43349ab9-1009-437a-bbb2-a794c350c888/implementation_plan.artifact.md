# Replace All Fonts with Manrope

The goal is to unify the typography of the Docuvio app by replacing all existing fonts (Impact, Montserrat, Bebasneue, Inter, Thunder) with the "Manrope" font family and removing the old font files.

## User Review Required

> [!IMPORTANT]
> This change will remove the following fonts from the project:
> - Impact
> - Montserrat
> - Bebasneue
> - Inter
> - Thunder
>
> All text previously using these fonts will now use **Manrope** with appropriate weights.

## Proposed Changes

### Theme & Typography

#### [MODIFY] [Type.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/theme/Type.kt)
- Define the `Manrope` `FontFamily` mapping the available TTF files to `FontWeight`s.
- Update the `Typography` object to use `Manrope` as the default `fontFamily`.
- Remove definitions for `ImpactFont`, `Montserrat`, `Bebasneue`, `Inter`, and `Thunder`.

#### [MODIFY] [Theme.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/theme/Theme.kt)
- Remove the redundant/no-op `Typography(...)` calls that explicitly reference `ImpactFont` and `Montserrat`.

### UI Screens

#### [MODIFY] [TermsScreen.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/ui/terms/TermsScreen.kt)
- Remove explicit `fontFamily = Montserrat` usage.

#### [MODIFY] [HomeScreen.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/ui/home/HomeScreen.kt)
- Remove explicit `fontFamily = Inter` usage and corresponding import.

#### [MODIFY] [OrdersScreen.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/ui/orders/OrdersScreen.kt)
- Remove explicit `fontFamily = Inter` usage and corresponding import.

### Resources

#### [DELETE] Font Files
- `app/src/main/res/font/impact.ttf`
- `app/src/main/res/font/thunder.ttf`
- `app/src/main/res/font/intertight_black.ttf`
- `app/src/main/res/font/montserrat_black.ttf`
- `app/src/main/res/font/bebasneue_regular.ttf`

## Verification Plan

### Automated Tests
- Perform a clean build to ensure no broken references remain.
- `gradlew assembleDebug`

### Manual Verification
- Inspect the UI in the emulator to ensure Manrope is applied everywhere.
- Verify that `Terms & Conditions` header and `Available Shops` title look correct with the new font.
