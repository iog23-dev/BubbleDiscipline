package com.bubblediscipline

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bubblediscipline.ui.theme.BubbleDisciplineTheme
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val appTheme by settingsViewModel.appTheme.collectAsState()
            
            // Definir colores basados en el tema
            val primaryColor = when (appTheme) {
                "Azul" -> Color(0xFF2196F3)
                "Verde" -> Color(0xFF4CAF50)
                "Naranja" -> Color(0xFFFF9800)
                "Morado" -> Color(0xFF9C27B0)
                else -> Color(0xFFFF4081) // Rosa por defecto
            }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = primaryColor,
                    secondary = primaryColor.copy(alpha = 0.7f),
                    tertiary = Color(0xFF03DAC5)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigationWrapper(primaryColor = primaryColor)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationWrapper(
    missionViewModel: MissionViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    primaryColor: Color
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf("Misiones") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "BubbleDiscipline 🫧",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("Mis Misiones") },
                    selected = currentScreen == "Misiones",
                    onClick = {
                        currentScreen = "Misiones"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Ajustes") },
                    selected = currentScreen == "Ajustes",
                    onClick = {
                        currentScreen = "Ajustes"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (currentScreen == "Misiones") "Panel de Disciplina" else "Ajustes de Burbuja") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                        }
                    },
                    actions = {
                        val context = LocalContext.current
                        IconButton(onClick = {
                             val intent = Intent(context, BubbleForegroundService::class.java)
                             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                 context.startForegroundService(intent)
                             } else {
                                 context.startService(intent)
                             }
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Test Burbuja", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { testAlarm(context) }) {
                            Icon(Icons.Default.Timer, contentDescription = "Test Alarma", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    "Misiones" -> MissionsScreen(missionViewModel, primaryColor)
                    "Ajustes" -> SettingsScreen(settingsViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsScreen(viewModel: MissionViewModel, primaryColor: Color) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingMission by remember { mutableStateOf<Mission?>(null) }

    val focusedMissionId by viewModel.focusedMissionId.collectAsState()
    val missionsList by viewModel.allMissions.collectAsState(initial = emptyList())
    val focusedMission = missionsList.find { it.id == focusedMissionId }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Activas", "Desactivadas")

    val filteredMissions = missionsList.filter { 
        if (selectedTab == 0) it.isEnabled else !it.isEnabled 
    }

    // Gestión de permisos
    var hasNotificationPermission by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            )
        } else {
            mutableStateOf(true)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasNotificationPermission = isGranted }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        editingMission = null
                        showSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir misión")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize()) {
                    Text("Misiones de Mantenimiento", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (filteredMissions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (selectedTab == 0) "No hay misiones activas" else "No hay misiones desactivadas",
                                style = MaterialTheme.typography.bodyLarge, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredMissions) { mission ->
                                BubbleItem(mission, primaryColor) {
                                    viewModel.setFocusedMission(mission.id)
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = focusedMission != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            focusedMission?.let { mission ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .clickable(enabled = true, onClick = { viewModel.setFocusedMission(null) }),
                    contentAlignment = Alignment.Center
                ) {
                    FocusedBubbleView(
                        mission = mission,
                        primaryColor = primaryColor,
                        onEdit = {
                            editingMission = mission
                            viewModel.setFocusedMission(null)
                            showSheet = true
                        },
                        onDelete = {
                            viewModel.delete(mission)
                            viewModel.setFocusedMission(null)
                        }
                    )
                }
            }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showSheet = false
                    editingMission = null
                },
                sheetState = sheetState
            ) {
                MissionEditorContent(
                    initialMission = editingMission,
                    primaryColor = primaryColor,
                    onSave = { mission ->
                        viewModel.saveMission(mission)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showSheet = false
                            editingMission = null
                        }
                    },
                    onCancel = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showSheet = false
                            editingMission = null
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val bubbleColor by viewModel.bubbleColor.collectAsState()
    val bubbleSpeed by viewModel.bubbleSpeed.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()

    val colors = listOf("#FF4081", "#2196F3", "#4CAF50", "#FF9800", "#9C27B0")
    val themes = listOf("Rosa", "Azul", "Verde", "Naranja", "Morado")

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Personalización", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Tema de la Aplicación", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            themes.forEach { theme ->
                FilterChip(
                    selected = appTheme == theme,
                    onClick = { viewModel.setAppTheme(theme) },
                    label = { Text(theme) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Color de las Burbujas", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            colors.forEach { colorHex ->
                val color = Color(android.graphics.Color.parseColor(colorHex))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (bubbleColor == colorHex) 4.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                        .clickable { viewModel.setBubbleColor(colorHex) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Velocidad de Movimiento: ${bubbleSpeed.toInt()}", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = bubbleSpeed,
            onValueChange = { viewModel.setBubbleSpeed(it) },
            valueRange = 5f..30f,
            steps = 5
        )
        Text("Controla qué tan rápido rebotan las burbujas por tu pantalla.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BubbleItem(mission: Mission, primaryColor: Color, onClick: () -> Unit) {
    val bubbleSize = 140.dp
    
    Box(
        modifier = Modifier
            .size(bubbleSize)
            .clip(CircleShape)
            .alpha(if (mission.isEnabled) 1f else 0.5f)
            .background(
                brush = Brush.radialGradient(
                    colors = if (mission.isEnabled) {
                        listOf(Color(0xFF424242), Color(0xFF212121))
                    } else {
                        listOf(Color(0xFF333333), Color(0xFF111111))
                    }
                )
            )
            .border(2.dp, if (mission.isEnabled) primaryColor else Color.Gray, CircleShape)
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (mission.isEnabled) "🫧" else "💤",
                fontSize = 24.sp
            )
            Text(
                text = mission.message,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = Color.White
            )
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", mission.hour, mission.minute),
                style = MaterialTheme.typography.labelSmall,
                color = if (mission.isEnabled) primaryColor else Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FocusedBubbleView(
    mission: Mission,
    primaryColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val size by animateDpAsState(targetValue = 300.dp, animationSpec = tween(500), label = "bubbleSize")
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.clickable(enabled = false) { }
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF424242), Color(0xFF121212))
                    )
                )
                .border(4.dp, if (mission.isEnabled) primaryColor else Color.Gray, CircleShape)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${if (mission.isEnabled) "🫧" else "💤"} ${mission.message}",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = String.format(Locale.getDefault(), "⏰ %02d:%02d", mission.hour, mission.minute),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (mission.isEnabled) primaryColor else Color.Gray,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildDaysString(mission),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBB86FC),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = onEdit,
                    containerColor = Color.DarkGray,
                    contentColor = primaryColor
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                Text("Editar", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = onDelete,
                    containerColor = Color.DarkGray,
                    contentColor = Color.Red
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }
                Text("Eliminar", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionEditorContent(
    initialMission: Mission?,
    primaryColor: Color,
    onSave: (Mission) -> Unit,
    onCancel: () -> Unit
) {
    var message by remember { mutableStateOf(initialMission?.message ?: "") }
    var hour by remember { mutableIntStateOf(initialMission?.hour ?: 8) }
    var minute by remember { mutableIntStateOf(initialMission?.minute ?: 0) }
    var isEnabled by remember { mutableStateOf(initialMission?.isEnabled ?: true) }
    
    var mon by remember { mutableStateOf(initialMission?.monday ?: false) }
    var tue by remember { mutableStateOf(initialMission?.tuesday ?: false) }
    var wed by remember { mutableStateOf(initialMission?.wednesday ?: false) }
    var thu by remember { mutableStateOf(initialMission?.thursday ?: false) }
    var fri by remember { mutableStateOf(initialMission?.friday ?: false) }
    var sat by remember { mutableStateOf(initialMission?.saturday ?: false) }
    var sun by remember { mutableStateOf(initialMission?.sunday ?: false) }

    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (initialMission == null) "Nueva Misión" else "Editar Misión",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("¿Qué debes hacer?") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Misión Activa", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = isEnabled,
                onCheckedChange = { isEnabled = it },
                colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true },
            colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Hora Programada", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Text("Toca para cambiar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Días de activación", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DayCheckbox("L", mon) { mon = it }
            DayCheckbox("M", tue) { tue = it }
            DayCheckbox("X", wed) { wed = it }
            DayCheckbox("J", thu) { thu = it }
            DayCheckbox("V", fri) { fri = it }
            DayCheckbox("S", sat) { sat = it }
            DayCheckbox("D", sun) { sun = it }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (message.isNotBlank()) {
                    onSave(
                        Mission(
                            id = initialMission?.id ?: 0,
                            message = message,
                            hour = hour,
                            minute = minute,
                            isEnabled = isEnabled,
                            monday = mon, tuesday = tue, wednesday = wed,
                            thursday = thu, friday = fri, saturday = sat, sunday = sun
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
        ) {
            Text("Guardar Misión", style = MaterialTheme.typography.titleMedium)
        }
        
        TextButton(onClick = onCancel) {
            Text("Cancelar", color = primaryColor)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = { selectedHour, selectedMinute ->
                hour = selectedHour
                minute = selectedMinute
                showTimePicker = false
            },
            initialHour = hour,
            initialMinute = minute
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    initialHour: Int,
    initialMinute: Int
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Selecciona la hora",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                TimePicker(state = timePickerState)
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancelar")
                    }
                    TextButton(onClick = {
                        onConfirm(timePickerState.hour, timePickerState.minute)
                    }) {
                        Text("Aceptar")
                    }
                }
            }
        }
    }
}

@Composable
fun DayCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        Checkbox(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
        )
    }
}

fun buildDaysString(m: Mission): String {
    val days = mutableListOf<String>()
    if (m.monday) days.add("Lun")
    if (m.tuesday) days.add("Mar")
    if (m.wednesday) days.add("Mie")
    if (m.thursday) days.add("Jue")
    if (m.friday) days.add("Vie")
    if (m.saturday) days.add("Sab")
    if (m.sunday) days.add("Dom")
    return if (days.size == 7) "Todos los días" else if (days.isEmpty()) "Ningún día" else days.joinToString(", ")
}

private fun testAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val triggerTime = Calendar.getInstance().apply {
        add(Calendar.SECOND, 10)
    }.timeInMillis

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
        Toast.makeText(context, "Alarma en 10 seg", Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intentOverlay = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intentOverlay)
        }
    }
}
