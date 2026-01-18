package com.example.deviseapp.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class RateRepository(
    context: Context
    ) {
    private val prefs = context.getSharedPreferences("rates_cache", Context.MODE_PRIVATE)
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BASIC)
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    private val api: ExchangeRateService by lazy {
        Retrofit.Builder()
            .baseUrl("https://open.er-api.com/")
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ExchangeRateService::class.java)
    }

    // Devises supportées par Frankfurter API
    private val supported = listOf(
        "EUR", "USD", "GBP", "CHF", "CAD", "AUD", "JPY", "CNY", "BRL", "NOK",
        "SEK", "DKK", "THB", "INR", "KRW", "MXN", "SGD", "HKD", "NZD", "ZAR",
        "TRY", "PLN", "CZK", "HUF", "RON", "ILS", "PHP", "MYR", "IDR", "ISK"
    )
    private val cacheKeyRates = "rates_json"
    private val cacheKeyBase = "base"
    private val cacheKeyTimestamp = "timestamp"

    suspend fun getRates(base: String = "EUR", forceRefresh: Boolean = false): Map<String, Double> =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val last = prefs.getLong(cacheKeyTimestamp, 0L)
            val cachedBase = prefs.getString(cacheKeyBase, null)
            if (!forceRefresh && now - last < TimeUnit.HOURS.toMillis(24) && cachedBase == base) {
                loadCachedRates() ?: fetchAndCache(base)
            } else {
                fetchAndCache(base)
            }
        }

    private fun loadCachedRates(): Map<String, Double>? {
        val json = prefs.getString(cacheKeyRates, null) ?: return null
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Double::class.javaObjectType)
        val adapter = moshi.adapter<Map<String, Double>>(type)
        return adapter.fromJson(json)
    }

    private suspend fun fetchAndCache(base: String): Map<String, Double> {
        val resp = api.latest(base = base)
        if (resp.result.lowercase() != "success") {
            throw IllegalStateException(resp.errorType ?: "Réponse API invalide")
        }
        val receivedBase = resp.baseCode ?: base
        val allRates = resp.rates ?: throw IllegalStateException("Taux non disponibles")
        val filtered = allRates.filterKeys { supported.contains(it) }
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Double::class.javaObjectType)
        val adapter = moshi.adapter<Map<String, Double>>(type)
        val json = adapter.toJson(filtered)
        prefs.edit()
            .putString(cacheKeyRates, json)
            .putString(cacheKeyBase, receivedBase)
            .putLong(cacheKeyTimestamp, System.currentTimeMillis())
            .apply()
        return filtered
    }

    fun getSupportedCurrencies(): List<String> = supported
}


