package com.verkada.pass.client.mobile.ui.views

import androidx.compose.runtime.Immutable

@Immutable
data class SdkReadyUiState(
    val refreshButtonState: ButtonState = ButtonState.Idle,
    val resetButtonState: ButtonState = ButtonState.Idle,
)

@Immutable
sealed class AppUiState {
    data object Unknown : AppUiState()
    data class Initializing(val sdkInit: SdkInitUiState) : AppUiState()
    data class Ready(val state: SdkReadyUiState = SdkReadyUiState()) : AppUiState()
}

sealed class UiEvent {
    object BuildServiceNotification : UiEvent()
    data class ShowSnackbar(val message: String) : UiEvent()
}
