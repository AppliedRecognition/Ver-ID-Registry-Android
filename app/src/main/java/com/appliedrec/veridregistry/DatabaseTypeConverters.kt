package com.appliedrec.veridregistry

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class DatabaseTypeConverters {
    @TypeConverter
    fun fromDate(date: Date): Long = date.time

    @TypeConverter
    fun toDate(timestamp: Long): Date = Date(timestamp)

    @TypeConverter
    fun floatArrayToBlob(value: FloatArray?): ByteArray? {
        if (value == null) return null
        val buffer = ByteBuffer
            .allocate(value.size * 4)
            .order(ByteOrder.LITTLE_ENDIAN)
        buffer.asFloatBuffer().put(value)
        return buffer.array()
    }

    @TypeConverter
    fun blobToFloatArray(bytes: ByteArray?): FloatArray? {
        if (bytes == null) return null
        val buf = ByteBuffer
            .wrap(bytes)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asFloatBuffer()
        val out = FloatArray(buf.remaining())
        buf.get(out)
        return out
    }
}
