package com.vlad_mod.glyph_water

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.vlad_mod.glyph_water.ui.theme.Theme


class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dataStore = DataStore(applicationContext)
        enableEdgeToEdge()
        setContent {
            Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(dataStore)
                }
            }
        }
    }
}
