package com.vlad_mod.glyph_water

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.text.format


@Composable
fun SettingsScreen(dataStore: DataStore) {

    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory {
        initializer { SettingsViewModel(dataStore) }
    })

    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Watter amount: ${"%.2f".format(state.waterAmount)}")
        Slider(
            value = state.waterAmount,
            onValueChange = { viewModel.onWaterAmountChange(it) },
            valueRange = 1f..24f,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Simulation speed: ${"%.2f".format(state.simSpeed)}")
        Slider(
            value = state.simSpeed,
            onValueChange = { viewModel.onSimSpeedChange(it) },
            valueRange = 1f..10f,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Particle size: ${"%.2f".format(state.particleSize)}")
        Slider(
            value = state.particleSize,
            onValueChange = { viewModel.onParticleSizeChange(it) },
            valueRange = 0.1f..1f,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Simulation behaviour: ${"%.2f".format(state.simType)}")
        Slider(
            value = state.simType,
            onValueChange = { viewModel.onSimTypeChange(it) },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )
        Text(text = "Changes simulation behaviour:\n0 - particles are more viscous \n1 - particles are less viscous ")

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Enable borders")

            Checkbox(
                checked = state.borders,
                onCheckedChange = { viewModel.onBorderChange(it) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val dataStore = DataStore(TestContext());
    SettingsScreen(dataStore)
}