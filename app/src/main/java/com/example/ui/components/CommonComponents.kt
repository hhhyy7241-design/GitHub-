package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark

@Composable
fun TelegramAvatar(
    imageUrl: String?,
    name: String,
    size: Dp = 48.dp,
    isGroup: Boolean = false,
    showOnlineDot: Boolean = false,
    badgeBorderColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null
) {
    // Outer Box is unbounded so the floating online badge extends smoothly outside the circle rim
    Box(
        modifier = Modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // Avatar circular content
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: if (isGroup) "G" else "C"
                val gradients = listOf(
                    listOf(Color(0xFF0D9488), Color(0xFF042F2E)),
                    listOf(Color(0xFF2563EB), Color(0xFF1E3A8A)),
                    listOf(Color(0xFF7C3AED), Color(0xFF4C1D95)),
                    listOf(Color(0xFFE11D48), Color(0xFF881337)),
                    listOf(Color(0xFFD97706), Color(0xFF78350F)),
                    listOf(Color(0xFF059669), Color(0xFF064E3B))
                )
                val colorIndex = (name.hashCode() and 0x7FFFFFFF) % gradients.size
                val brush = Brush.linearGradient(gradients[colorIndex])

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(brush),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGroup && name.length <= 1) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(size * 0.55f)
                        )
                    } else {
                        Text(
                            text = initial,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = (size.value * 0.42f).sp
                        )
                    }
                }
            }
        }

        // Floating Online Badge outside the avatar perimeter (Telegram/WhatsApp style)
        if (showOnlineDot) {
            val dotSize = (size.value * 0.28f).coerceIn(11f, 16f).dp
            val borderWidth = if (size > 44.dp) 2.5.dp else 2.dp

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .shadow(elevation = 3.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(OnlineGreen)
                    .border(borderWidth, badgeBorderColor, CircleShape)
            )
        }
    }
}
