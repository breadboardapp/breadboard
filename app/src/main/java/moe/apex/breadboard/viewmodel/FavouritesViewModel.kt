package moe.apex.breadboard.viewmodel

import androidx.lifecycle.ViewModel


class FavouritesViewModel : GridStateHolder by GridStateHolderDelegate(), ViewModel()
