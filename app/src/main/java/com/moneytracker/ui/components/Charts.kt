package com.moneytracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

@Composable
fun LineChartView(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setDrawGridBackground(false)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                axisRight.isEnabled = false
                legend.isEnabled = true
            }
        },
        update = { chart ->
            val entries = data.mapIndexed { index, pair ->
                Entry(index.toFloat(), pair.second.toFloat())
            }
            val dataSet = LineDataSet(entries, "Spending").apply {
                setDrawCircles(true)
                setDrawValues(false)
                color = ColorTemplate.getHoloBlue()
                setCircleColor(ColorTemplate.getHoloBlue())
                lineWidth = 2f
            }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(data.map { it.first })
            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
    )
}

@Composable
fun PieChartView(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                isRotationEnabled = true
                legend.isEnabled = true
            }
        },
        update = { chart ->
            val entries = data.map { PieEntry(it.value.toFloat(), it.key) }
            val dataSet = PieDataSet(entries, "").apply {
                colors = ColorTemplate.MATERIAL_COLORS.toList()
                valueTextSize = 12f
            }
            chart.data = PieData(dataSet)
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
    )
}

@Composable
fun BarChartView(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                setDrawGridBackground(false)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                axisRight.isEnabled = false
                legend.isEnabled = false
            }
        },
        update = { chart ->
            val entries = data.entries.mapIndexed { index, pair ->
                BarEntry(index.toFloat(), pair.value.toFloat())
            }
            val dataSet = BarDataSet(entries, "Spending").apply {
                colors = ColorTemplate.MATERIAL_COLORS.toList()
                valueTextSize = 12f
            }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(data.keys.toList())
            chart.data = BarData(dataSet)
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
    )
}
