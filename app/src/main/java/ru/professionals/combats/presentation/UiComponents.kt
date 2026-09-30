package ru.professionals.combats.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

internal val Pink = Color(0xFFFA5075)
internal val Ink = Color(0xFF030303)
internal val Muted = Color(0xFF777783)
internal val PinkGradient = Brush.linearGradient(listOf(Color(0xFFFF6480), Color(0xFFF22E63)))

@Composable
internal fun FigmaAsset(name: String, modifier: Modifier = Modifier, description: String? = null) {
    AsyncImage("file:///android_asset/figma/$name", description, modifier, contentScale = ContentScale.Fit)
}

@Composable
internal fun PageHeader(title: String, onBack: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                Box(Modifier.size(40.dp).clickable(onClick = onBack), contentAlignment = Alignment.CenterStart) {
                    FigmaAsset("schedule_back_icon.svg", Modifier.size(22.dp), "Back")
                }
            }
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
        }
        Text(title, color = Pink, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp))
    }
}

@Composable
internal fun Avatar(url: String?, name: String, size: Int = 48, onClick: (() -> Unit)? = null) {
    val modifier = Modifier.size(size.dp).clip(CircleShape).background(Color(0xFFFFE9EF))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    Box(modifier, contentAlignment = Alignment.Center) {
        if (url.isNullOrBlank()) Text(name.take(1).uppercase().ifEmpty { "?" }, color = Pink, fontWeight = FontWeight.Bold,
            fontSize = (size / 3).sp)
        else AsyncImage(url, "$name profile photo", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
internal fun EmptyState(title: String, message: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(message, color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (action != null) TextButton(onClick = onAction) { Text(action, color = Pink) }
    }
}

@Composable
internal fun ErrorBanner(message: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.padding(16.dp).fillMaxWidth(), color = Color(0xFFFFE7ED), shape = RoundedCornerShape(12.dp), shadowElevation = 6.dp) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), color = Color(0xFF9F173C), fontSize = 13.sp)
            TextButton(onClick = onClose) { Text("✕", color = Color(0xFF9F173C)) }
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(text, color = Pink, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
}
