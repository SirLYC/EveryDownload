//package com.lyc.everydownload.update
//
//import java.io.File
//
///**
// * @author liuyuchuan
// * @date 2019-07-04
// * @email kevinliu.sir@qq.com
// */
//sealed class UpdateState {
//    open fun check(): UpdateState? = null
//    open fun checkResult(info: UpdateInfo): UpdateState? = null
//    open fun download(path: String): UpdateState? = null
//    open fun downloadResult()
//
//    object Checking : UpdateState() {
//        override fun checkResult(info: UpdateInfo) = NewUpdate(info)
//    }
//
//    class NewUpdate(info: UpdateInfo) : UpdateState() {
//        override fun check() = Checking
//        override fun download(path: String): UpdateState? {
//
//        }
//    }
//
//    object Downloading: UpdateState() {
//
//    }
//
//    class Error()
//}
