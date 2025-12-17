package io.github.romantsisyk.nfccardreader.domain.model

/**
 * Sealed class representing the result of an NFC operation.
 *
 * This provides type-safe error handling for NFC operations,
 * replacing string-based error messages with structured error types.
 */
sealed class NfcResult<out T> {
    /**
     * Represents a successful NFC operation.
     *
     * @property data The result data from the operation
     */
    data class Success<T>(val data: T) : NfcResult<T>()

    /**
     * Represents a failed NFC operation.
     *
     * @property error The type of error that occurred
     * @property message Human-readable error message
     * @property exception Optional underlying exception
     */
    data class Error(
        val error: NfcError,
        val message: String,
        val exception: Throwable? = null
    ) : NfcResult<Nothing>()

    /**
     * Represents a loading state during NFC operation.
     */
    data object Loading : NfcResult<Nothing>()

    /**
     * Checks if this result is successful.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Checks if this result is an error.
     */
    val isError: Boolean get() = this is Error

    /**
     * Checks if this result is loading.
     */
    val isLoading: Boolean get() = this is Loading

    /**
     * Returns the data if successful, null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Returns the error if failed, null otherwise.
     */
    fun errorOrNull(): NfcError? = when (this) {
        is Error -> error
        else -> null
    }
}

/**
 * Enumeration of possible NFC errors.
 */
enum class NfcError {
    /** NFC is not available on this device */
    NFC_NOT_AVAILABLE,

    /** NFC is disabled in device settings */
    NFC_DISABLED,

    /** No NFC tag was detected */
    TAG_NOT_FOUND,

    /** The NFC tag type is not supported */
    UNSUPPORTED_TAG,

    /** Communication with the tag failed */
    COMMUNICATION_ERROR,

    /** Failed to parse the card data */
    PARSE_ERROR,

    /** The card data is invalid or corrupted */
    INVALID_DATA,

    /** Required dependencies are not initialized */
    INITIALIZATION_ERROR,

    /** An unknown error occurred */
    UNKNOWN_ERROR
}
