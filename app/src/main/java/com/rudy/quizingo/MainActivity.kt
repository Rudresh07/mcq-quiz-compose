package com.rudy.quizingo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.rudy.quizingo.ui.AppNavHost
import com.rudy.quizingo.ui.modules.ModuleListViewModel
import com.rudy.quizingo.ui.theme.QuizingoTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.androidx.viewmodel.ext.android.viewModel

/** How long the splash is allowed to wait on the module list's first load before
 *  falling through to the in-app spinner regardless - a slow/dead connection
 *  shouldn't leave the user on a static splash with no progress cue or way back. */
private const val SPLASH_MAX_WAIT_MS = 3_000L

class MainActivity : ComponentActivity() {

    // Same instance the Module List screen reads: AppNavHost explicitly passes this
    // Activity as the viewModelStoreOwner to its koinViewModel() call, so both resolve
    // through this Activity's ViewModelStore and observing it here doesn't trigger a
    // second module load.
    private val moduleListViewModel: ModuleListViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        lifecycleScope.launch {
            withTimeoutOrNull(SPLASH_MAX_WAIT_MS) {
                moduleListViewModel.uiState.first { !it.isLoading }
            }
            keepSplashOnScreen = false
        }

        enableEdgeToEdge()
        setContent {
            QuizingoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
