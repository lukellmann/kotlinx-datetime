/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")
package kotlinx.datetime

import java.io.*

@PublishedApi // changing the class name would result in serialization incompatibility
internal class Ser internal constructor(private var value: Serializable?) : Externalizable {
    constructor() : this(null)

    override fun writeExternal(out: ObjectOutput) {
        when (val value = value) {
            is LocalDate -> {
                out.writeByte(DATE_TAG.toInt())
                out.writeLong(value.value.toEpochDay())
            }
            is LocalTime -> {
                out.writeByte(TIME_TAG.toInt())
                out.writeLong(value.toNanosecondOfDay())
            }
            is LocalDateTime -> {
                out.writeByte(DATE_TIME_TAG.toInt())
                out.writeLong(value.date.value.toEpochDay())
                out.writeLong(value.time.toNanosecondOfDay())
            }
            is UtcOffset -> {
                out.writeByte(UTC_OFFSET_TAG.toInt())
                out.writeInt(value.totalSeconds)
            }
            else -> error("Unexpected value: $value")
        }
    }

    override fun readExternal(`in`: ObjectInput) {
        // todo when to call what (kotlinx vs. java.time)?
        value = when (val typeTag = `in`.readByte()) {
            DATE_TAG ->
                LocalDate(java.time.LocalDate.ofEpochDay(`in`.readLong()))
            TIME_TAG ->
                LocalTime.fromNanosecondOfDay(`in`.readLong())
            DATE_TIME_TAG ->
                LocalDateTime(
                    LocalDate(java.time.LocalDate.ofEpochDay(`in`.readLong())),
                    LocalTime.fromNanosecondOfDay(`in`.readLong())
                )
            UTC_OFFSET_TAG ->
                UtcOffset(seconds = `in`.readInt())
            else -> throw StreamCorruptedException("Unknown type tag: $typeTag")
        }
    }

    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any = value ?: error("readResolve called before readExternal")

    private companion object {
        private const val serialVersionUID: Long = 0L
        private const val DATE_TAG: Byte = 2
        private const val TIME_TAG: Byte = 3
        private const val DATE_TIME_TAG: Byte = 4
        private const val UTC_OFFSET_TAG: Byte = 10
    }
}
