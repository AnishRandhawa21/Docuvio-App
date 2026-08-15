# Refactoring and Improvements Plan

This plan addresses the architectural and structural issues identified in the codebase analysis to improve maintainability, security, and performance without changing the core application logic.

## User Review Required

> [!IMPORTANT]
> The "Terms Acceptance" state will now persist across app restarts using `DataStore`. Users will only see the Terms screen once unless they clear app data.

## Proposed Changes

---

### 1. Build Configuration
#### [MODIFY] [app/build.gradle.kts](file:///E:/Android-Projects/Docuvio-App/app/build.gradle.kts)
- Remove duplicate entries for `androidx.datastore:datastore-preferences`.
- Remove duplicate `androidx.compose.foundation` dependency.
- Clean up redundant Compose core library declarations already covered by the BOM.

---

### 2. UI Layer Refactoring
#### [NEW] [MainComponents.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/ui/main/MainComponents.kt)
- Move `FixSystemBars` and `DocuvioLoadingAnimation` from `MainActivity.kt` to this new file.

#### [MODIFY] [MainActivity.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/MainActivity.kt)
- Clean up the file by removing the moved composables.
- Integrate `DataStore` for persistent "Terms Acceptance" state.

---

### 3. Data & Logic Refactoring
#### [MOVE] [DocxConverter.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/ui/order/utils/DocxConverter.kt) -> `com.docuvio.app.data.api.DocxConverter`
- Relocate the converter to the data layer.

#### [MODIFY] [CreateOrderViewModel.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/viewmodel/CreateOrderViewModel.kt)
- Remove the unused and duplicate `convertDocxToPdf` method.
- Update references to the moved `DocxConverter`.

#### [MODIFY] [TokenManager.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/core/auth/TokenManager.kt)
- Add a new preference key and methods to save/get the `hasAcceptedTerms` status.

---

## Verification Plan

### Automated Tests
- Run existing unit tests (if any) to ensure no regressions in repository logic.
- Verify the build completes successfully after dependency cleanup: `./gradlew assembleDebug`

### Manual Verification
1.  **Persistence**: Open the app, accept terms, close the app, and reopen. The terms screen should not reappear.
2.  **Conversion**: Test the DOCX to PDF conversion in the "Create Order" flow to ensure the refactored utility still works correctly.
3.  **UI**: Verify that system bar colors and the loading animation still appear correctly after being moved.
