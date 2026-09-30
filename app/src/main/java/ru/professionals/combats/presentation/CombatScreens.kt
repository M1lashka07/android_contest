package ru.professionals.combats.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import ru.professionals.domain.*
import ru.professionals.uikit.*

@Composable
internal fun CombatInformationScreen(game: Combat?, userId: String?, busy: Boolean, onBack: () -> Unit,
    onJoin: () -> Unit, onStart: () -> Unit, onPlayer: (String) -> Unit, onRefresh: () -> Unit) {
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { clock = System.currentTimeMillis(); delay(1000) } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeader("Combat Information", onBack, { TextButton(onClick = onRefresh, enabled = !busy) { Text("Refresh", fontSize = 11.sp) } })
        if (game == null) { EmptyState("Combat", if (busy) "Loading game…" else "This game could not be loaded.", "Try again", onRefresh); return }
        Column(Modifier.padding(30.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(18.dp)).background(PinkGradient), contentAlignment = Alignment.Center) {
                Text(if (game.category == GameCategory.IMAGE) "▦" else "◉", color = Color.White, fontSize = 88.sp)
            }
            Text(game.title, fontWeight = FontWeight.Bold, fontSize = 25.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(game.category.name, color = Pink, fontSize = 12.sp)
                Text(game.status.name, color = Muted, fontSize = 12.sp)
            }
            Text(formatDate(game.startsAtEpochMillis), fontWeight = FontWeight.Bold)
            if (game.description.isNotBlank()) Text(game.description, color = Muted, fontSize = 13.sp)
            Text("Winning points: ${game.winningPoints}", color = Pink, fontWeight = FontWeight.Bold)
            CombatCard {
                Text("Players", fontWeight = FontWeight.Bold)
                TextButton(onClick = { onPlayer(game.hostId) }) { Text("Host${if (game.hostId == userId) " · You" else ""}") }
                if (game.guestId == null) Text("Waiting for an opponent", color = Muted, fontSize = 12.sp)
                else TextButton(onClick = { onPlayer(game.guestId!!) }) { Text("Opponent${if (game.guestId == userId) " · You" else ""}") }
            }
            when {
                game.status == GameStatus.FINISHED -> Text("This combat has finished.", color = Muted)
                userId != game.hostId && userId != game.guestId && game.guestId == null -> CombatButton("Join Combat", onJoin, Modifier.fillMaxWidth(), enabled = !busy, loading = busy)
                userId == game.hostId || userId == game.guestId -> {
                    val hasOpponent = game.guestId != null
                    val canStart = hasOpponent && clock >= game.startsAtEpochMillis
                    CombatButton(if (game.status == GameStatus.ACTIVE) "Enter Combat" else "Start Combat", onStart, Modifier.fillMaxWidth(), enabled = canStart && !busy, loading = busy)
                    Text(when { !hasOpponent -> "Share this combat and wait for an opponent."; !canStart -> "Starts in ${((game.startsAtEpochMillis - clock) / 60000).coerceAtLeast(0)} min."; else -> "Both players receive the same puzzle. Leaving an active game counts as a loss." }, color = Muted, fontSize = 12.sp)
                }
                else -> Text("This combat already has two players.", color = Muted)
            }
        }
    }
}

@Composable
internal fun PlayersScreen(players: List<Player>, busy: Boolean, onBack: () -> Unit, onRefresh: () -> Unit, onPlayer: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageHeader("Most Popular Players", onBack, { TextButton(onClick = onRefresh, enabled = !busy) { Text("Refresh", fontSize = 11.sp) } })
        LazyColumn(contentPadding = PaddingValues(horizontal = 30.dp)) {
            if (players.isEmpty()) item { EmptyState("Players", if (busy) "Loading players…" else "No players are available yet.") }
            items(players, key = { it.id }) { player -> PlayerRow(player) { onPlayer(player.id) } }
        }
    }
}

internal data class ProfileDraft(val name: String = "", val location: String = "")

@Composable
internal fun ProfileScreen(player: Player?, own: Boolean, draft: ProfileDraft, onDraft: (ProfileDraft) -> Unit,
    editing: Boolean, onEditing: (Boolean) -> Unit, busy: Boolean, onBack: () -> Unit, onSave: () -> Unit,
    onPhoto: () -> Unit, onStatistics: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()) {
        PageHeader(if (own) "My Profile" else "Player Information", onBack)
        if (player == null) { EmptyState("Player", if (busy) "Loading profile…" else "Profile unavailable."); return }
        Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Avatar(player.avatarUrl, player.name, 112, if (own) onPhoto else null)
            if (own) TextButton(onClick = onPhoto, enabled = !busy) { Text("Change photo", color = Pink) }
            if (editing && own) {
                CombatTextField(draft.name, { onDraft(draft.copy(name = it)) }, "Full Name")
                CombatTextField(draft.location, { onDraft(draft.copy(location = it)) }, "Location")
                CombatButton("Save profile", onSave, Modifier.fillMaxWidth(), enabled = !busy, loading = busy)
                TextButton(onClick = { onEditing(false) }) { Text("Cancel") }
            } else {
                Text(player.name, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                if (player.username.isNotBlank()) Text("@${player.username}", color = Pink, fontSize = 13.sp)
                if (player.location.isNotBlank()) Text(player.location, color = Muted, fontSize = 13.sp)
                Text("${player.points}", color = Pink, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                Text("GAME POINTS", color = Muted, fontSize = 11.sp)
                if (own) {
                    CombatButton("Edit profile", { onEditing(true) }, Modifier.fillMaxWidth())
                    CombatButton("My statistics", onStatistics, Modifier.fillMaxWidth(), secondary = true)
                }
            }
        }
    }
}

@Composable
internal fun GameScreen(game: Combat, moves: List<Int>, onMove: (Int) -> Unit, submitting: Boolean,
    onComplete: () -> Unit, onForfeit: () -> Unit) {
    var seconds by remember(game.id) { mutableIntStateOf(0) }
    var leaveDialog by remember { mutableStateOf(false) }
    LaunchedEffect(game.startedAtEpochMillis) {
        while (true) {
            seconds = ((System.currentTimeMillis() - (game.startedAtEpochMillis ?: System.currentTimeMillis())) / 1000).toInt().coerceAtLeast(0)
            delay(500)
        }
    }
    val board = remember(game.imageSeed, moves) {
        imageInitialBoard(game.imageSeed).toMutableList().apply {
            for (index in moves) {
                val empty = indexOf(0)
                if (index in indices && kotlin.math.abs(index / 3 - empty / 3) + kotlin.math.abs(index % 3 - empty % 3) == 1) {
                    this[empty] = this[index]; this[index] = 0
                }
            }
        }
    }
    val solved = if (game.category == GameCategory.IMAGE) board == (1..8).toList() + 0 else moves == circleTargetOrder()
    LaunchedEffect(solved) { if (solved) onComplete() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        PageHeader(if (game.category == GameCategory.IMAGE) "Image Combat" else "Circle Combat", { leaveDialog = true })
        Text(game.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 30.dp))
        CombatTimer(seconds, Modifier.padding(20.dp))
        Text(if (game.category == GameCategory.IMAGE) "Tap a tile next to the empty space.\nRestore the picture to win." else "Build the circles from largest to smallest.\nTap them in the correct order.",
            color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 30.dp))
        Spacer(Modifier.height(24.dp))
        if (game.category == GameCategory.IMAGE) ImagePuzzle(board, game.imageSeed, submitting || solved, onMove)
        else CirclePuzzle(game.imageSeed, moves, submitting || solved, onMove)
        Text("${moves.size} moves", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(20.dp))
        if (submitting) {
            CircularProgressIndicator(Modifier.size(24.dp), color = Pink)
            Text("Saving your result…", fontSize = 12.sp, modifier = Modifier.padding(12.dp))
        }
        if (solved && !submitting) CombatButton("Submit result", onComplete, Modifier.padding(horizontal = 30.dp))
        TextButton(onClick = { leaveDialog = true }) { Text("Forfeit game", color = Pink) }
    }
    if (leaveDialog) AlertDialog(onDismissRequest = { leaveDialog = false }, title = { Text("Leave combat?") },
        text = { Text("Leaving an active combat counts as a loss.") },
        confirmButton = { TextButton(onClick = { leaveDialog = false; onForfeit() }) { Text("Forfeit") } },
        dismissButton = { TextButton(onClick = { leaveDialog = false }) { Text("Keep playing") } })
}

@Composable
private fun ImagePuzzle(board: List<Int>, seed: Int, disabled: Boolean, onMove: (Int) -> Unit) {
    // A seed fixed on the server gives both opponents the same photograph.
    val painter = rememberAsyncImagePainter("https://picsum.photos/seed/combat-$seed/900/900")
    val painterState = painter.state
    if (painterState is AsyncImagePainter.State.Error) {
        EmptyState("Image unavailable", "Check your connection to load the shared puzzle image.")
    } else if (painterState !is AsyncImagePainter.State.Success) {
        Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Pink) }
    } else BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        val side = maxWidth
        Column(Modifier.size(side).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFE9EF))) {
            repeat(3) { row -> Row(Modifier.weight(1f)) {
                repeat(3) { col ->
                    val index = row * 3 + col
                    val value = board[index]
                    Box(Modifier.weight(1f).fillMaxHeight().padding(1.dp).clip(RoundedCornerShape(5.dp))
                        .clickable(enabled = value != 0 && !disabled) { onMove(index) }) {
                        if (value != 0) {
                            Canvas(Modifier.fillMaxSize()) {
                                clipRect {
                                    translate(-((value - 1) % 3) * size.width, -((value - 1) / 3) * size.height) {
                                        with(painter) { draw(Size(size.width * 3, size.height * 3)) }
                                    }
                                }
                            }
                            Box(Modifier.align(Alignment.BottomEnd).padding(4.dp).size(20.dp).background(Color.Black.copy(alpha = .5f), CircleShape), contentAlignment = Alignment.Center) {
                                Text(value.toString(), color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }
            } }
        }
    }
}

@Composable
private fun CirclePuzzle(seed: Int, moves: List<Int>, disabled: Boolean, onMove: (Int) -> Unit) {
    val order = remember(seed) { circleInitialOrder(seed) }
    val colors = listOf(Color(0xFFF4C73E), Color(0xFFA693F5), Color(0xFF79D4BB), Color(0xFF4BA7F3), Pink)
    Box(Modifier.size(265.dp).border(1.dp, Color(0xFFF0E9EC), CircleShape), contentAlignment = Alignment.Center) {
        moves.forEach { value -> Box(Modifier.size((value * 44 + 30).dp).background(colors[value - 1], CircleShape)) }
        if (moves.isEmpty()) Text("Start with the largest", color = Muted, fontSize = 12.sp)
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        order.forEach { value ->
            val used = value in moves
            Box(Modifier.size(62.dp).clickable(enabled = !disabled && !used) { onMove(value) }, contentAlignment = Alignment.Center) {
                Box(Modifier.size((value * 8 + 18).dp).background(if (used) Color(0xFFEDEAF0) else colors[value - 1], CircleShape), contentAlignment = Alignment.Center) {
                    Text(if (used) "✓" else value.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun ResultScreen(game: Combat?, userId: String?, onHome: () -> Unit, onStatistics: () -> Unit) {
    val won = game?.winnerId != null && game.winnerId == userId
    Column(Modifier.fillMaxSize().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(if (won) "★" else "◉", color = Pink, fontSize = 90.sp)
        Text(if (won) "You won!" else "Combat finished", color = Pink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(if (won) "Your result has been saved." else "Your game has ended. Open statistics to see your results.", color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 24.dp))
        CombatButton("Back to Home", onHome, Modifier.fillMaxWidth())
        CombatButton("View statistics", onStatistics, Modifier.fillMaxWidth().padding(top = 12.dp), secondary = true)
    }
}
