# Verkada Pass Android SDK

The Verkada Pass Android SDK enables host applications to authenticate users against a Verkada organization and unlock doors and elevators over Bluetooth Low Energy.

## Requirements

- Android API 23+
- The host application must be annotated with `@HiltAndroidApp`

## Installation

Add the GitHub Packages repository and the SDK dependency to your project.

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
    implementation("com.verkada.android.pass.sdk:ble:0.1.0")
}
```


## Permissions

Request `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, and `BLUETOOTH_ADVERTISE` at runtime before calling `start()`.

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

Pull the list of readers the user is authorized to unlock from the Verkada backend and cache them locally.

```kotlin
VerkadaPassBle.fetchDevices(context)
    .onFailure { error ->
        when (error) {
            is FetchDevicesError.MissingOrganizationId -> { /* configure() was not called or did not complete successfully */ }
            is FetchDevicesError.Network -> { /* error.statusCode, error.message */ }
        }
    }
```

### 4. Start BLE

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

### 5. Stop BLE

Stop the foreground service when BLE is no longer needed.

```kotlin
VerkadaPassBle.stop(context)
```

### 6. Clear configuration

Clear all cached credentials. After this call `isConfigured` returns `false` and `configure()` must be called again before the next session.

```kotlin
VerkadaPassBle.clearConfiguration(context)
```

## API Reference

### `VerkadaPassBle`

| Method | Description |
|---|---|
| `generateChallenge(context)` | Generates a PKCE challenge. Forward the returned string to your backend to obtain an SDK token. |
| `configure(context, sdkToken, clientId, shard)` | Exchanges the SDK token for credentials and registers the device's BLE public key. |
| `isConfigured(context)` | Returns `true` if valid credentials are cached from a previous `configure()` call. |
| `fetchDevices(context)` | Fetches and caches the list of readers the user can unlock. |
| `start(context, notificationId, notification)` | Starts the BLE foreground service. |
| `stop(context)` | Stops the BLE foreground service. |
| `clearConfiguration(context)` | Clears all cached credentials and stops BLE. |

### `Shard`

| Value | Region |
|---|---|
| `Shard.US` | United States (default) |
| `Shard.EU` | Europe |
| `Shard.AU` | Australia |
| `Shard.GOV` | US Government |

### `SdkResult<T, E>`

All fallible operations return `SdkResult<T, E>`. Use the `onSuccess` and `onFailure` extension functions to handle each case.

```kotlin
result
    .onSuccess { value -> }
    .onFailure { error -> }
```
