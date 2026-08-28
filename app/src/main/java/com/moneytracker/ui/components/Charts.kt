/**
 * Chart primitives for the Statistics screen (line / pie / bar).
 *
 * Built directly on `androidx.compose.foundation.Canvas` rather than a
 * third-party charting library to keep the APK small and the rendering
 * behaviour under our control.
 */
package com.moneytracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    // Build the dataset once per `data` change at the composable scope and
    // only invalidate MPAndroidChart when the reference actually differs.
    // The previous implementation rebuilt the dataset inside `update` and
    // re-invalidated on every recomposition, which forced a full
    // measure+layout+draw cycle on every Statistics-tab navigation, even
    // when the data hadn't changed. The `remember` here is in the
    // composable scope, so it's allowed; the closure captures it.
    val lineData = remember(data) {
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
        LineData(dataSet)
    }
    val xLabels = remember(data) { data.map { it.first } }
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
            if (chart.data !== lineData) {
                chart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
                chart.data = lineData
                chart.invalidate()
            }
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
    // See LineChartView for the memoisation rationale.
    val pieData = remember(data) {
        val entries = data.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
        }
        PieData(dataSet)
    }
    AndroidView(
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                isRotationEnabled = true
                legend.isEnabled = true
            }
        },
        update = { chart ->
            if (chart.data !== pieData) {
                chart.data = pieData
                chart.invalidate()
            }
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
    // See LineChartView for the memoisation rationale.
    val barData = remember(data) {
        val entries = data.entries.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.value.toFloat())
        }
        val dataSet = BarDataSet(entries, "Spending").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
        }
        BarData(dataSet)
    }
    val xLabels = remember(data) { data.keys.toList() }
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
            if (chart.data !== barData) {
                chart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
                chart.data = barData
                chart.invalidate()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
    )
}
