package io.qrx.scan.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

class RubberBandOverscrollEffect(
    private val scope: CoroutineScope
) : OverscrollEffect {

    private val overscrollOffset = Animatable(0f)

    @Volatile
    private var containerHeight = 800f

    val snappedOffset: Float
        get() = if (abs(overscrollOffset.value) > 0.5f)
            overscrollOffset.value.roundToInt().toFloat()
        else 0f

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
            overscrollOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                initialVelocity = remainingVelocity.y * 0.3f
            )
        }
    }

    override val isInProgress: Boolean
        get() = abs(overscrollOffset.value) > 0.5f

    override val node: DelegatableNode = object : Modifier.Node(), LayoutModifierNode {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            containerHeight = placeable.height.toFloat()
            return layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }
}

fun Modifier.rubberBandOffset(effect: RubberBandOverscrollEffect): Modifier =
    graphicsLayer { translationY = effect.snappedOffset }

@Composable
fun rememberRubberBandOverscrollEffect(): RubberBandOverscrollEffect {
    val scope = rememberCoroutineScope()
    return remember { RubberBandOverscrollEffect(scope) }
}
