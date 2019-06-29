package com.lyc.everydownload.preference

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.view.inputmethod.EditorInfo
import androidx.core.net.toFile
import androidx.core.widget.doAfterTextChanged
import androidx.preference.*
import com.lyc.downloader.YCDownloader
import com.lyc.downloader.utils.DownloadStringUtil
import com.lyc.everydownload.R
import com.lyc.everydownload.file.FileExploreActivity
import com.lyc.everydownload.util.doWithRWPermission
import com.lyc.everydownload.util.requestForResult
import java.util.*
import kotlin.math.min


/**
 * @author liuyuchuan
 * @date 2019-06-27
 * @email kevinliu.sir@qq.com
 */
class PreferenceFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private val prefMap = WeakHashMap<String, Preference>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference)
        val maxRunningTaskPref = preferenceScreen.findPreference<ListPreference>(getString(R.string.key_max_running_tasks))!!
        val maxSupportRunningTask = YCDownloader.getMaxSupportRunningTask()
        val maxRunningTasksValues = Array(maxSupportRunningTask + 1) {
            it.toString()
        }
        maxRunningTaskPref.entryValues = maxRunningTasksValues
        maxRunningTaskPref.entries = maxRunningTasksValues.clone()
        maxRunningTaskPref.setDefaultValue(min(4, maxSupportRunningTask))
        val defaultDownloadDir = preferenceScreen.findPreference<SingleValuePreference>(getString(R.string.key_default_download_dir))!!
        defaultDownloadDir.onPreferenceClickListener = this
        defaultDownloadDir.defaultValue = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).absolutePath

        val speedLimitPref = preferenceManager.findPreference<EditTextPreference>(getString(R.string.key_speed_limit))!!
        setSpeedLimitSummary(speedLimitPref)
        speedLimitPref.setDialogMessage(R.string.hint_speed_limit)
        var enter = false
        speedLimitPref.setOnBindEditTextListener { editText ->
            editText.inputType = EditorInfo.TYPE_CLASS_NUMBER
            editText.doAfterTextChanged {
                it?.let { editable ->
                    if (enter) {
                        return@let
                    }
                    enter = true

                    try {
                        val text = editText.text.toString()
                        if (text.isBlank()) {
                            return@let
                        }
                        val bps = text.run {
                            toIntOrNull() ?: toLongOrNull() ?: toBigDecimalOrNull()
                        }

                        if (bps == null) {
                            editable.clear()
                            editable.append("0")
                        } else if (bps !is Int || bps > 102400) {
                            editable.clear()
                            editable.append("102400")
                        }
                    } finally {
                        enter = false
                    }
                }
            }
        }
        initPref(preferenceScreen)
    }

    override fun onStart() {
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        super.onStart()
    }

    override fun onPause() {
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    private fun initPref(preference: Preference?) {
        if (preference is PreferenceGroup) {
            for (i in 0 until preference.preferenceCount) {
                initPref(preference.getPreference(i))
            }
        } else {
            updatePrefSummary(preference)
        }
    }

    private fun updatePrefSummary(preference: Preference?) {
        when (preference) {
            is ListPreference -> {
                preference.summary = preference.entry
            }

            is SingleValuePreference -> {
                preference.summary = preference.value
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        getPreference<Preference>(key)?.let {
            when (key) {
                getString(R.string.key_speed_limit) -> {
                    (it as? EditTextPreference)?.let(this::setSpeedLimitSummary)
                }
                else -> {
                    updatePrefSummary(it)
                }
            }
        }
    }

    private fun setSpeedLimitSummary(preference: EditTextPreference) {
        val value = preference.text.toIntOrNull() ?: 0
        if (value == 0) {
            preference.summary = getString(R.string.no_speed_limit)
        } else {
            preference.summary = DownloadStringUtil.bpsToString(value * 1024.0)
        }
        if (value.toString() != preference.text) {
            preference.text = value.toString()
        }
    }

    private fun <T : Preference> getPreference(key: String): T? {
        var preference = prefMap[key]
        if (preference == null) {
            preference = findPreference(key)
            preference?.let { prefMap.put(key, preference) }
        }

        @Suppress("UNCHECKED_CAST")
        return preference as? T
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            getString(R.string.key_default_download_dir) -> {
                choosePath(preference)
                true
            }
            else -> {
                false
            }
        }
    }

    private fun choosePath(preference: Preference) {
        if (preference is SingleValuePreference) {
            context?.doWithRWPermission(
                    action = {
                        requestForResult(Intent(context, FileExploreActivity::class.java).apply {
                            putExtra(FileExploreActivity.KEY_DIR, true)
                            putExtra(FileExploreActivity.KEY_PATH, preference.value)
                        }, 233) { code, intent ->
                            if (code == Activity.RESULT_OK) {
                                preference.value = intent?.data?.toFile()?.canonicalPath
                                preference.summary = preference.value
                            }
                        }
                    },

                    reGrantAction = {
                        choosePath(preference)
                    }
            )
        }
    }
}
