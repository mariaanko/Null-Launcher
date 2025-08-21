package com.wordpress.mariaanko.nulllauncher

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                typography = AppTypography
            ) {
                MinimalLauncherApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalLauncherApp() {
    val context = LocalContext.current
    val pm = context.packageManager

    val apps = remember {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(intent, 0)
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString() }
    }

    var query by remember { mutableStateOf("") }

    val suggestions = remember(query) {
        if (query.isBlank()) emptyList()
        else apps.filter {
            it.loadLabel(pm).toString().contains(query, ignoreCase = true)
        }
    }

    MaterialTheme {
        BackHandler(enabled = true) {
            query = ""
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(96.dp))
                Clock()
                SearchBar(
                    query = query,
                    onQueryChange = { query = it }
                )

                SystemInfo(query)

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    items(suggestions.size) { index ->
                        val appInfo = suggestions[index]
                        val label = appInfo.loadLabel(pm).toString()

                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val launchIntent =
                                        pm.getLaunchIntentForPackage(appInfo.activityInfo.packageName)
                                    if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                    }
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search apps…") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(62.dp)),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Search
        )
    )
}

@Composable
fun Clock() {
    var dateTime by remember { mutableStateOf(getCurrentDateTime()) }

    LaunchedEffect(Unit) {
        while (true) {
            dateTime = getCurrentDateTime()
            kotlinx.coroutines.delay(1000)
        }
    }

    Text(
        text = dateTime,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

private fun getCurrentDateTime(): String {
    val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return formatter.format(java.util.Date())
}

@Composable
fun SystemInfo(query: String) {
    val context = LocalContext.current

    if (query.isBlank()) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val memInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memInfo)
        val freeRamMB = memInfo.availMem / (1024 * 1024)
        val ramMB = memInfo.totalMem / (1024 * 1024)

        // Storage info
        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalBytes = statFs.blockSizeLong * statFs.blockCountLong
        val availableBytes = statFs.blockSizeLong * statFs.availableBlocksLong
        val totalGB = totalBytes / (1024 * 1024 * 1024)
        val freeGB = availableBytes / (1024 * 1024 * 1024)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Battery: $batteryLevel%", fontSize = 14.sp, color = Color.White)
            Text("RAM: ${freeRamMB}MB free of ${ramMB} GB", fontSize = 14.sp, color = Color.White)
            Text("Storage: ${freeGB}GB / ${totalGB}GB", fontSize = 14.sp, color = Color.White)
        }
    }
}

val AppFont = FontFamily(
    Font(R.font.lower_pixel_regular, FontWeight.Normal)
)

val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)