package me.devoxin.rule34.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.devoxin.rule34.ItemDecorator
import me.devoxin.rule34.R
import me.devoxin.rule34.adapters.RecyclerAdapter
import me.devoxin.rule34.ScrollListener

class ResultsActivity : AppCompatActivity() {
    private lateinit var recyclerAdapter: RecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tagList = intent.getStringArrayExtra("tagList")
                ?: throw IllegalStateException("Missing tagList!")

        recyclerAdapter = RecyclerAdapter(this, *tagList) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            finish()
        }
        initAdapter()
    }

    private fun initAdapter() {
        val recyclerView = findViewById<RecyclerView>(R.id.list)
        val viewManager = GridLayoutManager(this, 3)

        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = recyclerAdapter
            addItemDecoration(ItemDecorator())
            addOnScrollListener(ScrollListener(recyclerAdapter))
        }
    }
}
