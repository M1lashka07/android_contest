package ru.professionals.combats.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.professionals.uikit.*

@Composable
internal fun SplashScreen() {
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds().background(PinkGradient)) {
        val sx = maxWidth / 375.dp
        val sy = maxHeight / 812.dp
        FigmaAsset("splash_group7.svg", Modifier.offset((100.09f * sx).dp, (273.09f * sy).dp).size((173.905f * sx).dp, (102.654f * sx).dp), "GAAMETIME")
        FigmaAsset("splash_graphics.svg", Modifier.offset((-26 * sx).dp, (642 * sy).dp).requiredSize((431 * sx).dp, (285 * sx).dp))
    }
}

@Composable
internal fun OnboardingScreen(initialPage: Int, onPage: (Int) -> Unit, onFinish: () -> Unit) {
    val pager = rememberPagerState(initialPage = initialPage.coerceIn(0, 2)) { 3 }
    val scope = rememberCoroutineScope()
    val titles = listOf("Get Paid! Playing Video Game", "Schedule Games With Friends", "Text, Audio and Video Chat")
    val descriptions = listOf("Earn points and real cash when you win a battle with no delay in cashing out",
        "Easily create an upcoming event and get ready for battle. Yeah! real combat fella.",
        "Intuitive real life experience on mobile. Chat with fellow gamers before and after combat for free!")
    LaunchedEffect(pager.currentPage) { onPage(pager.currentPage) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxHeight < 650.dp || LocalDensity.current.fontScale > 1.15f) {
            HorizontalPager(pager, Modifier.fillMaxSize().safeDrawingPadding()) { page ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    BoxWithConstraints(Modifier.widthIn(max = 348.dp).fillMaxWidth().aspectRatio(if (page == 2) 316f / 294f else 348f / 233f)) {
                        FigmaAsset("onboarding${page + 1}_image.png", Modifier.fillMaxSize())
                        if (page == 1) {
                            val artScale = maxWidth / 348.dp
                            FigmaAsset("onboarding2_group45.svg", Modifier.offset((39f * artScale).dp, (34f * artScale).dp)
                                .size((35.09f * artScale).dp, (44.84f * artScale).dp).graphicsLayer(rotationZ = -37.12f))
                            FigmaAsset("onboarding2_group46.svg", Modifier.offset((171.6f * artScale).dp, (20.5f * artScale).dp)
                                .size((23.63f * artScale).dp, (26.23f * artScale).dp).graphicsLayer(rotationZ = 38.14f))
                        }
                    }
                    Text(titles[page], color = Pink, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 360.dp))
                    Text(descriptions[page], color = Ink, fontSize = 14.sp, lineHeight = 21.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 360.dp))
                    if (page == 2) CombatButton("Let’s Combat!", onFinish)
                    TextButton(onClick = onFinish) { Text("Skip", color = Pink, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(3) { index -> FigmaAsset("onboarding${page + 1}_${if (index == page) "active_dot" else "dot"}.svg",
                            Modifier.size(10.dp).clickable { scope.launch { pager.animateScrollToPage(index) } }) }
                    }
                }
            }
            return@BoxWithConstraints
        }
        val sx = maxWidth / 375.dp
        val sy = maxHeight / 812.dp
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            Box(Modifier.fillMaxSize()) {
                val artwork = when (page) { 0 -> listOf(48f, 164f, 327f, 232f); 1 -> listOf(27f, 163f, 348f, 233f); else -> listOf(30f, 144f, 316f, 294f) }
                FigmaAsset("onboarding${page + 1}_image.png", Modifier.offset((artwork[0] * sx).dp, (artwork[1] * sy).dp).size((artwork[2] * sx).dp, (artwork[3] * sx).dp))
                if (page == 1) {
                    FigmaAsset("onboarding2_group45.svg", Modifier.offset((66 * sx).dp, (197 * sy).dp).size((35.09f * sx).dp, (44.84f * sx).dp).graphicsLayer(rotationZ = -37.12f))
                    FigmaAsset("onboarding2_group46.svg", Modifier.offset((198.6f * sx).dp, (183.5f * sy).dp).size((23.63f * sx).dp, (26.23f * sx).dp).graphicsLayer(rotationZ = 38.14f))
                }
                Text(titles[page],
                    color = Pink, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (431 * sy).dp).width((224 * sx).dp))
                Text(descriptions[page],
                    color = Ink, fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (539 * sy).dp).width((224 * sx).dp))
            }
        }
        if (pager.currentPage == 2) CombatButton("Let’s Combat!", onFinish, Modifier.align(Alignment.TopCenter).offset(y = (661 * sy).dp).width((210 * sx).dp))
        else TextButton(onClick = onFinish, modifier = Modifier.align(Alignment.TopCenter).offset(y = (651 * sy).dp)) {
            Text("Skip", color = Pink, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
        }
        Row(Modifier.align(Alignment.TopCenter).offset(y = (757 * sy).dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { index -> FigmaAsset("onboarding${pager.currentPage + 1}_${if (index == pager.currentPage) "active_dot" else "dot"}.svg",
                Modifier.size(10.dp).clickable { scope.launch { pager.animateScrollToPage(index) } }) }
        }
    }
}

internal data class AuthDraft(
    val fullName: String = "", val userName: String = "", val phone: String = "", val email: String = "",
    val countryCode: String = "+7",
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun AuthScreen(
    register: Boolean, draft: AuthDraft, onDraft: (AuthDraft) -> Unit, busy: Boolean,
    onSubmit: (String, String) -> Unit, onSwitch: () -> Unit, onLegal: (String) -> Unit,
    onInfo: (String) -> Unit,
) {
    // Passwords deliberately stay only in memory; never in preferences or a saved-state bundle.
    var password by remember(register) { mutableStateOf("") }
    var confirmation by remember(register) { mutableStateOf("") }
    var validation by remember(register) { mutableStateOf<String?>(null) }
    var termsAccepted by remember { mutableStateOf(false) }
    val scroll = key(register) { rememberLazyListState() }
    LazyColumn(state = scroll, modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(), horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Spacer(Modifier.height((if (register) 65.dp else 95.dp) - status))
            Box(Modifier.fillMaxWidth()) {
                FigmaAsset(if (register) "createaccount_image.png" else "login_image.png",
                    Modifier.align(Alignment.TopStart).padding(start = if (register) 27.dp else 0.dp).size(if (register) 321.dp else 323.dp, if (register) 247.dp else 205.dp))
            }
            Spacer(Modifier.height(if (register) 12.dp else 27.dp))
        }
        stickyHeader {
            Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(start = 52.dp, end = 52.dp, top = 14.dp, bottom = if (register) 14.dp else 12.dp)) {
                Text(if (register) "Create Account" else "Welcome Back!", color = Pink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(start = 52.dp, end = 60.dp), verticalArrangement = Arrangement.spacedBy(if (register) 4.dp else 0.dp)) {
                Text(if (register) "Hi, kindly fill in the form to proceed\ncombat" else "Hi, Kindly login to continue battle", fontSize = 12.sp, lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp))
                if (register) {
                    CombatTextField(draft.fullName, { onDraft(draft.copy(fullName = it)) }, "Full Name")
                    CombatTextField(draft.userName, { onDraft(draft.copy(userName = it)) }, "User Name")
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        CombatTextField(draft.countryCode, { onDraft(draft.copy(countryCode = it)) }, "Code", Modifier.width(35.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                        CombatTextField(draft.phone, { onDraft(draft.copy(phone = it)) }, "Your Phone", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    }
                }
                CombatTextField(draft.email, { onDraft(draft.copy(email = it)) }, "Email", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    error = if (validation != null && !Regex("^[a-z0-9]+@[a-z0-9]+\\.ru$").matches(draft.email.trim())) "Use name@domain.ru (lowercase letters and numbers)" else null)
                CombatTextField(password, { password = it }, "Password", password = true)
                if (register) CombatTextField(confirmation, { confirmation = it }, "Confirm Password", password = true)
                else Text("Forgot Password?", fontSize = 12.sp, lineHeight = 18.sp, color = Ink,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp).clickable(role = Role.Button) {
                        onInfo("Password recovery requires a configured recovery redirect. Contact the application administrator.")
                    })
                if (register) {
                    CombatCheckbox(termsAccepted, { termsAccepted = it }, "I accept the terms")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { onLegal("privacy") }) { Text("Privacy policy", fontSize = 10.sp) }
                        TextButton(onClick = { onLegal("terms") }) { Text("User agreement", fontSize = 10.sp) }
                    }
                }
                validation?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                Spacer(Modifier.height(if (register) 14.dp else 31.dp))
                CombatButton(if (register) "Create Account" else "Let’s Combat!", {
                    validation = when {
                        !Regex("^[a-z0-9]+@[a-z0-9]+\\.ru$").matches(draft.email.trim()) -> "Email must match name@domain.ru using lowercase letters and numbers."
                        register && draft.fullName.trim().length !in 2..80 -> "Full name must contain 2 to 80 characters."
                        password.length < 8 -> "Password must contain at least 8 characters."
                        register && password != confirmation -> "Passwords do not match."
                        register && !termsAccepted -> "Read and accept the user agreement."
                        else -> null
                    }
                    if (validation == null) onSubmit(draft.email.trim(), password)
                }, Modifier.align(Alignment.CenterHorizontally).offset(x = 4.5.dp).width(210.dp), enabled = !busy, loading = busy)
                if (!register) {
                    Text("Connect With:", Modifier.fillMaxWidth().offset(x = 4.5.dp).padding(top = 19.dp), color = Pink, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
                    Row(Modifier.fillMaxWidth().offset(x = 4.5.dp).padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
                        listOf("google", "facebook").forEach { provider ->
                            Box(Modifier.size(50.dp, 48.dp).clickable { onInfo("$provider sign-in is not configured for this project. Use email and password.") }, contentAlignment = Alignment.Center) {
                                FigmaAsset("login_$provider.svg", Modifier.size(35.dp, 34.dp), "$provider sign in")
                            }
                        }
                    }
                }
                Text(if (register) "Already have an account?" else "Don’t have an account?", Modifier.fillMaxWidth().offset(x = 4.5.dp).padding(top = if (register) 28.dp else 7.dp), fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
                Text(if (register) "Login" else "Create Account", color = Pink, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 18.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).offset(x = 4.5.dp).clickable(role = Role.Button, onClick = onSwitch))
            }
        }
    }
}
