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
import android.provider.ContactsContract
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bubblediscipline.ui.theme.BubbleDisciplineTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val appTheme by settingsViewModel.appTheme.collectAsState()
            
            val primaryColor = when (appTheme) {
                "Azul" -> Color(0xFF2196F3)
                "Verde" -> Color(0xFF4CAF50)
                "Naranja" -> Color(0xFFFF9800)
                "Morado" -> Color(0xFF9C27B0)
                else -> Color(0xFFFF4081)
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
    statsViewModel: StatsViewModel = viewModel(),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "BubbleDiscipline",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                DrawerItem("Mis Misiones", Icons.Default.Star, currentScreen) { currentScreen = "Misiones"; scope.launch { drawerState.close() } }
                DrawerItem("El Calabozo", Icons.Default.Gavel, currentScreen) { currentScreen = "El Calabozo"; scope.launch { drawerState.close() } }
                DrawerItem("Vacaciones", Icons.Default.BeachAccess, currentScreen) { currentScreen = "Vacaciones"; scope.launch { drawerState.close() } }
                DrawerItem("Estadísticas", Icons.Default.BarChart, currentScreen) { currentScreen = "Estadísticas"; scope.launch { drawerState.close() } }
                DrawerItem("Ajustes", Icons.Default.Settings, currentScreen) { currentScreen = "Ajustes"; scope.launch { drawerState.close() } }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentScreen) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                        }
                    },
                    actions = {
                        val context = LocalContext.current
                        IconButton(onClick = {
                             val intent = Intent(context, BubbleForegroundService::class.java)
                             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { context.startForegroundService(intent) }
                             else { context.startService(intent) }
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Test", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { testAlarm(context) }) {
                            Icon(Icons.Default.Timer, contentDescription = "Alarm", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    "Misiones" -> MissionsScreen(missionViewModel, statsViewModel, primaryColor)
                    "El Calabozo" -> DungeonScreen(statsViewModel)
                    "Vacaciones" -> VacationsScreen(settingsViewModel)
                    "Estadísticas" -> StatsScreen(statsViewModel)
                    "Ajustes" -> SettingsScreen(settingsViewModel, statsViewModel)
                }
            }
        }
    }
}

@Composable
fun DrawerItem(label: String, icon: ImageVector, currentScreen: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = currentScreen == label,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsScreen(viewModel: MissionViewModel, statsViewModel: StatsViewModel, primaryColor: Color) {
    val scope = rememberCoroutineScope()
    
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingMission by remember { mutableStateOf<Mission?>(null) }

    val focusedMissionId by viewModel.focusedMissionId.collectAsState()
    val missionsList by viewModel.allMissions.collectAsState(initial = emptyList())
    val focusedMission = missionsList.find { it.id == focusedMissionId }

    var selectedTab by remember { mutableIntStateOf(0) }
    val filteredMissions = missionsList.filter { if (selectedTab == 0) it.isEnabled else !it.isEnabled }

    val streakData by statsViewModel.getDailyResults(7).collectAsState(initial = emptyList())

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { editingMission = null; showSheet = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) { Icon(Icons.Default.Add, contentDescription = null) }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                WelcomeHeader(streakData)

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Activas") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Pausadas") })
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                if (filteredMissions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Vacio", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredMissions) { mission ->
                            BubbleItem(mission, primaryColor) { viewModel.setFocusedMission(mission.id) }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = focusedMission != null, enter = fadeIn(), exit = fadeOut()) {
            focusedMission?.let { mission ->
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f))
                        .clickable { viewModel.setFocusedMission(null) },
                    contentAlignment = Alignment.Center
                ) {
                    FocusedBubbleView(mission, primaryColor, 
                        onEdit = { editingMission = mission; viewModel.setFocusedMission(null); showSheet = true },
                        onDelete = { viewModel.delete(mission); viewModel.setFocusedMission(null) }
                    )
                }
            }
        }

        if (showSheet) {
            ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
                MissionEditorContent(initialMission = editingMission, primaryColor = primaryColor,
                    onSave = { mission -> viewModel.saveMission(mission); scope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false } },
                    onCancel = { scope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false } }
                )
            }
        }
    }
}

@Composable
fun WelcomeHeader(streak: List<DayStatus>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("¡Hola! 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Tu disciplina diaria:", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                streak.forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = day.dateLabel, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val color = when (day.success) {
                            true -> Color.Green
                            false -> Color.Red
                            null -> Color.Gray.copy(alpha = 0.3f)
                        }
                        val text = when (day.success) {
                            true -> "V"
                            false -> "X"
                            null -> "-"
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (day.isToday) 2.dp else 0.dp,
                                    color = if (day.isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationsScreen(viewModel: SettingsViewModel) {
    val vacationUntil by viewModel.vacationUntil.collectAsState()
    val dateState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.BeachAccess, contentDescription = null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Modo Vacaciones", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Desactiva todas las burbujas hasta una fecha concreta.", textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (vacationUntil > System.currentTimeMillis()) {
            val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            Text("Inactivo hasta el:", style = MaterialTheme.typography.labelLarge)
            Text(df.format(Date(vacationUntil)), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { viewModel.setVacationUntil(0L) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Cancelar Vacaciones")
            }
        } else {
            Button(onClick = { showDatePicker = true }) {
                Text("Programar Vacaciones")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { viewModel.setVacationUntil(it) }
                    showDatePicker = false
                }) { Text("Aceptar") }
            }
        ) { DatePicker(state = dateState) }
    }
}

@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val stats by viewModel.stats.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Tus Logros", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        StatSection("Hoy", stats.day)
        StatSection("Esta Semana", stats.week)
        StatSection("Este Mes", stats.month)
        StatSection("Histórico Total", stats.total)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                StatLine("Tiempo Medio de Reacción", "${stats.avgTimeSeconds} seg", MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun StatSection(title: String, data: PeriodStats) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Activadas", data.activated.toString(), MaterialTheme.colorScheme.onSurface)
                StatItem("Explotadas", data.exploded.toString(), Color.Green)
                StatItem("Retrasadas", data.failed.toString(), Color.Yellow)
                StatItem("Pánico", data.panic.toString(), Color.Red)
            }
        }
    }
}

@Composable
fun DungeonScreen(viewModel: StatsViewModel) {
    val history by viewModel.allHistory.collectAsState(initial = emptyList())
    val failedMissions = history.filter { it.wasPanicUsed }.sortedByDescending { it.activationTime }
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("El Calabozo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Red)
        Text("Aquí yacen tus fracasos más estrepitosos.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(16.dp))

        if (failedMissions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SentimentVerySatisfied, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("¡Limpio! No tienes pecados registrados.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(failedMissions) { entry: MissionHistory ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dangerous, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(entry.missionMessage, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Cancelada el ${sdf.format(Date(entry.activationTime))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StatLine(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, statsViewModel: StatsViewModel) {
    val context = LocalContext.current
    val bubbleSpeed by viewModel.bubbleSpeed.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val whatsappContact by viewModel.whatsappContact.collectAsState()
    val sendPanicMessage by viewModel.sendPanicMessage.collectAsState()

    var contactNumber by remember { mutableStateOf(whatsappContact) }
    var showConfirmReset by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri -> uri?.let { viewModel.exportDatabase(it) } }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { viewModel.importDatabase(it) } }
    )

    // Sincronizar estado local con DataStore cuando cambie externamente
    LaunchedEffect(whatsappContact) {
        contactNumber = whatsappContact
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
        onResult = { uri ->
            uri?.let {
                val number = getPhoneNumber(context, it)
                if (number != null) {
                    val cleanNumber = number.replace(Regex("[^0-9]"), "")
                    contactNumber = cleanNumber
                    viewModel.setWhatsappContact(cleanNumber)
                }
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                contactPickerLauncher.launch(null)
            } else {
                Toast.makeText(context, "Permiso de contactos denegado", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Personalización") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Disciplina") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Sistema") })
        }

        Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
            when (selectedTab) {
                0 -> { // Personalización
                    Text("Tema Visual", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Rosa", "Azul", "Verde", "Naranja", "Morado").forEach { theme ->
                            FilterChip(selected = appTheme == theme, onClick = { viewModel.setAppTheme(theme) }, label = { Text(theme) })
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Velocidad de Burbujas", style = MaterialTheme.typography.titleMedium)
                    Slider(value = bubbleSpeed, onValueChange = { viewModel.setBubbleSpeed(it) }, valueRange = 5f..30f)
                }
                1 -> { // Disciplina (Castigo)
                    Text("Configuración de Castigo", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Enviar mensaje de WhatsApp", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = sendPanicMessage,
                            onCheckedChange = { viewModel.setSendPanicMessage(it) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (sendPanicMessage) {
                        OutlinedTextField(
                            value = contactNumber,
                            onValueChange = { 
                                contactNumber = it
                                viewModel.setWhatsappContact(it)
                            },
                            label = { Text("Número WhatsApp") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Ej: 34600000000") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                        contactPickerLauncher.launch(null)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                    }
                                }) {
                                    Icon(Icons.Default.ContactPage, contentDescription = "Elegir contacto")
                                }
                            }
                        )
                        Text(
                            "Se enviará un mensaje a este contacto si usas el botón del pánico.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                2 -> { // Sistema (Datos, Backup)
                    Text("Copia de Seguridad", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { exportLauncher.launch("bubble_discipline_backup.db") }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.FileUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exportar")
                        }
                        Button(onClick = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Importar")
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Zona Peligrosa", style = MaterialTheme.typography.titleMedium, color = Color.Red)
                    Button(
                        onClick = { showConfirmReset = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reiniciar Estadísticas")
                    }
                }
            }
        }
    }

    if (showConfirmReset) {
        AlertDialog(
            onDismissRequest = { showConfirmReset = false },
            title = { Text("¿Reiniciar todo?") },
            text = { Text("Se borrará tu historial de disciplina permanentemente.") },
            confirmButton = {
                TextButton(onClick = { statsViewModel.resetStats(); showConfirmReset = false }) { Text("BORRAR") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmReset = false }) { Text("Cancelar") }
            }
        )
    }
}

// Función para extraer el número de teléfono del contacto seleccionado
private fun getPhoneNumber(context: Context, contactUri: Uri): String? {
    var phoneNumber: String? = null
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(contactUri, null, null, null, null)
    
    if (cursor != null && cursor.moveToFirst()) {
        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
        val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
        
        if (idIndex != -1 && hasPhoneIndex != -1) {
            val contactId = cursor.getString(idIndex)
            val hasPhoneNumber = cursor.getString(hasPhoneIndex)
            
            if (hasPhoneNumber == "1") {
                val phonesCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    arrayOf(contactId),
                    null
                )
                
                if (phonesCursor != null && phonesCursor.moveToFirst()) {
                    val numberIndex = phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numberIndex != -1) {
                        phoneNumber = phonesCursor.getString(numberIndex)
                    }
                    phonesCursor.close()
                }
            }
        }
        cursor.close()
    }
    return phoneNumber
}

@Composable
fun BubbleItem(mission: Mission, primaryColor: Color, onClick: () -> Unit) {
    val categoryIcon = getCategoryIcon(mission.category)
    Box(
        modifier = Modifier.size(140.dp).clip(CircleShape).alpha(if (mission.isEnabled) 1f else 0.5f)
            .background(Brush.radialGradient(listOf(Color(0xFF424242), Color(0xFF212121))))
            .border(2.dp, if (mission.isEnabled) primaryColor else Color.Gray, CircleShape)
            .clickable { onClick() }.padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = if (mission.isEnabled) categoryIcon else "💤", fontSize = 24.sp)
            Text(text = mission.message, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2, color = Color.White)
            Text(text = String.format(Locale.getDefault(), "%02d:%02d", mission.hour, mission.minute), style = MaterialTheme.typography.labelSmall, color = if (mission.isEnabled) primaryColor else Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FocusedBubbleView(mission: Mission, primaryColor: Color, onEdit: () -> Unit, onDelete: () -> Unit) {
    val size by animateDpAsState(targetValue = 300.dp, animationSpec = tween(500), label = "")
    val categoryIcon = getCategoryIcon(mission.category)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF424242), Color(0xFF121212))))
                .border(4.dp, if (mission.isEnabled) primaryColor else Color.Gray, CircleShape).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${if (mission.isEnabled) categoryIcon else "💤"} ${mission.message}", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = mission.category, style = MaterialTheme.typography.labelLarge, color = primaryColor.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = String.format(Locale.getDefault(), "⏰ %02d:%02d", mission.hour, mission.minute), style = MaterialTheme.typography.headlineSmall, color = if (mission.isEnabled) primaryColor else Color.Gray, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = buildDaysString(mission), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFBB86FC), textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(onClick = onEdit, containerColor = Color.DarkGray, contentColor = primaryColor) { Icon(Icons.Default.Edit, null) }
                Text("Editar", color = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(onClick = onDelete, containerColor = Color.DarkGray, contentColor = Color.Red) { Icon(Icons.Default.Delete, null) }
                Text("Eliminar", color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionEditorContent(initialMission: Mission?, primaryColor: Color, onSave: (Mission) -> Unit, onCancel: () -> Unit) {
    var message by remember { mutableStateOf(initialMission?.message ?: "") }
    var hour by remember { mutableIntStateOf(initialMission?.hour ?: 8) }
    var minute by remember { mutableIntStateOf(initialMission?.minute ?: 0) }
    var isEnabled by remember { mutableStateOf(initialMission?.isEnabled ?: true) }
    var category by remember { mutableStateOf(initialMission?.category ?: "General") }
    var mon by remember { mutableStateOf(initialMission?.monday ?: false) }; var tue by remember { mutableStateOf(initialMission?.tuesday ?: false) }
    var wed by remember { mutableStateOf(initialMission?.wednesday ?: false) }; var thu by remember { mutableStateOf(initialMission?.thursday ?: false) }
    var fri by remember { mutableStateOf(initialMission?.friday ?: false) }; var sat by remember { mutableStateOf(initialMission?.saturday ?: false) }
    var sun by remember { mutableStateOf(initialMission?.sunday ?: false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val categories = listOf("General", "Salud", "Trabajo", "Deporte", "Alimentación", "Estudios", "Juego", "Familia", "Amigos", "Médico", "Fiesta", "Hogar", "Dinero", "Viajes")
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = if (initialMission == null) "Nueva Misión" else "Editar Misión", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("¿Qué debes hacer?") }, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(8.dp))
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoría") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            category = selectionOption
                            expanded = false
                        }
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Misión Activa"); Switch(checked = isEnabled, onCheckedChange = { isEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = primaryColor))
        }
        Card(modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true }, colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f))) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = primaryColor)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            DayCheckbox("L", mon) { mon = it }; DayCheckbox("M", tue) { tue = it }; DayCheckbox("X", wed) { wed = it }; DayCheckbox("J", thu) { thu = it }
            DayCheckbox("V", fri) { fri = it }; DayCheckbox("S", sat) { sat = it }; DayCheckbox("D", sun) { sun = it }
        }
        Button(onClick = { if (message.isNotBlank()) onSave(Mission(id = initialMission?.id ?: 0, message = message, hour = hour, minute = minute, isEnabled = isEnabled, category = category, monday = mon, tuesday = tue, wednesday = wed, thursday = thu, friday = fri, saturday = sat, sunday = sun)) },
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text("Guardar Misión") }
        TextButton(onClick = onCancel) { Text("Cancelar", color = primaryColor) }
    }

    if (showTimePicker) {
        TimePickerDialog(onDismissRequest = { showTimePicker = false },
            onConfirm = { h, m -> hour = h; minute = m; showTimePicker = false }, initialHour = hour, initialMinute = minute)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(onDismissRequest: () -> Unit, onConfirm: (Int, Int) -> Unit, initialHour: Int, initialMinute: Int) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    Dialog(onDismissRequest = onDismissRequest) {
        Card(shape = MaterialTheme.shapes.extraLarge) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) { Text("Cancelar") }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Aceptar") }
                }
            }
        }
    }
}

@Composable
fun DayCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

fun buildDaysString(m: Mission): String {
    val days = mutableListOf<String>()
    if (m.monday) days.add("Lun"); if (m.tuesday) days.add("Mar"); if (m.wednesday) days.add("Mie")
    if (m.thursday) days.add("Jue"); if (m.friday) days.add("Vie"); if (m.saturday) days.add("Sab"); if (m.sunday) days.add("Dom")
    return if (days.size == 7) "Todos los días" else if (days.isEmpty()) "Ningún día" else days.joinToString(", ")
}

fun getCategoryIcon(category: String): String {
    return when (category) {
        "Salud" -> "🍎"
        "Trabajo" -> "💼"
        "Deporte" -> "🏃"
        "Alimentación" -> "🍲"
        "Estudios" -> "📚"
        "Juego" -> "🎮"
        "Familia" -> "👨‍👩‍👧‍👦"
        "Amigos" -> "👫"
        "Médico" -> "🩺"
        "Fiesta" -> "🥳"
        "Hogar" -> "🏠"
        "Dinero" -> "💰"
        "Viajes" -> "✈️"
        else -> "🫧"
    }
}

private fun testAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val triggerTime = Calendar.getInstance().apply { add(Calendar.SECOND, 10) }.timeInMillis
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent) }
        else { alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent) }
        Toast.makeText(context, "Alarma en 10 seg", Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) }
    }
}
