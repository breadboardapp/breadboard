package moe.apex.rule34.viewmodel

import androidx.lifecycle.ViewModel


class FavouritesViewModel : GridStateHolder by GridStateHolderDelegate(), ViewModel()
