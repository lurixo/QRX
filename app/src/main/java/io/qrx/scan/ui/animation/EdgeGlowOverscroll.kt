package io.qrx.scan.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

class EdgeGlowOverscrollEffect(
    private val scope: CoroutineScope,
    private val glowColor: Color
) : OverscrollEffect {

    private val overscrollOffset = Animatable(0f)

    @Volatile
    private var containerHeight = 800f

    private fun dampingFactor(currentOffset: Float): Float {
        val maxOffset = containerHeight * 0.3f
        val progress = (abs(currentOffset) / maxOffset).coerceIn(0f, 1f)
        return (1f - MD3Motion.EmphasizedDecelerate.transform(progress))
            .coerceIn(0.05f, 0.5f)
    }

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        val sameDirection = sign(delta.y) == sign(overscrollOffset.value)

        if (abs(overscrollOffset.value) > 0.5f && !sameDirection) {
            val prevValue = overscrollOffset.value
            val newValue = prevValue + delta.y
            if (sign(prevValue) != sign(newValue)) {
                scope.launch { overscrollOffset.snapTo(0f) }
                val remaining = Offset(delta.x, delta.y + prevValue)
                val scrollConsumed = performScroll(remaining)
                val unconsumedY = remaining.y - scrollConsumed.y
                if (abs(unconsumedY) > 0.5f) {
                    val damped = unconsumedY * dampingFactor(0f)
                    scope.launch { overscrollOffset.snapTo(damped) }
                }
                return delta
            } else {
                scope.launch { overscrollOffset.snapTo(newValue) }
                return delta
            }
        }

        val scrollConsumed = performScroll(delta)
        val unconsumedY = delta.y - scrollConsumed.y

        if (abs(unconsumedY) > 0.5f) {
            val damped = unconsumedY * dampingFactor(overscrollOffset.value)
            scope.launch {
                overscrollOffset.snapTo(overscrollOffset.value + damped)
            }
        }

        return delta
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        val remainingVelocity = performFling(velocity)

        if (abs(overscrollOffset.value) > 0.5f) {
            scope.launch {
                try {
                    overscrollOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                } finally {
                    overscrollOffset.snapTo(0f)
                }
            }
        }
    }

    override val isInProgress: Boolean
        get() = false

    override val node: DelegatableNode = object : Modifier.Node(), DrawModifierNode {
        override fun ContentDrawScope.draw() {
            containerHeight = size.height
            drawContent()

            val offset = overscrollOffset.value
            if (abs(offset) > 0.5f) {
                val intensity = (abs(offset) / (size.height * 0.15f)).coerceIn(0f, 1f)
                val glowHeight = size.height * 0.12f * intensity
                if (offset > 0f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.3f * intensity),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = glowHeight
                        ),
                        size = Size(size.width, glowHeight)
                    )
                } else {
                    val top = size.height - glowHeight
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                glowColor.copy(alpha = 0.3f * intensity)
                            ),
                            startY = top,
                            endY = size.height
                        ),
                        topLeft = Offset(0f, top),
                        size = Size(size.width, glowHeight)
                    )
                }
            }
        }
    }
}

@Composable
fun rememberEdgeGlowOverscrollEffect(): EdgeGlowOverscrollEffect {
    val scope = rememberCoroutineScope()
    val glowColor = MaterialTheme.colorScheme.primary
    return remember(glowColor) { EdgeGlowOverscrollEffect(scope, glowColor) }
}
