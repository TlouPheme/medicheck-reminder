package com.example.medicheckreminder.ui.health

import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.medicheckreminder.R
import com.example.medicheckreminder.databinding.BottomSheetAddMeasurementBinding
import com.example.medicheckreminder.databinding.FragmentHealthMeasurementsBinding
import com.example.medicheckreminder.domain.model.MeasurementType
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HealthMeasurementsFragment : Fragment(R.layout.fragment_health_measurements) {

    private var _binding: FragmentHealthMeasurementsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HealthViewModel by viewModels()
    private val adapter = HealthMeasurementAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHealthMeasurementsBinding.bind(view)

        ChartSetupHelper.prepare(binding.chartMeasurements)
        binding.recyclerMeasurements.adapter = adapter
        binding.btnAddMeasurement.setOnClickListener { showAddSheet() }
        binding.fabAddMeasurement.setOnClickListener { showAddSheet() }

        setupTabs()
        applyChartColors()
        observeState()
    }

    private fun setupTabs() {
        val labels = listOf(
            R.string.health_tab_weight,
            R.string.health_tab_blood_pressure,
            R.string.health_tab_blood_sugar
        )
        if (binding.tabMetrics.tabCount == 0) {
            labels.forEach { res ->
                binding.tabMetrics.addTab(binding.tabMetrics.newTab().setText(res))
            }
        }
        binding.tabMetrics.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.selectType(MeasurementType.entries[tab.position])
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun applyChartColors() {
        viewModel.setChartColors(
            MaterialColors.getColor(binding.chartMeasurements, com.google.android.material.R.attr.colorPrimary),
            MaterialColors.getColor(binding.chartMeasurements, com.google.android.material.R.attr.colorSecondary)
        )
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.textLatestValue.text = state.latestLabel
                    binding.textLatestMeta.text = state.recordedAtLabel.ifBlank {
                        getString(R.string.health_no_readings)
                    }
                    binding.textChange.text = state.changeLabel
                    val changeColorAttr = when (state.changeDirection) {
                        1 -> com.google.android.material.R.attr.colorError
                        -1 -> com.google.android.material.R.attr.colorSecondary
                        else -> com.google.android.material.R.attr.colorOnSurfaceVariant
                    }
                    binding.textChange.setTextColor(
                        MaterialColors.getColor(binding.textChange, changeColorAttr)
                    )
                    binding.textTarget.text = state.targetLabel
                    bindTargetTint(state.inTarget)
                    ChartSetupHelper.bind(
                        chart = binding.chartMeasurements,
                        series = state.chartSeries,
                        targetMin = state.targetMin,
                        targetMax = state.targetMax
                    )
                    binding.chartMeasurements.contentDescription = if (state.chartSeries.all { it.points.isEmpty() }) {
                        getString(R.string.a11y_health_chart_empty, typeLabel(state.selectedType))
                    } else {
                        getString(
                            R.string.a11y_health_chart,
                            typeLabel(state.selectedType),
                            state.measurements.size
                        )
                    }
                    adapter.submitList(state.measurements.take(8))
                    binding.recyclerMeasurements.isVisible = state.measurements.isNotEmpty()
                    binding.textRecentEmpty.isVisible = state.measurements.isEmpty()
                }
            }
        }
    }

    private fun bindTargetTint(inTarget: Boolean?) {
        val colorAttr = when (inTarget) {
            true -> com.google.android.material.R.attr.colorSecondary
            false -> com.google.android.material.R.attr.colorError
            null -> com.google.android.material.R.attr.colorOnSurfaceVariant
        }
        binding.textTarget.setTextColor(MaterialColors.getColor(binding.textTarget, colorAttr))
    }

    private fun showAddSheet() {
        val type = viewModel.uiState.value.selectedType
        val sheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetAddMeasurementBinding.inflate(layoutInflater)
        sheet.setContentView(sheetBinding.root)

        val recordedAt = Calendar.getInstance()
        val isBp = type == MeasurementType.BLOOD_PRESSURE
        sheetBinding.textSheetTitle.setText(titleFor(type))
        sheetBinding.tilValue.isVisible = !isBp
        sheetBinding.layoutBp.isVisible = isBp
        sheetBinding.tilValue.setHint(hintFor(type))
        sheetBinding.etDateTime.setText(formatSheetDate(recordedAt.timeInMillis))
        sheetBinding.etDateTime.showSoftInputOnFocus = false
        val openPicker = View.OnClickListener {
            pickDateTime(recordedAt) { sheetBinding.etDateTime.setText(formatSheetDate(it)) }
        }
        sheetBinding.etDateTime.setOnClickListener(openPicker)
        sheetBinding.tilDateTime.setEndIconOnClickListener { openPicker.onClick(it) }

        sheetBinding.btnSave.setOnClickListener {
            val parsed = parseInput(type, sheetBinding) ?: return@setOnClickListener
            viewModel.addMeasurement(parsed.first, parsed.second, recordedAt.timeInMillis)
            sheet.dismiss()
            Snackbar.make(binding.root, R.string.health_saved, Snackbar.LENGTH_SHORT).show()
        }
        sheet.show()
    }

    private fun parseInput(
        type: MeasurementType,
        sheetBinding: BottomSheetAddMeasurementBinding
    ): Pair<Float, Float?>? {
        if (type == MeasurementType.BLOOD_PRESSURE) {
            val systolic = sheetBinding.etSystolic.text?.toString()?.toFloatOrNull()
            val diastolic = sheetBinding.etDiastolic.text?.toString()?.toFloatOrNull()
            sheetBinding.tilSystolic.error = if (systolic == null) {
                getString(R.string.error_health_value)
            } else {
                null
            }
            sheetBinding.tilDiastolic.error = if (diastolic == null) {
                getString(R.string.error_health_value)
            } else {
                null
            }
            if (systolic == null || diastolic == null) return null
            if (systolic <= diastolic) {
                sheetBinding.tilSystolic.error = getString(R.string.error_health_bp_order)
                return null
            }
            return systolic to diastolic
        }

        val value = sheetBinding.etValue.text?.toString()?.toFloatOrNull()
        sheetBinding.tilValue.error = if (value == null) {
            getString(R.string.error_health_value)
        } else {
            null
        }
        return value?.let { it to null }
    }

    private fun pickDateTime(target: Calendar, onPicked: (Long) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.health_pick_date)
            .setSelection(toUtcMillis(target))
            .build()
        picker.addOnPositiveButtonClickListener { utcMillis ->
            val local = fromUtcMillis(utcMillis)
            target.set(Calendar.YEAR, local.get(Calendar.YEAR))
            target.set(Calendar.MONTH, local.get(Calendar.MONTH))
            target.set(Calendar.DAY_OF_MONTH, local.get(Calendar.DAY_OF_MONTH))
            showTimePicker(target, onPicked)
        }
        picker.show(parentFragmentManager, "health_date")
    }

    private fun showTimePicker(target: Calendar, onPicked: (Long) -> Unit) {
        val is24Hour = DateFormat.is24HourFormat(requireContext())
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(if (is24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(target.get(Calendar.HOUR_OF_DAY))
            .setMinute(target.get(Calendar.MINUTE))
            .setTitleText(R.string.health_pick_time)
            .build()
        picker.addOnPositiveButtonClickListener {
            target.set(Calendar.HOUR_OF_DAY, picker.hour)
            target.set(Calendar.MINUTE, picker.minute)
            onPicked(target.timeInMillis)
        }
        picker.show(parentFragmentManager, "health_time")
    }

    private fun titleFor(type: MeasurementType): Int = when (type) {
        MeasurementType.WEIGHT -> R.string.health_add_weight
        MeasurementType.BLOOD_PRESSURE -> R.string.health_add_bp
        MeasurementType.BLOOD_SUGAR -> R.string.health_add_sugar
    }

    private fun typeLabel(type: MeasurementType): String = getString(
        when (type) {
            MeasurementType.WEIGHT -> R.string.health_tab_weight
            MeasurementType.BLOOD_PRESSURE -> R.string.health_tab_blood_pressure
            MeasurementType.BLOOD_SUGAR -> R.string.health_tab_blood_sugar
        }
    )

    private fun hintFor(type: MeasurementType): Int = when (type) {
        MeasurementType.WEIGHT -> R.string.health_hint_weight
        MeasurementType.BLOOD_PRESSURE -> R.string.health_hint_systolic
        MeasurementType.BLOOD_SUGAR -> R.string.health_hint_sugar
    }

    private fun formatSheetDate(millis: Long): String {
        return SimpleDateFormat("EEE, MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(millis))
    }

    private fun toUtcMillis(calendar: Calendar): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.clear()
        utc.set(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        return utc.timeInMillis
    }

    private fun fromUtcMillis(utcMillis: Long): Calendar {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = utcMillis
        val local = Calendar.getInstance()
        local.clear()
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        return local
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
