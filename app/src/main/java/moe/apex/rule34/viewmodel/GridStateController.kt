package moe.apex.rule34.viewmodel

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState


interface GridStateHolder {
    var staggeredGridState: LazyStaggeredGridState
    var uniformGridState: LazyGridState

    fun resetGridStates() {
        staggeredGridState = LazyStaggeredGridState()
        uniformGridState = LazyGridState()
    }
}


class GridStateHolderDelegate : GridStateHolder {
    override var staggeredGridState = LazyStaggeredGridState()
    override var uniformGridState = LazyGridState()
}
