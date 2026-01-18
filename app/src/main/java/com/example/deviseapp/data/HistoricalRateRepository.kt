package com.example.deviseapp.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Repository pour l'historique des taux de change
 * Stratégie cache-first avec Firebase Firestore pour le support hors ligne
 */
class HistoricalRateRepository(context: Context) {
    
    companion object {
        private const val TAG = "HistoricalRateRepo"
        private const val CACHE_VALIDITY_HOURS = 24L
        private const val COLLECTION_RATE_HISTORY = "rate_history"
    }
    
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BASIC)
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    private val api: HistoricalRateService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.frankfurter.dev/")
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(HistoricalRateService::class.java)
    }
    
    // Devises supportées par Frankfurter API
    private val supportedCurrencies = setOf(
        "EUR", "USD", "GBP", "CHF", "CAD", "JPY", "AUD", "CNY", "BRL", "NOK",
        "BGN", "CZK", "DKK", "HUF", "PLN", "RON", "SEK", "ISK", "TRY", "ZAR",
        "HKD", "IDR", "ILS", "INR", "KRW", "MXN", "MYR", "NZD", "PHP", "SGD", "THB"
    )
    
    /**
     * Vérifie si une paire de devises est supportée pour l'historique
     */
    fun isSupported(baseCurrency: String, targetCurrency: String): Boolean {
        return baseCurrency in supportedCurrencies && targetCurrency in supportedCurrencies
    }
    
    /**
     * Récupère l'historique des taux de change
     * @param baseCurrency Devise de base
     * @param targetCurrency Devise cible
     * @param years Nombre d'années d'historique (1 ou 5)
     * @return Map de date -> taux
     */
    suspend fun getHistoricalRates(
        baseCurrency: String,
        targetCurrency: String,
        years: Int
    ): Map<String, Double> = withContext(Dispatchers.IO) {
        
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val cacheKey = "${baseCurrency}_${targetCurrency}_${years}y"
        
        // 1. Essayer de charger depuis le cache Firestore
        val cachedData = loadFromCache(userId, cacheKey)
        if (cachedData != null) {
            Log.d(TAG, "Données chargées depuis le cache pour $cacheKey")
            return@withContext cachedData
        }
        
        // 2. Sinon, récupérer depuis l'API
        Log.d(TAG, "Téléchargement des données depuis l'API pour $cacheKey")
        val rates = fetchFromApi(baseCurrency, targetCurrency, years)
        
        // 3. Sauvegarder dans le cache
        saveToCache(userId, cacheKey, rates)
        
        rates
    }
    
    private suspend fun loadFromCache(userId: String, cacheKey: String): Map<String, Double>? {
        return try {
            val docRef = firestore
                .collection("users")
                .document(userId)
                .collection(COLLECTION_RATE_HISTORY)
                .document(cacheKey)
            
            val snapshot = docRef.get().await()
            
            if (!snapshot.exists()) {
                return null
            }
            
            val lastUpdated = snapshot.getTimestamp("lastUpdated")?.toDate()
            if (lastUpdated == null || isCacheExpired(lastUpdated)) {
                Log.d(TAG, "Cache expiré pour $cacheKey")
                return null
            }
            
            @Suppress("UNCHECKED_CAST")
            val ratesData = snapshot.get("rates") as? Map<String, Any> ?: return null
            
            // Convertir les valeurs en Double
            ratesData.mapValues { (_, value) ->
                when (value) {
                    is Double -> value
                    is Long -> value.toDouble()
                    is Number -> value.toDouble()
                    else -> 0.0
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur chargement cache: ${e.message}")
            null
        }
    }
    
    private fun isCacheExpired(lastUpdated: Date): Boolean {
        val now = System.currentTimeMillis()
        val cacheTime = lastUpdated.time
        val validityMs = TimeUnit.HOURS.toMillis(CACHE_VALIDITY_HOURS)
        return (now - cacheTime) > validityMs
    }
    
    private suspend fun fetchFromApi(
        baseCurrency: String,
        targetCurrency: String,
        years: Int
    ): Map<String, Double> {
        val endDate = Calendar.getInstance()
        val startDate = Calendar.getInstance().apply {
            add(Calendar.YEAR, -years)
        }
        
        val dateRange = "${dateFormat.format(startDate.time)}..${dateFormat.format(endDate.time)}"
        
        val response = api.getTimeSeries(
            dateRange = dateRange,
            base = baseCurrency,
            symbols = targetCurrency
        )
        
        // Transformer les données: Map<date, Map<currency, rate>> -> Map<date, rate>
        return response.rates.mapValues { (_, currencyRates) ->
            currencyRates[targetCurrency] ?: 0.0
        }
    }
    
    private suspend fun saveToCache(userId: String, cacheKey: String, rates: Map<String, Double>) {
        try {
            val docRef = firestore
                .collection("users")
                .document(userId)
                .collection(COLLECTION_RATE_HISTORY)
                .document(cacheKey)
            
            val data = hashMapOf(
                "rates" to rates,
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )
            
            docRef.set(data).await()
            Log.d(TAG, "Données sauvegardées dans le cache pour $cacheKey")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur sauvegarde cache: ${e.message}")
            // Ne pas propager l'erreur - le cache est optionnel
        }
    }
}
