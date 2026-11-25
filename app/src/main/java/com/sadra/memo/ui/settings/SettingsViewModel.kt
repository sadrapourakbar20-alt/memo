package com.sadra.memo.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.data.backup.BackupManager
import com.sadra.memo.data.preferences.SettingsRepository
import com.sadra.memo.data.preferences.ThemeMode
import com.sadra.memo.domain.usecase.TaskManager
import com.sadra.memo.reminder.ClassScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val backup: BackupManager,
    private val taskManager: TaskManager,
    private val classScheduler: ClassScheduler
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val hasPin: StateFlow<Boolean> = repository.pinHash
        .map { !it.isNullOrEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val biometricEnabled: StateFlow<Boolean> = repository.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setBiometric(value: Boolean) {
        viewModelScope.launch { repository.setBiometric(value) }
    }

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            repository.setPin(pin)
            _message.value = "قفل برنامه فعال شد."
        }
    }

    fun disablePin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.verifyPin(pin)
            if (ok) {
                repository.clearPin()
                _message.value = "قفل برنامه غیرفعال شد."
            }
            onResult(ok)
        }
    }

    fun exportTo(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _message.value = try {
                backup.export(resolver, uri)
                "پشتیبان با موفقیت ذخیره شد."
            } catch (e: Exception) {
                "ذخیره پشتیبان ناموفق بود: " + (e.message ?: "خطای نامشخص")
            }
        }
    }

    fun importFrom(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _message.value = try {
                backup.import(resolver, uri)
                taskManager.rescheduleAll()
                classScheduler.rescheduleAll()
                "اطلاعات با موفقیت بازیابی شد."
            } catch (e: Exception) {
                "بازیابی ناموفق بود (اطلاعات فعلی دست‌نخورده موند): " + (e.message ?: "فایل نامعتبره")
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
