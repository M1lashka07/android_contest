package ru.professionals.combats.presentation

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ru.professionals.combats.platform.createCameraUri
import ru.professionals.combats.platform.openLegal
import ru.professionals.domain.CombatRepository
import ru.professionals.uikit.CombatBottomBar
import ru.professionals.uikit.CombatTheme

/** All screens consume the domain abstraction; no composable reaches into the network library. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombatApp(repository: CombatRepository) {
    val context = LocalContext.current
    val vm: CombatViewModel = viewModel(factory = remember(repository) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CombatViewModel(context.applicationContext, repository) as T
        }
    })
    val scope = rememberCoroutineScope()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    var photoMenu by remember { mutableStateOf(false) }
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var leaveConfirmation by remember { mutableStateOf(false) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) vm.changePhoto(uri) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { vm.changePhoto(Uri.parse(it)) }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        vm.updateSchedule(vm.scheduleDraft.copy(remind = allowed))
        if (!allowed) vm.showError("Notifications are disabled. You can enable them in Android settings.")
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && (context as? Activity)?.isChangingConfigurations != true) vm.onBackground()
            if (event == Lifecycle.Event.ON_START) vm.onForeground()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun navigate(destination: String) {
        scope.launch { drawer.close() }
        if (destination == "privacy" || destination == "terms") openLegal(context, destination)
        else vm.navigate(destination)
    }
    BackHandler(drawer.isOpen) { scope.launch { drawer.close() } }
    BackHandler(!drawer.isOpen && vm.screen !in setOf("splash", "home", "login", "onboarding")) {
        if (vm.screen == "game") leaveConfirmation = true else vm.back()
    }
    CombatTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                when (vm.screen) {
                    "splash" -> SplashScreen()
                    "onboarding" -> OnboardingScreen(vm.onboardingPage, vm::updateOnboarding, vm::finishOnboarding)
                    "login", "register" -> AuthScreen(vm.screen == "register", vm.authDraft, vm::updateAuth, vm.busy,
                        { email, password -> vm.authenticate(vm.screen == "register", email, password) },
                        { vm.navigate(if (vm.screen == "register") "login" else "register", false) },
                        { openLegal(context, it) }, vm::showError)
                    else -> ModalNavigationDrawer(drawerState = drawer, gesturesEnabled = vm.screen == "home" || drawer.isOpen,
                        drawerContent = {
                            ModalDrawerSheet {
                                MenuContent(vm.player, { scope.launch { drawer.close() } }, ::navigate, {
                                    scope.launch { drawer.close() }; vm.signOut()
                                })
                            }
                        }) {
                        val bottomVisible = vm.screen in setOf("home", "discover", "statistics", "players", "profile")
                        Scaffold(modifier = Modifier.statusBarsPadding(), containerColor = MaterialTheme.colorScheme.background,
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            bottomBar = {
                                if (bottomVisible) CombatBottomBar(
                                    selected = listOf("statistics", "discover", "schedule", "players", "profile").indexOf(vm.screen),
                                    onSelect = { navigate(listOf("statistics", "discover", "schedule", "players", "profile")[it]) })
                            }) { contentPadding ->
                            Box(Modifier.fillMaxSize().padding(contentPadding).then(if (!bottomVisible) Modifier.navigationBarsPadding() else Modifier)) {
                                when (vm.screen) {
                                    "home" -> LandingScreen(vm.player, vm.popular, { scope.launch { drawer.open() } }, ::navigate, vm::openPlayer)
                                    "schedule" -> ScheduleScreen(vm.scheduleDraft, { updated ->
                                        if (updated.remind && !vm.scheduleDraft.remind && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else vm.updateSchedule(updated)
                                    }, vm.busy, vm::back, vm::publish)
                                    "success" -> SuccessScreen { vm.navigate("home", false) }
                                    "discover" -> DiscoverScreen(vm.games, vm.query, vm::updateQuery, vm.category, vm::updateCategory,
                                        vm.filter, vm::updateFilter, vm.loading, vm::back, vm::openCombat, vm::refreshForScreen)
                                    "combat" -> CombatInformationScreen(vm.combat, vm.userId, vm.busy || vm.loading, vm::back, vm::join, vm::start, vm::openPlayer, vm::refreshForScreen)
                                    "players" -> PlayersScreen(vm.popular, vm.loading, vm::back, vm::refreshForScreen, vm::openPlayer)
                                    "profile", "player" -> ProfileScreen(if (vm.screen == "profile") vm.player else vm.viewedPlayer, vm.screen == "profile", vm.profileDraft,
                                        vm::updateProfileDraft, vm.editing, vm::updateEditing, vm.busy || vm.loading, vm::back, vm::saveProfile,
                                        { photoMenu = true }, { vm.navigate("statistics") })
                                    "statistics" -> StatisticsScreen(vm.statistics, vm.loading, vm::back, vm::refreshForScreen)
                                    "game" -> vm.combat?.let { GameScreen(it, vm.moves, vm::makeMove, vm.busy, vm::finishGame, vm::leaveGame) }
                                    "result" -> ResultScreen(vm.combat, vm.userId, { vm.navigate("home", false) }, { vm.navigate("statistics", false) })
                                    "departed" -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                                        EmptyState("You left the combat", "Leaving counts as a loss. The result will be sent to the server when the connection is restored.", "Retry syncing", vm::syncForfeit)
                                        TextButton(onClick = { vm.navigate("home", false) }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Back to Home") }
                                    }
                                    else -> EmptyState("Screen unavailable", "Return to the main page.", "Home") { vm.navigate("home", false) }
                                }
                                if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter), color = Pink)
                            }
                        }
                    }
                }
                vm.error?.let { ErrorBanner(it, vm::dismissError, Modifier.align(Alignment.TopCenter).statusBarsPadding()) }
            }
        }
        if (photoMenu) AlertDialog(onDismissRequest = { photoMenu = false }, title = { Text("Profile photo") }, text = { Text("Choose a photo or take a new one.") },
            confirmButton = { TextButton(onClick = { photoMenu = false; gallery.launch("image/*") }) { Text("Gallery") } },
            dismissButton = { TextButton(onClick = {
                photoMenu = false
                try { val uri = createCameraUri(context); cameraUri = uri.toString(); camera.launch(uri) }
                catch (failure: Exception) {
                    Log.e("CombatApp", "[CombatApp]: Ошибка — Camera launch failed", failure)
                    vm.showError(failure.message ?: "Camera is unavailable.")
                }
            }) { Text("Camera") } })
        if (leaveConfirmation) AlertDialog(onDismissRequest = { leaveConfirmation = false }, title = { Text("Leave combat?") },
            text = { Text("Leaving an active combat counts as a loss.") },
            confirmButton = { TextButton(onClick = { leaveConfirmation = false; vm.leaveGame() }) { Text("Forfeit") } },
            dismissButton = { TextButton(onClick = { leaveConfirmation = false }) { Text("Keep playing") } })
    }
}
