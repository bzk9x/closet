package com.bzk9x.closet.utils

import android.view.View

fun View.ensureMinTouchTarget(minTouchDp: Int = 48) {
    TouchTargetExpander.expand(this, minTouchDp)
}

