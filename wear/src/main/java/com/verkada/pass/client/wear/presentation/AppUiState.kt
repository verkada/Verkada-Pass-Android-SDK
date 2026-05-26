package com.verkada.pass.client.wear.presentation

import androidx.compose.runtime.Immutable

enum class ButtonState { Idle, Loading }

enum class StepState { Waiting, InProgress, Complete }

@Immutable
data class SdkInitUiState(
    val step1: StepState = StepState.InProgress,
    val challenge: String? = null,
    val step2: StepState = StepState.Waiting,
    val exchangeButtonState: ButtonState = ButtonState.Idle,
)

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
    data object BuildServiceNotification : UiEvent()
    data class ShowToast(val message: String) : UiEvent()
}
