package com.example.medicheckreminder.ui.history

import android.graphics.Color
import android.os.Bundle
import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.content.FileProvider
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.medicheckreminder.R
import com.example.medicheckreminder.databinding.FragmentHistoryBinding
import com.example.medicheckreminder.ui.widget.ViewState
import com.example.medicheckreminder.util.PdfReportExporter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private val pdfExporter = PdfReportExporter()

    private lateinit var calendarAdapter: CalendarDayAdapter
    private lateinit var logAdapter: DoseLogAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryBinding.bind(view)

        setupMenu()
        setupCalendar()
        setupLogs()
        setupFilters()
        observeState()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_history, menu)
                val item = menu.findItem(R.id.action_export)
                val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_export)?.mutate()
                val attrs = requireContext().obtainStyledAttributes(
                    intArrayOf(com.google.android.material.R.attr.colorOnSurface)
                )
                icon?.setTint(attrs.getColor(0, Color.BLACK))
                attrs.recycle()
                item.icon = icon
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return if (menuItem.itemId == R.id.action_export) {
                    exportReport()
                    true
                } else {
                    false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupCalendar() {
        calendarAdapter = CalendarDayAdapter(viewModel::selectDate)
        binding.recyclerCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.recyclerCalendar.adapter = calendarAdapter
        binding.recyclerCalendar.itemAnimator = null
        binding.btnPrevMonth.setOnClickListener { viewModel.shiftMonth(-1) }
        binding.btnNextMonth.setOnClickListener { viewModel.shiftMonth(1) }
    }

    private fun setupLogs() {
        logAdapter = DoseLogAdapter()
        binding.recyclerDoseLogs.adapter = logAdapter
    }

    private fun setupFilters() {
        binding.chipLast7.setOnClickListener {
            viewModel.setFilter(HistoryFilter.LAST_7)
        }
        binding.chipLast30.setOnClickListener {
            viewModel.setFilter(HistoryFilter.LAST_30)
        }
        binding.chipCustom.setOnClickListener {
            showCustomRangePicker()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.textMonthTitle.text = state.monthTitle
                    binding.textSelectedDate.text = periodHeading(state)
                    calendarAdapter.submitList(state.calendarDays)
                    logAdapter.submitList(state.selectedLogs)
                    binding.stateLayout.setState(
                        if (state.selectedLogs.isEmpty()) {
                            ViewState.Empty(
                                titleRes = R.string.empty_history_title,
                                subtitleRes = R.string.empty_history_subtitle
                            )
                        } else {
                            ViewState.Content
                        }
                    )
                    syncFilterChips(state)
                }
            }
        }
    }

    private fun periodHeading(state: HistoryUiState): String {
        return when (state.filter) {
            HistoryFilter.LAST_7 -> getString(R.string.history_filter_7)
            HistoryFilter.LAST_30 -> getString(R.string.history_filter_30)
            HistoryFilter.CUSTOM -> state.customRangeLabel
                ?: getString(R.string.history_filter_custom)
        }
    }

    private fun syncFilterChips(state: HistoryUiState) {
        binding.chipLast7.isChecked = state.filter == HistoryFilter.LAST_7
        binding.chipLast30.isChecked = state.filter == HistoryFilter.LAST_30
        binding.chipCustom.isChecked = state.filter == HistoryFilter.CUSTOM
        binding.chipCustom.text = state.customRangeLabel
            ?: getString(R.string.history_filter_custom)
    }

    private fun showCustomRangePicker() {
        val state = viewModel.uiState.value
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.history_filter_custom)
            .setSelection(Pair(state.rangeStartMillis, state.rangeEndMillis))
            .build()
        picker.addOnPositiveButtonClickListener { range ->
            val start = range.first
            val end = range.second
            if (start != null && end != null) {
                viewModel.setCustomRange(start, end)
            }
        }
        picker.show(parentFragmentManager, "history_custom_range")
    }

    private fun exportReport() {
        val state = viewModel.uiState.value
        val result = pdfExporter.export(
            requireContext(),
            PdfReportExporter.Params(
                logs = viewModel.logsForExport(),
                rangeStartMillis = state.rangeStartMillis,
                rangeEndMillis = state.rangeEndMillis,
                title = getString(R.string.export_pdf_title)
            )
        )
        val message = when (result) {
            is PdfReportExporter.Result.Success -> {
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    result.file
                )
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(share, getString(R.string.export_report)))
                getString(R.string.export_pdf_success, result.file.name)
            }
            is PdfReportExporter.Result.Failure -> getString(result.messageRes)
        }
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
