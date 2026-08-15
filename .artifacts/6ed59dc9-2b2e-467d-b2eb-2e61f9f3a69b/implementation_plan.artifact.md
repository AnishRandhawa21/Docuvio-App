# Replace Walk-in Order with Print Session

This plan outlines the steps to completely replace the legacy Walk-in Order feature with a new Print Session system, incorporating Supabase Realtime for live updates and maintaining the existing Razorpay payment flow.

## User Review Required

> [!IMPORTANT]
> - The project currently lacks Supabase dependencies and a QR scanner implementation. I will need to add these.
> - I will use the `io.github.jan-tennert.supabase:postgrest-kt` and `io.github.jan-tennert.supabase:realtime-kt` libraries for Supabase integration.
> - I will add `androidx.camera:camera-camera2` and `com.google.mlkit:barcode-scanning` for the QR scanner.

## Proposed Changes

### Phase 1: Cleanup Old Feature

#### [DELETE] [WalkInOrderScreen.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/ui/order/WalkInOrderScreen.kt)
#### [DELETE] [WalkInOrderViewModel.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/viewmodel/WalkInOrderViewModel.kt)
#### [DELETE] [WalkInOrderViewModelFactory.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/viewmodel/WalkInOrderViewModelFactory.kt)
#### [DELETE] [WalkInOrderRequest.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/data/model/WalkInOrderRequest.kt)
#### [DELETE] [WalkInFloatingpayBar.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/ui/order/utils/WalkInFloatingpayBar.kt)

#### [MODIFY] [OrderApi.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/data/api/OrderApi.kt)
- Remove `createWalkInOrder` and `attachWalkInDocument`.

#### [MODIFY] [OrderRepository.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/data/repository/OrderRepository.kt)
- Remove `createWalkInOrder` and `attachWalkInDocument`.

#### [MODIFY] [Routes.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/ui/navigation/Routes.kt)
- Replace `WalkInOrder` with `PrintSession` and `QRScanner`.

#### [MODIFY] [AppNavHost.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/ui/navigation/AppNavHost.kt)
- Update routes and replace `WalkInOrderScreen` with `PrintSessionScreen`.

---

### Phase 2: Add Dependencies & Infrastructure

#### [MODIFY] [libs.versions.toml](file:///E:/Android-Projects/Docuvio-App/gradle/libs.versions.toml)
- Add Supabase (Postgrest, Realtime) and CameraX/ML Kit versions and libraries.

#### [MODIFY] [build.gradle.kts](file:///E:/Android-Projects/Docuvio-App/app/build.gradle.kts)
- Apply new dependencies.

#### [NEW] [PrintSessionApi.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/data/api/PrintSessionApi.kt)
- Define Retrofit endpoints for Print Sessions.

#### [NEW] [PrintSessionModels.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/data/model/PrintSessionModels.kt)
- `PrintSession`, `PrintSessionFile`, `PrintSessionQuote`, etc.

---

### Phase 3: Implement Print Session Logic

#### [NEW] [PrintSessionRepository.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/data/repository/PrintSessionRepository.kt)
- Implement all session actions (start, connect, details, upload, payment).
- Integrate Supabase Realtime listener.

#### [NEW] [PrintSessionViewModel.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/viewmodel/PrintSessionViewModel.kt)
- Manage session state and realtime updates.

#### [NEW] [PrintSessionScreen.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/ui/printsession/PrintSessionScreen.kt)
- Multi-step Compose UI (Details -> Upload -> Review -> Quote -> Payment -> Status).

---

### Phase 4: QR & Navigation Integration

#### [NEW] [QRScannerScreen.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/ui/qr/QRScannerScreen.kt)
- CameraX-based scanner to extract shop code.

#### [MODIFY] [HomeScreen.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/ui/home/HomeScreen.kt)
- Update "Order Now" to navigate to `QRScannerScreen`.

## Verification Plan

### Automated Tests
- Build the project to ensure no compilation errors.
- Unit tests for `PrintSessionViewModel` state transitions (mocking repository).

### Manual Verification
1. Open app -> Home Screen.
2. Click "Order Now" -> Should open QR Scanner (mock or real).
3. Scan/Enter public code -> Should start session.
4. Enter Name/Phone -> Submit.
5. Pick files -> Upload.
6. Verify UI reacts to session status changes (simulated via Supabase Realtime if possible).
7. Test Razorpay payment flow.
8. Close app and reopen -> Verify session token persistence.
