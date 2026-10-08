# Verkada Pass Android SDK

The Verkada Pass Android SDK enables host applications to authenticate users against a Verkada organization and unlock doors and elevators over Bluetooth Low Energy or via remote unlock.

## Requirements

- **Platform:** Android API 23+
- **Language:** Kotlin
- **Distribution:** Maven (GitHub Packages)

## Installation

The Verkada Pass SDK requires a dedicated API token. Contact [Verkada support](mailto:support@verkada.com) to request one.

Once you have a token, add the GitHub Packages repository to your project, then add the SDK as a dependency.

In `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://www.jitpack.io") }
        maven {
            url = uri("https://maven.pkg.github.com/verkada/Verkada-Pass-Android-SDK")
            credentials {
                username = "x-access-token"
                password = "<GITHUB_TOKEN>"
            }
        }
    }
}
```

In your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.verkada.android.pass.sdk:ble:1.0.1")
}
```


## Permissions

The SDK declares its Bluetooth and location permissions in its manifest; your app is responsible for requesting the runtime ones.

| Permission | Purpose |
|---|---|
| `BLUETOOTH_SCAN` | Discover nearby Verkada readers. Request before calling `start()`. |
| `BLUETOOTH_CONNECT` | Connect to readers and advertise as a BLE credential. Request before calling `start()`. |
| `BLUETOOTH_ADVERTISE` | Advertise the device as a Verkada Pass credential. Request before calling `start()`. |
| `ACCESS_FINE_LOCATION` | Enforce geofencing on remote unlocks. Only required if your organization has geofencing enabled; request before calling `unlockDevice()`. |

`ACCESS_COARSE_LOCATION` is also declared in the manifest but doesn't need to be requested separately if `ACCESS_FINE_LOCATION` is granted.

## Usage

### 1. Generate a challenge

Before configuring, generate a PKCE challenge and send it to your backend to obtain an SDK token.

```kotlin
val challenge = VerkadaPassBle.generateChallenge(context)
// Forward `challenge` to your backend to obtain an SDK token
```

### 2. Configure

Exchange the SDK token for credentials. Call this once per session before any other SDK method.

```kotlin
VerkadaPassBle.configure(
    context = context,
    sdkToken = "<sdk-token>",
    clientId = "<client-id>",
    shard = Shard.US,
)
    .onSuccess {
        // SDK is ready
    }
    .onFailure { error ->
        when (error) {
            is ConfigureError.MissingCodeVerifier -> { /* generateChallenge() was not called or did not complete successfully */ }
            is ConfigureError.MissingOrganizationId -> { /* token did not resolve an org */ }
            is ConfigureError.MissingUserId -> { /* token did not resolve a user */ }
            is ConfigureError.Network -> { /* error.statusCode, error.message */ }
        }
    }
```

On subsequent launches, check `isConfigured` to skip the configure step:

```kotlin
if (VerkadaPassBle.isConfigured(context)) {
    // proceed directly to start()
}
```

### 3. Fetch devices

Pull the list of readers the user is authorized to unlock from the Verkada backend and cache them locally. Returns a list of `DoorSection` objects, each grouping related `DoorRow` entries by building and floor.

```kotlin
VerkadaPassBle.fetchDevices(context)
    .onSuccess { sections ->
        for (section in sections) {
            // section.name    — display name, e.g. "HQ / Floor 2"
            // section.rows    — list of DoorRow (Door or Elevator)
            for (row in section.rows) {
                when (row) {
                    is DoorRow.Door -> {
                        // row.id, row.name
                        // row.imageUrl  — full URL to the door's display image, or null
                        // row.lockState — StateFlow<DoorRow.LockState>, observe for unlock progress
                    }
                    is DoorRow.Elevator -> {
                        // row.id, row.name
                        // row.lockState — StateFlow<DoorRow.LockState>
                    }
                }
            }
        }
    }
    .onFailure { error ->
        when (error) {
            is FetchDevicesError.MissingUserId -> { /* configure() was not called or did not complete successfully */ }
            is FetchDevicesError.MissingOrganizationId -> { /* configure() was not called or did not complete successfully */ }
            is FetchDevicesError.Network -> { /* error.statusCode, error.message */ }
            is FetchDevicesError.Aggregated -> {
                // One or more network requests failed but cached data is available.
                // error.cachedSections contains the last known reader list.
                // error.errors lists each individual failure.
            }
        }
    }
```

### 4. Unlock a device manually

Trigger a remote unlock for a specific door or elevator. Pass the `DoorRow` received from `fetchDevices`. The `DoorRow.lockState` flow updates automatically as the unlock progresses.

```kotlin
VerkadaPassBle.unlockDevice(context, doorRow)
    .onSuccess { duration ->
        // duration — seconds the lock will remain unlocked, or null if unspecified
        // doorRow.lockState emits Unlocked(duration) automatically
    }
    .onFailure { error ->
        when (error) {
            is RemoteUnlockError.UnlockInProgress -> { /* a previous unlock attempt is still running */ }
            is RemoteUnlockError.NoScheduledAccess -> { /* user has no active access schedule for this reader */ }
            is RemoteUnlockError.NoLocationPermission -> { /* ACCESS_FINE_LOCATION not granted; required when geofencing is enabled */ }
            is RemoteUnlockError.NoBuildingGeofenceRegion -> { /* reader's building has no geofence configured */ }
            is RemoteUnlockError.OutsideGeofenceRegion -> { /* device is outside the allowed geofence radius */ }
            is RemoteUnlockError.Network -> { /* error.statusCode, error.message */ }
            is RemoteUnlockError.MissingUserId -> { /* configure() was not called or did not complete successfully */ }
            is RemoteUnlockError.MissingOrganizationId -> { /* configure() was not called or did not complete successfully */ }
        }
    }
```

Observe `lockState` in your UI to react to unlock transitions:

```kotlin
// Compose
val lockState by doorRow.lockState.collectAsState()
when (lockState) {
    is DoorRow.LockState.Locked    -> { /* show unlock button */ }
    is DoorRow.LockState.Unlocking -> { /* show progress indicator */ }
    is DoorRow.LockState.Unlocked  -> { /* show unlocked state with (lockState as DoorRow.LockState.Unlocked).duration */ }
}
```

### 5. Start BLE

Start the foreground service that runs BLE scanning and advertising. Pass a notification that will be shown while the service is active.

```kotlin
VerkadaPassBle.start(
    context = context,
    notificationId = 1,
    notification = notification,
)
    .onFailure { error ->
        when (error) {
            is StartError.MissingUserId -> { /* configure() was not called or did not complete successfully */ }
        }
    }
```

Once started, the SDK automatically unlocks doors and elevators as the user approaches Verkada readers.

### 6. Observe nearby devices

While BLE is running, observe the doors and elevators currently detected in range. This reflects live proximity, independent of the automatic BLE unlock flow — useful for UI that shows "nearby" readers as the user walks around.

```kotlin
viewModelScope.launch {
    VerkadaPassBle.nearbyDevices(context).collect { section ->
        // section.rows — doors/elevators currently detected nearby
    }
}
```

### 7. Stop BLE

Stop the foreground service when BLE is no longer needed.

```kotlin
VerkadaPassBle.stop(context)
```

### 8. Clear configuration

Clear all cached credentials. After this call `isConfigured` returns `false` and `configure()` must be called again before the next session.

```kotlin
VerkadaPassBle.clearConfiguration(context)
    .onFailure { error ->
        when (error) {
            is ClearConfigurationError.Database -> { /* error.message, error.cause — failed to clear local database */ }
            is ClearConfigurationError.Keystore -> { /* error.message, error.cause — failed to clear encrypted credential storage */ }
            is ClearConfigurationError.Unknown -> { /* error.cause */ }
            is ClearConfigurationError.Aggregated -> { /* error.errors — more than one of the above failed */ }
        }
    }
```

## Unlock paths

The SDK supports two complementary unlock paths:

- **Manual (remote) unlock** — explicit, app-driven. The host calls `unlockDevice(context, doorRow)`, typically from a button tap. The SDK validates schedules and geofence, then asks the Verkada backend to release the lock. Use this when you want a visible Unlock action in your UI.
- **BLE unlock** — implicit, reader-driven, proximity-based. Once `start()` is running, the device advertises itself as a Verkada Pass credential. When the phone comes within range of a reader the user is permitted to unlock, the reader can release the lock automatically without any call from the app. Whether this happens, and how close the phone has to be, is governed by the reader's settings in Verkada Command. Use `nearbyDevices(context)` to observe which readers are currently in range if your UI needs to reflect that.

## API Reference

### `VerkadaPassBle`

| Method | Returns | Description |
|---|---|---|
| `generateChallenge(context)` | `String` | Generates a PKCE challenge. Forward the returned string to your backend to obtain an SDK token. |
| `configure(context, sdkToken, clientId, shard)` | `SdkResult<Unit, ConfigureError>` | Exchanges the SDK token for credentials and registers the device's BLE public key. |
| `isConfigured(context)` | `Boolean` | Returns `true` if valid credentials are cached from a previous `configure()` call. |
| `fetchDevices(context)` | `SdkResult<List<DoorSection>, FetchDevicesError>` | Fetches and caches the list of readers the user can unlock, grouped by building and floor. |
| `unlockDevice(context, doorRow)` | `SdkResult<Int?, RemoteUnlockError>` | Remotely unlocks the given door or elevator. Returns the unlock duration in seconds. Updates `doorRow.lockState` automatically. |
| `start(context, notificationId, notification)` | `SdkResult<Unit, StartError>` | Starts the BLE foreground service for automatic proximity unlock. |
| `nearbyDevices(context)` | `StateFlow<DoorSection>` | Emits the doors/elevators currently detected nearby over BLE. Reflects live proximity; independent of the automatic BLE unlock flow. |
| `stop(context)` | `Unit` | Stops the BLE foreground service. |
| `clearConfiguration(context)` | `SdkResult<Unit, ClearConfigurationError>` | Clears all cached credentials and stops BLE. |

### `Shard`

| Value | Region |
|---|---|
| `Shard.US` | United States (default) |
| `Shard.EU` | Europe |
| `Shard.AU` | Australia |
| `Shard.GOV` | US Government |
| `Shard.OHIO` | United States (Ohio) |

### `DoorSection`

| Property | Type | Description |
|---|---|---|
| `name` | `String` | Display name for the section, e.g. `"HQ / Floor 2"`. |
| `building` | `Building?` | The building this section belongs to, or `null` if uncategorised. |
| `floor` | `Floor?` | The floor this section belongs to, or `null` for elevator sections. |
| `rows` | `List<DoorRow>` | The doors or elevators in this section. |

### `Building`

| Property | Type | Description |
|---|---|---|
| `id` | `String` | Unique building identifier. |
| `name` | `String` | Display name. |
| `latitude` | `Double?` | Building latitude, or `null` if not set. |
| `longitude` | `Double?` | Building longitude, or `null` if not set. |

### `Floor`

| Property | Type | Description |
|---|---|---|
| `id` | `String` | Unique floor identifier. |
| `buildingId` | `String` | The building this floor belongs to. |
| `name` | `String?` | Display name, or `null` if unnamed. |
| `sortOrder` | `Int` | Position used to order floors within a building. |

### `DoorRow`

`DoorRow` is a sealed class with two subtypes:

**`DoorRow.Door`**

| Property | Type | Description |
|---|---|---|
| `id` | `String` | Unique door identifier. |
| `name` | `String` | Display name. |
| `imageUrl` | `String?` | Full URL to the door's display image, or `null` if unavailable. |
| `floorId` | `String?` | Floor the door belongs to. |
| `buildingId` | `String?` | Building the door belongs to. |
| `lockState` | `StateFlow<DoorRow.LockState>` | Current lock state. Observe to drive UI. |

**`DoorRow.Elevator`**

| Property | Type | Description |
|---|---|---|
| `id` | `String` | Unique elevator identifier. |
| `name` | `String` | Display name. |
| `buildingId` | `String?` | Building the elevator belongs to. |
| `floorIds` | `String?` | Floor(s) the elevator serves. |
| `lockState` | `StateFlow<DoorRow.LockState>` | Current lock state. Observe to drive UI. |

### `DoorRow.LockState`

| State | Description |
|---|---|
| `Locked` | Idle. No unlock in progress. |
| `Unlocking` | An unlock request is in flight. |
| `Unlocked(duration: Int)` | Lock released. `duration` is the number of seconds the lock will remain open. |

### `ConfigureError`

| Case | Description |
|---|---|
| `MissingCodeVerifier` | `generateChallenge()` was not called, or its result was not the one exchanged for this SDK token. |
| `MissingOrganizationId` | The SDK token did not resolve to an organization. |
| `MissingUserId` | The SDK token did not resolve to a user. |
| `Network(statusCode, message)` | The token exchange or BLE key registration request failed. |

### `FetchDevicesError`

| Case | Description |
|---|---|
| `MissingUserId` | `configure()` was not called, or did not complete successfully. |
| `MissingOrganizationId` | `configure()` was not called, or did not complete successfully. |
| `Network(statusCode, message)` | The request failed and no cached data is available. |
| `Aggregated(errors, cachedSections)` | One or more of the underlying requests failed, but `cachedSections` contains the last known reader list so the UI can still render something. `errors` lists each individual failure. |

### `RemoteUnlockError`

| Case | Description |
|---|---|
| `MissingUserId` | `configure()` was not called, or did not complete successfully. |
| `MissingOrganizationId` | `configure()` was not called, or did not complete successfully. |
| `UnlockInProgress` | A previous unlock attempt for this reader is still running. |
| `NoScheduledAccess` | The user has no active access schedule for this reader. |
| `NoLocationPermission` | `ACCESS_FINE_LOCATION` is not granted; required only when the organization has geofencing enabled. |
| `NoBuildingGeofenceRegion` | The reader's building has no geofence region configured. |
| `OutsideGeofenceRegion` | The device is outside the allowed geofence radius. |
| `Network(statusCode, message)` | The unlock request failed. |

### `StartError`

| Case | Description |
|---|---|
| `MissingUserId` | `configure()` was not called, or did not complete successfully. |

### `ClearConfigurationError`

| Case | Description |
|---|---|
| `Database(message, cause)` | Failed to clear the local reader/building/floor cache. |
| `Keystore(message, cause)` | Failed to clear the encrypted credential store. |
| `Unknown(cause)` | An unexpected error occurred while clearing state. |
| `Aggregated(errors)` | More than one of the above failed; `errors` lists each individual failure. |

### `SdkResult<T, E>`

All fallible operations return `SdkResult<T, E>`. Use the `onSuccess` and `onFailure` extension functions to handle each case.

```kotlin
result
    .onSuccess { value -> }
    .onFailure { error -> }
```

## License

See [LICENSE.txt](LICENSE.txt).
