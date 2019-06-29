package com.lyc.everydownload.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference

/**
 * @author liuyuchuan
 * @date 2019-06-29
 * @email kevinliu.sir@qq.com
 */
class SingleValuePreference : Preference {
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?) : super(context)

    private var _value: String? = null

    var defaultValue: String? = null

    var value: String?
        get() = _value ?: getPersistedString(defaultValue)
        set(value) {
            if (_value != value) {
                _value = value
                persistString(value)
                notifyDependencyChange(shouldDisableDependents())
                notifyChanged()
            }
        }

}
