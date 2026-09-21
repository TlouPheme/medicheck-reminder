package com.example.medicheckreminder.ui.health

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicheckreminder.MediCheckApp
import com.example.medicheckreminder.R
import com.example.medicheckreminder.domain.model.HealthMeasurement
import com.example.medicheckreminder.domain.model.MeasurementTarget
import com.example.medicheckreminder.domain.model.MeasurementType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class HealthUiState(
    val selectedType: MeasurementType = MeasurementType.WEIGHT,
    val measurements: List<HealthMeasurement> = emptyList(),
    val latestLabel: String = "—",
    val recordedAtLabel: String = "",
    val changeLabel: String = "—",
    val changeDirection: Int = 0,
    val targetLabel: String = "",
    val inTarget: Boolean? = null,
    val chartSeries: List<ChartSeries> = emptyList(),
    val targetMin: Float? = null,
    val targetMax: Float? = null
)

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MediCheckApp).container.healthRepository
    private var measurements: List<HealthMeasurement> = emptyList()

    private val targets = emptyMap<MeasurementType, MeasurementTarget>()

    private val _uiState = MutableStateFlow(buildState(MeasurementType.WEIGHT, emptyList()))
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    private var selectedType: MeasurementType = MeasurementType.WEIGHT
    private var chartColors: Pair<Int, Int> = 0 to 0

    init {
        viewModelScope.launch {
            repository.observeAll().collect { stored ->
                measurements = stored
                publish()
            }
        }
    }

    fun setChartColors(primary: Int, secondary: Int) {
        chartColors = primary to secondary
        publish()
    }

    fun selectType(type: MeasurementType) {
        selectedType = type
        publish()
    }

    fun addMeasurement(
        value: Float,
        secondaryValue: Float?,
        recordedAtMillis: Long
    ) {
        viewModelScope.launch {
            repository.add(
                HealthMeasurement(
                    id = UUID.randomUUID().toString(),
                    type = selectedType,
                    value = value,
                    secondaryValue = secondaryValue,
                    recordedAtMillis = recordedAtMillis
                )
            )
        }
    }

    private fun publish() {
        _uiState.value = buildState(selectedType, chartColors.toList())
    }

    private fun buildState(type: MeasurementType, colors: List<Int>): HealthUiState {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        val recent = measurements
            .filter { it.type == type && it.recordedAtMillis >= cutoff }
            .sortedByDescending { it.recordedAtMillis }
        val latest = recent.firstOrNull()
        val previous = recent.getOrNull(1)
        val target = targets[type]
        val primary = colors.getOrElse(0) { 0xFF2563EB.toInt() }
        val secondary = colors.getOrElse(1) { 0xFF16A34A.toInt() }

        val series = if (type == MeasurementType.BLOOD_PRESSURE) {
            listOf(
                ChartSeries(
                    label = getString(R.string.health_hint_systolic),
                    points = toPoints(recent.reversed(), useSecondary = false),
                    color = primary
                ),
                ChartSeries(
                    label = getString(R.string.health_hint_diastolic),
                    points = toPoints(recent.reversed(), useSecondary = true),
                    color = secondary
                )
            )
        } else {
            listOf(
                ChartSeries(
                    label = typeLabel(type),
                    points = toPoints(recent.reversed(), useSecondary = false),
                    color = primary
                )
            )
        }

        return HealthUiState(
            selectedType = type,
            measurements = recent,
            latestLabel = latest?.let { formatValue(type, it) } ?: "—",
            recordedAtLabel = latest?.let { formatDateTime(it.recordedAtMillis) }.orEmpty(),
            changeLabel = if (latest == null) "" else formatChange(type, latest, previous),
            changeDirection = changeDirection(latest, previous),
            targetLabel = formatTarget(type, target),
            inTarget = latest?.let { isInTarget(type, it, target) },
            chartSeries = series,
            targetMin = target?.min,
            targetMax = target?.max
        )
    }

    private fun toPoints(
        chronological: List<HealthMeasurement>,
        useSecondary: Boolean
    ): List<ChartPoint> {
        val dayFormat = SimpleDateFormat("d MMM", Locale.getDefault())
        return chronological.mapIndexedNotNull { index, item ->
            val y = if (useSecondary) item.secondaryValue else item.value
            y?.let {
                ChartPoint(
                    x = index.toFloat(),
                    y = it,
                    xLabel = dayFormat.format(Date(item.recordedAtMillis))
                )
            }
        }
    }

    private fun formatValue(type: MeasurementType, item: HealthMeasurement): String {
        return when (type) {
            MeasurementType.WEIGHT -> getString(R.string.health_value_weight, item.value)
            MeasurementType.BLOOD_PRESSURE -> getString(
                R.string.health_value_bp,
                item.value.toInt(),
                item.secondaryValue?.toInt() ?: 0
            )
            MeasurementType.BLOOD_SUGAR -> getString(R.string.health_value_sugar, item.value.toInt())
        }
    }

    private fun formatChange(
        type: MeasurementType,
        latest: HealthMeasurement?,
        previous: HealthMeasurement?
    ): String {
        if (latest == null || previous == null) return getString(R.string.health_no_previous)
        val delta = latest.value - previous.value
        val secondaryDelta = if (type == MeasurementType.BLOOD_PRESSURE) {
            (latest.secondaryValue ?: 0f) - (previous.secondaryValue ?: 0f)
        } else {
            null
        }
        return when (type) {
            MeasurementType.WEIGHT -> getString(R.string.health_change_weight, delta)
            MeasurementType.BLOOD_PRESSURE -> getString(
                R.string.health_change_bp,
                delta,
                secondaryDelta ?: 0f
            )
            MeasurementType.BLOOD_SUGAR -> getString(R.string.health_change_sugar, delta)
        }
    }

    private fun changeDirection(latest: HealthMeasurement?, previous: HealthMeasurement?): Int {
        if (latest == null || previous == null) return 0
        val delta = latest.value - previous.value
        return when {
            abs(delta) < 0.05f -> 0
            delta > 0 -> 1
            else -> -1
        }
    }

    private fun formatTarget(type: MeasurementType, target: MeasurementTarget?): String {
        if (target == null) return getString(R.string.health_target_unset)
        return when (type) {
            MeasurementType.WEIGHT ->
                getString(R.string.health_target_weight, target.min, target.max)
            MeasurementType.BLOOD_PRESSURE ->
                getString(
                    R.string.health_target_bp,
                    target.min,
                    target.max,
                    target.secondaryMin ?: 0f,
                    target.secondaryMax ?: 0f
                )
            MeasurementType.BLOOD_SUGAR ->
                getString(R.string.health_target_sugar, target.min, target.max)
        }
    }

    private fun isInTarget(
        type: MeasurementType,
        item: HealthMeasurement,
        target: MeasurementTarget?
    ): Boolean? {
        if (target == null) return null
        val primaryOk = item.value in target.min..target.max
        val secondaryOk = if (type == MeasurementType.BLOOD_PRESSURE) {
            val low = target.secondaryMin ?: return primaryOk
            val high = target.secondaryMax ?: return primaryOk
            val dia = item.secondaryValue ?: return false
            dia in low..high
        } else {
            true
        }
        return primaryOk && secondaryOk
    }

    private fun typeLabel(type: MeasurementType): String = when (type) {
        MeasurementType.WEIGHT -> getString(R.string.health_tab_weight)
        MeasurementType.BLOOD_PRESSURE -> getString(R.string.health_tab_blood_pressure)
        MeasurementType.BLOOD_SUGAR -> getString(R.string.health_tab_blood_sugar)
    }

    private fun formatDateTime(millis: Long): String {
        return SimpleDateFormat("EEE, MMM d · HH:mm", Locale.getDefault()).format(Date(millis))
    }

    private fun getString(resId: Int, vararg args: Any): String {
        return getApplication<Application>().getString(resId, *args)
    }
}
