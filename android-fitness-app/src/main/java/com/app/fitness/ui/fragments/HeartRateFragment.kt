package com.app.fitness.ui.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.app.fitness.R
import com.app.fitness.databinding.FragmentHeartRateBinding
import com.app.fitness.services.sensors.HeartRateService
import com.app.fitness.ui.viewmodels.HeartRateViewModel
import com.app.fitness.utils.FirebaseAnalyticsManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class HeartRateFragment : Fragment() {

    private var _binding: FragmentHeartRateBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HeartRateViewModel by viewModels()
    private var heartRateService: HeartRateService? = null
    private var isBound = false

    companion object {
        private const val TAG = "HeartRateFragment"
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as HeartRateService.HeartRateBinder
            heartRateService = binder.getService()
            isBound = true
            
            if (heartRateService?.isHeartRateMonitorAvailable() == true) {
                startHeartRateMonitoring()
            } else {
                showSensorError()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            heartRateService = null
            isBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHeartRateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupChart()
        setupObservers()
        setupRefreshLayout()
        bindHeartRateService()
        
        FirebaseAnalyticsManager.logFeatureUsed("heart_rate_monitor")
    }

    private fun setupChart() {
        binding.heartRateChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = object : ValueFormatter() {
                    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    
                    override fun getFormattedValue(value: Float): String {
                        return dateFormat.format(Date(value.toLong()))
                    }
                }
            }
            
            axisLeft.apply {
                setDrawGridLines(false)
                axisMinimum = 40f
                axisMaximum = 220f
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = true
        }
    }

    private fun setupObservers() {
        viewModel.currentHeartRate.observe(viewLifecycleOwner) { heartRate ->
            updateHeartRateDisplay(heartRate)
        }

        viewModel.heartRateHistory.observe(viewLifecycleOwner) { history ->
            updateHeartRateChart(history)
        }

        viewModel.averageHeartRate.observe(viewLifecycleOwner) { average ->
            binding.averageHeartRate.text = getString(R.string.average_heart_rate, average)
        }

        viewModel.maxHeartRate.observe(viewLifecycleOwner) { max ->
            binding.maxHeartRate.text = getString(R.string.max_heart_rate, max)
        }

        viewModel.minHeartRate.observe(viewLifecycleOwner) { min ->
            binding.minHeartRate.text = getString(R.string.min_heart_rate, min)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
    }

    private fun setupRefreshLayout() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun bindHeartRateService() {
        Intent(requireContext(), HeartRateService::class.java).also { intent ->
            requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun startHeartRateMonitoring() {
        heartRateService?.startMonitoring { heartRate ->
            viewModel.updateHeartRate(heartRate, 0)
        }
    }

    private fun updateHeartRateDisplay(heartRate: Int) {
        binding.currentHeartRate.text = heartRate.toString()
        binding.heartRateUnit.visibility = View.VISIBLE
        
        // Update heart rate zone indication
        when (heartRate) {
            in 40..60 -> binding.heartRateZone.setText(R.string.heart_rate_zone_rest)
            in 61..100 -> binding.heartRateZone.setText(R.string.heart_rate_zone_light)
            in 101..140 -> binding.heartRateZone.setText(R.string.heart_rate_zone_cardio)
            in 141..180 -> binding.heartRateZone.setText(R.string.heart_rate_zone_intense)
            else -> binding.heartRateZone.setText(R.string.heart_rate_zone_max)
        }
    }

    private fun updateHeartRateChart(history: List<com.app.fitness.data.database.HeartRateEntity>) {
        val entries = history.map { 
            Entry(it.timestamp.toFloat(), it.heartRate.toFloat())
        }

        val dataSet = LineDataSet(entries, getString(R.string.heart_rate_chart_label)).apply {
            color = resources.getColor(R.color.chart_heart_rate, null)
            setCircleColor(resources.getColor(R.color.chart_heart_rate, null))
            setDrawCircles(true)
            setDrawCircleHole(false)
            setDrawValues(false)
            lineWidth = 2f
            circleRadius = 4f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.heartRateChart.data = LineData(dataSet)
        binding.heartRateChart.invalidate()
    }

    private fun showSensorError() {
        binding.sensorErrorGroup.visibility = View.VISIBLE
        binding.heartRateGroup.visibility = View.GONE
        FirebaseAnalyticsManager.logSensorError("heart_rate", "Sensor not available")
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isBound) {
            heartRateService?.stopMonitoring()
            requireContext().unbindService(serviceConnection)
            isBound = false
        }
        _binding = null
    }
}