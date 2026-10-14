package com.example.snapprintmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snapprintmonitor.ui.theme.SnapPrintMonitorTheme
import com.google.firebase.database.*
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent {
            SnapPrintMonitorTheme(darkTheme = false, dynamicColor = false) {
                PrinterStatusScreen()
            }
        }
    }
}

private val Ink = Color(0xFF232323)
private val SecondaryInk = Color(0xFF626262)

@Composable
fun PrinterStatusScreen() {
    val canon = rememberPrinterRecord("canon")
    val epson = rememberPrinterRecord("epson")
    var connected by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val ref = FirebaseDatabase.getInstance().getReference(".info/connected")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { connected = snapshot.value == true }
            override fun onCancelled(error: DatabaseError) { connected = false }
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFFD5D5D5))) {
        val artwork = painterResource(R.drawable.background_android)
        // Fit preserves the entire title and ornaments on tall phone screens.
        val artworkHeight = minOf(maxHeight, maxWidth * (artwork.intrinsicSize.height / artwork.intrinsicSize.width))
        val artworkTop = (maxHeight - artworkHeight) / 2
        val headerBottom = artworkTop + artworkHeight * 0.29f
        val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

        Image(
            painter = artwork,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(top = maxOf(headerBottom, topInset) + 12.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("KIOSK MONITOR", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Text("PRINTER STATUS", color = SecondaryInk, fontSize = 10.sp, letterSpacing = 1.5.sp)
            PrinterCard("CANON", canon, connected)
            PrinterCard("EPSON", epson, connected)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PrinterCard(name: String, record: PrinterRecord, connected: Boolean) {
    val displayState = when {
        record.loading -> "LOADING"
        record.error != null || !connected -> "UNKNOWN"
        else -> record.state
    }
    val indicator = when (displayState) {
        "READY" -> Color(0xFF3C8054)
        "PRINTING", "NOT READY" -> Color(0xFFB87520)
        "OFFLINE", "ERROR" -> Color(0xFFB43B3B)
        else -> Color(0xFF777777)
    }
    val description = when {
        record.error != null -> record.error
        !connected -> "Firebase disconnected"
        else -> when (displayState) {
            "READY" -> "Printer Online"
            "PRINTING" -> "Printing"
            "OFFLINE" -> "Printer Offline"
            "ERROR" -> "Printer Error"
            "NOT READY" -> "Printer Not Ready"
            "LOADING" -> "Waiting for status"
            else -> "Status Unknown"
        }
    }
    val updated = record.updatedAt?.let {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(it))
    } ?: "—"
    Card(
        modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9), contentColor = Ink),
        border = BorderStroke(1.dp, Color(0xFFD3D3D3)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(name, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(10.dp).background(indicator, CircleShape))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(displayState, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    Text(description, color = SecondaryInk, fontSize = 13.sp)
                }
            }
            Text("Last Updated: $updated", color = SecondaryInk, fontSize = 11.sp)
        }
    }
}

private data class PrinterRecord(
    val state: String = "UNKNOWN",
    val updatedAt: Long? = null,
    val loading: Boolean = true,
    val error: String? = null
)

@Composable
private fun rememberPrinterRecord(key: String): PrinterRecord {
    var record by remember(key) { mutableStateOf(PrinterRecord()) }
    DisposableEffect(key) {
        val ref = FirebaseDatabase.getInstance().getReference("printers").child(key)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Read untyped values: malformed objects, lists and scalars never enter a typed conversion.
                val values = snapshot.value as? Map<*, *>
                val state = (values?.get("state") as? String)
                    ?.takeIf { it in setOf("READY", "PRINTING", "OFFLINE", "ERROR", "NOT READY", "UNKNOWN") }
                    ?: "UNKNOWN"
                val rawTime = values?.get("updatedAt")
                val timestamp = when (rawTime) {
                    is Long -> rawTime
                    is Double -> rawTime.takeIf { it.isFinite() && it % 1.0 == 0.0 }?.toLong()
                    else -> null
                }?.takeIf { it in 1L..253402300799999L }
                record = PrinterRecord(state, timestamp, loading = false)
            }
            override fun onCancelled(error: DatabaseError) {
                record = PrinterRecord(loading = false, error = "Firebase read failed")
            }
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }
    return record
}
