package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.data.notification.NotificationHelper
import com.example.data.notification.MoodleBackgroundSyncService
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.ProfileDrawerContent
import com.example.ui.screens.SetupProfileScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var mainViewModel: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)
        MoodleBackgroundSyncService.start(this)
        handleIntent(intent)
        setContent {
            val viewModel: MainViewModel = viewModel()
            mainViewModel = viewModel
            val themeMode by viewModel.themeMode.collectAsState()

            LaunchedEffect(intent) {
                handleIntent(intent)
            }

            MyApplicationTheme(themeMode = themeMode) {
                ChatProApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val chatId = intent?.getStringExtra(NotificationHelper.EXTRA_CHAT_ID)
        if (!chatId.isNullOrBlank()) {
            mainViewModel?.navigateTo(Screen.ChatDetail(chatId))
        }
    }
}

@Composable
fun ChatProApp(viewModel: MainViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen is Screen.ChatList,
        drawerContent = {
            ProfileDrawerContent(
                viewModel = viewModel,
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val screen = currentScreen) {
                is Screen.Onboarding, is Screen.SetupProfile -> {
                    SetupProfileScreen(
                        viewModel = viewModel,
                        onSetupComplete = {
                            viewModel.navigateTo(Screen.ChatList)
                        }
                    )
                }

                is Screen.ChatList -> {
                    ChatListScreen(
                        viewModel = viewModel,
                        onOpenDrawer = {
                            coroutineScope.launch { drawerState.open() }
                        }
                    )
                }

                is Screen.ChatDetail -> {
                    ChatDetailScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                is Screen.DeviceBanned -> {
                    com.example.ui.screens.DeviceBannedScreen()
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 16.dp)
            )
        }
    }
}
