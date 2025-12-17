# 🔧 Test NullPointerException Fix

## Problem

Tests were failing with `NullPointerException` at specific lines in the test methods.

## Root Cause

The `GeofenceNotificationUtils` was being called in the implementation but not mocked in the tests.
When the implementation tried to call methods like:

- `GeofenceNotificationUtils.showGeofenceErrorNotification()`
- `GeofenceNotificationUtils.showGeofenceEnterNotification()`
- `GeofenceNotificationUtils.showGeofenceExitNotification()`

It would fail because the object wasn't mocked, causing NullPointerException.

## Solution

Added mock configuration for `GeofenceNotificationUtils` in the `setUp()` method:

```kotlin
@Before
fun setUp() {
    // ... existing mocks ...
    
    // Mock GeofenceNotificationUtils to prevent null pointer exceptions
    mockkObject(GeofenceNotificationUtils)
    every { GeofenceNotificationUtils.showGeofenceErrorNotification(any(), any()) } just runs
    every { GeofenceNotificationUtils.showGeofenceEnterNotification(any(), any()) } just runs
    every { GeofenceNotificationUtils.showGeofenceExitNotification(any(), any()) } just runs
}
```

## Changes Made

**File:** `app/src/test/java/com/example/presentmate/geofence/GeofenceBroadcastReceiverTest.kt`

Added in `setUp()` method:

- `mockkObject(GeofenceNotificationUtils)` - Mock the singleton/object
- Three `every { GeofenceNotificationUtils.show*() } just runs` - Mock each notification method

## Result

✅ All tests should now pass without NullPointerException

## Commands to Verify

```bash
# Run the specific test class
./gradlew test --tests "com.example.presentmate.geofence.GeofenceBroadcastReceiverTest"

# Run all unit tests
./gradlew test

# Run with verbose output
./gradlew test --tests "com.example.presentmate.geofence.GeofenceBroadcastReceiverTest" -i
```

## Expected Output

```
GeofenceBroadcastReceiverTest > onReceive with enter transition should start a new session PASSED ✅
GeofenceBroadcastReceiverTest > onReceive with exit transition should end an ongoing session PASSED ✅
GeofenceBroadcastReceiverTest > onReceive with enter transition when session exists should not create new session PASSED ✅
GeofenceBroadcastReceiverTest > onReceive with exit transition when no session exists should not update PASSED ✅
GeofenceBroadcastReceiverTest > onReceive with geofencing error should not process event PASSED ✅

5 tests completed successfully ✅
```
