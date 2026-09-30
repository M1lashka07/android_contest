package ru.professionals.combats.presentation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.professionals.domain.*
import ru.professionals.uikit.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
internal fun LandingScreen(player: Player?, popular: List<Player>, onMenu: () -> Unit, onNavigate: (String) -> Unit, onPlayer: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clickable(onClick = onMenu), contentAlignment = Alignment.CenterStart) {
                FigmaAsset("landing_hamburger.svg", Modifier.size(20.dp), "Open menu")
            }
            Spacer(Modifier.weight(1f))
            Avatar(player?.avatarUrl, player?.name ?: "Player", 26) { onNavigate("profile") }
            Text(player?.name ?: "Player", Modifier.padding(start = 8.dp).clickable { onNavigate("profile") }, fontSize = 10.sp)
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 30.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { LandingCard("Schedule", "Easily schedule event/games then find like minded players for battle. You up for it?", "landing_image1.png", "schedule", onNavigate, 112, 107) }
            item { LandingCard("Statistics", "All data from previous and upcoming games can be found here", "landing_image.png", "statistics", onNavigate, 121, 91) }
            item { LandingCard("Discover Combats", "Find out what’s new and compete among players with new challenges and earn game points", "landing_image2.png", "discover", onNavigate, 180, 102) }
            item { LandingCard("Message Players", "Found the profile of a player that interests you? Start a conversation", "landing_image3.png", "players", onNavigate, 131, 103) }
            item {
                SectionLabel("MOST POPULAR PLAYERS")
                if (popular.isEmpty()) Text("Players will appear here after their first games.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp))
            }
            items(popular, key = { it.id }) { person ->
                PlayerRow(person) { onPlayer(person.id) }
            }
        }
    }
}

@Composable
private fun LandingCard(title: String, body: String, asset: String, target: String, onNavigate: (String) -> Unit, imageWidth: Int, imageHeight: Int) {
    Box(Modifier.fillMaxWidth().height(169.dp).clip(RoundedCornerShape(10.dp)).background(PinkGradient).clickable { onNavigate(target) }) {
        Column(Modifier.padding(start = 24.dp, top = 25.dp).width(166.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(body, color = Color.White, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 12.dp))
        }
        FigmaAsset(asset, Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 14.dp).size(imageWidth.dp, imageHeight.dp))
        FigmaAsset("landing_forward_arrow.svg", Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 24.dp).size(15.dp, 10.dp))
        if (target == "schedule") {
            FigmaAsset("landing_vector.svg", Modifier.align(Alignment.BottomEnd).padding(end = 11.dp, bottom = 12.dp).size(131.dp, 14.dp))
            FigmaAsset("landing_vector1.svg", Modifier.align(Alignment.TopEnd).padding(end = 50.dp, top = 20.dp).size(29.dp, 5.dp))
            FigmaAsset("landing_vector2.svg", Modifier.align(Alignment.TopEnd).padding(end = 75.dp, top = 40.dp).size(8.dp))
        } else if (target == "statistics") {
            FigmaAsset("landing_ellipse2.svg", Modifier.align(Alignment.TopEnd).padding(end = 22.dp, top = 34.dp).size(22.dp))
            FigmaAsset("landing_ellipse3.svg", Modifier.align(Alignment.TopEnd).padding(end = 75.dp, top = 56.dp).size(9.dp))
        }
    }
}

@Composable
internal fun MenuContent(player: Player?, onClose: () -> Unit, onNavigate: (String) -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxHeight().width(320.dp).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp)) {
        Box(Modifier.align(Alignment.End).size(44.dp).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
            FigmaAsset("sidemenu_close_icon.svg", Modifier.size(26.dp), "Close menu")
        }
        Row(Modifier.padding(vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(player?.avatarUrl, player?.name ?: "Player", 54)
            Column(Modifier.padding(start = 16.dp)) {
                Text(player?.name ?: "Player", color = Pink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${player?.points ?: 0} game points", color = Color(0xFFB98D1D), fontSize = 10.sp)
            }
        }
        val entries = listOf(Triple("My Profile", "profile", "profile_icon"), Triple("Schedule", "schedule", "schedule_icon"),
            Triple("Statistics", "statistics", "statistics_icon"), Triple("Discover Combat", "discover", "location_pin"),
            Triple("Players", "players", "chat"), Triple("Privacy Policy", "privacy", "change_language_icon"), Triple("User Agreement", "terms", "change_skin"))
        entries.forEach { (title, target, icon) ->
            Row(Modifier.fillMaxWidth().height(58.dp).clickable { onNavigate(target) }, verticalAlignment = Alignment.CenterVertically) {
                FigmaAsset("sidemenu_$icon.svg", Modifier.size(18.dp))
                Text(title, Modifier.padding(start = 24.dp), fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(35.dp))
        TextButton(onClick = onLogout, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            FigmaAsset("sidemenu_logout_icon.svg", Modifier.size(18.dp))
            Text("Logout", color = Ink, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

internal data class ScheduleDraft(val title: String = "", val category: GameCategory = GameCategory.IMAGE,
    val start: Long = System.currentTimeMillis() + 3_600_000, val end: Long = System.currentTimeMillis() + 7_200_000,
    val prize: String = "100", val description: String = "", val remind: Boolean = false)

@Composable
internal fun ScheduleScreen(draft: ScheduleDraft, onDraft: (ScheduleDraft) -> Unit, busy: Boolean, onBack: () -> Unit, onPublish: () -> Unit) {
    var categoryExpanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        PageHeader("Schedule Game", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 31.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            CombatTextField(draft.title, { onDraft(draft.copy(title = it)) }, "Game Name")
            Box {
                CombatSelect("Category", draft.category.name.lowercase().replaceFirstChar { it.uppercase() }, { categoryExpanded = true })
                DropdownMenu(categoryExpanded, { categoryExpanded = false }) {
                    GameCategory.entries.forEach { category -> DropdownMenuItem(text = { Text(category.name) }, onClick = { onDraft(draft.copy(category = category)); categoryExpanded = false }) }
                }
            }
            CombatTextField(draft.prize, { onDraft(draft.copy(prize = it.filter(Char::isDigit))) }, "Winning Price (points)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            SectionLabel("FROM")
            DateTimeRow(draft.start) { onDraft(draft.copy(start = it)) }
            SectionLabel("TO")
            DateTimeRow(draft.end) { onDraft(draft.copy(end = it)) }
            Spacer(Modifier.height(20.dp))
            CombatTextField(draft.description, { onDraft(draft.copy(description = it)) }, "Description")
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("REMINDERS", color = Pink, fontSize = 12.sp, modifier = Modifier.weight(1f))
                CombatCheckbox(draft.remind, { onDraft(draft.copy(remind = it)) }, "Notification")
            }
            CombatButton("Publish", onPublish, Modifier.align(Alignment.CenterHorizontally).width(224.dp), enabled = !busy, loading = busy)
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun DateTimeRow(value: Long, onChange: (Long) -> Unit) {
    val context = LocalContext.current
    val date = Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        TextButton(onClick = {
            DatePickerDialog(context, { _, year, month, day ->
                val selected = Calendar.getInstance().apply { timeInMillis = value; set(year, month, day) }
                onChange(selected.timeInMillis)
            }, date.year, date.monthValue - 1, date.dayOfMonth).show()
        }, modifier = Modifier.weight(1f)) { Text(date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")), color = Ink, fontSize = 12.sp) }
        TextButton(onClick = {
            TimePickerDialog(context, { _, hour, minute ->
                val selected = Calendar.getInstance().apply { timeInMillis = value; set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                onChange(selected.timeInMillis)
            }, date.hour, date.minute, true).show()
        }) { Text(date.format(DateTimeFormatter.ofPattern("HH:mm")), color = Ink, fontSize = 12.sp) }
    }
}

@Composable
internal fun SuccessScreen(onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(36.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✓", color = Pink, fontSize = 82.sp)
        Text("Success!", color = Pink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Your game has been published.", color = Muted, modifier = Modifier.padding(vertical = 24.dp))
        CombatButton("Back to Home", onContinue)
    }
}

@Composable
internal fun DiscoverScreen(games: List<Combat>, query: String, onQuery: (String) -> Unit, category: String, onCategory: (String) -> Unit,
    filter: String, onFilter: (String) -> Unit, busy: Boolean, onBack: () -> Unit, onOpen: (String) -> Unit, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageHeader("Discover Combats", onBack, { TextButton(onClick = onRefresh, enabled = !busy) { Text("Refresh", fontSize = 11.sp) } })
        Column(Modifier.padding(horizontal = 30.dp)) {
            CombatTextField(query, onQuery, "Search games")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("All", "Image", "Circle").forEach { label ->
                FilterChip(category == label, { onCategory(label) }, label = { Text(label, fontSize = 11.sp) })
            } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Upcoming", "Live", "Finished").forEach { label ->
                FilterChip(filter == label, { onFilter(label) }, label = { Text(label, fontSize = 11.sp) })
            } }
        }
        val visible = games.filter { game ->
            game.title.contains(query, ignoreCase = true) && (category == "All" || game.category.name.equals(category, true)) &&
                when (filter) { "Finished" -> game.status == GameStatus.FINISHED; "Live" -> game.status == GameStatus.ACTIVE; else -> game.status == GameStatus.SCHEDULED }
        }.sortedBy { it.startsAtEpochMillis }
        if (busy && games.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 30.dp))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (visible.isEmpty() && !busy) item { EmptyState("No games yet", "Try another filter or schedule the first combat.", "Refresh", onRefresh) }
            items(visible, key = { it.id }) { game ->
                CombatCard(Modifier.fillMaxWidth().clickable { onOpen(game.id) }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (game.category == GameCategory.CIRCLE) "◉" else "▦", color = if (game.category == GameCategory.CIRCLE) Color(0xFF4BA7F3) else Pink, fontSize = 36.sp, modifier = Modifier.padding(end = 16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(game.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${game.category.name} · ${formatDate(game.startsAtEpochMillis)}", color = Muted, fontSize = 11.sp)
                            Text(if (game.guestId == null) "Opponent wanted" else game.status.name.lowercase(), color = Pink, fontSize = 11.sp)
                        }
                        Text("›", color = Pink, fontSize = 28.sp)
                    }
                }
            }
        }
    }
}

internal fun formatDate(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM, HH:mm"))

@Composable
internal fun PlayerRow(player: Player, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(player.avatarUrl, player.name)
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(player.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${player.points} points", color = Pink, fontSize = 12.sp)
        }
        Text("›", color = Pink, fontSize = 25.sp)
    }
}

@Composable
internal fun StatisticsScreen(stats: PlayerStatistics?, busy: Boolean, onBack: () -> Unit, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeader("Statistics", onBack, { TextButton(onClick = onRefresh, enabled = !busy) { Text("Refresh", fontSize = 11.sp) } })
        if (stats == null) { EmptyState("Your statistics", if (busy) "Loading your games…" else "Connect to the server to load your results."); return }
        Column(Modifier.padding(horizontal = 30.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            CombatCard {
                Text("THIS WEEK EARNINGS", color = Muted, fontSize = 11.sp)
                Text("${stats.weeklyPoints}", color = Pink, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                Text("game points", color = Muted, fontSize = 11.sp)
                Row(Modifier.fillMaxWidth().height(150.dp).padding(top = 20.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.Bottom) {
                    val max = stats.dailyPoints.maxOrNull()?.coerceAtLeast(1) ?: 1
                    stats.dailyPoints.forEachIndexed { i, points ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(points.toString(), color = Muted, fontSize = 9.sp)
                            Box(Modifier.width(20.dp).height((points.toFloat() / max * 95).coerceAtLeast(3f).dp).clip(RoundedCornerShape(6.dp)).background(PinkGradient))
                            Text(listOf("M", "T", "W", "T", "F", "S", "S").getOrElse(i) { "" }, color = Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
            CombatCard {
                Text("Played Games", fontWeight = FontWeight.Bold)
                Text("Won games by category", color = Muted, fontSize = 11.sp)
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatCircle("Circle", stats.circleWins, Color(0xFF4BA7F3))
                    StatCircle("Image", stats.imageWins, Pink)
                }
            }
            CombatCard {
                Text("Scheduled Games", fontWeight = FontWeight.Bold)
                Text("${stats.scheduledThisWeek}", color = Pink, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Text("created this week", color = Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StatCircle(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) { drawCircle(color.copy(alpha = .14f)); drawCircle(color, style = androidx.compose.ui.graphics.drawscope.Stroke(6.dp.toPx())) }
            Text(value.toString(), color = color, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
    }
}
