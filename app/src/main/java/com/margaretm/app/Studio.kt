package com.margaretm.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =====================================================================================
//  Studio.kt  -  "Paint Studio": finger-paint + word stickers. Every sticker speaks its name,
//  so painting a scene doubles as vocabulary practice.
// =====================================================================================
sealed class Mark
class LineMark(val color: Color, val w: Float) : Mark() { val pts = mutableStateListOf<Offset>() }
class StampMark(val e: String, val at: Offset, val size: Float) : Mark()

private val studioColors = listOf(Color(0xFFE05A5A), Color(0xFFEB9A4B), Color(0xFFE8CB5A), Color(0xFF5FBF7F), Color(0xFF4F9BE0), Color(0xFF9A7BE0), Color(0xFFD96FA4), Color(0xFF8C6B55), Color(0xFF1B2430), Color(0xFFF2F5F8))   // the one place real colour is welcome: the child's paint box

@Composable fun PaintStudio(v: Voice, onDone: () -> Unit, back: () -> Unit) {
    val pal = LocalPal.current
    val marks = remember { mutableStateListOf<Mark>() }
    var tool by remember { mutableIntStateOf(0) }
    var color by remember { mutableStateOf(studioColors[0]) }
    var width by remember { mutableFloatStateOf(14f) }
    var theme by remember { mutableIntStateOf(0) }
    var stamp by remember { mutableStateOf(bank.first { it.theme == themeNames[0] }) }
    var scene by remember { mutableIntStateOf(0) }
    var rewarded by remember { mutableStateOf(false) }
    val items = remember(theme) { bank.filter { it.theme == themeNames[theme] } }
    fun maybeReward() { if (!rewarded && marks.size >= 8) { rewarded = true; onDone(); v.say("What a beautiful painting!") } }
    LaunchedEffect(Unit) { v.say("Welcome to the paint studio! Draw, or tap a sticker to place it.") }

    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(8.dp)) {
        TopBar("🎨 Paint Studio", back) { Chip("✨ ${marks.size}") }
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)).border(1.dp, Hair, RoundedCornerShape(20.dp))) {
            Canvas(Modifier.fillMaxSize()
                .pointerInput(tool, color, width) {
                    detectTapGestures { o ->
                        if (tool == 1) { marks.add(StampMark(stamp.e, o, 96f)); v.say(stamp.w.lowercase()) }
                        else { val l = LineMark(color, width); l.pts.add(o); l.pts.add(o + Offset(0.1f, 0f)); marks.add(l) }
                        maybeReward()
                    }
                }
                .pointerInput(tool, color, width) {
                    if (tool == 0) detectDragGestures(onDragStart = { o -> val l = LineMark(color, width); l.pts.add(o); marks.add(l) }, onDragEnd = { maybeReward() }) { ch, _ ->
                        ch.consume(); (marks.lastOrNull() as? LineMark)?.pts?.add(ch.position)
                    }
                }) {
                // scene
                when (scene) {
                    0 -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF2B4A63), Color(0xFF4E7391))), Offset.Zero, size)
                        drawRect(Color(0xFF3E6B53), Offset(0f, size.height * .80f), Size(size.width, size.height * .20f))
                        drawCircle(Color(0xFFEFE3A3), size.width * .09f, Offset(size.width * .82f, size.height * .14f)) }
                    1 -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF070B12), Color(0xFF1B2638))), Offset.Zero, size)
                        drawCircle(Color(0xFFFFF3B0), size.width * .07f, Offset(size.width * .8f, size.height * .15f))
                        for (i in 0 until 24) drawCircle(Color.White.copy(alpha = .8f), 2.5f, Offset(((i * 137) % 100) / 100f * size.width, ((i * 59) % 70) / 100f * size.height)) }
                    else -> drawRect(Color(0xFFF2F5F8), Offset.Zero, size)
                }
                for (m in marks) when (m) {
                    is LineMark -> if (m.pts.size > 1) {
                        val p = Path(); p.moveTo(m.pts[0].x, m.pts[0].y)
                        for (j in 1 until m.pts.size) p.lineTo(m.pts[j].x, m.pts[j].y)
                        drawPath(p, m.color, style = Stroke(width = m.w, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    is StampMark -> text2d(m.e, m.at.x, m.at.y, m.size, Color.Black, shadow = false)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            listOf("✏️ Brush", "🧸 Stickers").forEachIndexed { i, t ->
                Box(Modifier.weight(1f).background(if (tool == i) pal.a1 else Panel2, RoundedCornerShape(14.dp)).clickable { tool = i; if (i == 1) v.say(stamp.w.lowercase()) }.padding(vertical = 8.dp), Alignment.Center) {
                    Text(t, fontWeight = FontWeight.SemiBold, color = if (tool == i) Dark else Ink) } }
        }
        if (tool == 0) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
            studioColors.forEach { c -> Box(Modifier.size(if (c == color) 42.dp else 34.dp).background(c, CircleShape).border(if (c == color) 3.dp else 1.dp, if (c == color) pal.a1 else Hair, CircleShape).clickable { color = c }) }
            Spacer(Modifier.width(8.dp))
            listOf(6f, 14f, 28f).forEach { w -> Box(Modifier.size(38.dp).background(if (w == width) pal.a1.copy(alpha = .3f) else Panel2, CircleShape).clickable { width = w }, Alignment.Center) {
                Box(Modifier.size((w / 1.3f + 4f).dp).background(Ink, CircleShape)) } }
        } else {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), Arrangement.spacedBy(6.dp)) {
                themeNames.forEachIndexed { i, n -> Box(Modifier.background(if (theme == i) pal.a1 else Panel2, RoundedCornerShape(12.dp)).clickable { theme = i; stamp = bank.first { it.theme == themeNames[i] } }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(n, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (theme == i) Dark else Ink) } }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
                items.forEach { it0 -> Box(Modifier.background(if (it0.w == stamp.w) pal.a1.copy(alpha = .3f) else Panel2, RoundedCornerShape(14.dp)).clickable { stamp = it0; v.say(it0.w.lowercase()) }.padding(8.dp)) { Text(it0.e, fontSize = 32.sp) } }
            }
            Text("Tap the picture to place  ${stamp.e} ${stamp.w}", fontSize = 12.sp, color = Muted, modifier = Modifier.fillMaxWidth())
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            MiniBtn("↩ Undo", Modifier.weight(1f), emptyList()) { if (marks.isNotEmpty()) marks.removeAt(marks.lastIndex) }
            MiniBtn("🖼️ Scene", Modifier.weight(1f), emptyList()) { scene = (scene + 1) % 3 }
            MiniBtn("🗑️ Clear", Modifier.weight(1f), emptyList()) { marks.clear() }
        }
    }
}
