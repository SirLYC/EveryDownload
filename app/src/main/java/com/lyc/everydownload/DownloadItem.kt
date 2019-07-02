package com.lyc.everydownload

import android.os.Parcel
import android.os.Parcelable
import java.util.*

/**
 * @author liuyuchuan
 * @date 2019-04-23
 * @email kevinliu.sir@qq.com
 */
data class DownloadItem(
        var id: Long = 0,
        var path: String,
        var filename: String,
        var url: String,
        var bps: Double = 0.toDouble(),
        var totalSize: Long = 0,
        var downloadedSize: Long = 0,
        var createdTime: Date,
        var finishedTime: Date? = null,
        var downloadState: Int = 0,
        var errorCode: Int? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readDouble(),
            parcel.readLong(),
            parcel.readLong(),
            Date(parcel.readLong()),
            parcel.readByte().let {
                return@let if (it == 1.toByte()) {
                    Date(parcel.readLong())
                } else {
                    null
                }
            },
            parcel.readInt(),
            parcel.readValue(Int::class.java.classLoader) as? Int)

    override fun equals(other: Any?): Boolean {
        return this.id == (other as? DownloadItem)?.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(path)
        parcel.writeString(filename)
        parcel.writeString(url)
        parcel.writeDouble(bps)
        parcel.writeLong(totalSize)
        parcel.writeLong(downloadedSize)
        parcel.writeLong(createdTime.time)
        finishedTime?.time.let {
            if (it == null) {
                parcel.writeByte(0)
            } else {
                parcel.writeByte(1)
                parcel.writeLong(it)
            }
        }
        parcel.writeInt(downloadState)
        parcel.writeValue(errorCode)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DownloadItem> {
        const val UPDATE_PROGRESS = 0
        const val UPDATE_INFO = 1
        const val UPDATE_SELECT = 2

        override fun createFromParcel(parcel: Parcel): DownloadItem {
            return DownloadItem(parcel)
        }

        override fun newArray(size: Int): Array<DownloadItem?> {
            return arrayOfNulls(size)
        }
    }
}
