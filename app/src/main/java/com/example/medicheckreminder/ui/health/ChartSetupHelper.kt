package com.example.medicheckreminder.ui.health

import android.graphics.drawable.GradientDrawable
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.color.MaterialColors

object ChartSetupHelper {

    fun prepare(chart: LineChart) {
        val onSurfaceVariant = MaterialColors.getColor(
            chart,
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )
        val outline = MaterialColors.getColor(
            chart,
            com.google.android.material.R.attr.colorOutline
        )
        val noData = MaterialColors.getColor(
            chart,
            com.google.android.material.R.attr.colorOnSurface
        )

        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.setScaleEnabled(true)
        chart.setDrawGridBackground(false)
        chart.setNoDataText(chart.context.getString(com.example.medicheckreminder.R.string.health_chart_empty))
        chart.setNoDataTextColor(noData)
        chart.extraBottomOffset = 8f
        chart.minOffset = 8f

        val scaledText = 12f * chart.resources.configuration.fontScale
        chart.axisRight.isEnabled = false
        chart.axisLeft.apply {
            textColor = onSurfaceVariant
            textSize = scaledText
            gridColor = outline
            axisLineColor = outline
            setDrawAxisLine(false)
            granularity = 1f
        }
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = onSurfaceVariant
            textSize = scaledText
            gridColor = outline
            setDrawGridLines(false)
            setDrawAxisLine(false)
            granularity = 1f
            isGranularityEnabled = true
        }
        chart.legend.apply {
            textColor = onSurfaceVariant
            textSize = scaledText
            isEnabled = true
            formSize = 10f * chart.resources.configuration.fontScale
        }
    }

    fun bind(
        chart: LineChart,
        series: List<ChartSeries>,
        targetMin: Float? = null,
        targetMax: Float? = null
    ) {
        if (series.all { it.points.isEmpty() }) {
            chart.clear()
            chart.invalidate()
            return
        }

        val labels = linkedMapOf<Float, String>()
        series.forEach { item ->
            item.points.forEach { point -> labels[point.x] = point.xLabel }
        }

        val dataSets = series
            .filter { it.points.isNotEmpty() }
            .map { item -> toDataSet(chart, item) }

        chart.axisLeft.removeAllLimitLines()
        targetMin?.let { chart.axisLeft.addLimitLine(limitLine(chart, it, dashed = true)) }
        targetMax?.let { chart.axisLeft.addLimitLine(limitLine(chart, it, dashed = false)) }

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return labels[value] ?: labels[kotlin.math.round(value).toFloat()] ?: ""
            }
        }
        chart.xAxis.setLabelCount(6, false)
        chart.legend.isEnabled = dataSets.size > 1
        val lineData = LineData()
        dataSets.forEach { lineData.addDataSet(it) }
        chart.data = lineData
        chart.invalidate()
    }

    private fun toDataSet(chart: LineChart, series: ChartSeries): LineDataSet {
        val entries = series.points.map { Entry(it.x, it.y) }
        return LineDataSet(entries, series.label).apply {
            color = series.color
            setCircleColor(series.color)
            lineWidth = 2.2f
            circleRadius = 3.2f
            setDrawCircleHole(true)
            circleHoleColor = MaterialColors.getColor(
                chart,
                com.google.android.material.R.attr.colorSurface
            )
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.12f
            setDrawFilled(true)
            fillDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(series.color, android.graphics.Color.TRANSPARENT)
            )
            fillAlpha = 80
            setDrawHighlightIndicators(true)
            highLightColor = series.color
        }
    }

    private fun limitLine(chart: LineChart, value: Float, dashed: Boolean): LimitLine {
        val color = MaterialColors.getColor(
            chart,
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )
        return LimitLine(value).apply {
            lineColor = color
            lineWidth = 1f
            if (dashed) {
                enableDashedLine(8f, 6f, 0f)
            }
        }
    }
}
