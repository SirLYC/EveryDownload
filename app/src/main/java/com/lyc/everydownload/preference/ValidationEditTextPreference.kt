package com.lyc.everydownload.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * @author liuyuchuan
 * @date 2019-06-28
 * @email kevinliu.sir@qq.com
 */
open class ValidationEditTextPreference : DialogPreference {

    private lateinit var textInputLayout: TextInputLayout
    private lateinit var textInputEditText: TextInputEditText

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    open fun validateInput(text: CharSequence): String? {
        return null
    }


}
