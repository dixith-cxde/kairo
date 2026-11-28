package app.kairo.ui.common

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Drag a row by its handle to reorder a LazyColumn (canvas "drag_indicator" handles). The lifted
 * row follows the finger; when its middle crosses a neighbour with a key [canSwap] accepts, the two
 * swap through [onMove] (keys of the dragged row and the row it moves onto). [onDone] runs on lift,
 * to save the order once.
 */
@Stable
class ReorderState(internal val list: LazyListState) {
    var dragging by mutableStateOf<Any?>(null); internal set
    var offset by mutableFloatStateOf(0f); internal set
}

@Composable
fun rememberReorder(list: LazyListState) = remember(list) { ReorderState(list) }

/** On the handle: starts a drag of the row with [key]. */
@Composable
fun Modifier.reorderHandle(state: ReorderState, key: Any, canSwap: (Any) -> Boolean, onMove: (Any, Any) -> Unit, onDone: () -> Unit): Modifier {
    val move by rememberUpdatedState(onMove)
    val done by rememberUpdatedState(onDone)
    val swap by rememberUpdatedState(canSwap)
    return pointerInput(key) {
        detectDragGestures(
            onDragStart = { state.dragging = key; state.offset = 0f },
            onDragEnd = { state.dragging = null; state.offset = 0f; done() },
            onDragCancel = { state.dragging = null; state.offset = 0f; done() },
        ) { change, amount ->
            change.consume()
            state.offset += amount.y
            val items = state.list.layoutInfo.visibleItemsInfo
            val me = items.firstOrNull { it.key == key } ?: return@detectDragGestures
            val mid = me.offset + state.offset + me.size / 2f
            val target = items.firstOrNull { it.key != key && swap(it.key) && mid > it.offset && mid < it.offset + it.size }
            if (target != null) {
                // The row's slot moves to the target's place; keep it under the finger.
                state.offset += me.offset - target.offset
                move(key, target.key)
            }
        }
    }
}

/** On the row: lifts the dragged one above the others. */
fun Modifier.reorderItem(state: ReorderState, key: Any): Modifier =
    if (state.dragging == key) zIndex(1f).graphicsLayer { translationY = state.offset }.shadow(8.dp, androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
    else this
