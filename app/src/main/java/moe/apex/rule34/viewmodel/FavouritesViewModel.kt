package moe.apex.rule34.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel


@SuppressLint("MutableCollectionMutableState")
class FavouritesViewModel : GridStateHolder by GridStateHolderDelegate(), ViewModel()
