package com.example.medicheckreminder.ui.home

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.medicheckreminder.MediCheckApp
import com.example.medicheckreminder.R
import com.example.medicheckreminder.databinding.FragmentHomeBinding
import com.example.medicheckreminder.domain.model.Dose
import com.example.medicheckreminder.ui.widget.ViewState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var doseAdapter: DoseAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        setupHeader()
        setupRecyclerView()
        setupSwipeRefresh()
        setupOfflineBanner()
        observeUiState()
    }

    private fun setupHeader() {
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        binding.textDate.text = sdf.format(Date())
    }

    private fun setupRecyclerView() {
        doseAdapter = DoseAdapter(
            onTakenClick = { dose ->
                viewModel.markAsTaken(dose)
                announceDoseStatus(dose, R.string.status_taken)
            },
            onSnoozeClick = { dose ->
                viewModel.snooze(dose)
                announceDoseStatus(dose, R.string.status_snoozed)
            },
            onSkipClick = { dose ->
                viewModel.markAsSkipped(dose)
                announceDoseStatus(dose, R.string.status_skipped)
            }
        )
        binding.recyclerDoses.adapter = doseAdapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary_light)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.sync()
        }
    }

    /**
     * Offline banner: [HomeUiState.isOffline] drives visibility when there is already
     * content. A first-load failure uses [ViewState.Error] instead.
     */
    private fun setupOfflineBanner() {
        binding.layoutOfflineBanner.root.setOnClickListener { viewModel.sync() }
        binding.layoutOfflineBanner.btnSync.setOnClickListener { viewModel.sync() }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefresh.isRefreshing = state.isLoading && state.doses.isNotEmpty()
                    binding.textGreeting.text = greetingFor()

                    binding.layoutProgress.progressAdherence.setProgressCompat(
                        state.todayAdherence,
                        true
                    )
                    binding.layoutProgress.progressAdherence.contentDescription =
                        getString(R.string.a11y_adherence, state.todayAdherence)
                    binding.layoutProgress.textProgressPercentage.text =
                        getString(R.string.home_adherence_percent, state.todayAdherence)

                    doseAdapter.submitList(state.doses)
                    binding.stateLayout.setState(homeViewState(state))

                    binding.layoutOfflineBanner.root.isVisible =
                        state.isOffline && state.doses.isNotEmpty()
                }
            }
        }
    }

    private fun homeViewState(state: HomeUiState): ViewState {
        return when {
            state.isLoading && state.doses.isEmpty() -> ViewState.Loading
            state.isOffline && state.doses.isEmpty() -> ViewState.Error(
                messageRes = R.string.error_generic_message,
                onRetry = viewModel::sync
            )
            state.doses.isEmpty() -> ViewState.Empty(
                titleRes = R.string.empty_today_doses,
                subtitleRes = R.string.empty_today_doses_subtitle
            )
            else -> ViewState.Content
        }
    }

    private fun greetingFor(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..16 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
        val greeting = getString(greetingRes)
        val name = (requireContext().applicationContext as MediCheckApp)
            .container.accountStore
            .displayName()
        return if (name.isEmpty()) {
            greeting
        } else {
            getString(R.string.greeting_named, greeting, name)
        }
    }

    private fun announceDoseStatus(dose: Dose, statusRes: Int) {
        binding.root.announceForAccessibility(
            getString(
                R.string.a11y_dose_status_changed,
                dose.medicationName,
                getString(statusRes)
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
