package com.example.deviseapp.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.deviseapp.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Activité affichant l'historique des taux de change sous forme de graphique
 */
class HistoryActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_BASE_CURRENCY = "base_currency"
        const val EXTRA_TARGET_CURRENCY = "target_currency"
    }
    
    private val viewModel: HistoryViewModel by viewModels()
    
    private lateinit var lineChart: LineChart
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var errorView: LinearLayout
    private lateinit var errorText: TextView
    private lateinit var currencyPairText: TextView
    private lateinit var periodToggleGroup: MaterialButtonToggleGroup
    
    private var baseCurrency = "EUR"
    private var targetCurrency = "USD"
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormatter = SimpleDateFormat("MMM yy", Locale.FRANCE)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        
        // Récupérer les devises depuis l'intent
        baseCurrency = intent.getStringExtra(EXTRA_BASE_CURRENCY) ?: "EUR"
        targetCurrency = intent.getStringExtra(EXTRA_TARGET_CURRENCY) ?: "USD"
        
        setupViews()
        setupObservers()
        
        // Charger les données
        viewModel.loadHistory(baseCurrency, targetCurrency, 1)
    }
    
    private fun setupViews() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        lineChart = findViewById(R.id.lineChart)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        errorView = findViewById(R.id.errorView)
        errorText = findViewById(R.id.errorText)
        currencyPairText = findViewById(R.id.currencyPairText)
        periodToggleGroup = findViewById(R.id.periodToggleGroup)
        
        // Setup toolbar
        toolbar.title = "Historique $baseCurrency → $targetCurrency"
        toolbar.setNavigationOnClickListener { finish() }
        
        // Setup currency pair text
        currencyPairText.text = "1 $baseCurrency = ? $targetCurrency"
        
        // Setup period toggle
        periodToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn1Year -> viewModel.setPeriod(1)
                    R.id.btn5Years -> viewModel.setPeriod(5)
                    R.id.btn15Years -> viewModel.setPeriod(15)
                }
            }
        }
        
        // Setup retry button
        findViewById<MaterialButton>(R.id.retryButton).setOnClickListener {
            val years = viewModel.selectedPeriod.value ?: 1
            viewModel.loadHistory(baseCurrency, targetCurrency, years)
        }
        
        // Setup chart
        setupChart()
    }
    
    private fun setupChart() {
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            
            // Setup X axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelRotationAngle = -45f
            }
            
            // Setup Y axis
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
            }
            axisRight.isEnabled = false
            
            // Legend
            legend.isEnabled = false
            
            // Animation
            animateX(500)
        }
    }
    
    private fun setupObservers() {
        viewModel.loading.observe(this) { isLoading ->
            loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(this) { error ->
            if (error != null) {
                errorView.visibility = View.VISIBLE
                errorText.text = error
                lineChart.visibility = View.GONE
            } else {
                errorView.visibility = View.GONE
                lineChart.visibility = View.VISIBLE
            }
        }
        
        viewModel.historicalRates.observe(this) { rates ->
            if (rates.isNotEmpty()) {
                updateChart(rates)
                
                // Update current rate display
                val latestRate = rates.last().second
                currencyPairText.text = "1 $baseCurrency = ${String.format("%.4f", latestRate)} $targetCurrency"
            }
        }
        
        viewModel.selectedPeriod.observe(this) { years ->
            // Update toggle group selection
            val buttonId = when (years) {
                1 -> R.id.btn1Year
                5 -> R.id.btn5Years
                15 -> R.id.btn15Years
                else -> R.id.btn1Year
            }
            if (periodToggleGroup.checkedButtonId != buttonId) {
                periodToggleGroup.check(buttonId)
            }
        }
    }
    
    private fun updateChart(rates: List<Pair<String, Double>>) {
        // Create entries for chart
        val entries = rates.mapIndexed { index, (_, rate) ->
            Entry(index.toFloat(), rate.toFloat())
        }
        
        // Create dataset
        val dataSet = LineDataSet(entries, "$baseCurrency/$targetCurrency").apply {
            color = getColor(R.color.purple_500)
            setCircleColor(getColor(R.color.purple_500))
            lineWidth = 2f
            circleRadius = 0f // Hide circles for cleaner look with many data points
            setDrawCircleHole(false)
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = getColor(R.color.purple_200)
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        
        // Set data
        lineChart.data = LineData(dataSet)
        
        // Custom X axis formatter for dates
        lineChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < rates.size) {
                    try {
                        val date = dateFormatter.parse(rates[index].first)
                        date?.let { displayDateFormatter.format(it) } ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                } else ""
            }
        }
        
        // Reduce label count based on period
        val labelCount = if (rates.size > 100) 6 else 12
        lineChart.xAxis.setLabelCount(labelCount, true)
        
        lineChart.invalidate()
    }
}
