package com.kalendar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kalendar.app.data.local.entity.EventEntity
import com.kalendar.app.util.DateUtils

/**
 * A timeline item for the Day View matching the exact aesthetic:
 * 
 * 08:00 ─────────────────────
 *       Mathematics
 *       08:00 — 09:00
 */
@Composable
fun TimelineItem(
    event: EventEntity,
    calendarColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
    ) {
        // Time header + horizontal rule: 08:00 ─────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time text
            Text(
                text = DateUtils.formatTime24(event.startTime),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Glowing dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(calendarColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Horizontal line extending to end
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Event Card slightly indented underneath
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(48.dp))
            EventCard(
                event = event,
                calendarColor = calendarColor,
                onClick = onClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
