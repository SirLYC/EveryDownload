package com.lyc.everydownload.preference

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.lyc.everydownload.R
import kotlinx.android.synthetic.main.activity_main.*

/**
 * @author liuyuchuan
 * @date 2019-06-27
 * @email kevinliu.sir@qq.com
 */
class PreferenceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val tag = PreferenceFragment::class.java.simpleName
        if (supportFragmentManager.findFragmentByTag(tag) == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, PreferenceFragment(), tag)
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> false
        }
    }
}
