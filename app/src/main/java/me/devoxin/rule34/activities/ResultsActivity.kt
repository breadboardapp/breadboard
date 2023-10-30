package me.devoxin.rule34.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.devoxin.rule34.ItemDecorator
import me.devoxin.rule34.R
import me.devoxin.rule34.ScrollListener
import me.devoxin.rule34.adapters.RecyclerAdapter

class ResultsActivity : AppCompatActivity() {
    private lateinit var recyclerAdapter: RecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tagList = intent.getStringArrayExtra("tagList")
                ?: throw IllegalStateException("Missing tagList!")

        recyclerAdapter = RecyclerAdapter(this, *tagList, toastCallback = ::makeToast) {
            makeToast(it)
            finish()
        }

        initAdapter()
    }

    private fun makeToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
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
