package com.vlad_mod.glyph_water.toys.animation

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.ui.text.font.FontVariation
import androidx.core.math.MathUtils.clamp
import com.nothing.ketchum.GlyphMatrixManager
import com.vlad_mod.glyph_water.DataStore
import com.vlad_mod.glyph_water.SettingsState
import com.vlad_mod.glyph_water.toys.GlyphMatrixService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow


class WaterToy : GlyphMatrixService("Water Toy") {

    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val uiScope = CoroutineScope(Dispatchers.Main)

    //water Sim
    private var flip: FLIP? = null;

    // accelerometer stuff
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                if (abs(event.values[1].toDouble()) > abs(gravityX)) {
                    gravityX = event.values[1].toDouble()
                }

                if (abs(event.values[1].toDouble()) > abs(gravityY)) {
                    gravityY = event.values[0].toDouble()
                }

            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Optional
        }
    }
    var gravityX = 0.0;
    var gravityY = 0.0;


    @SuppressLint("ServiceCast")
    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        backgroundScope.launch {
            // init fluid sim
            // load settings
            val dataStore = DataStore(applicationContext)

            val settings: SettingsState = SettingsState(
                dataStore.waterAmountFlow.first(),
                dataStore.simSpeedFlow.first(),
                dataStore.particleSizeFlow.first(),
                dataStore.simTypeFlow.first(),
                dataStore.borderFlow.first(),
            );

            // initialization, taken from TenMinutePhysics
            var res = 24;
            var tankHeight = 24.0;
            var tankWidth = 24.0;
            var h = tankHeight / res;
            var density = 500.0;

            var relWaterHeight = settings.waterAmount / 24.0;
            var relWaterWidth = settings.waterAmount / 24.0;

            var r = settings.particleSize.toDouble();    // particle radius w.r.t. cell size
            var dx = 2.0 * r;
            var dy = dx;

            var numX = floor((relWaterWidth * tankWidth - 2.0 * h - 2.0 * r) / dx).toInt();
            var numY = floor((relWaterHeight * tankHeight - 2.0 * h - 2.0 * r) / dy).toInt();
            var maxParticles = numX * numY;


            flip = FLIP(density, tankWidth, tankHeight, h, r, maxParticles);
            flip!!.numParticles = numX * numY;

            //add water
            var p = 0;
            for (i in 0 until numX) {
                for (j in 0 until numY) {
                    flip!!.particlePos[p++] = h + r + dx * i + (if (j % 2 == 0) 0.0 else r);
                    flip!!.particlePos[p++] = h + r + dy * j
                }
            }

            //add borders
            var n = flip!!.fNumY;
            for (i in 0 until flip!!.fNumX) {
                for (j in 0 until flip!!.fNumY) {
                    var s = 1.0;    // fluid
                    if (settings.borders) {
                        //circular
//                        val x = i.toDouble()-12.0;
//                        val y = j.toDouble()-12.0;
//                        if (abs(x.pow(2) + y.pow(2) - RADIUS.pow(2)) < 20) {
//                            s = 0.0; //solid
//
//                        }
                        //square border
                        if (i == 0 || i == flip!!.fNumX - 1 || j == 0 || j == flip!!.fNumY - 1) {
                            s = 0.0; //solid
                        }
                    }
                    flip!!.s[i * n + j] = s
                }
            }

            //init accelerometer
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            accelerometer?.let { sensor ->
                sensorManager.registerListener(
                    sensorListener,
                    sensor,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }

            //loop
            while (isActive) {
                val array = generateNextAnimationFrame(settings)
                uiScope.launch {
                    glyphMatrixManager.setMatrixFrame(array)
                }
                // wait a bit
                delay((DELAY * 1000).toLong())
            }
        }
    }

    override fun performOnServiceDisconnected(context: Context) {
        backgroundScope.cancel()
        sensorManager.unregisterListener(sensorListener);
    }

    private fun generateNextAnimationFrame(settings: SettingsState): IntArray {
        flip!!.simulate(
            DELAY * settings.simSpeed,
            gravityX,
            gravityY,
            settings.simType.toDouble(),
            1,
            1,
            1.0,
            true,
            true,
            500.0,
            500.0,
            0.0
        )
        gravityX = 0.0;
        gravityY = 0.0;

        val grid = Array(HEIGHT * WIDTH) { 0 }

        for (n in 0..<HEIGHT * WIDTH) {

            var color = ((((flip!!.cellColor[n * 3 + 0] +
                    flip!!.cellColor[n * 3 + 1] +
                    flip!!.cellColor[n * 3 + 2]) / 3) - (flip!!.cellColor[n * 3 + 2] * 0.5)) * 4096)
            color = clamp(color, 0.0, 4096.0);

            grid[n] = color.toInt();
        }
        return grid.toIntArray()
    }

    private companion object {
        private const val WIDTH = 25
        private const val HEIGHT = 25
        private const val RADIUS = 13.0

        //delay less than 15ms slows down animation
        private const val DELAY = 0.015//s

    }
}