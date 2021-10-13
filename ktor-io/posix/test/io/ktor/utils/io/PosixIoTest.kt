package io.ktor.utils.io

import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.errors.*
import io.ktor.utils.io.internal.utils.*
import io.ktor.utils.io.internal.utils.test.*
import io.ktor.utils.io.streams.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.test.*

class PosixIoTest {
    private val filename = "build/test.tmp"
    private lateinit var buffer: Buffer

    @BeforeTest
    fun setup() {
        buffer = Buffer(DefaultAllocator.alloc(4096))
        buffer.resetForWrite()
        buffer.writeFully("test".encodeToByteArray())

        unlink(filename)
    }

    @AfterTest
    fun cleanup() {
        DefaultAllocator.free(buffer.memory)
        unlink(filename)
    }

    @Test
    fun testFFunctions() {
        val descriptor = fopen(filename, "w") ?: return

        descriptor.use { file ->
            assertEquals(4, fwrite(buffer, file).convert(), "Expected all bytes to be written")
        }
        buffer.resetForWrite()
        fopen(filename, "r")!!.use { file ->
            assertEquals(4, fread(buffer, file).convert(), "Expected all bytes to be read")
            assertEquals("test", buffer.readText())
            assertEquals(0, fread(buffer, file).convert(), "Expected EOF")
        }
    }

    @Test
    fun testFunctions() {
        val descriptor = open(filename, O_WRONLY or O_CREAT, 420)
        if (descriptor < 0) {
            return
        }

        descriptor.use { file ->
            assertEquals(4, write(file, buffer).toInt(), "Expected all bytes to be written")
        }
        buffer.resetForWrite()
        open(filename, O_RDONLY).use { file ->
            assertEquals(4, read(file, buffer).toInt(), "Expected all bytes to be read")
            assertEquals("test", buffer.readText())
            assertEquals(0, read(file, buffer).toInt(), "Expected EOF")
        }
    }

    @Test
    fun testInputOutputForFileDescriptor() {
        val file = open(filename, O_WRONLY or O_CREAT, 420)
        if (file < 0) {
            return
        }

        Output(file.checkError("open(C|W)")).use { out ->
            out.append("test")
        }

        Input(open(filename, O_RDONLY).checkError("open(R)")).use { input ->
            assertEquals("test", input.readText())
        }
    }

    @Test
    fun testInputOutputForFileInstance() {
        val file = fopen(filename, "w") ?: return

        Output(file).use { out ->
            out.append("test")
        }

        Input(fopen(filename, "r")!!).use { input ->
            assertEquals("test", input.readText())
        }
    }

    @Test
    fun testInputDoubleCloseFD() {
        val fd = open(filename, O_WRONLY or O_CREAT, 420)

        if (fd < 0) {
            return
        }

        val input = Input(fd)
        close(fd)
        input.close()
    }

    @Test
    fun testInputDoubleCloseFD2() {
        val fd = open(filename, O_WRONLY or O_CREAT, 420)
        if (fd < 0) {
            return
        }

        val input = Input(fd)
        input.close()
        input.close()
    }

    @Test
    fun testInputDoubleCloseFILE() {
        val fopen = fopen(filename, "w") ?: return
        val input = Input(fopen)
        input.close()
        input.close()
    }

    @Test
    fun testOutputDoubleCloseFD() {
        val fd = open(filename, O_WRONLY or O_CREAT, 420)
        if (fd < 0) {
            return
        }

        val output = Output(fd)
        close(fd)
        output.close()
    }

    @Test
    fun testOutputDoubleCloseFD2() {
        val fd = open(filename, O_WRONLY or O_CREAT, 420)
        if (fd < 0) {
            return
        }

        val output = Output(fd)
        output.close()
        output.close()
    }

    @Test
    fun testOutputDoubleCloseFILE() {
        val file = fopen(filename, "w") ?: return

        val output = Output(file)
        output.close()
        output.close()
    }

    private inline fun Int.use(block: (Int) -> Unit) {
        checkError()
        try {
            block(this)
        } finally {
            close(this)
        }
    }

    @Suppress("unused")
    internal fun Int.checkError(action: String = ""): Int = when {
        this < 0 -> memScoped { throw PosixException.forErrno(posixFunctionName = action) }
        else -> this
    }

    @Suppress("unused")
    internal fun Long.checkError(action: String = ""): Long = when {
        this < 0 -> memScoped { throw PosixException.forErrno(posixFunctionName = action) }
        else -> this
    }

    private val ZERO: size_t = 0u

    @Suppress("unused")
    internal fun size_t.checkError(action: String = ""): size_t = when (this) {
        ZERO -> errno.let { errno ->
            when (errno) {
                0 -> this
                else -> memScoped { throw PosixException.forErrno(posixFunctionName = action) }
            }
        }
        else -> this
    }
}
