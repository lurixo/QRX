package io.qrx.scan.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import kotlin.math.abs

private class EmphasizedEasing : Easing {
    private val curve1 = CubicBezierSegment(
        p0x = 0f, p0y = 0f,
        p1x = 0.05f, p1y = 0f,
        p2x = 0.133333f, p2y = 0.06f,
        p3x = 0.166666f, p3y = 0.4f
    )
    private val curve2 = CubicBezierSegment(
        p0x = 0.166666f, p0y = 0.4f,
        p1x = 0.208333f, p1y = 0.82f,
        p2x = 0.25f, p2y = 1f,
        p3x = 1f, p3y = 1f
    )

    override fun transform(fraction: Float): Float {
        return when {
            fraction <= 0f -> 0f
            fraction >= 1f -> 1f
            fraction < 0.166666f -> curve1.solve(fraction)
            else -> curve2.solve(fraction)
        }
    }

    private class CubicBezierSegment(
        private val p0x: Float, private val p0y: Float,
        private val p1x: Float, private val p1y: Float,
        private val p2x: Float, private val p2y: Float,
        private val p3x: Float, private val p3y: Float
    ) {
        fun solve(x: Float): Float {
            val t = findT(x)
            return evalY(t)
        }

        private fun findT(x: Float): Float {
            var t = x
            for (i in 0 until 8) {
                val currentX = evalX(t)
                val diff = currentX - x
                if (abs(diff) < 0.0001f) break
                val derivative = evalDX(t)
                if (abs(derivative) < 0.0001f) break
                t -= diff / derivative
                t = t.coerceIn(0f, 1f)
            }
            return t
        }

        private fun evalX(t: Float): Float {
            val mt = 1f - t
            return mt * mt * mt * p0x + 3f * mt * mt * t * p1x + 3f * mt * t * t * p2x + t * t * t * p3x
        }

        private fun evalY(t: Float): Float {
            val mt = 1f - t
            return mt * mt * mt * p0y + 3f * mt * mt * t * p1y + 3f * mt * t * t * p2y + t * t * t * p3y
        }

        private fun evalDX(t: Float): Float {
            val mt = 1f - t
            return 3f * mt * mt * (p1x - p0x) + 6f * mt * t * (p2x - p1x) + 3f * t * t * (p3x - p2x)
        }
    }
}

object MD3Motion {
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val Emphasized: Easing = EmphasizedEasing()
    val Standard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val StandardDecelerate = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val StandardAccelerate = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)

    object Duration {
        const val SHORT1 = 50
        const val SHORT2 = 100
        const val SHORT3 = 150
        const val SHORT4 = 200
        const val MEDIUM1 = 250
        const val MEDIUM2 = 300
        const val MEDIUM3 = 350
        const val MEDIUM4 = 400
        const val LONG1 = 450
        const val LONG2 = 500
        const val LONG3 = 550
        const val LONG4 = 600
    }

    fun <T> standardSpec(durationMillis: Int = Duration.SHORT4): FiniteAnimationSpec<T> =
        tween(durationMillis, easing = Standard)

    fun <T> emphasizedSpec(durationMillis: Int = Duration.MEDIUM2): FiniteAnimationSpec<T> =
        tween(durationMillis, easing = Emphasized)

    fun <T> emphasizedDecelerateSpec(durationMillis: Int = Duration.MEDIUM2): FiniteAnimationSpec<T> =
        tween(durationMillis, easing = EmphasizedDecelerate)

    fun <T> emphasizedAccelerateSpec(durationMillis: Int = Duration.MEDIUM2): FiniteAnimationSpec<T> =
        tween(durationMillis, easing = EmphasizedAccelerate)

    /**
     * MD3 标准按压动画规格
     * 按下时快速响应，松开时平滑恢复
     * @param isPressed 当前是否按下状态
     */
    fun <T> pressSpec(isPressed: Boolean): FiniteAnimationSpec<T> =
        tween(
            durationMillis = if (isPressed) Duration.SHORT2 else Duration.SHORT3,
            easing = EmphasizedDecelerate
        )

    /**
     * MD3 快速按压动画规格（用于小型交互元素如 IconButton）
     * @param isPressed 当前是否按下状态
     */
    fun <T> pressSpecFast(isPressed: Boolean): FiniteAnimationSpec<T> =
        tween(
            durationMillis = if (isPressed) Duration.SHORT1 else Duration.SHORT2,
            easing = EmphasizedDecelerate
        )

    fun <T> springSpec(
        dampingRatio: Float = Spring.DampingRatioMediumBouncy,
        stiffness: Float = Spring.StiffnessMedium
    ): FiniteAnimationSpec<T> = spring(dampingRatio, stiffness)
}

object MD3Transitions {
    fun fadeThrough(durationMillis: Int = MD3Motion.Duration.MEDIUM2): ContentTransform =
        fadeIn(tween(durationMillis, easing = MD3Motion.StandardDecelerate)) togetherWith
        fadeOut(tween(durationMillis / 3, easing = MD3Motion.StandardAccelerate))

    fun sharedAxisX(forward: Boolean, durationMillis: Int = MD3Motion.Duration.MEDIUM2): ContentTransform {
        val offsetFraction = if (forward) 1 else -1
        return (slideInHorizontally(
            tween(durationMillis, easing = MD3Motion.EmphasizedDecelerate)
        ) { it * offsetFraction / 4 } + fadeIn(
            tween(durationMillis, easing = MD3Motion.StandardDecelerate)
        )) togetherWith (slideOutHorizontally(
            tween(durationMillis, easing = MD3Motion.EmphasizedAccelerate)
        ) { -it * offsetFraction / 4 } + fadeOut(
            tween(durationMillis / 3, easing = MD3Motion.StandardAccelerate)
        ))
    }

    fun sharedAxisY(forward: Boolean, durationMillis: Int = MD3Motion.Duration.MEDIUM2): ContentTransform {
        val offsetFraction = if (forward) 1 else -1
        return (slideInVertically(
            tween(durationMillis, easing = MD3Motion.EmphasizedDecelerate)
        ) { it * offsetFraction / 4 } + fadeIn(
            tween(durationMillis, easing = MD3Motion.StandardDecelerate)
        )) togetherWith (slideOutVertically(
            tween(durationMillis, easing = MD3Motion.EmphasizedAccelerate)
        ) { -it * offsetFraction / 4 } + fadeOut(
            tween(durationMillis / 3, easing = MD3Motion.StandardAccelerate)
        ))
    }

    fun containerTransformIn(durationMillis: Int = MD3Motion.Duration.MEDIUM4): EnterTransition =
        fadeIn(tween(durationMillis, easing = MD3Motion.EmphasizedDecelerate)) +
        scaleIn(
            initialScale = 0.85f,
            animationSpec = tween(durationMillis, easing = MD3Motion.EmphasizedDecelerate)
        )

    fun containerTransformOut(durationMillis: Int = MD3Motion.Duration.MEDIUM4): ExitTransition =
        fadeOut(tween(durationMillis / 2, easing = MD3Motion.EmphasizedAccelerate)) +
        scaleOut(
            targetScale = 0.85f,
            animationSpec = tween(durationMillis, easing = MD3Motion.EmphasizedAccelerate)
        )
}

object MD3ListAnimations {
    fun enterTransition(index: Int, baseDelay: Int = 30): EnterTransition {
        val delay = (index * baseDelay).coerceAtMost(150)
        return fadeIn(
            tween(
                durationMillis = MD3Motion.Duration.MEDIUM2,
                delayMillis = delay,
                easing = MD3Motion.EmphasizedDecelerate
            )
        ) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(
                durationMillis = MD3Motion.Duration.MEDIUM2,
                delayMillis = delay,
                easing = MD3Motion.EmphasizedDecelerate
            )
        )
    }

    fun exitTransition(): ExitTransition =
        fadeOut(
            tween(
                durationMillis = MD3Motion.Duration.SHORT4,
                easing = MD3Motion.EmphasizedAccelerate
            )
        ) + slideOutVertically(
            targetOffsetY = { -it / 4 },
            animationSpec = tween(
                durationMillis = MD3Motion.Duration.SHORT4,
                easing = MD3Motion.EmphasizedAccelerate
            )
        )

    fun <T> fadeInSpec(index: Int, baseDelay: Int = 30): FiniteAnimationSpec<T> =
        tween(
            durationMillis = MD3Motion.Duration.MEDIUM2,
            delayMillis = (index * baseDelay).coerceAtMost(150),
            easing = MD3Motion.EmphasizedDecelerate
        )

    fun <T> fadeOutSpec(): FiniteAnimationSpec<T> =
        tween(
            durationMillis = MD3Motion.Duration.SHORT4,
            easing = MD3Motion.EmphasizedAccelerate
        )

    fun <T> placementSpec(): FiniteAnimationSpec<T> =
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
}

object MD3StateAnimations {
    fun contentEnter(): EnterTransition =
        fadeIn(
            tween(MD3Motion.Duration.MEDIUM2, easing = MD3Motion.EmphasizedDecelerate)
        ) + scaleIn(
            initialScale = 0.92f,
            animationSpec = tween(MD3Motion.Duration.MEDIUM2, easing = MD3Motion.EmphasizedDecelerate)
        )

    fun contentExit(): ExitTransition =
        fadeOut(
            tween(MD3Motion.Duration.SHORT4, easing = MD3Motion.EmphasizedAccelerate)
        ) + scaleOut(
            targetScale = 0.92f,
            animationSpec = tween(MD3Motion.Duration.SHORT4, easing = MD3Motion.EmphasizedAccelerate)
        )

    fun emptyStateEnter(): EnterTransition =
        fadeIn(
            tween(MD3Motion.Duration.MEDIUM4, easing = MD3Motion.StandardDecelerate)
        ) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(MD3Motion.Duration.MEDIUM4, easing = MD3Motion.EmphasizedDecelerate)
        )

    fun loadingEnter(): EnterTransition =
        fadeIn(tween(MD3Motion.Duration.SHORT4, easing = MD3Motion.StandardDecelerate))

    fun loadingExit(): ExitTransition =
        fadeOut(tween(MD3Motion.Duration.SHORT4, easing = MD3Motion.StandardAccelerate))
}

object MD3FabAnimations {
    fun enter(): EnterTransition =
        fadeIn(
            tween(MD3Motion.Duration.MEDIUM1, easing = MD3Motion.EmphasizedDecelerate)
        ) + scaleIn(
            initialScale = 0.6f,
            animationSpec = tween(MD3Motion.Duration.MEDIUM2, easing = MD3Motion.EmphasizedDecelerate)
        )

    fun exit(): ExitTransition =
        fadeOut(
            tween(MD3Motion.Duration.SHORT3, easing = MD3Motion.EmphasizedAccelerate)
        ) + scaleOut(
            targetScale = 0.6f,
            animationSpec = tween(MD3Motion.Duration.SHORT4, easing = MD3Motion.EmphasizedAccelerate)
        )
}

object MD3DialogAnimations {
    fun enter(): EnterTransition =
        fadeIn(
            tween(MD3Motion.Duration.MEDIUM2, easing = MD3Motion.EmphasizedDecelerate)
        ) + scaleIn(
            initialScale = 0.9f,
            animationSpec = tween(MD3Motion.Duration.MEDIUM2, easing = MD3Motion.EmphasizedDecelerate)
        )

    fun exit(): ExitTransition =
        fadeOut(
            tween(MD3Motion.Duration.SHORT4, easing = MD3Motion.EmphasizedAccelerate)
        ) + scaleOut(
            targetScale = 0.9f,
            animationSpec = tween(MD3Motion.Duration.SHORT4, easing = MD3Motion.EmphasizedAccelerate)
        )
}
