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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            BubbleDisciplineTheme {
                MainNavigationWrapper()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationWrapper(viewModel: MissionViewModel = viewModel()) {
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
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Panel de Disciplina") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                        }
                    },
                    actions = {
                        // Botones de prueba rápidos en la barra superior
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
                    "Misiones" -> MissionsScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsScreen(viewModel: MissionViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var editingMission by remember { mutableStateOf<Mission?>(null) }

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

    val missionsList by viewModel.allMissions.collectAsState(initial = emptyList())

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
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Misiones Actuales", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (missionsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay misiones. ¡Crea una!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(missionsList) { mission ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            onClick = {
                                editingMission = mission
                                showSheet = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mission.message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = String.format(Locale.getDefault(), "⏰ %02d:%02d", mission.hour, mission.minute),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = buildDaysString(mission),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    IconButton(onClick = {
                                        editingMission = mission
                                        showSheet = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.tertiary)
                                    }
                                    IconButton(onClick = { viewModel.delete(mission) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionEditorContent(
    initialMission: Mission?,
    onSave: (Mission) -> Unit,
    onCancel: () -> Unit
) {
    var message by remember { mutableStateOf(initialMission?.message ?: "") }
    var hour by remember { mutableIntStateOf(initialMission?.hour ?: 8) }
    var minute by remember { mutableIntStateOf(initialMission?.minute ?: 0) }
    
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

        Spacer(modifier = Modifier.height(24.dp))

        // Selector de Hora Moderno (Click para abrir reloj)
        Card(
            modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Hora Programada", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
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
                            monday = mon, tuesday = tue, wednesday = wed,
                            thursday = thu, friday = fri, saturday = sat, sunday = sun
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Guardar Misión", style = MaterialTheme.typography.titleMedium)
        }
        
        TextButton(onClick = onCancel) {
            Text("Cancelar")
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
