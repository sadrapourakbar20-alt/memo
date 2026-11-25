package com.sadra.memo.ui.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.biometric.BiometricManager
import androidx.core.content.IntentCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sadra.memo.reminder.NotificationHelper
import com.sadra.memo.MemoApplication
import com.sadra.memo.data.preferences.ThemeMode
import com.sadra.memo.ui.factoryOf

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as MemoApplication
    val vm: SettingsViewModel = viewModel(
        factory = remember {
            factoryOf {
                SettingsViewModel(
                    app.container.settingsRepository,
                    app.container.backupManager,
                    app.container.taskManager,
                    app.container.classScheduler
                )
            }
        }
    )
    val mode by vm.themeMode.collectAsState()
    val hasPin by vm.hasPin.collectAsState()
    val biometricOn by vm.biometricEnabled.collectAsState()
    var soundTitle by remember { mutableStateOf<String?>(null) }
    val message by vm.message.collectAsState()

    var showSetPin by remember { mutableStateOf(false) }
    var showDisablePin by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportTo(context.contentResolver, uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingImport = uri }

    fun refreshSoundTitle() {
        val saved = NotificationHelper.soundUri(context)
        soundTitle = if (saved == null) null else try {
            RingtoneManager.getRingtone(context, Uri.parse(saved))?.getTitle(context)
        } catch (e: Exception) {
            null
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { refreshSoundTitle() }

    val soundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (data != null) {
            val picked = IntentCompat.getParcelableExtra(data, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            NotificationHelper.setSound(context, picked)
            refreshSoundTitle()
        }
    }

    val canBiometric = BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("تنظیمات", style = MaterialTheme.typography.headlineSmall)

        val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)

        Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("تم برنامه", style = MaterialTheme.typography.titleMedium)
                listOf(
                    ThemeMode.SYSTEM to "خودکار (مثل گوشی)",
                    ThemeMode.LIGHT to "روشن",
                    ThemeMode.DARK to "تاریک"
                ).forEach { (value, label) ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = mode == value, onClick = { vm.setTheme(value) })
                        Text(label)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("یادآوری‌ها", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (canExact) "آلارم دقیق فعاله و یادآوری‌ها سر ساعت میان."
                    else "آلارم دقیق خاموشه. یادآوری‌ها ممکنه چند دقیقه دیر برسن.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (!canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:" + context.packageName)
                                )
                            )
                        },
                        modifier = Modifier.padding(top = 10.dp)
                    ) { Text("فعال کردن آلارم دقیق") }
                }
                Text(
                    text = "صدای اعلان: " + (soundTitle ?: "پیش‌فرض گوشی"),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
                OutlinedButton(
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "صدای یادآوری")
                            val cur = NotificationHelper.soundUri(context)
                            if (cur != null) putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(cur))
                        }
                        soundLauncher.launch(intent)
                    },
                    modifier = Modifier.padding(top = 6.dp)
                ) { Text("انتخاب صدای اعلان") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("قفل برنامه", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (hasPin) "قفل فعاله. هر بار که برنامه رو ببندی، دفعه بعد رمز می‌خواد."
                    else "با یک رمز عددی، ورود به برنامه رو قفل کن.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasPin) {
                        OutlinedButton(onClick = { showSetPin = true }) { Text("تغییر رمز") }
                        OutlinedButton(onClick = { showDisablePin = true }) { Text("غیرفعال کردن") }
                    } else {
                        Button(onClick = { showSetPin = true }) { Text("فعال کردن قفل") }
                    }
                }
                if (hasPin) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (canBiometric) "ورود با اثر انگشت" else "اثر انگشت روی این گوشی تنظیم نشده",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = biometricOn && canBiometric,
                            enabled = canBiometric,
                            onCheckedChange = { vm.setBiometric(it) }
                        )
                    }
                }
                Text(
                    text = "اگه رمز رو فراموش کنی، باید برنامه رو پاک و دوباره نصب کنی و اطلاعات از بین می‌ره. پس پشتیبان بگیر.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("پشتیبان‌گیری", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "همه یادداشت‌ها، کارها، کلاس‌ها و تنظیمات ترم تو یک فایل ذخیره می‌شه. فایل رو جایی امن (مثلاً پیام‌های ذخیره‌شده تلگرام) نگه دار.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exportLauncher.launch("memo-backup.json") }) { Text("گرفتن پشتیبان") }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) { Text("بازیابی") }
                }
                if (message != null) {
                    Text(
                        text = message ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Text(
            text = "Memo نسخه ۰٫۴",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showSetPin) {
        var first by remember { mutableStateOf("") }
        var second by remember { mutableStateOf("") }
        val valid = first.length >= 4 && first == second
        AlertDialog(
            onDismissRequest = { showSetPin = false },
            title = { Text("تعیین رمز") },
            text = {
                Column {
                    OutlinedTextField(
                        value = first,
                        onValueChange = { first = it.filter { c -> c.isDigit() }.take(8) },
                        label = { Text("رمز (حداقل ۴ رقم)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    OutlinedTextField(
                        value = second,
                        onValueChange = { second = it.filter { c -> c.isDigit() }.take(8) },
                        label = { Text("تکرار رمز") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
                    onClick = { vm.setPin(first); showSetPin = false }
                ) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { showSetPin = false }) { Text("انصراف") } }
        )
    }

    if (showDisablePin) {
        var current by remember { mutableStateOf("") }
        var wrong by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showDisablePin = false },
            title = { Text("غیرفعال کردن قفل") },
            text = {
                Column {
                    OutlinedTextField(
                        value = current,
                        onValueChange = { current = it.filter { c -> c.isDigit() }.take(8); wrong = false },
                        label = { Text("رمز فعلی") },
                        singleLine = true,
                        isError = wrong,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    if (wrong) {
                        Text("رمز اشتباهه.", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.disablePin(current) { ok ->
                        if (ok) showDisablePin = false else wrong = true
                    }
                }) { Text("غیرفعال کن") }
            },
            dismissButton = { TextButton(onClick = { showDisablePin = false }) { Text("انصراف") } }
        )
    }

    val importUri = pendingImport
    if (importUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("بازیابی از پشتیبان") },
            text = { Text("همه یادداشت‌ها، کارها و کلاس‌های فعلی با محتوای فایل جایگزین می‌شن. ادامه بدم؟") },
            confirmButton = {
                TextButton(onClick = {
                    vm.importFrom(context.contentResolver, importUri)
                    pendingImport = null
                }) { Text("جایگزین کن") }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("انصراف") } }
        )
    }
}
