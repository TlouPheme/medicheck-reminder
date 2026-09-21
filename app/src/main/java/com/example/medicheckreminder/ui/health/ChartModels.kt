package com.example.medicheckreminder.ui.health

data class ChartPoint(
    val x: Float,
    val y: Float,
    val xLabel: String
)

data class ChartSeries(
    val label: String,
    val points: List<ChartPoint>,
    val color: Int
)
