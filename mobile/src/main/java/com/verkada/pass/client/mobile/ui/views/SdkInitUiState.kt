package com.verkada.pass.client.mobile.ui.views

import androidx.compose.runtime.Immutable

enum class StepState { Waiting, InProgress, Complete }

@Immutable
data class SdkInitUiState(
    val step1: StepState = StepState.InProgress,
    val challenge: String? = null,
    val step2: StepState = StepState.Waiting,
    val exchangeButtonState: ButtonState = ButtonState.Idle,
)
