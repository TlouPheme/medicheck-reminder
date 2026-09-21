package com.example.medicheckreminder.ui.medications

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.doOnPreDraw
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.medicheckreminder.R
import com.example.medicheckreminder.databinding.BottomSheetMedicationActionsBinding
import com.example.medicheckreminder.databinding.FragmentMedicationsListBinding
import com.example.medicheckreminder.domain.model.Medication
import com.example.medicheckreminder.ui.motion.NavMotion
import com.example.medicheckreminder.ui.motion.postponeEnterUntilDrawn
import com.example.medicheckreminder.ui.widget.ViewState
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class MedicationsListFragment : Fragment(R.layout.fragment_medications_list) {

    private var _binding: FragmentMedicationsListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MedicationsViewModel by viewModels()
    private lateinit var adapter: PillCabinetAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMedicationsListBinding.bind(view)
        postponeEnterUntilDrawn(binding.root)

        setupSearch()
        setupGrid()
        observeUiState()
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.setQuery(text?.toString().orEmpty())
        }
    }

    private fun setupGrid() {
        val spanCount = if (resources.getBoolean(R.bool.is_tablet)) 3 else 2
        binding.recyclerMedications.layoutManager = GridLayoutManager(requireContext(), spanCount)
        adapter = PillCabinetAdapter(
            onClick = { medication, icon -> openEditor(medication.id, icon) },
            onLongClick = ::showActionsSheet
        )
        binding.recyclerMedications.adapter = adapter
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.medications) {
                        binding.root.doOnPreDraw { startPostponedEnterTransition() }
                    }
                    binding.stateLayout.setState(medicationsViewState(state))
                    binding.root.doOnPreDraw { startPostponedEnterTransition() }
                }
            }
        }
    }

    private fun medicationsViewState(state: MedicationsUiState): ViewState {
        return when {
            state.isLoading -> ViewState.Loading
            state.errorMessage != null -> ViewState.Error(
                message = state.errorMessage.takeIf { it.isNotBlank() },
                onRetry = viewModel::retry
            )
            state.medications.isEmpty() && state.query.isBlank() -> ViewState.Empty(
                titleRes = R.string.empty_medications_title,
                subtitleRes = R.string.empty_medications_subtitle,
                ctaRes = R.string.add_first_medication,
                onCtaClick = { openEditor(null) }
            )
            state.medications.isEmpty() -> ViewState.Empty(
                titleRes = R.string.empty_search_title,
                subtitleRes = R.string.empty_search_subtitle
            )
            else -> ViewState.Content
        }
    }

    private fun showActionsSheet(medication: Medication) {
        val sheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetMedicationActionsBinding.inflate(layoutInflater)
        sheet.setContentView(sheetBinding.root)

        sheetBinding.textSheetTitle.text = medication.name
        sheetBinding.btnEdit.setOnClickListener {
            sheet.dismiss()
            openEditor(medication.id)
        }
        sheetBinding.btnDelete.setOnClickListener {
            sheet.dismiss()
            viewModel.delete(medication)
        }
        sheet.show()
    }

    private fun openEditor(medicationId: String?, sharedIcon: View? = null) {
        val args = bundleOf("medicationId" to medicationId)
        if (!medicationId.isNullOrBlank() && sharedIcon != null) {
            val extras = FragmentNavigatorExtras(
                sharedIcon to NavMotion.pillTransitionName(medicationId)
            )
            findNavController().navigate(
                R.id.action_medicationsListFragment_to_addEditMedicationFragment,
                args,
                null,
                extras
            )
        } else {
            findNavController().navigate(
                R.id.action_medicationsListFragment_to_addEditMedicationFragment,
                args
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
