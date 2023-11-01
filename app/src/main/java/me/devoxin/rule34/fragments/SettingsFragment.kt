package me.devoxin.rule34.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import me.devoxin.rule34.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
