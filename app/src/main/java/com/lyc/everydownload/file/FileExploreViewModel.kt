package com.lyc.everydownload.file

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lyc.everydownload.util.ObservableList
import com.lyc.everydownload.util.SingleLiveEvent
import java.io.File

/**
 * @author liuyuchuan
 * @date 2019-06-19
 * @email kevinliu.sir@qq.com
 */
class FileExploreViewModel : ViewModel() {
    lateinit var root: File
    internal val errorMsg = SingleLiveEvent<String>()
    internal val successMsg = SingleLiveEvent<String>()
    internal val dirLiveData = MutableLiveData<File>()
    internal val fileListLiveData = MutableLiveData(ObservableList<Any>(mutableListOf()))
    internal val currentDir
        get() = dirLiveData.value
    private var lastViewDir: File? = null
    internal val itemList
        get() = fileListLiveData.value!!
    private var onlyDir = false
    fun lastViewDir(): File? {
        val result = lastViewDir
        lastViewDir = null
        return result
    }

    fun setup(root: File, currentPath: String, onlyDir: Boolean) {
        this.root = root
        if (!root.exists() || !root.isDirectory) {
            throw IllegalArgumentException("root must be an existing directory!")
        }
        File(currentPath).let {
            if (it.exists() && it.isDirectory) {
                chDir(it)
            } else {
                chDir(root)
            }
        }
        this.onlyDir = onlyDir
    }

    fun refresh() {
        val value = dirLiveData.value
        if (value == null || !value.isDirectory) {
            errorMsg.value = "当前文件夹已失效"
            chDir(root)
            return
        }

        value.listFiles()?.let(this::updateList)
    }

    private fun updateList(array: Array<out File>) {
        val arr = if (onlyDir) {
            array.filterTo(mutableListOf()) { file ->
                file.isDirectory
            }
        } else {
            array.toMutableList<Any>()
        }
        this.fileListLiveData.value = ObservableList(arr)
    }

    fun chDir(dir: File) {
        if (!dir.exists() || !dir.isDirectory) {
            errorMsg.value = "无法访问目标文件夹"
            return
        }
        dirLiveData.value = dir
        dir.listFiles()?.let(this::updateList)
    }

    fun mkDir(name: String): String? {
        if (name.isBlank()) {
            return "文件夹名不能为空"
        }

        dirLiveData.value?.let { parent ->
            val file = File(parent, name)
            return if (file.exists()) {
                "文件已存在"
            } else if (!file.mkdir()) {
                "创建文件夹失败"
            } else {
                refresh()
                null
            }
        }

        return "创建失败"
    }

    fun del(file: File) {
        if (!file.exists()) {
            errorMsg.value = "删除文件不存在"
            itemList.remove(file)
        } else if (file.deleteRecursively()) {
            successMsg.value = "删除成功"
            itemList.remove(file)
        } else {
            errorMsg.value = "删除失败"
        }
    }

    fun back() {
        dirLiveData.value?.let { cur ->
            if (cur == root) {
                return
            }
            lastViewDir = cur
            chDir(cur.parentFile)
        }
    }

    fun isRoot() = dirLiveData.value == root || currentDir?.exists() != true
}
