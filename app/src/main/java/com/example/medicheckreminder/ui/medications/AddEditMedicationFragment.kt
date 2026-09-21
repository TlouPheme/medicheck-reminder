package com.example.medicheckreminder.ui.medications

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.medicheckreminder.R
import com.example.medicheckreminder.databinding.FragmentAddEditMedicationBinding
import com.example.medicheckreminder.domain.model.Frequency
import com.example.medicheckreminder.ui.motion.NavMotion
import com.example.medicheckreminder.util.ColorGenerator
import com.example.medicheckreminder.util.MedicationDraft
import com.example.medicheckreminder.util.MedicationValidator
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch

class AddEditMedicationFragment : Fragment(R.layout.fragment_add_edit_medication) {

    private var _binding: FragmentAddEditMedicationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddEditMedicationViewModel by viewModels()

    private var formBound = false
    private var renderedTimes: List<String> = emptyList()
    private var isEditMode = false
    private var enterStarted = false
    private val startEnter = Runnable { startEnterTransition() }

    private val dayChips: List<Pair<Chip, Int>>
        get() = listOf(
            binding.chipDayMon to 1,
            binding.chipDayTue to 2,
            binding.chipDayWed to 3,
            binding.chipDayThu to 4,
            binding.chipDayFri to 5,
            binding.chipDaySat to 6,
            binding.chipDaySun to 7
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddEditMedicationBinding.bind(view)

        val medicationId = arguments?.getString(ARG_MEDICATION_ID)
        if (!medicationId.isNullOrBlank()) {
            binding.cardPillIcon.transitionName = NavMotion.pillTransitionName(medicationId)
        }
        postponeEnterTransition()
        view.postDelayed(startEnter, NavMotion.POSTPONE_TIMEOUT_MS)

        setupMenu()
        setupFrequencyDropdown()
        setupDayChips()
        setupStockHintUpdates()
        binding.etName.doAfterTextChanged { applyPillSwatch(it?.toString().orEmpty()) }
        binding.btnAddTime.setOnClickListener { showTimePicker() }

        viewModel.load(medicationId)
        observeState()
        observeEvents()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_add_edit_medication, menu)
                val saveItem = menu.findItem(R.id.action_save)
                val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_check)?.mutate()
                val attrs = requireContext().obtainStyledAttributes(
                    intArrayOf(com.google.android.material.R.attr.colorOnSurface)
                )
                icon?.setTint(attrs.getColor(0, 0xFF1E293B.toInt()))
                attrs.recycle()
                saveItem.icon = icon
                menu.findItem(R.id.action_delete).isVisible = isEditMode
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_save -> {
                        viewModel.save(collectDraft())
                        true
                    }
                    R.id.action_delete -> {
                        confirmDelete()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupFrequencyDropdown() {
        val labels = resources.getStringArray(R.array.frequency_options)
        binding.dropdownFrequency.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                labels
            )
        )
        binding.dropdownFrequency.keyListener = null
        binding.dropdownFrequency.setOnItemClickListener { _, _, position, _ ->
            viewModel.setFrequency(Frequency.entries[position])
            updateFrequencyDependentUi(Frequency.entries[position])
            updateDaysRemainingHint()
        }
    }

    private fun setupDayChips() {
        dayChips.forEach { (chip, day) ->
            chip.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setDay(day, isChecked)
                updateDaysRemainingHint()
            }
        }
    }

    private fun setupStockHintUpdates() {
        binding.etStock.doAfterTextChanged { updateDaysRemainingHint() }
        binding.etInterval.doAfterTextChanged { updateDaysRemainingHint() }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoaded && !formBound) {
                        bindForm(state)
                        formBound = true
                        startEnterTransition()
                    }
                    if (formBound) {
                        renderTimes(state.times)
                        updateFrequencyDependentUi(state.frequency)
                        applyErrors(state.errors)
                    }

                    if (isEditMode != state.isEditMode) {
                        isEditMode = state.isEditMode
                        requireActivity().invalidateOptionsMenu()
                    }
                    updateTitle(state.isEditMode)
                    binding.root.isEnabled = !state.isSaving
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect {
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun bindForm(state: AddEditUiState) {
        binding.etName.setText(state.name)
        binding.etDosage.setText(state.dosage)
        binding.etInterval.setText(state.intervalHours)
        binding.etStock.setText(state.stockCount)
        binding.etNotes.setText(state.notes)

        val labels = resources.getStringArray(R.array.frequency_options)
        val index = Frequency.entries.indexOf(state.frequency).coerceAtLeast(0)
        binding.dropdownFrequency.setText(labels[index], false)

        dayChips.forEach { (chip, day) ->
            chip.isChecked = day in state.daysOfWeek
        }
        updateDaysRemainingHint()
        applyPillSwatch(state.name)
    }

    private fun renderTimes(times: List<String>) {
        if (times == renderedTimes) return
        renderedTimes = times
        binding.chipGroupTimes.removeAllViews()
        times.forEach { time ->
            val chip = Chip(requireContext()).apply {
                text = time
                isCloseIconVisible = true
                isCheckable = false
                isClickable = false
                isFocusable = true
                minimumHeight = resources.getDimensionPixelSize(R.dimen.min_touch_target)
                setEnsureMinTouchTargetSize(true)
                closeIconContentDescription = getString(R.string.cd_remove_time, time)
                setOnCloseIconClickListener { viewModel.removeTime(time) }
            }
            binding.chipGroupTimes.addView(chip)
        }
        updateDaysRemainingHint()
    }

    private fun updateFrequencyDependentUi(frequency: Frequency) {
        binding.layoutDays.isVisible = frequency == Frequency.SPECIFIC_DAYS
        binding.tilInterval.isVisible = frequency == Frequency.EVERY_X_HOURS
        binding.layoutTimes.isVisible = frequency != Frequency.AS_NEEDED
    }

    private fun applyErrors(errors: MedicationValidator.Result) {
        binding.tilName.error = errors.nameError?.let { getString(it) }
        binding.tilDosage.error = errors.dosageError?.let { getString(it) }
        binding.tilInterval.error = errors.intervalError?.let { getString(it) }
        binding.tilStock.error = errors.stockError?.let { getString(it) }
        setInlineError(binding.textTimesError, errors.timesError)
        setInlineError(binding.textDaysError, errors.daysError)
    }

    private fun setInlineError(view: TextView, errorRes: Int?) {
        view.isVisible = errorRes != null
        view.text = errorRes?.let { getString(it) }
    }

    private fun collectDraft(): MedicationDraft {
        val state = viewModel.uiState.value
        return MedicationDraft(
            name = binding.etName.text?.toString().orEmpty(),
            dosage = binding.etDosage.text?.toString().orEmpty(),
            frequency = state.frequency,
            times = state.times,
            daysOfWeek = state.daysOfWeek,
            intervalHours = binding.etInterval.text?.toString().orEmpty(),
            stockCount = binding.etStock.text?.toString().orEmpty(),
            notes = binding.etNotes.text?.toString().orEmpty()
        )
    }

    private fun updateDaysRemainingHint() {
        val days = MedicationValidator.daysRemaining(collectDraft())
        binding.tilStock.helperText = when {
            viewModel.uiState.value.frequency == Frequency.AS_NEEDED ->
                getString(R.string.stock_days_remaining_as_needed)
            days == null -> getString(R.string.stock_days_remaining_unknown)
            else -> getString(R.string.stock_days_remaining, days)
        }
    }

    private fun applyPillSwatch(name: String) {
        val swatch = ColorGenerator.fromName(name.ifBlank { getString(R.string.app_name) })
        binding.cardPillIcon.setCardBackgroundColor(swatch.background)
        binding.imagePillIcon.imageTintList = ColorStateList.valueOf(swatch.onBackground)
    }

    private fun startEnterTransition() {
        if (enterStarted) return
        val root = _binding?.root ?: return
        enterStarted = true
        root.removeCallbacks(startEnter)
        root.doOnPreDraw { startPostponedEnterTransition() }
    }

    private fun showTimePicker() {
        val is24Hour = DateFormat.is24HourFormat(requireContext())
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(if (is24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(8)
            .setMinute(0)
            .setTitleText(R.string.select_time)
            .build()
        picker.addOnPositiveButtonClickListener {
            viewModel.addTime("%02d:%02d".format(picker.hour, picker.minute))
        }
        picker.show(parentFragmentManager, "medication_time")
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_medication_title)
            .setMessage(R.string.delete_medication_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete() }
            .show()
    }

    private fun updateTitle(isEdit: Boolean) {
        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(
            if (isEdit) R.string.edit_medication else R.string.add_medication
        )
    }

    override fun onDestroyView() {
        view?.removeCallbacks(startEnter)
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_MEDICATION_ID = "medicationId"
    }
}
