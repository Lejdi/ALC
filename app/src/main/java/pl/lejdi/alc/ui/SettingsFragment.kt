package pl.lejdi.alc.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import pl.lejdi.alc.R
import pl.lejdi.alc.ui.compose.*
import pl.lejdi.alc.util.Constants
import pl.lejdi.alc.util.DarkMode
import pl.lejdi.alc.viewmodel.SettingsViewModel

@ExperimentalMaterialApi
class SettingsFragment : Fragment() {

    private lateinit var callback: SettingsToALCCallback

    interface SettingsToALCCallback {
        fun sendAssistantSetting()
    }

    private lateinit var viewModel: SettingsViewModel

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            val newSetting = !viewModel.assistantEnabled.value
            viewModel.setAssistantSetting(newSetting)
            callback.sendAssistantSetting()
        }
    }

    companion object {
        var isDisplayed = false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SettingsToALCCallback) {
            callback = context
        } else {
            throw RuntimeException(context.toString() + "must implement SettingsToALCCallback")
        }
        isDisplayed = true

        val factory: ViewModelProvider.Factory =
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        viewModel = ViewModelProvider(this, factory).get(SettingsViewModel::class.java)
        viewModel.initHawk(context)
    }

    override fun onDetach() {
        isDisplayed = false
        super.onDetach()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.getAssistantSetting()
        viewModel.getDarkModeSetting()

        return ComposeView(requireContext()).apply {
            setContent {
                ALCTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SettingsScreen(
                            setInfoDialog = {
                                viewModel.setInfoDialogDisplayed(true)
                            }
                        )
                        AssistantInfoDialog(
                            dialogDisplayed = viewModel.infoDialogDisplayed.value,
                            dismiss = {
                                viewModel.setInfoDialogDisplayed(false)
                            }
                        )
                        PermissionRequiredDialog(
                            dialogDisplayed = viewModel.permissionDialogDisplayed.value,
                            dismiss = {
                                viewModel.setPermissionDialogDisplayed(false)
                            }
                        )
                    }
                }
            }
        }
    }


    private fun onHelpClick() {
        val emailAddress = "lejdi.alc@gmail.com"
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${emailAddress}")
            putExtra(Intent.EXTRA_EMAIL, emailAddress)
            putExtra(Intent.EXTRA_SUBJECT, "Feedback/Support")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                getString(R.string.email_app_error),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setDarkMode() {
        when (viewModel.darkMode.value) {
            DarkMode.OFF -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            DarkMode.ON -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            DarkMode.AUTO -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.setPermissionDialogDisplayed(true)
        } else {
            val newSetting = !viewModel.assistantEnabled.value
            viewModel.setAssistantSetting(newSetting)
            callback.sendAssistantSetting()
        }
    }

    @Composable
    private fun PermissionRequiredDialog(
        dialogDisplayed: Boolean,
        dismiss: () -> Unit
    ) {
        if (dialogDisplayed) {
            AlertDialog(
                title = {
                    Text(
                        text = stringResource(id = R.string.permission_dialog_title),
                        style = LocalTextStyle.current.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.W600
                        )
                    )
                },
                text = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.permission_dialog_text),
                            style = LocalTextStyle.current.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.W400
                            )
                        )
                    }
                },
                buttons = {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                dismiss()
                                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            modifier = Modifier
                                .padding(8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.ok),
                                style = LocalTextStyle.current.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.W600,
                                    color = getReportIssueTextColor(!MaterialTheme.colors.isLight)
                                )
                            )
                        }
                    }
                },
                onDismissRequest = dismiss
            )
        }
    }

    @Composable
    private fun AssistantInfoDialog(
        dialogDisplayed: Boolean,
        dismiss: () -> Unit
    ) {
        if (dialogDisplayed) {
            AlertDialog(
                title = {
                    Text(
                        text = stringResource(id = R.string.assistant_dialog_title),
                        style = LocalTextStyle.current.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.W600
                        )
                    )
                },
                text = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.assistant_dialog_text),
                            style = LocalTextStyle.current.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.W400
                            )
                        )
                    }
                },
                buttons = {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        TextButton(
                            onClick = dismiss,
                            modifier = Modifier
                                .padding(8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.ok),
                                style = LocalTextStyle.current.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.W600,
                                    color = getReportIssueTextColor(!MaterialTheme.colors.isLight)
                                )
                            )
                        }
                    }
                },
                onDismissRequest = dismiss
            )
        }
    }

    @Composable
    private fun SettingsScreen(
        setInfoDialog: () -> Unit
    ) {
        val textStyle = LocalTextStyle.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                //dark mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.dark_mode),
                        style = LocalTextStyle.current.copy(
                            fontSize = settingsTextSize,
                            fontWeight = FontWeight.W600,
                            color = getTextColor(!MaterialTheme.colors.isLight)
                        )
                    )
                    Column {
                        OutlinedTextField(
                            value = stringResource(id = viewModel.darkMode.value.stringRes),
                            onValueChange = {},
                            modifier = Modifier
                                .width(120.dp)
                                .clickable(
                                    onClick = {
                                        viewModel.setDropdownExpanded(!viewModel.dropdownExpanded.value)
                                    },
                                    indication = ripple(bounded = true),
                                    interactionSource = remember { MutableInteractionSource() }
                                ),
                            enabled = false,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.W600,
                                color = getAlternativeTextColor(!MaterialTheme.colors.isLight)
                            ),
                            trailingIcon = {
                                Icon(
                                    imageVector = if (viewModel.dropdownExpanded.value)
                                        Icons.Filled.ArrowDropUp
                                    else
                                        Icons.Filled.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenu(
                            expanded = viewModel.dropdownExpanded.value,
                            onDismissRequest = {
                                viewModel.setDropdownExpanded(false)
                            },
                        ) {
                            DarkMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    onClick = {
                                        viewModel.setDropdownExpanded(false)
                                        viewModel.setDarkModeSetting(mode)
                                        setDarkMode()
                                    },
                                    modifier = Modifier.width(120.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = mode.stringRes),
                                        style = textStyle.copy(
                                            color = getAlternativeTextColor(!MaterialTheme.colors.isLight)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.padding(4.dp))
                //assistant
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.voice_assistant),
                            style = LocalTextStyle.current.copy(
                                fontSize = settingsTextSize,
                                fontWeight = FontWeight.W600,
                                color = getTextColor(!MaterialTheme.colors.isLight)
                            )
                        )
                        IconButton(
                            onClick = {
                                setInfoDialog()
                            }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = HelpIcon),
                                contentDescription = null,
                                tint = getIconColor(!MaterialTheme.colors.isLight)
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = viewModel.assistantEnabled.value,
                            onCheckedChange = {
                                checkPermission()
                            },
                            colors = getSwitchColors(!MaterialTheme.colors.isLight)
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                //help
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.report_issue),
                        style = LocalTextStyle.current.copy(
                            fontSize = settingsTextSize,
                            fontWeight = if (!MaterialTheme.colors.isLight) FontWeight.W700 else FontWeight.W600,
                            color = getReportIssueTextColor(!MaterialTheme.colors.isLight)
                        ),
                        modifier = Modifier.clickable {
                            onHelpClick()
                        }
                    )
                }
                Spacer(modifier = Modifier.padding(4.dp))
                //version
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = stringResource(id = R.string.version),
                        style = LocalTextStyle.current.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W600,
                            color = getAlternativeTextColor(!MaterialTheme.colors.isLight)
                        )
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = Constants.APP_VERSION,
                        style = LocalTextStyle.current.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W600,
                            color = getAlternativeTextColor(!MaterialTheme.colors.isLight)
                        )
                    )
                }
            }
        }
    }
}