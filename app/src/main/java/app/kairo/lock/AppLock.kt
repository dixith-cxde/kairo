package app.kairo.lock

import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * App lock (same model as Stash): Android's own prompt — fingerprint or face, falling back to the
 * device PIN, pattern or password. Kairo never sees or stores any of it.
 */
object AppLock {
    private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    /** The lock screen covers the app. */
    var locked by mutableStateOf(false)
    /** The system prompt is up (the PIN screen briefly takes the app to the background). */
    var authenticating by mutableStateOf(false)
        private set
    private var backgroundAt = 0L
    private var lastAuthEnd = 0L

    /** The phone has a screen lock or enrolled biometrics, so the lock can work. */
    fun canLock(activity: FragmentActivity) =
        BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    /** Shows the system prompt; calls back with true when the person proved it's them. */
    fun authenticate(activity: FragmentActivity, title: String = "Unlock Kairo", onResult: (Boolean) -> Unit) {
        if (authenticating) return
        authenticating = true
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = done(true)
            override fun onAuthenticationError(code: Int, msg: CharSequence) = done(false)
            private fun done(ok: Boolean) {
                authenticating = false
                lastAuthEnd = SystemClock.elapsedRealtime()
                onResult(ok)
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle("Use your fingerprint, face or screen lock")
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build(),
        )
    }

    fun unlock(activity: FragmentActivity) = authenticate(activity) { ok -> if (ok) locked = false }

    // Lifecycle hooks from MainActivity.
    fun onBackground() { if (!authenticating) backgroundAt = SystemClock.elapsedRealtime() }

    fun onForeground(enabled: Boolean, lockAfterSec: Int) {
        if (!enabled || authenticating || backgroundAt == 0L) return
        // Returning from our own PIN screen is not "leaving the app".
        if (SystemClock.elapsedRealtime() - lastAuthEnd < 1500) return
        if (SystemClock.elapsedRealtime() - backgroundAt >= lockAfterSec * 1000L) locked = true
    }
}
