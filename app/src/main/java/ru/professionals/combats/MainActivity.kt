package ru.professionals.combats

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.professionals.combats.presentation.CombatApp

/** Android entry point and dependency composition root. Created 30-09-2026; author: training project. */
class MainActivity : ComponentActivity() {
    private val repository get() = (application as CombatApplication).repository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleLog("onCreate")
        setContent { CombatApp(repository) }
    }
    override fun onStart() { super.onStart(); lifecycleLog("onStart") }
    override fun onResume() { super.onResume(); lifecycleLog("onResume") }
    override fun onPause() { lifecycleLog("onPause"); super.onPause() }
    override fun onStop() { lifecycleLog("onStop"); super.onStop() }
    override fun onDestroy() { lifecycleLog("onDestroy"); super.onDestroy() }
    private fun lifecycleLog(event: String) { Log.i("MainActivity", "[MainActivity]: Lifecycle — $event") }
}
