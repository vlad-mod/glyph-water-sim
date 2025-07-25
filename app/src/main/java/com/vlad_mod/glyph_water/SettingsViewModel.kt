package com.vlad_mod.glyph_water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsState(
    val waterAmount: Float = 20f,
    val simSpeed: Float = 4f,
    val particleSize: Float = 0.3f,
    val borders: Boolean = true
)

class SettingsViewModel(private val dataStore: DataStore) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                dataStore.waterAmountFlow,
                dataStore.simSpeedFlow,
                dataStore.borderFlow,
                dataStore.particleSizeFlow
            ) { water, sim, borders, particleSize ->
                SettingsState(water, sim, particleSize, borders)
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun onWaterAmountChange(waterAmount: Float) {
        _uiState.value = _uiState.value.copy(waterAmount = waterAmount)
        viewModelScope.launch { dataStore.saveWaterAmount(waterAmount) }
    }

    fun onSimSpeedChange(simSpeed: Float) {
        _uiState.value = _uiState.value.copy(simSpeed = simSpeed)
        viewModelScope.launch { dataStore.saveSimSpeed(simSpeed) }
    }

    fun onParticleSizeChange(particleSize: Float) {
        _uiState.value = _uiState.value.copy(particleSize = particleSize)
        viewModelScope.launch { dataStore.saveParticleSize(particleSize) }
    }

    fun onBorderChange(borders: Boolean) {
        _uiState.value = _uiState.value.copy(borders = borders)
        viewModelScope.launch { dataStore.saveBorder(borders) }
    }
}
