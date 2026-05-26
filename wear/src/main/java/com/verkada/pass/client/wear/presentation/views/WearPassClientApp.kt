package com.verkada.pass.client.wear.presentation.views

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.verkada.pass.client.wear.presentation.AppUiState
import com.verkada.pass.client.wear.presentation.theme.VerkadaPassAndroidClientTheme

@Composable
fun WearPassClientApp(
    appState: AppUiState,
    onTokenSubmit: (String) -> Unit,
    onRefreshDoors: () -> Unit,
    onResetSdk: () -> Unit,
) {
    VerkadaPassAndroidClientTheme {
        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        ) {
            when (appState) {
                AppUiState.Unknown -> WearLoadingScreen()
                is AppUiState.Initializing -> WearInitScreen(
                    state = appState.sdkInit,
                    onTokenSubmit = onTokenSubmit,
                )
                is AppUiState.Ready -> WearReadyScreen(
                    state = appState.state,
                    onRefreshDoors = onRefreshDoors,
                    onResetSdk = onResetSdk,
                )
            }
        }
    }
}
