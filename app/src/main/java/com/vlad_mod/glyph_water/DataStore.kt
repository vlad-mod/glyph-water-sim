package com.vlad_mod.glyph_water

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

private const val DATASTORE_NAME = "settings"
private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class DataStore(private val context: Context) {

    companion object {
        val WATER_KEY = floatPreferencesKey("waterAmount")
        val SIM_SPEED_KEY = floatPreferencesKey("simSpeed")
        val PARTICLE_SIZE_KEY = floatPreferencesKey("particleSize")
        val SIM_TYPE_KEY = floatPreferencesKey("simType")
        val BORDER_KEY = booleanPreferencesKey("borders")
    }

    val waterAmountFlow: Flow<Float> = context.dataStore.data
        .map { it[WATER_KEY] ?: 20f }


    val simSpeedFlow: Flow<Float> = context.dataStore.data
        .map { it[SIM_SPEED_KEY] ?: 5f }

    val particleSizeFlow: Flow<Float> = context.dataStore.data
        .map { it[PARTICLE_SIZE_KEY] ?: 0.3f }

    val simTypeFlow: Flow<Float> = context.dataStore.data
        .map { it[SIM_TYPE_KEY] ?: 0.95f }

    val borderFlow: Flow<Boolean> = context.dataStore.data
        .map { it[BORDER_KEY] ?: true }

    suspend fun saveWaterAmount(water: Float) {
        context.dataStore.edit { it[WATER_KEY] = water }
    }

    suspend fun saveSimSpeed(simSpeed: Float) {
        context.dataStore.edit { it[SIM_SPEED_KEY] = simSpeed }
    }

    suspend fun saveParticleSize(particleSize: Float) {
        context.dataStore.edit { it[PARTICLE_SIZE_KEY] = particleSize }
    }

    suspend fun saveSimType(simType: Float) {
        context.dataStore.edit { it[SIM_TYPE_KEY] = simType }
    }

    suspend fun saveBorder(border: Boolean) {
        context.dataStore.edit { it[BORDER_KEY] = border }
    }
}
