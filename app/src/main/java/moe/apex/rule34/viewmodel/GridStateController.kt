package moe.apex.rule34.viewmodel

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


interface GridStateHolder {
    var staggeredGridState: LazyStaggeredGridState
    var uniformGridState: LazyGridState

    fun resetGridStates() {
        staggeredGridState = LazyStaggeredGridState()
        uniformGridState = LazyGridState()
    }
}


class GridStateHolderDelegate : GridStateHolder {
    override var staggeredGridState by mutableStateOf(LazyStaggeredGridState())
    override var uniformGridState by mutableStateOf(LazyGridState())
}
