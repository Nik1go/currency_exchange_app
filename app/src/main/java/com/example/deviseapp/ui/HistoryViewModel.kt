package com.example.deviseapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.deviseapp.data.HistoricalRateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * ViewModel pour l'écran d'historique des taux de change
 */
class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    
    private val repository = HistoricalRateRepository(app)
    
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _historicalRates = MutableLiveData<List<Pair<String, Double>>>()
    val historicalRates: LiveData<List<Pair<String, Double>>> = _historicalRates
    
    private val _selectedPeriod = MutableLiveData(1) // 1 an par défaut
    val selectedPeriod: LiveData<Int> = _selectedPeriod
    
    private var currentJob: Job? = null
    private var currentBaseCurrency: String = ""
    private var currentTargetCurrency: String = ""
    
    /**
     * Vérifie si les devises sont supportées pour l'historique
     */
    fun isSupported(baseCurrency: String, targetCurrency: String): Boolean {
        return repository.isSupported(baseCurrency, targetCurrency)
    }
    
    /**
     * Charge l'historique des taux de change
     */
    fun loadHistory(baseCurrency: String, targetCurrency: String, years: Int = 1) {
        currentBaseCurrency = baseCurrency
        currentTargetCurrency = targetCurrency
        _selectedPeriod.value = years
        
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            try {
                val rates = repository.getHistoricalRates(
                    baseCurrency = baseCurrency,
                    targetCurrency = targetCurrency,
                    years = years
                )
                
                // Trier par date et convertir en liste de pairs
                val sortedRates = rates.entries
                    .sortedBy { it.key }
                    .map { it.key to it.value }
                
                _historicalRates.postValue(sortedRates)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Erreur lors du chargement")
            } finally {
                _loading.postValue(false)
            }
        }
    }
    
    /**
     * Change la période sélectionnée et recharge les données
     */
    fun setPeriod(years: Int) {
        if (_selectedPeriod.value != years && currentBaseCurrency.isNotEmpty()) {
            loadHistory(currentBaseCurrency, currentTargetCurrency, years)
        }
    }
}
