package com.sadra.memo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.sadra.memo.data.preferences.ThemeMode
import com.sadra.memo.ui.LockScreen
import com.sadra.memo.ui.MemoApp
import com.sadra.memo.ui.theme.MemoTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private var unlocked by mutableStateOf(false)
    private var pinEnabled = false

    override fun onResume() {
        super.onResume()
        val app = application as MemoApplication
        app.container.appScope.launch {
            app.container.smsProcessor.syncRecentFromDeviceInbox()
        }
    }

    override fun onStop() {
        super.onStop()
        if (pinEnabled) unlocked = false
    }

    private fun showBiometric() {
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("باز کردن Memo")
            .setNegativeButtonText("استفاده از رمز")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlocked = true
                }
            }
        )
        prompt.authenticate(info)
    }

    private fun biometricAvailable(): Boolean =
        BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MemoApplication
        val settings = app.container.settingsRepository
        setContent {
            val mode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val pinHash by settings.pinHash.collectAsState(initial = null)
            val biometricOn by settings.biometricEnabled.collectAsState(initial = false)
            val systemDark = isSystemInDarkTheme()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.argb(0xe6, 0xFF, 0xFF, 0xFF),
                        Color.argb(0x80, 0x1b, 0x1b, 0x1b)
                    ) { dark }
                )
                onDispose { }
            }

            val hash = pinHash
            SideEffect { pinEnabled = !hash.isNullOrEmpty() }
            LaunchedEffect(hash) {
                if (hash != null && hash.isEmpty()) unlocked = true
            }

            MemoTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    when {
                        hash == null -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        )
                        !hash.isNullOrEmpty() && !unlocked -> LockScreen(
                            verify = { pin -> settings.verifyPin(pin) },
                            onUnlocked = { unlocked = true },
                            biometricEnabled = biometricOn && biometricAvailable(),
                            onBiometric = { showBiometric() }
                        )
                        else -> MemoApp()
                    }
                }
            }
        }
    }
}
