package me.devoxin.rule34.ui

import android.content.Context
import android.util.AttributeSet
import com.jsibbold.zoomage.ZoomageView

class ZoomageViewEx : ZoomageView {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int): super(context, attrs, defStyle)

    val isZoomed: Boolean
        get() = currentScaleFactor > 1.0

    override fun setScaleType(scaleType: ScaleType?) {
        if (!isZoomed) {
            super.setScaleType(scaleType)
        }
    }
}
