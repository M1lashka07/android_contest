package ru.professionals.uikit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import java.util.Locale

/** Stateless reusable controls, with visual interaction state only. Created 30-09-2026; author: training project. */
@Composable fun CombatButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, secondary: Boolean = false, loading: Boolean = false) {
    val gradient = if (enabled && !loading) listOf(CombatColors.PinkLight, CombatColors.PinkDeep) else listOf(Color(0xFFEDADC0), Color(0xFFEDADC0))
    Button(onClick, enabled = enabled && !loading, modifier = modifier.defaultMinSize(minWidth = 210.dp).height(58.dp)
        .clip(CircleShape).background(if (secondary) Brush.linearGradient(listOf(CombatColors.Pale, CombatColors.Pale)) else Brush.linearGradient(gradient)),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent,
            contentColor = if (secondary) CombatColors.Pink else Color.White, disabledContentColor = if (secondary) CombatColors.Muted else Color.White),
        contentPadding = PaddingValues(horizontal = 24.dp)) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp).semantics { contentDescription = "Loading" }, color = if (secondary) CombatColors.Pink else Color.White, strokeWidth = 2.dp)
        else Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable fun CombatTextField(value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, password: Boolean = false, error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default) {
    var visible by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        BasicTextField(value, onValueChange, singleLine = true, keyboardOptions = keyboardOptions,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = CombatColors.Ink),
            cursorBrush = SolidColor(CombatColors.Pink),
            visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth().heightIn(min = 49.dp).semantics { contentDescription = label },
            decorationBox = { input ->
                Row(Modifier.fillMaxWidth().heightIn(min = 49.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).padding(top = 10.dp)) {
                        if (value.isEmpty()) Text(label, style = MaterialTheme.typography.bodyMedium)
                        input()
                    }
                    if (password) IconButton(onClick = { visible = !visible }, modifier = Modifier.size(40.dp)) {
                        Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            if (visible) "Hide password" else "Show password", Modifier.size(14.dp))
                    }
                }
            })
        HorizontalDivider(color = if (error == null) CombatColors.Pink else MaterialTheme.colorScheme.error, thickness = 1.dp)
        if (error != null) Text(error, Modifier.padding(top = 4.dp).semantics { this.error(error) },
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable fun CombatCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CombatColors.Pale)) {
        Column(Modifier.padding(24.dp), content = content)
    }
}

@Composable fun CombatPagination(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier.semantics { contentDescription = "Page ${current + 1} of $total" }, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(total) { Box(Modifier.size(10.dp).clip(CircleShape).background(if (it == current) CombatColors.Pink else Color(0xFFFFD5DF))) }
    }
}

@Composable fun CombatTimer(seconds: Int, modifier: Modifier = Modifier) {
    Text(String.format(Locale.ROOT, "%02d:%02d", seconds.coerceAtLeast(0) / 60, seconds.coerceAtLeast(0) % 60),
        modifier.semantics { contentDescription = "Elapsed time $seconds seconds" }, color = CombatColors.Pink, style = MaterialTheme.typography.headlineMedium)
}

@Composable fun CombatCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) {
    Row(modifier.clickable { onCheckedChange(!checked) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable fun CombatSelect(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().clickable(onClick = onClick).padding(top = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (value.isNotBlank()) Text(label, color = CombatColors.Muted, style = MaterialTheme.typography.bodySmall)
                Text(value.ifBlank { label }, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Outlined.KeyboardArrowDown, "Choose $label", Modifier.size(20.dp))
        }
        HorizontalDivider(Modifier.padding(top = 12.dp), color = CombatColors.Pink)
    }
}

/** Picker contents are supplied by the host; this library owns the presentation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CombatSelectSheet(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Text(title, Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge)
        options.forEach { option ->
            ListItem(headlineContent = { Text(option) }, leadingContent = { RadioButton(option == selected, onClick = null) },
                modifier = Modifier.clickable { onSelect(option); onDismiss() })
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable fun CombatBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val icons = listOf("statistics_icon", "location_pin", "schedule", "chat", "profile")
    val labels = listOf("Statistics", "Discover", "Schedule", "Chat", "Profile")
    val context = LocalContext.current
    @Composable fun Asset(name: String, modifier: Modifier, description: String? = null) {
        val request = remember(name) { ImageRequest.Builder(context).data("file:///android_asset/combat_uikit/landing_$name.svg").decoderFactory(SvgDecoder.Factory()).build() }
        AsyncImage(request, description, modifier, contentScale = ContentScale.Fit)
    }
    Column(Modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(375f / 109f)) {
            val scale = maxWidth / 375.dp
            Asset("container_task_bar", Modifier.fillMaxSize())
            Asset("base_ellipse", Modifier.align(Alignment.TopCenter).padding(top = (3 * scale).dp).size((81 * scale).dp))
            Row(Modifier.fillMaxWidth().padding(top = (27 * scale).dp), verticalAlignment = Alignment.Top) {
                labels.forEachIndexed { index, label ->
                    Column(Modifier.weight(1f).height((72 * scale).dp).clickable { onSelect(index) }
                        .semantics { this.selected = index == selected; contentDescription = label },
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        if (index == 2) {
                            Asset(icons[index], Modifier.size((25.5f * scale).dp, (30.5f * scale).dp))
                        } else {
                            Spacer(Modifier.height((17 * scale).dp))
                            val iconSize = when (index) { 0 -> 18.7f to 16.2f; 1 -> 13.9f to 17.8f; 3 -> 17f to 15.6f; else -> 16.5f to 16.5f }
                            Asset(icons[index], Modifier.size((iconSize.first * scale).dp, (iconSize.second * scale).dp))
                            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White)
                            if (index == selected) Box(Modifier.width(14.dp).height(2.dp).background(Color.White, CircleShape))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.fillMaxWidth().background(CombatColors.Pink).windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
