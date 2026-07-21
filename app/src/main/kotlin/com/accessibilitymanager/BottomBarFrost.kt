package com.accessibilitymanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class BottomBarFrostSpec(
    val enabled: Boolean,
    val blurRadiusDp: Float,
    val tintAlpha: Float,
    val noiseFactor: Float,
    val fallbackAlpha: Float,
)

internal fun bottomBarFrostSpec(value: Float): BottomBarFrostSpec {
    val frost = sanitizeBottomBarFrost(value)
    if (frost == 0f) {
        return BottomBarFrostSpec(
            enabled = false,
            blurRadiusDp = 0f,
            tintAlpha = 1f,
            noiseFactor = 0f,
            fallbackAlpha = 1f,
        )
    }

    return BottomBarFrostSpec(
        enabled = true,
        blurRadiusDp = 52f * frost,
        tintAlpha = 1f - (0.58f * frost),
        noiseFactor = 0.10f * frost,
        fallbackAlpha = 1f - (0.32f * frost),
    )
}

internal fun Modifier.bottomBarFrostSource(
    frost: Float,
    state: HazeState,
): Modifier = if (bottomBarFrostSpec(frost).enabled) hazeSource(state) else this

@Composable
internal fun FrostedBottomNavigationBar(
    frost: Float,
    state: HazeState,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val spec = bottomBarFrostSpec(frost)
    val surface = MiuixTheme.colorScheme.surface
    val style = remember(surface, spec) { frostHazeStyle(surface, spec) }
    val effectModifier = if (spec.enabled) {
        modifier.hazeEffect(state = state, style = style)
    } else {
        modifier
    }

    NavigationBar(
        modifier = effectModifier,
        color = if (spec.enabled) Color.Transparent else surface,
        content = content,
    )
}

@Composable
internal fun BottomBarFrostPreview(frost: Float) {
    val spec = bottomBarFrostSpec(frost)
    val state = rememberHazeState(blurEnabled = spec.enabled)
    val surface = MiuixTheme.colorScheme.surface
    val style = remember(surface, spec) { frostHazeStyle(surface, spec) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(if (spec.enabled) Modifier.hazeSource(state) else Modifier)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FrostPreviewBlock(
                color = MiuixTheme.colorScheme.primary,
                height = 42,
            )
            FrostPreviewBlock(
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                height = 28,
            )
            FrostPreviewBlock(
                color = MiuixTheme.colorScheme.error,
                height = 48,
            )
            FrostPreviewBlock(
                color = MiuixTheme.colorScheme.primary.copy(alpha = 0.55f),
                height = 34,
            )
            FrostPreviewBlock(
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                height = 40,
            )
        }
        if (spec.enabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.58f)
                    .hazeEffect(state = state, style = style),
            )
        }
    }
}

@Composable
private fun RowScope.FrostPreviewBlock(
    color: Color,
    height: Int,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(height.dp)
            .background(color, RoundedCornerShape(8.dp)),
    )
}

private fun frostHazeStyle(
    surface: Color,
    spec: BottomBarFrostSpec,
): HazeStyle = HazeStyle(
    backgroundColor = surface,
    tint = HazeTint(surface.copy(alpha = spec.tintAlpha)),
    blurRadius = spec.blurRadiusDp.dp,
    noiseFactor = spec.noiseFactor,
    fallbackTint = HazeTint(surface.copy(alpha = spec.fallbackAlpha)),
)
