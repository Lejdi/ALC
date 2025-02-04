package pl.lejdi.alc.viewmodel

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.orhanobut.hawk.Hawk
import pl.lejdi.alc.util.DarkMode
import pl.lejdi.alcmusicplayer.util.Constants.Companion.HAWK_ASSISTANT

class SettingsViewModel : ViewModel() {

    private val _darkMode: MutableState<DarkMode> =
        mutableStateOf(DarkMode.OFF)
    val darkMode: State<DarkMode> get() = _darkMode

    private val _assistantEnabled: MutableState<Boolean> =
        mutableStateOf(false)
    val assistantEnabled: State<Boolean> get() = _assistantEnabled

    private val _infoDialogDisplayed: MutableState<Boolean> =
        mutableStateOf(false)
    val infoDialogDisplayed: State<Boolean> get() = _infoDialogDisplayed
    fun setInfoDialogDisplayed(newSetting: Boolean) {
        _infoDialogDisplayed.value = newSetting
    }

    private val _permissionDialogDisplayed: MutableState<Boolean> =
        mutableStateOf(false)
    val permissionDialogDisplayed: State<Boolean> get() = _permissionDialogDisplayed
    fun setPermissionDialogDisplayed(newSetting: Boolean) {
        _permissionDialogDisplayed.value = newSetting
    }

    private val _dropdownExpanded: MutableState<Boolean> =
        mutableStateOf(false)
    val dropdownExpanded: State<Boolean> get() = _dropdownExpanded
    fun setDropdownExpanded(newSetting: Boolean) {
        _dropdownExpanded.value = newSetting
    }

    fun initHawk(context: Context) {
        Hawk.init(context).build()
    }

    fun getDarkModeSetting() {
        _darkMode.value = Hawk.get(pl.lejdi.alc.util.Constants.HAWK_DARK_MODE)
    }

    fun getAssistantSetting() {
        _assistantEnabled.value = Hawk.get(HAWK_ASSISTANT)
    }

    fun setDarkModeSetting(newSetting: DarkMode) {
        _darkMode.value = newSetting
        Hawk.put(pl.lejdi.alc.util.Constants.HAWK_DARK_MODE, newSetting)
    }

    fun setAssistantSetting(newSetting: Boolean) {
        _assistantEnabled.value = newSetting
        Hawk.put(HAWK_ASSISTANT, newSetting)
    }
}