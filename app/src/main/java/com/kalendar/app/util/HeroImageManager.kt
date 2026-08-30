package com.kalendar.app.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate

/**
 * Manages dynamic hero image fetching via the Pexels API.
 * 
 * Features:
 * - Real-time fetching from Pexels API using portrait orientation for mobile screens
 * - Seasonal and nature queries that auto-rotate daily
 * - 3-photo persistent cache in SharedPreferences + Coil disk cache for 100% offline support
 * - Automatic fallback to bundled high-res seasonal drawables
 */
object HeroImageManager {

    private const val TAG = "HeroImageManager"
    // Pexels API key stub for open source repository. Replace with your own key from https://www.pexels.com/api/
    private const val PEXELS_API_KEY = "YOUR_PEXELS_API_KEY_HERE"
    private const val PREFS_NAME = "kalendar_hero_images"
    private const val KEY_CACHE_PREFIX = "hero_img_"

    private val _currentImageUrl = MutableStateFlow<String?>(null)
    val currentImageUrl: StateFlow<String?> = _currentImageUrl

    /**
     * Curated natural landscape queries tuned for each season.
     */
    private val seasonalQueries = mapOf(
        SeasonHelper.Season.WINTER to listOf(
            "winter snowy mountains landscape",
            "misty winter pine forest",
            "frozen lake snow mountain",
            "winter sunset snowy peaks"
        ),
        SeasonHelper.Season.SPRING to listOf(
            "spring blooming cherry blossoms",
            "green hills morning mist nature",
            "vibrant spring meadow flowers",
            "spring valley sunlight nature"
        ),
        SeasonHelper.Season.SUMMER to listOf(
            "golden sunset ocean mountains",
            "lush summer mountain valley",
            "serene tropical nature landscape",
            "golden hour alpine lake"
        ),
        SeasonHelper.Season.AUTUMN to listOf(
            "golden autumn forest foliage",
            "autumn mountain road fall leaves",
            "misty autumn lake colorful trees",
            "fall colors nature landscape"
        )
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Get the cached image URL for a given date from SharedPreferences.
     */
    fun getCachedUrl(context: Context, date: LocalDate): String? {
        val key = "$KEY_CACHE_PREFIX$date"
        return getPrefs(context).getString(key, null)
    }

    /**
     * Save the image URL for a given date in SharedPreferences and maintain max 3 entries.
     */
    private fun saveCachedUrl(context: Context, date: LocalDate, url: String) {
        val prefs = getPrefs(context)
        prefs.edit().putString("$KEY_CACHE_PREFIX$date", url).apply()

        // Clean up older caches (keep only last 3 days)
        val validKeys = setOf(
            "$KEY_CACHE_PREFIX${date.minusDays(1)}",
            "$KEY_CACHE_PREFIX$date",
            "$KEY_CACHE_PREFIX${date.plusDays(1)}"
        )
        val allKeys = prefs.all.keys.filter { it.startsWith(KEY_CACHE_PREFIX) }
        val toRemove = allKeys.filter { it !in validKeys }
        if (toRemove.isNotEmpty()) {
            val editor = prefs.edit()
            toRemove.forEach { editor.remove(it) }
            editor.apply()
        }
    }

    /**
     * Fetches a high quality portrait nature photo URL from Pexels API for the specified date.
     * Uses day-of-year to rotate queries and pages for fresh daily content.
     */
    suspend fun fetchPexelsPhoto(context: Context, date: LocalDate): String? = withContext(Dispatchers.IO) {
        if (PEXELS_API_KEY.isBlank() || PEXELS_API_KEY.startsWith("YOUR_")) {
            return@withContext null
        }
        try {
            val season = SeasonHelper.getSeason(date)
            val queries = seasonalQueries[season] ?: seasonalQueries[SeasonHelper.Season.SUMMER]!!
            val queryIndex = (date.dayOfYear % queries.size).coerceIn(0, queries.size - 1)
            val query = queries[queryIndex]
            val page = (date.dayOfYear % 10) + 1 // Cycle through pages 1-10 for variety

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val apiUrl = "https://api.pexels.com/v1/search?query=$encodedQuery&orientation=portrait&per_page=15&page=$page"

            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", PEXELS_API_KEY)
                connectTimeout = 10000
                readTimeout = 10000
                instanceFollowRedirects = true
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                connection.disconnect()

                val json = JSONObject(response)
                val photos = json.optJSONArray("photos")
                if (photos != null && photos.length() > 0) {
                    // Pick a photo based on day of month for deterministic daily rotation
                    val photoIndex = (date.dayOfMonth % photos.length()).coerceIn(0, photos.length() - 1)
                    val photoObj = photos.getJSONObject(photoIndex)
                    val src = photoObj.getJSONObject("src")

                    // Prefer portrait or large2x for sharp rendering on high-DPI screens
                    val photoUrl = src.optString("portrait").ifBlank {
                        src.optString("large2x").ifBlank {
                            src.optString("large")
                        }
                    }

                    if (photoUrl.isNotBlank()) {
                        saveCachedUrl(context, date, photoUrl)
                        preloadImage(context, photoUrl)
                        return@withContext photoUrl
                    }
                }
            } else {
                Log.w(TAG, "Pexels API response code: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching photo from Pexels API", e)
        }
        return@withContext null
    }

    /**
     * Preload image into Coil disk and memory cache.
     */
    private fun preloadImage(context: Context, imageUrl: String) {
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
        context.imageLoader.enqueue(request)
    }

    /**
     * Prefetch images for yesterday, today, and tomorrow into cache.
     */
    fun prefetchRecentImages(context: Context, referenceDate: LocalDate = LocalDate.now()) {
        CoroutineScope(Dispatchers.IO).launch {
            val dates = listOf(
                referenceDate,
                referenceDate.minusDays(1),
                referenceDate.plusDays(1)
            )
            for (date in dates) {
                val cached = getCachedUrl(context, date)
                if (cached != null) {
                    preloadImage(context, cached)
                } else {
                    fetchPexelsPhoto(context, date)
                }
            }
        }
    }
}
