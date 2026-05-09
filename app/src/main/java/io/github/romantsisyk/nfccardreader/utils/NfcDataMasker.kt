package io.github.romantsisyk.nfccardreader.utils

object NfcDataMasker {

    /**
     * Masks a PAN leaving only the last 4 digits visible.
     * Handles EMV BCD-encoded PANs where shorter card numbers are padded
     * with a trailing 'F' nibble (e.g., 15-digit Amex: "378282246310005F").
     */
    fun maskPan(pan: String): String {
        if (pan.isEmpty()) return ""
        // Strip BCD padding nibble before masking so the regex sees only digits
        val digits = pan.trimEnd('F', 'f')
        if (digits.length < 4) return pan
        return digits.replace(Regex("\\d(?=\\d{4})"), "X")
    }

    /**
     * Masks Track 2 data, hiding all but the last 4 digits of the PAN portion
     * and obscuring the field separator.
     */
    fun maskTrack2Data(track2: String): String {
        if (track2.isEmpty()) return ""
        val digits = track2.trimEnd('F', 'f')
        val masked = digits.replace(Regex("\\d(?=\\d{4})"), "X")
        return if (masked.contains('=')) masked.replace('=', 'X') else masked
    }
}
