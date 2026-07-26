# UI Alignment and Scroll Padding Fixes

This plan addresses two UI issues: content being hidden under the bottom navigation bar in the Orders screen and vertical alignment inconsistency between the Home and Orders screen headers.

## Proposed Changes

### [Component Name] UI - Orders Screen

#### [MODIFY] [OrdersScreen.kt](file:///E:/Android-Projects/Docuvio-App/app/src/main/java/com/docuvio/app/ui/orders/OrdersScreen.kt)
- **Fix Bottom Padding:** Add `bottom = 100.dp` to the `contentPadding` of all `LazyColumn` instances (loading skeletons and actual orders list). This ensures the last item is fully visible above the bottom navigation bar.
- **Fix Header Alignment:** Remove the `top = 16.dp` padding from the header `Row` to align it with the Home screen's header.

## Verification Plan

### Manual Verification
- **Orders Screen:** Scroll to the bottom and verify that the last order card is fully visible above the floating bottom bar.
- **Navigation Alignment:** Switch between **Home** and **Orders** tabs and verify that the top titles ("Available Shops" and "Orders") are vertically aligned.
- **Skeleton Screen:** Trigger a refresh (pull-to-refresh) and verify that the loading skeleton also respects the new bottom padding.
