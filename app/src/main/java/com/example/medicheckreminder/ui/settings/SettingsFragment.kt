package com.example.medicheckreminder.ui.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.medicheckreminder.R
import com.example.medicheckreminder.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var adapter: SettingsAdapter

    private val ringtonePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        viewModel.setSoundUri(uri?.toString())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        adapter = SettingsAdapter(
            onDropdownClick = ::showDropdown,
            onToggle = ::onToggle,
            onNavigationClick = viewModel::onRowClicked,
            onTextCommitted = ::onTextChanged,
            onDangerClick = { showLogoutConfirm() }
        )
        binding.recyclerSettings.adapter = adapter
        binding.recyclerSettings.itemAnimator = null
        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.rows.collect { adapter.submitList(it) }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            SettingsEvent.LoggedOut -> findNavController().navigate(
                                R.id.action_settingsFragment_to_loginFragment
                            )
                            is SettingsEvent.OpenUrl -> openUrl(event.url)
                            is SettingsEvent.PickSound -> openSoundPicker(event.currentUri)
                        }
                    }
                }
            }
        }
    }

    private fun onToggle(id: String, checked: Boolean) {
        when (id) {
            SettingIds.NOTIFICATIONS -> viewModel.onNotificationsToggled(checked)
            SettingIds.VIBRATION -> viewModel.onVibrationToggled(checked)
            SettingIds.ESCALATION -> viewModel.onEscalationToggled(checked)
        }
    }

    private fun onTextChanged(id: String, value: String) {
        when (id) {
            SettingIds.CAREGIVER_NAME -> viewModel.onCaregiverNameChanged(value)
            SettingIds.CAREGIVER_PHONE -> viewModel.onCaregiverPhoneChanged(value)
            SettingIds.CAREGIVER_EMAIL -> viewModel.onCaregiverEmailChanged(value)
        }
    }

    private fun showDropdown(row: SettingRow.Dropdown) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(row.title)
            .setSingleChoiceItems(row.options.toTypedArray(), row.selectedIndex) { dialog, which ->
                when (row.id) {
                    SettingIds.LANGUAGE -> viewModel.onLanguageSelected(which)
                    SettingIds.THEME -> viewModel.onThemeSelected(which)
                    SettingIds.SNOOZE -> viewModel.onSnoozeSelected(which)
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showLogoutConfirm() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_logout_title)
            .setMessage(R.string.settings_logout_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.settings_logout) { _, _ -> viewModel.logout() }
            .show()
    }

    private fun openSoundPicker(currentUri: String) {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.settings_sound))
            when (currentUri) {
                "", SettingIds.SOUND_SILENT -> putExtra(
                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    if (currentUri == SettingIds.SOUND_SILENT) {
                        null
                    } else {
                        Settings.System.DEFAULT_NOTIFICATION_URI
                    }
                )
                else -> putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri.toUri())
            }
        }
        try {
            ringtonePicker.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Snackbar.make(binding.root, R.string.settings_sound_picker_unavailable, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            Snackbar.make(binding.root, R.string.settings_link_unavailable, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    override fun onPause() {
        commitFocusedText()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun commitFocusedText() {
        val focused = _binding?.recyclerSettings?.findFocus() as? EditText ?: return
        val id = focused.tag as? String ?: return
        onTextChanged(id, focused.text?.toString().orEmpty())
    }
}
