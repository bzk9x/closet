package com.bzk9x.closet.utils

import android.graphics.Rect
import android.view.View
import android.view.TouchDelegate
import java.util.WeakHashMap
import kotlin.math.max
import kotlin.math.roundToInt

object TouchTargetExpander {

    // parent -> MultiTouchDelegate (weak refs via WeakHashMap to avoid leaking parents)
    private val delegateMap = WeakHashMap<View, MultiTouchDelegate>()

    // child -> parent to help with removals
    private val childParentMap = WeakHashMap<View, View>()

    // parent -> OnLayoutChangeListener so we can detach it on cleanup
    private val parentLayoutListeners = WeakHashMap<View, View.OnLayoutChangeListener>()

    private fun dpToPx(view: View, dp: Int): Int {
        val density = view.resources.displayMetrics.density
        return (dp * density).roundToInt()
    }

    /**
     * Expand the touch area of [child] to at least [minTouchDp] (dp).
     * Safe to call multiple times; it will update the delegate.
     */
    fun expand(child: View, minTouchDp: Int = 48) {
        val parent = child.parent as? View ?: return

        parent.post {
            val multi = delegateMap.getOrPut(parent) {
                val m = MultiTouchDelegate(parent)
                parent.touchDelegate = m
                installLayoutListener(parent, m)
                m
            }

            val minPx = dpToPx(child, minTouchDp)
            val rect = computeExpandedRect(child, parent, minPx)
            val touchDelegate = TouchDelegate(rect, child)
            multi.addOrReplace(child, touchDelegate)

            childParentMap[child] = parent
        }
    }

    /**
     * Remove expanded touch area for [child]. If last child removed, clean up parent listener
     * and unset parent.touchDelegate.
     */
    fun remove(child: View) {
        val parent = childParentMap[child] ?: return
        val multi = delegateMap[parent] ?: return

        parent.post {
            val removed = multi.remove(child)
            childParentMap.remove(child)
            if (removed && multi.size() == 0) {
                // cleanup
                delegateMap.remove(parent)
                parent.touchDelegate = null
                val layoutListener = parentLayoutListeners.remove(parent)
                if (layoutListener != null) parent.removeOnLayoutChangeListener(layoutListener)
            }
        }
    }

    /**
     * Computes an expanded Rect for the child's hit area relative to the parent.
     * Ensures width & height are at least minPx, expanding evenly around the view bounds.
     */
    private fun computeExpandedRect(child: View, parent: View, minPx: Int): Rect {
        val rect = Rect()
        child.getHitRect(rect) // rect is in parent's coordinates

        val wInc = max(0, minPx - rect.width())
        val hInc = max(0, minPx - rect.height())

        rect.left -= wInc / 2
        rect.right += wInc / 2
        rect.top -= hInc / 2
        rect.bottom += hInc / 2

        // Keep inside parent bounds to avoid passing touches to outside the parent area
        rect.left = rect.left.coerceAtLeast(0)
        rect.top = rect.top.coerceAtLeast(0)
        rect.right = rect.right.coerceAtMost(parent.width)
        rect.bottom = rect.bottom.coerceAtMost(parent.height)

        return rect
    }

    /**
     * Install a layout-change listener on the parent that recomputes child rects when layout changes.
     */
    private fun installLayoutListener(parent: View, multi: MultiTouchDelegate) {
        if (parentLayoutListeners.containsKey(parent)) return

        val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            // Recompute rects for all children that reference this parent.
            // Walk childParentMap to find children belonging to this parent.
            val children = childParentMap.keys.filter { childParentMap[it] === parent }
            for (child in children) {
                // If child was removed or not attached, skip
                if (child.parent !== parent) {
                    remove(child)
                    continue
                }
                // For safety, use the same min (48dp) default; if you need per-child min
                // store and retrieve it in a map. For simplicity we use 48dp here.
                val minPx = dpToPx(child, 48)
                val rect = computeExpandedRect(child, parent, minPx)
                val delegate = TouchDelegate(rect, child)
                multi.addOrReplace(child, delegate)
            }
        }

        parent.addOnLayoutChangeListener(listener)
        parentLayoutListeners[parent] = listener
    }

}