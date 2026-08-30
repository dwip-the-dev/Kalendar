package com.kalendar.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kalendar.app.ui.theme.HeroDayOfWeek
import com.kalendar.app.ui.theme.HeroSerifNumber
import com.kalendar.app.ui.theme.HeroYearMonth
import com.kalendar.app.util.DateUtils
import com.kalendar.app.util.HeroImageManager
import com.kalendar.app.util.SeasonHelper
import java.time.LocalDate

/**
 * Ultra-high-performance Hero Image card matching Mockup #1.
 * Uses hardware-accelerated direct AsyncImage painter with zero subcomposition latency.
 */
@Composable
fun HeroImage(
    date: LocalDate,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fallbackRes = remember(date) { SeasonHelper.getHeroImageRes(date) }

    // Instant load from memory/disk cache
    var imageUrl by remember(date) {
        mutableStateOf(HeroImageManager.getCachedUrl(context, date))
    }

    // Background fetch from Pexels API
    LaunchedEffect(date) {
        val fetched = HeroImageManager.fetchPexelsPhoto(context, date)
        if (fetched != null) {
            imageUrl = fetched
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
    ) {
        // Direct GPU AsyncImage with zero subcomposition overhead
        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(200)
                    .allowHardware(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                placeholder = painterResource(id = fallbackRes),
                error = painterResource(id = fallbackRes),
                contentDescription = "Nature landscape",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = fallbackRes),
                contentDescription = "Seasonal hero",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Top gradient overlay for crisp text readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Overlay Typography matching Mockup #1
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 28.dp, end = 24.dp)
        ) {
            Text(
                text = "${date.year} / ${date.monthValue}",
                style = HeroYearMonth,
                color = Color.White.copy(alpha = 0.95f)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = HeroSerifNumber,
                    color = Color.White
                )

                Text(
                    text = DateUtils.getDayOfWeekName(date).lowercase().replaceFirstChar { it.uppercase() },
                    style = HeroDayOfWeek,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}
