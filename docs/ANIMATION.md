# QRX Animation System

This document describes the Material Design 3 animation system implemented in QRX.

## Architecture

```
ui/
├── animation/
│   └── MD3Motion.kt          # Animation specifications
└── components/
    └── MD3Components.kt      # Reusable animated components
```

## MD3Motion.kt

Centralized animation specifications following MD3 motion guidelines.

### Duration Constants

```kotlin
object Duration {
    const val SHORT1 = 50      // Micro-interactions
    const val SHORT2 = 100     // Quick feedback
    const val SHORT3 = 150     // Button press
    const val SHORT4 = 200     // Icon transitions
    
    const val MEDIUM1 = 250    // FAB animations
    const val MEDIUM2 = 300    // State changes
    const val MEDIUM3 = 350    // Dialog animations
    const val MEDIUM4 = 400    // Complex transitions
    
    const val LONG1 = 450      // Page transitions
    const val LONG2 = 500      // Container transforms
    const val LONG3 = 550      // Extended animations
    const val LONG4 = 600      // Full-screen transitions
}
```

### Easing Curves

| Curve | Usage | Value |
|-------|-------|-------|
| `EmphasizedDecelerate` | Entry animations | cubic-bezier(0.05, 0.7, 0.1, 1.0) |
| `EmphasizedAccelerate` | Exit animations | cubic-bezier(0.3, 0.0, 0.8, 0.15) |
| `Emphasized` | State changes | path: M 0,0 C 0.05,0,0.133333,0.06,0.166666,0.4 C 0.208333,0.82,0.25,1,1,1 |
| `Standard` | Property transitions | cubic-bezier(0.2, 0.0, 0.0, 1.0) |
| `StandardDecelerate` | Fade in | cubic-bezier(0.0, 0.0, 0.0, 1.0) |
| `StandardAccelerate` | Fade out | cubic-bezier(0.3, 0.0, 1.0, 1.0) |

### Transition Presets

```kotlin
// Fade through transition (content state changes)
MD3Transitions.fadeThrough()

// Shared axis X (horizontal navigation)
MD3Transitions.sharedAxisX(forward = true)

// Shared axis Y (vertical navigation)
MD3Transitions.sharedAxisY(forward = true)
```

### Animation Presets

```kotlin
// List item animations
MD3ListAnimations.fadeInSpec(index)   // Staggered entry with index
MD3ListAnimations.fadeOutSpec()       // Exit animation
MD3ListAnimations.placementSpec()     // Reorder spring

// FAB animations
MD3FabAnimations.enter()  // Scale + fade in
MD3FabAnimations.exit()   // Scale + fade out

// State animations
MD3StateAnimations.contentEnter()     // Content appears
MD3StateAnimations.contentExit()      // Content disappears
MD3StateAnimations.emptyStateEnter()  // Empty state appears

// Dialog animations
MD3DialogAnimations.enter()  // Scale + fade in
MD3DialogAnimations.exit()   // Scale + fade out
```

### Helper Functions

Simplified animation spec creation:

```kotlin
// Standard property transitions (color, alpha)
MD3Motion.standardSpec()                    // Default: SHORT4 (200ms)
MD3Motion.standardSpec(Duration.MEDIUM2)    // Custom duration

// State/size change animations
MD3Motion.emphasizedSpec()                  // Default: MEDIUM2 (300ms)

// Entry animations (elements appearing)
MD3Motion.emphasizedDecelerateSpec()        // Default: MEDIUM2 (300ms)

// Exit animations (elements disappearing)
MD3Motion.emphasizedAccelerateSpec()        // Default: MEDIUM2 (300ms)

// Press feedback animations
MD3Motion.pressSpec(isPressed)              // 100ms press, 150ms release
MD3Motion.pressSpecFast(isPressed)          // 50ms press, 100ms release (icons)
```

Usage example:
```kotlin
// Before (verbose)
val scale by animateFloatAsState(
    targetValue = if (selected) 1f else 0.9f,
    animationSpec = tween(
        durationMillis = MD3Motion.Duration.SHORT3,
        easing = MD3Motion.EmphasizedDecelerate
    )
)

// After (concise)
val scale by animateFloatAsState(
    targetValue = if (selected) 1f else 0.9f,
    animationSpec = MD3Motion.emphasizedDecelerateSpec(MD3Motion.Duration.SHORT3)
)
```

## MD3Components.kt

Reusable animated UI components.

### MD3PressableSurface

Generic pressable surface with scale animation.

```kotlin
MD3PressableSurface(
    onClick = { /* action */ },
    pressScale = 0.96f,  // Scale when pressed
    color = Color.Transparent
) {
    // Content
}
```

### MD3PressableIconButton

Icon button with press feedback.

```kotlin
MD3PressableIconButton(
    onClick = { /* action */ },
    icon = Icons.Default.Delete,
    contentDescription = "Delete",
    tint = MaterialTheme.colorScheme.primary,
    pressScale = 0.85f
)
```

### MD3ActionButton

Action button with icon and text.

```kotlin
MD3ActionButton(
    icon = Icons.Default.QrCodeScanner,
    text = "Scan",
    onClick = { /* action */ }
)
```

### MD3AnimatedDialog

Dialog with enter/exit animations.

```kotlin
MD3AnimatedDialog(
    visible = showDialog,
    onDismissRequest = { showDialog = false },
    title = "Confirm Delete",
    text = "Are you sure you want to delete?",
    confirmText = "Delete",
    dismissText = "Cancel",
    onConfirm = { /* delete */ }
)
```

### MD3SelectionIcon

Animated selection checkbox icon.

```kotlin
MD3SelectionIcon(
    selected = isSelected,
    selectedTint = MaterialTheme.colorScheme.primary,
    unselectedTint = MaterialTheme.colorScheme.outline
)
```

### MD3ToggleIcon

Animated toggle icon (e.g., flash on/off).

```kotlin
MD3ToggleIcon(
    isOn = isFlashOn,
    onIcon = Icons.Default.FlashOn,
    offIcon = Icons.Default.FlashOff,
    contentDescription = "Flash",
    tint = Color.White
)
```

### MD3TextField

Text field with error shake animation.

```kotlin
MD3TextField(
    value = text,
    onValueChange = { text = it },
    label = "Enter content",
    placeholder = "Type here...",
    isError = hasError,
    errorMessage = "Invalid input"
)
```

## Usage Examples

### List with Staggered Animation

```kotlin
LazyColumn {
    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
        ItemCard(
            modifier = Modifier.animateItem(
                fadeInSpec = MD3ListAnimations.fadeInSpec(index),
                fadeOutSpec = MD3ListAnimations.fadeOutSpec(),
                placementSpec = MD3ListAnimations.placementSpec()
            )
        )
    }
}
```

### State Transition

```kotlin
AnimatedContent(
    targetState = isEmpty,
    transitionSpec = { MD3Transitions.fadeThrough() }
) { empty ->
    if (empty) {
        EmptyState()
    } else {
        ContentList()
    }
}
```

### Navigation Transition

```kotlin
AnimatedContent(
    targetState = currentScreen,
    transitionSpec = { MD3Transitions.sharedAxisX(forward = true) }
) { screen ->
    when (screen) {
        Screen.List -> ListScreen()
        Screen.Detail -> DetailScreen()
    }
}
```

## Best Practices

1. **Use appropriate durations**: SHORT for micro-interactions, MEDIUM for state changes, LONG for page transitions.

2. **Match easing to direction**: Use Decelerate for entries, Accelerate for exits.

3. **Stagger list items**: Use 30ms delay per item, capped at 150ms total.

4. **Spring for repositioning**: Use spring animations for list reordering.

5. **Consistent component usage**: Prefer MD3Components over custom implementations.

6. **Avoid hardcoded values**: Always use MD3Motion constants instead of literal numbers.

7. **Use shape parameter**: For pressable surfaces with rounded corners, use the `shape` parameter instead of `.clip()` to avoid corner clipping issues.

## Refactoring Summary

### v4.1 Optimization

| Change | Description |
|--------|-------------|
| Duration standardization | ~33% faster, aligned with MD3 spec |
| Emphasized curve | Path-based two-segment cubic bezier interpolation |
| Helper functions | `standardSpec()`, `emphasizedSpec()`, `pressSpec()` etc. |
| Manual tween replacement | 24 instances replaced with helper functions |
| Import cleanup | Removed unused `tween`, `Spring`, `spring` imports |

### Removed Patterns

| Pattern | Count | Replacement |
|---------|-------|-------------|
| `tween(300)`, `tween(200)` etc. | 9 | `MD3Motion.emphasizedDecelerateSpec()` |
| `tween(..., easing = Standard)` | 8 | `MD3Motion.standardSpec()` |
| `tween(..., easing = Emphasized)` | 4 | `MD3Motion.emphasizedSpec()` |
| Manual press animation | 6 | `MD3Motion.pressSpec(isPressed)` |
| `fadeIn()` (no params) | 20 | `fadeIn(MD3Motion.emphasizedDecelerateSpec())` |
| `scaleIn()` (no params) | 18 | `MD3FabAnimations.enter()` |
| Private easing constants | 3 | `MD3Motion.EmphasizedDecelerate` |
| **Total** | **68** | - |

### Refactored Files

| File | Changes |
|------|---------|
| `MD3Motion.kt` | +68 lines: path interpolation, helper functions |
| `MainActivity.kt` | Navigation animations → `MD3Transitions.containerTransformIn/Out()` |
| `ScanScreen.kt` | List animations → `MD3ListAnimations.fadeInSpec/fadeOutSpec/placementSpec` |
| `HistoryScreen.kt` | Tab switch, list animations, `standardSpec()`, `emphasizedSpec()` |
| `CameraScanScreen.kt` | Result card, success icon → `MD3StateAnimations.contentEnter/Exit()` |
| `QRCodeGenerateScreen.kt` | FAB animations, list animations |
| `BarcodeGenerateScreen.kt` | FAB animations, list animations |
| `ScanResultCard.kt` | `standardSpec()`, `emphasizedSpec()` |
| `MD3Components.kt` | `standardSpec()`, `emphasizedDecelerateSpec()`, `pressSpec()` |
| `QRXSnackbar.kt` | `emphasizedDecelerateSpec()`, `emphasizedAccelerateSpec()` |

### MD3 System Usage

| Component | Usage Count |
|-----------|-------------|
| `MD3Motion.Duration` | 42 |
| `MD3Motion.Easing` | 56 |
| `MD3Motion helper functions` | 24 |
| `MD3ListAnimations` | 15 |
| `MD3Transitions` | 12 |
| `MD3FabAnimations` | 16 |
| `MD3StateAnimations` | 6 |
| `MD3DialogAnimations` | 2 |
| **Total** | **173** |

## Performance Notes

- All animations use hardware-accelerated properties (scale, alpha, translation)
- Spring animations are more performant than keyframe animations
- Crossfade is more efficient than full AnimatedContent for simple icon swaps
