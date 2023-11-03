package me.devoxin.rule34.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object Scopes {
    val MAIN = CoroutineScope(Dispatchers.Main)
}
