package com.jarvis.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.jarvis.assistant.ui.navigation.JarvisNavHost
import com.jarvis.assistant.ui.theme.JarvisTheme

/**
 * NOTE ON LOCK SCREEN: setShowWhenLocked/turnScreenOn (declared in the manifest + here) let
 * this Activity display ON TOP of the lock screen — the officially supported mechanism used by
 * things like incoming-call screens. It does NOT unlock the device or bypass the user's PIN/
 * biometric; the OS still enforces authentication for anything sensitive underneath.
 */
class MainActivity : ComponentActivity() {

    private var hasMicPermission by mutableStateOf(false)

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        hasMicPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        setContent {
            JarvisTheme {
                JarvisNavHost(
                    hasMicPermission = hasMicPermission,
                    onRequestMicPermission = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )
            }
        }
    }
}
