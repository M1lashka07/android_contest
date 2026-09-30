package ru.professionals.storybook

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.professionals.uikit.*

/**
 * Standalone catalog of every public UIKit component and its supported visual states.
 * Created: 30-09-2026. Participant number: pending assignment by the competition.
 * Uses local example state only; no account, backend or application business logic is required.
 */
class StorybookActivity : ComponentActivity() {
    /** Creates the interactive catalog; [savedInstanceState] restores framework state. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleEvent("Created")
        enableEdgeToEdge()
        setContent { CombatTheme { Storybook() } }
    }

    /** Records when the catalog becomes visible. */
    override fun onStart() { super.onStart(); lifecycleEvent("Started") }
    /** Records when the catalog can receive input. */
    override fun onResume() { super.onResume(); lifecycleEvent("Resumed") }
    /** Records when the catalog loses foreground input. */
    override fun onPause() { lifecycleEvent("Paused"); super.onPause() }
    /** Records when the catalog becomes hidden. */
    override fun onStop() { lifecycleEvent("Stopped"); super.onStop() }
    /** Records destruction without logging any entered example text. */
    override fun onDestroy() { lifecycleEvent("Destroyed"); super.onDestroy() }

    private fun lifecycleEvent(event: String) {
        Log.i("Storybook", "[Storybook]: $event — UIKit component catalog")
    }
}

@Composable
private fun Storybook() {
    var presses by rememberSaveable { mutableIntStateOf(0) }
    var emptyText by rememberSaveable { mutableStateOf("") }
    var filledText by rememberSaveable { mutableStateOf("Stone Stellar") }
    var invalidText by rememberSaveable { mutableStateOf("name@example.com") }
    var password by rememberSaveable { mutableStateOf("example-password") }
    var checked by rememberSaveable { mutableStateOf(true) }
    var unchecked by rememberSaveable { mutableStateOf(false) }
    var selection by rememberSaveable { mutableStateOf("") }
    var chosenSelection by rememberSaveable { mutableStateOf("Image") }
    var openSelect by rememberSaveable { mutableIntStateOf(0) }
    var selectedTab by rememberSaveable { mutableIntStateOf(2) }
    val options = listOf("Image", "Circle")
    val tabs = listOf("Statistics", "Discover", "Schedule", "Chat", "Profile")

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("UIKit Storybook", style = MaterialTheme.typography.headlineLarge, color = CombatColors.Pink)
            Text(
                "Every public UIKit component, with labeled supported states. Tap controls to inspect focus, input and selection.",
                style = MaterialTheme.typography.bodyLarge
            )

            CatalogSection("01 · Theme and typography") {
                Text("Primary / gradient / text / surfaces", style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorSwatch("Pink", CombatColors.Pink, Modifier.weight(1f))
                    ColorSwatch("Light", CombatColors.PinkLight, Modifier.weight(1f))
                    ColorSwatch("Deep", CombatColors.PinkDeep, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorSwatch("Ink", CombatColors.Ink, Modifier.weight(1f))
                    ColorSwatch("Pale", CombatColors.Pale, Modifier.weight(1f))
                    ColorSwatch("Blue", CombatColors.Blue, Modifier.weight(1f))
                }
                Text("Headline Large · 24 / 36", style = MaterialTheme.typography.headlineLarge)
                Text("Headline Medium · 22 / 33", style = MaterialTheme.typography.headlineMedium)
                Text("Title Large · 18 / 27", style = MaterialTheme.typography.titleLarge)
                Text("Title Medium · 14 / 21", style = MaterialTheme.typography.titleMedium)
                Text("Body Large · 14 / 21", style = MaterialTheme.typography.bodyLarge)
                Text("Body Medium · 12 / 18", style = MaterialTheme.typography.bodyMedium)
                Text("Body Small · 10 / 15", style = MaterialTheme.typography.bodySmall)
                Text("Label Large · 14 / 21", style = MaterialTheme.typography.labelLarge)
                Text("Label Medium · 12 / 18", style = MaterialTheme.typography.labelMedium)
                Text("Label Small · 10 / 15", style = MaterialTheme.typography.labelSmall)
            }

            CatalogSection("02 · CombatButton") {
                StateLabel("Primary / enabled")
                CombatButton("Let's Combat!", { presses++ })
                StateLabel("Primary / disabled")
                CombatButton("Unavailable", {}, enabled = false)
                StateLabel("Primary / loading")
                CombatButton("Loading", {}, loading = true)
                StateLabel("Secondary / enabled")
                CombatButton("Back", { presses++ }, secondary = true)
                StateLabel("Secondary / disabled")
                CombatButton("Unavailable", {}, secondary = true, enabled = false)
                StateLabel("Secondary / loading")
                CombatButton("Loading", {}, secondary = true, loading = true)
                Text("Button activations: $presses", style = MaterialTheme.typography.bodyMedium)
            }

            CatalogSection("03 · CombatTextField") {
                StateLabel("Empty; tap to inspect focused state")
                CombatTextField(emptyText, { emptyText = it }, "Full Name")
                StateLabel("Filled; editable")
                CombatTextField(filledText, { filledText = it }, "User Name")
                StateLabel("Error and supporting text")
                CombatTextField(
                    invalidText, { invalidText = it }, "Email",
                    error = "Use lowercase Latin letters or digits and a .ru domain",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                StateLabel("Password / hidden initially; eye toggles visible state")
                CombatTextField(
                    password, { password = it }, "Password", password = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Text("Focus, keyboard and password visibility are interactive states.", style = MaterialTheme.typography.bodySmall)
            }

            CatalogSection("04 · CombatSelect + CombatSelectSheet") {
                StateLabel("Empty selection; opens bottom sheet")
                CombatSelect("Category", selection, { openSelect = 1 })
                StateLabel("Populated selection; opens sheet with selected radio state")
                CombatSelect("Category", chosenSelection, { openSelect = 2 })
                Text("Choose an option or dismiss the sheet using Back, the scrim or a downward swipe.", style = MaterialTheme.typography.bodySmall)
            }

            CatalogSection("05 · CombatPagination") {
                (0..2).forEach { page ->
                    StateLabel("Page ${page + 1} / 3")
                    CombatPagination(page, 3)
                }
                StateLabel("Single page")
                CombatPagination(0, 1)
            }

            CatalogSection("06 · CombatCard") {
                StateLabel("Text content")
                CombatCard(Modifier.fillMaxWidth()) {
                    Text("Schedule", style = MaterialTheme.typography.titleMedium, color = CombatColors.Pink)
                    Spacer(Modifier.height(8.dp))
                    Text("Arrange the next game with your friends.", style = MaterialTheme.typography.bodyMedium)
                }
                StateLabel("Composed content: text, timer and action")
                CombatCard(Modifier.fillMaxWidth()) {
                    Text("Image game", style = MaterialTheme.typography.titleMedium)
                    CombatTimer(83)
                    Spacer(Modifier.height(12.dp))
                    CombatButton("Open", { presses++ }, modifier = Modifier.fillMaxWidth())
                }
            }

            CatalogSection("07 · CombatTimer") {
                StateLabel("Initial / zero")
                CombatTimer(0)
                StateLabel("Active / 1 minute 23 seconds")
                CombatTimer(83)
                StateLabel("Minutes over two digits / 100 minutes")
                CombatTimer(6000)
                StateLabel("Negative input is clamped to zero")
                CombatTimer(-1)
            }

            CatalogSection("08 · CombatCheckbox") {
                StateLabel("Initially checked; interactive")
                CombatCheckbox(checked, { checked = it }, "Notification")
                StateLabel("Initially unchecked; interactive")
                CombatCheckbox(unchecked, { unchecked = it }, "Notification")
            }

            CatalogSection("09 · CombatBottomBar") {
                StateLabel("Interactive selection: ${tabs[selectedTab]}")
                CombatBottomBar(selectedTab, { selectedTab = it })
                tabs.forEachIndexed { index, name ->
                    StateLabel("Selected: $name")
                    CombatBottomBar(index, {})
                }
            }

            Text(
                "This catalog covers the current public UIKit API. Additional design states require their source frames; unavailable Figma frames are recorded in docs/design.md.",
                style = MaterialTheme.typography.bodySmall,
                color = CombatColors.Muted
            )
        }
    }

    if (openSelect != 0) {
        val selected = if (openSelect == 1) selection else chosenSelection
        CombatSelectSheet(
            title = "Category",
            options = options,
            selected = selected,
            onSelect = { if (openSelect == 1) selection = it else chosenSelection = it },
            onDismiss = { openSelect = 0 }
        )
    }
}

@Composable
private fun CatalogSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(Modifier.padding(top = 12.dp, bottom = 8.dp), color = CombatColors.Pale)
        Text(title, style = MaterialTheme.typography.titleLarge, color = CombatColors.Pink)
        content()
    }
}

@Composable
private fun StateLabel(label: String) {
    Text(label, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun ColorSwatch(name: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(40.dp).background(color, RoundedCornerShape(8.dp)))
        Text(name, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall)
    }
}
