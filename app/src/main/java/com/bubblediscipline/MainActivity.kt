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
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bubblediscipline.ui.theme.BubbleDisciplineTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BubbleDisciplineTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier, viewModel: MissionViewModel = viewModel()) {
    val context = LocalContext.current
    
    // Estados de permisos
    var hasNotificationPermission by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            )
        } else {
            mutableStateOf(true)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Estado del formulario
    var message by remember { mutableStateOf("") }
    var hourText by remember { mutableStateOf("") }
    var minuteText by remember { mutableStateOf("") }
    
    // Estados para los días de la semana
    var mon by remember { mutableStateOf(false) }
    var tue by remember { mutableStateOf(false) }
    var wed by remember { mutableStateOf(false) }
    var thu by remember { mutableStateOf(false) }
    var fri by remember { mutableStateOf(false) }
    var sat by remember { mutableStateOf(false) }
    var sun by remember { mutableStateOf(false) }

    // Escuchar las misiones guardadas en tiempo real
    val missionsList by viewModel.allMissions.collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Crear Nueva Misión de Disciplina", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Mensaje (Ej: ¡Haz la cama!)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedTextField(
                value = hourText,
                onValueChange = { hourText = it },
                label = { Text("Hora (0-23)") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = minuteText,
                onValueChange = { minuteText = it },
                label = { Text("Minuto (0-59)") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Días que debe sonar:", style = MaterialTheme.typography.bodyMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DayCheckbox("L", mon) { mon = it }
            DayCheckbox("M", tue) { tue = it }
            DayCheckbox("X", wed) { wed = it }
            DayCheckbox("J", thu) { thu = it }
            DayCheckbox("V", fri) { fri = it }
            DayCheckbox("S", sat) { sat = it }
            DayCheckbox("D", sun) { sun = it }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val h = hourText.toIntOrNull()
                val m = minuteText.toIntOrNull()

                if (message.isNotBlank() && h != null && m != null && h in 0..23 && m in 0..59) {
                    val newMission = Mission(
                        message = message,
                        hour = h,
                        minute = m,
                        monday = mon, tuesday = tue, wednesday = wed,
                        thursday = thu, friday = fri, saturday = sat, sunday = sun
                    )
                    viewModel.insert(newMission)
                    
                    // Limpiar formulario
                    message = ""
                    hourText = ""
                    minuteText = ""
                    mon = false; tue = false; wed = false; thu = false; fri = false; sat = false; sun = false
                    
                    Toast.makeText(context, "¡Misión guardada!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Datos incorrectos", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Misión")
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Botones de prueba
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                if (checkPermissions(context, hasNotificationPermission, permissionLauncher)) {
                    val intent = Intent(context, BubbleForegroundService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                }
            }) {
                Text("Burbuja Ya")
            }

            Button(onClick = {
                testAlarm(context)
            }) {
                Text("Alarma (10s)")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Misiones Actuales:", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(missionsList) { mission ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(mission.message, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                String.format("Hora: %02d:%02d", mission.hour, mission.minute),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(onClick = { viewModel.delete(mission) }) {
                            Text("Borrar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun checkPermissions(
    context: Context, 
    hasNotificationPermission: Boolean, 
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        return false
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
        Toast.makeText(context, "Concede el permiso de mostrar sobre otras apps", Toast.LENGTH_LONG).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
        return false
    }
    return true
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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    BubbleDisciplineTheme {
        MainScreen()
    }
}