package com.example.deviseapp.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Service API pour récupérer l'historique des taux de change
 * Utilise l'API Frankfurter (https://frankfurter.dev)
 */
interface HistoricalRateService {
    
    /**
     * Récupère les taux historiques sur une période donnée
     * @param dateRange Format: "start_date..end_date" (ex: "2024-01-01..2025-01-01")
     * @param base Devise de base (ex: "EUR")
     * @param symbols Devise cible (ex: "USD")
     */
    @GET("v1/{dateRange}")
    suspend fun getTimeSeries(
        @Path("dateRange") dateRange: String,
        @Query("base") base: String,
        @Query("symbols") symbols: String
    ): TimeSeriesResponse
}

@JsonClass(generateAdapter = true)
data class TimeSeriesResponse(
    @Json(name = "base") val base: String,
    @Json(name = "start_date") val startDate: String,
    @Json(name = "end_date") val endDate: String,
    @Json(name = "rates") val rates: Map<String, Map<String, Double>>
)
