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
    internal val currentDir
        get() = dirLiveData.value
    internal val itemList = ObservableList<Any>(mutableListOf())

    fun setup(root: File) {
        this.root = root
        if (!root.exists() || !root.isDirectory) {
            throw IllegalArgumentException("root must be an existing directory!")
        }
        chDir(root)
    }

    fun refresh() {
        val value = dirLiveData.value
        if (value == null || !value.isDirectory) {
            errorMsg.value = "当前文件夹已失效"
            chDir(root)
            return
        }

        value.listFiles()?.let {
            itemList.clear()
            itemList.addAll(it)
        }
    }

    fun chDir(dir: File) {
        if (!dir.exists() || !dir.isDirectory) {
            errorMsg.value = "无法访问目标文件夹"
            return
        }
        dir.listFiles()?.let {
            dirLiveData.value = dir
            itemList.clear()
            itemList.addAll(it)
        }
    }

    fun mkDir(name: String) {
        if (name.isBlank()) {
            errorMsg.value = "文件夹名不能为空"
            return
        }

        dirLiveData.value?.let { parent ->
            val file = File(parent, name)
            if (file.exists()) {
                errorMsg.value = "文件已存在"
            } else if (!file.mkdir()) {
                errorMsg.value = "创建文件夹失败"
            } else {
                itemList.add(0, file)
            }
        }
    }

    fun del(file: File) {
        if (!file.exists()) {
            errorMsg.value = "删除文件不存在"
            itemList.remove(file)
        } else if (file.delete()) {
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
            chDir(cur.parentFile)
        }
    }

    fun isRoot() = dirLiveData.value == root
}
