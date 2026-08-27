/**
 * The single activity that hosts the Compose tree.
 *
 * On `onCreate`:
 *  1. Enables edge-to-edge layout.
 *  2. Reads theme + locale from [MainViewModel] (which does a synchronous
 *     first read of DataStore so the first frame already has the correct
 *     values — no flicker, no recreate loop).
 *  3. Renders [com.moneytracker.ui.navigation.MoneyTrackerNavigation]
 *     inside the Material 3 theme.
 *
 * There is intentionally **no** `LaunchedEffect(locale) { recreate() }`
 * here. The previous implementation triggered an Activity recreate loop
 * on cold start because DataStore's first emission was a synthetic
 * placeholder, not the saved locale. The locale is now applied exactly
 * once by [com.moneytracker.MoneyTrackerApplication.onCreate] before
 * this Activity exists, and user-initiated locale changes go through
 * the Settings screen (which calls recreate() itself, exactly once).
 */
package com.moneytracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.ui.navigation.MoneyTrackerNavigation
import com.moneytracker.ui.theme.MoneyTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val theme by mainViewModel.theme.collectAsState()
            val darkTheme = when (theme) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            // The locale is read once, synchronously, in MainViewModel.init and
            // already applied by MoneyTrackerApplication.onCreate before this
            // Activity exists, so the first frame already matches the saved
            // preference. No LaunchedEffect / recreate() loop here — those were
            // causing the screen to flash black twice on cold start and were
            // silently overwriting the user\'s saved locale to "system" on
            // every launch. The Settings screen handles user-initiated locale
            // changes itself and calls recreate() exactly once when needed.
            MoneyTrackerTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MoneyTrackerNavigation()
                }
            }
        }
    }
}
