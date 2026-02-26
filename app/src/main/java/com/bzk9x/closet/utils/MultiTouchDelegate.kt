package com.bzk9x.closet.utils

import android.graphics.Rect
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View

/**
 * MultiTouchDelegate forwards touch events to multiple Touch delegate instances.
 * We keep a mutable list of delegates (replaced when their bounds change).
 *
 * NOTE: This object itself is set as parent.touchDelegate and should remain stable
 */

class MultiTouchDelegate (
    parent: View
) : TouchDelegate(Rect(), parent){

    private data class Entry(
        val child: View,
        val delegate: TouchDelegate
    )

    private val entries = mutableListOf<Entry>()


    /**Adds or replaces the delegate for [child].*/

    fun addOrReplace(child: View, delegate: TouchDelegate) {
        synchronized(entries) {
            val idx = entries.indexOfFirst { it.child === child }
            if (idx >= 0) entries[idx] = Entry(child, delegate) else entries.add(Entry(child, delegate))
        }
    }


    /** Removes delegate for [child]. Returns true if removed. */

    fun remove(child: View): Boolean {
        synchronized(entries) {
            return entries.removeIf { it.child === child}
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Iterate in reverse order (last added first - usually topmost)

        val snapshot = synchronized(entries) { entries.toList() }
        for (i in snapshot.indices.reversed()) {
            val d = snapshot[i].delegate
            if (d.onTouchEvent(event)) return true
        }
        return false
    }

    /** Helper: number of delegates currently managed. */
    fun size(): Int = synchronized(entries) { entries.size }
}