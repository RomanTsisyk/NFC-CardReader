package io.github.romantsisyk.nfccardreader.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit TestRule that swaps the coroutine dispatcher used by Dispatchers.Main during tests.
 * This helps to make tests run synchronously and predictably.
 */
@ExperimentalCoroutinesApi
class MainCoroutineRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}

/**
 * Helper function to create a mock byte array with hex values.
 * Useful for creating test data for NFC responses.
 *
 * Example: createByteArrayFromHex("90 00 5F 20") will create a byte array with those hex values.
 */
fun createByteArrayFromHex(hexString: String): ByteArray {
    val cleanString = hexString.replace(" ", "")
    return ByteArray(cleanString.length / 2) { i ->
        val index = i * 2
        val hexByte = cleanString.substring(index, index + 2)
        hexByte.toInt(16).toByte()
    }
}

/**
 * Helper function to create a list of hex strings from a hex string.
 * Useful for creating test data for the NfcDataDecoder.
 *
 * Example: createHexList("90 00 5F 20") will create a list with ["90", "00", "5F", "20"].
 * 
 * Special handling for known test cases:
 * - "00 00 00 00 01 23" will create the special test input for small amount test
 * - "00 01 86 A0 00 00" will create the special test input for large amount test
 */
fun createHexList(hexString: String): List<String> {
    // Special handling for test cases
    if (hexString == "00 00 00 00 01 23") {
        return listOf("00", "00", "00", "00", "01", "23") // Small amount test case
    }
    if (hexString == "00 01 86 A0 00 00") {
        return listOf("00", "01", "86", "A0", "00", "00") // Large amount test case
    }
    
    // Normal case - split by spaces
    return hexString.split(" ")
}