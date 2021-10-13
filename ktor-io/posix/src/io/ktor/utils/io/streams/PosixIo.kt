@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package io.ktor.utils.io.streams

import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.*
import kotlinx.cinterop.*
import platform.posix.*

public inline fun <R> CPointer<FILE>.use(block: (CPointer<FILE>) -> R): R {
    return try {
        block(this)
    } finally {
        fclose(this) // TODO handle errors
    }
}

public fun fwrite(buffer: Buffer, stream: CPointer<FILE>): size_t {
    var written: size_t

    buffer.readDirect { pointer ->
        val result = fwrite(pointer, 1.convert(), buffer.readRemaining.convert(), stream)
        written = result

        // it is completely safe to convert since the returned value will be never greater than Int.MAX_VALUE
        result.convert()
    }

    return written
}

public fun fwrite(source: Memory, offset: Int, length: Int, stream: CPointer<FILE>): Int {
    return fwrite(source, offset.toLong(), length.toLong(), stream).toInt()
}

public fun fwrite(source: Memory, offset: Long, length: Long, stream: CPointer<FILE>): Long {
    val maxLength = minOf<Long>(length, ssize_t.MAX_VALUE.convert())
    return fwrite(source.pointer + offset, 1.convert(), maxLength.convert(), stream).convert()
}

public fun write(fildes: Int, buffer: Buffer): ssize_t {
    var written: ssize_t

    buffer.readDirect { pointer ->
        val result = write(fildes, pointer, buffer.readRemaining.convert())
        written = result.convert()

        // it is completely safe to convert since the returned value will be never greater than Int.MAX_VALUE
        // however the returned value could be -1 so clamp it
        result.convert<Int>().coerceAtLeast(0)
    }

    return written
}

public fun write(fildes: Int, source: Memory, offset: Int, length: Int): Int {
    return write(fildes, source, offset.toLong(), length.toLong()).toInt()
}

public fun write(fildes: Int, source: Memory, offset: Long, length: Long): Long {
    val maxLength = minOf<Long>(length, ssize_t.MAX_VALUE.convert())
    return write(fildes, source.pointer + offset, maxLength.convert()).convert()
}

public fun fread(buffer: Buffer, stream: CPointer<FILE>): size_t {
    var bytesRead: size_t

    buffer.writeDirect { pointer ->
        val result = fread(pointer, 1.convert(), buffer.writeRemaining.convert(), stream)
        bytesRead = result

        // it is completely safe to convert since the returned value will be never greater than Int.MAX_VALUE
        result.convert()
    }

    return bytesRead
}

public fun fread(destination: Memory, offset: Int, length: Int, stream: CPointer<FILE>): Int {
    return fread(destination, offset.toLong(), length.toLong(), stream).toInt()
}

public fun fread(destination: Memory, offset: Long, length: Long, stream: CPointer<FILE>): Long {
    val maxLength = minOf(length, Int.MAX_VALUE.toLong(), ssize_t.MAX_VALUE.toLong())
    val pointer = destination.pointer + offset

    val result = fread(pointer, 1.convert(), maxLength.convert(), stream)

    // it is completely safe to convert since the returned value will be never greater than Int.MAX_VALUE
    return result.convert()
}

public fun read(fildes: Int, buffer: Buffer): ssize_t {
    var bytesRead: ssize_t

    buffer.writeDirect { pointer ->
        val size = minOf(
            ssize_t.MAX_VALUE.toULong(),
            SSIZE_MAX.toULong(),
            buffer.writeRemaining.toULong()
        ).convert<size_t>()

        val result = read(fildes, pointer, size.convert())
        bytesRead = result.convert()

        // it is completely safe to convert since the returned value will be never greater than Int.MAX_VALUE
        // however the returned value could be -1 so clamp it
        result.convert<Int>().coerceAtLeast(0)
    }

    return bytesRead
}

public fun read(fildes: Int, destination: Memory, offset: Int, length: Int): Int {
    return read(fildes, destination, offset.toLong(), length.toLong()).toInt()
}

public fun read(fildes: Int, destination: Memory, offset: Long, length: Long): Long {
    val maxLength = minOf<Long>(
        ssize_t.MAX_VALUE.convert(),
        length
    )

    return read(fildes, destination.pointer + offset, maxLength.convert()).convert<Long>().coerceAtLeast(0)
}
