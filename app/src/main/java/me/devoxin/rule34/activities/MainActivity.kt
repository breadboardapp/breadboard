package me.devoxin.rule34.activities

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.bugsnag.android.Bugsnag
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.google.android.material.chip.ChipGroup
import me.devoxin.rule34.R
import me.devoxin.rule34.adapters.AutoCompleteAdapter

class MainActivity : AppCompatActivity() {
    private val tagList = mutableListOf<AutoCompleteAdapter.TagSuggestion>()
    var lastFocusTime = 0L

    private fun addTag(tagSuggestion: AutoCompleteAdapter.TagSuggestion): Boolean {
        return if (tagList.contains(tagSuggestion)) {
            val chips = findViewById<ChipGroup>(R.id.tagList)
            val index = tagList.indexOf(tagSuggestion)
            val chip = chips.getChildAt(index) as Chip
            val chipStyle = if (tagSuggestion.exclude) R.style.Widget_R34_ChipExcluded else R.style.Widget_R34_ChipIncluded
            val drawable = ChipDrawable.createFromAttributes(this@MainActivity, null, 0, chipStyle)

            tagList[index] = tagSuggestion
            chip.text = tagSuggestion.value
            chip.tag = tagSuggestion
            chip.setChipDrawable(drawable)
            false
        } else {
            tagList.add(tagSuggestion)
            true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bugsnag.start(this)
        setContentView(R.layout.activity_search)

        val chips = findViewById<ChipGroup>(R.id.tagList)
        val tagSearch = findViewById<AutoCompleteTextView>(R.id.tagSearch)
        tagSearch.setAdapter(AutoCompleteAdapter(this))

        tagSearch.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
                val tagSuggestion = adapterView.getItemAtPosition(position) as AutoCompleteAdapter.TagSuggestion
                if (!addTag(tagSuggestion)) return tagSearch.text.clear()
                //if (!tagList.add(tagSuggestion)) return tagSearch.text.clear()

                val chipStyle = if (tagSuggestion.exclude) R.style.Widget_R34_ChipExcluded else R.style.Widget_R34_ChipIncluded
                val drawable = ChipDrawable.createFromAttributes(this@MainActivity, null, 0, chipStyle)
                val chip = Chip(this@MainActivity).apply {
                    tag = tagSuggestion
                    text = tagSuggestion.value
                    setChipDrawable(drawable)

                    setOnClickListener {
                        val tag = it.tag as AutoCompleteAdapter.TagSuggestion
                        tagList.remove(tag)
                        chips.removeView(it)
                        tagSearch.setText(tag.formattedTag)
                        tagSearch.setSelection(tag.formattedTag.length)
                    }
                    setOnCloseIconClickListener {
                        tagList.remove(it.tag)
                        chips.removeView(it)
                    }
                }

                chips.addView(chip)
                tagSearch.text.clear()
            }
        }

        tagSearch.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            // This Mega Jank™ exists because upon transition, focus will be lost, so we combat this by implementing a small buffer.
            if (hasFocus) lastFocusTime = System.currentTimeMillis()
            if (!hasFocus && System.currentTimeMillis() - lastFocusTime < 100) return@OnFocusChangeListener v.requestFocus().let {}
            val visibility = if (hasFocus) View.GONE else View.VISIBLE
            setTitleVisibility(visibility)
        }

        tagSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable) = Unit

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty() || start >= s.length || start < 0) return

                val content = s.trim().takeIf { it.isNotEmpty() }?.toString()

                if (content == null) {
                    if (s[start] == '\n') {
                        findViewById<Button>(R.id.search).performClick()
                    }

                    return tagSearch.text.clear()
                }
            }
        })
    }

    private fun setTitleVisibility(visibility: Int) {
        findViewById<Space>(R.id.titleSpace).visibility = visibility
        findViewById<LinearLayout>(R.id.titleLayout).visibility = visibility
    }

    fun onSearchClick(v: View) {
        val intent = Intent(this, ResultsActivity::class.java)
        intent.putExtra("tagList", tagList.map { it.formattedTag }.toTypedArray())
        this.startActivity(intent)
    }

    fun onApplicationClick(v: View) {
        val textView = findViewById<AutoCompleteTextView>(R.id.tagSearch).also { it.clearFocus() }
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(textView.windowToken, 0)
    }
}
