package moe.apex.rule34.viewmodel

import android.annotation.SuppressLint
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.lifecycle.ViewModel


@SuppressLint("MutableCollectionMutableState")
class FavouritesViewModel : ViewModel() {
    val uniformGridState = LazyGridState()
    val staggeredGridState = LazyStaggeredGridState()
}
