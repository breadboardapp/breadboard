package me.devoxin.rule34

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.devoxin.rule34.adapters.RecyclerAdapter

class ScrollListener(private val adapter: RecyclerAdapter) : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        val layoutManager = recyclerView.layoutManager as? GridLayoutManager
            ?: return

        if (layoutManager.findLastCompletelyVisibleItemPosition() >= adapter.itemCount - 1) {
            recyclerView.post { adapter.loadMore() }
        }
    }
}
