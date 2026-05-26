package com.verkada.pass.client.wear.presentation

import android.Manifest
import android.app.Notification
import android.content.Context
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verkada.android.pass.sdk.ble.VerkadaPassBle
import com.verkada.android.pass.sdk.data.api.results.ConfigureError
import com.verkada.android.pass.sdk.data.api.results.FetchDevicesError
import com.verkada.android.pass.sdk.data.api.results.onFailure
import com.verkada.android.pass.sdk.data.api.results.onSuccess
import com.verkada.android.pass.sdk.data.models.Shard
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WearClientViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppUiState>(AppUiState.Unknown)
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (VerkadaPassBle.isConfigured(context)) {
                _uiState.value = AppUiState.Ready()
            } else {
                _uiState.value = AppUiState.Initializing(SdkInitUiState())
                generateChallenge()
            }
        }
    }

    private fun generateChallenge() {
        updateInitState { it.copy(step1 = StepState.InProgress) }
        viewModelScope.launch {
            delay(3000) // @TODO: Remove
            val challenge = VerkadaPassBle.generateChallenge(context)
            updateInitState {
                it.copy(
                    step1 = StepState.Complete,
                    challenge = challenge,
                    step2 = StepState.InProgress,
                )
            }
        }
    }

    fun exchangeToken(sdkToken: String) {
        updateInitState { it.copy(exchangeButtonState = ButtonState.Loading) }
        viewModelScope.launch {
            delay(3000) // @TODO: Remove
            VerkadaPassBle.configure(
                context = context,
                clientId = context.packageName,
                sdkToken = sdkToken,
                shard = Shard.US,
            )
                .onSuccess {
                    _uiState.value = AppUiState.Ready()
                }
                .onFailure { error ->
                    when (error) {
                        is ConfigureError.MissingCodeVerifier -> Toast.makeText(
                            context,
                            "Error configuring SDK: Missing code verifier",
                            Toast.LENGTH_LONG,
                        ).show()
                        is ConfigureError.Network -> Toast.makeText(
                            context,
                            "Network error configuring SDK: ${error.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    updateInitState { it.copy(exchangeButtonState = ButtonState.Idle) }
                }
        }
    }

    fun refreshDoors() {
        updateReadyState { it.copy(refreshButtonState = ButtonState.Loading) }
        viewModelScope.launch {
            delay(3000) // @TODO: Remove
            VerkadaPassBle.fetchDevices(context)
                .onFailure { error ->
                    when (error) {
                        is FetchDevicesError.MissingOrganizationId -> Toast.makeText(
                            context,
                            "Error fetching devices: Missing organization ID",
                            Toast.LENGTH_LONG,
                        ).show()
                        is FetchDevicesError.Network -> Toast.makeText(
                            context,
                            "Network error fetching devices: ${error.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            updateReadyState { it.copy(refreshButtonState = ButtonState.Idle) }
        }
    }

    fun resetSdk() {
        updateReadyState { it.copy(resetButtonState = ButtonState.Loading) }
        viewModelScope.launch {
            delay(3000) // @TODO: Remove
            VerkadaPassBle.stop(context)
            VerkadaPassBle.clearConfiguration(context)
            _uiState.value = AppUiState.Initializing(SdkInitUiState())
            generateChallenge()
        }
    }

    private fun updateInitState(update: (SdkInitUiState) -> SdkInitUiState) {
        val current = _uiState.value as? AppUiState.Initializing ?: return
        _uiState.value = current.copy(sdkInit = update(current.sdkInit))
    }

    private fun updateReadyState(update: (SdkReadyUiState) -> SdkReadyUiState) {
        val current = _uiState.value as? AppUiState.Ready ?: return
        _uiState.value = current.copy(state = update(current.state))
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT])
    fun startBleService(notification: Notification) {
        VerkadaPassBle.start(
            context = context,
            notificationId = 1,
            notification = notification,
        )
    }
}
