package com.verkada.pass.client.mobile

import android.Manifest
import android.app.Notification
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verkada.android.pass.sdk.ble.VerkadaPassBle
import com.verkada.android.pass.sdk.data.api.results.ConfigureError
import com.verkada.android.pass.sdk.data.api.results.FetchDevicesError
import com.verkada.android.pass.sdk.data.api.results.StartError
import com.verkada.android.pass.sdk.data.api.results.onFailure
import com.verkada.android.pass.sdk.data.api.results.onSuccess
import com.verkada.android.pass.sdk.data.models.Shard
import com.verkada.pass.client.mobile.ui.views.AppUiState
import com.verkada.pass.client.mobile.ui.views.ButtonState
import com.verkada.pass.client.mobile.ui.views.SdkInitUiState
import com.verkada.pass.client.mobile.ui.views.SdkReadyUiState
import com.verkada.pass.client.mobile.ui.views.StepState
import com.verkada.pass.client.mobile.ui.views.UiEvent
import com.verkada.pass.client.mobile.ui.views.UiEvent.BuildServiceNotification
import com.verkada.pass.client.mobile.ui.views.UiEvent.ShowSnackbar
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppUiState>(AppUiState.Unknown)
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (VerkadaPassBle.isConfigured(context)) {
                _uiState.value = AppUiState.Ready()
                _events.trySend(BuildServiceNotification)
            } else {
                _uiState.value = AppUiState.Initializing(SdkInitUiState())
                generateChallenge()
            }
        }
    }

    private fun updateInitState(update: (SdkInitUiState) -> SdkInitUiState) {
        val current = _uiState.value as? AppUiState.Initializing ?: return
        _uiState.value = current.copy(sdkInit = update(current.sdkInit))
    }

    private fun generateChallenge() {
        updateInitState { it.copy(step1 = StepState.InProgress) }
        viewModelScope.launch {
            val challenge = VerkadaPassBle.generateChallenge(context)
            updateInitState {
                it.copy(
                    step1 = StepState.Complete,
                    challenge = challenge,
                    step2 = StepState.InProgress
                )
            }
        }
    }

    fun exchangeToken(sdkToken: String) {
        updateInitState { it.copy(exchangeButtonState = ButtonState.Loading) }
        viewModelScope.launch {
            VerkadaPassBle.configure(
                context = context,
                clientId = context.packageName,
                sdkToken = sdkToken,
                shard = Shard.US,
            )
                .onSuccess {
                    _uiState.value = AppUiState.Ready()
                    _events.trySend(BuildServiceNotification)
                }
                .onFailure { error ->
                    when (error) {
                        is ConfigureError.MissingCodeVerifier -> {
                            _events.trySend(ShowSnackbar(context.getString(R.string.error_configuring_sdk_missing_code_verifier)))
                        }

                        is ConfigureError.Network -> {
                            _events.trySend(
                                ShowSnackbar(
                                    context.getString(
                                        R.string.network_error_configuring_sdk,
                                        error.message
                                    )
                                )
                            )
                        }

                        is ConfigureError.MissingOrganizationId -> {
                            _events.trySend(ShowSnackbar(context.getString(R.string.error_configuring_sdk_missing_organization_id)))
                        }

                        is ConfigureError.MissingUserId -> {
                            _events.trySend(ShowSnackbar(context.getString(R.string.error_configuring_sdk_missing_user_id)))
                        }
                    }
                    updateInitState { it.copy(exchangeButtonState = ButtonState.Idle) }
                }
        }
    }

    fun refreshDoors() {
        updateReadyState { it.copy(refreshButtonState = ButtonState.Loading) }
        viewModelScope.launch {
            VerkadaPassBle.fetchDevices(context)
                .onSuccess {
                    _events.trySend(ShowSnackbar(context.getString(R.string.devices_refreshed)))
                }
                .onFailure { error ->
                    when (error) {
                        is FetchDevicesError.MissingOrganizationId -> {
                            _events.trySend(ShowSnackbar(context.getString(R.string.error_fetching_devices_missing_organization_id)))
                        }

                        is FetchDevicesError.Network -> {
                            _events.trySend(
                                ShowSnackbar(
                                    context.getString(
                                        R.string.network_error_fetching_devices,
                                        error.message
                                    )
                                )
                            )
                        }
                    }
                }
            updateReadyState { it.copy(refreshButtonState = ButtonState.Idle) }
        }
    }

    fun resetSdk() {
        updateReadyState { it.copy(resetButtonState = ButtonState.Loading) }
        viewModelScope.launch {
            VerkadaPassBle.stop(context)
            VerkadaPassBle.clearConfiguration(context)
            _uiState.value = AppUiState.Initializing(SdkInitUiState())
            generateChallenge()
        }
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
            notification = notification
        )
            .onSuccess {
                _events.trySend(ShowSnackbar(context.getString(R.string.ble_service_started)))
            }
            .onFailure {
                when (it) {
                    is StartError.MissingUserId -> _events.trySend(ShowSnackbar(context.getString(R.string.error_starting_ble_service_missing_user_id)))
                }
            }
    }
}
