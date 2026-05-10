package io.github.romantsisyk.nfccardreader.utils

import java.math.BigInteger
import java.util.Currency
import java.util.Locale

object NfcDataDecoder {

    // ISO 4217 numeric (BCD 4-char) → alpha-3 currency code
    private val currencyCodes = mapOf(
        "0840" to "USD", "0978" to "EUR", "0980" to "UAH", "0826" to "GBP",
        "0392" to "JPY", "0124" to "CAD", "0036" to "AUD", "0756" to "CHF",
        "0156" to "CNY", "0643" to "RUB", "0356" to "INR", "0410" to "KRW",
        "0702" to "SGD", "0344" to "HKD", "0554" to "NZD", "0710" to "ZAR",
        "0792" to "TRY", "0784" to "AED", "0682" to "SAR", "0376" to "ILS"
    )

    private val transactionTypes = mapOf(
        "00" to "Purchase",
        "01" to "Cash Advance",
        "09" to "Purchase with Cashback",
        "20" to "Return/Refund",
        "21" to "Deposit",
        "31" to "Balance Inquiry",
        "50" to "Quasi-Cash",
        "90" to "Authorization Only"
    )

    // ISO 3166-1 numeric (decimal) → alpha-2 country code
    // Country codes are BCD-encoded in EMV; strip leading zeros to get the decimal key.
    private val iso3166NumericToAlpha2 = mapOf(
        "840" to "US", "980" to "UA", "826" to "GB", "392" to "JP",
        "124" to "CA", "36"  to "AU", "756" to "CH", "156" to "CN",
        "643" to "RU", "276" to "DE", "250" to "FR", "380" to "IT",
        "724" to "ES", "616" to "PL", "203" to "CZ", "348" to "HU",
        "642" to "RO", "703" to "SK", "191" to "HR", "578" to "NO",
        "752" to "SE", "208" to "DK", "246" to "FI", "372" to "IE",
        "528" to "NL", "56"  to "BE", "76"  to "BR", "356" to "IN",
        "410" to "KR", "702" to "SG", "344" to "HK", "554" to "NZ",
        "710" to "ZA", "792" to "TR", "682" to "SA", "784" to "AE",
        "376" to "IL", "484" to "MX", "32"  to "AR", "152" to "CL",
        "170" to "CO", "604" to "PE", "858" to "UY"
    )

    fun decodeAmount(bytes: List<String>): String {
        if (bytes.isEmpty()) return "0.00"
        return try {
            val joinedHex = bytes.joinToString("")
            val amount = BigInteger(joinedHex, 16).toLong() / 100.0
            String.format(Locale.US, "%.2f", amount)
        } catch (e: NumberFormatException) {
            "0.00"
        }
    }

    fun decodeCurrency(bytes: List<String>): String {
        val bcdKey = bytes.joinToString("")  // e.g. "0840"
        currencyCodes[bcdKey]?.let { return it }
        // Fallback: interpret BCD as decimal numeric code
        return try {
            val numericCode = bcdKey.trimStart('0').ifEmpty { "0" }.toInt()
            Currency.getAvailableCurrencies()
                .find { it.numericCode == numericCode }
                ?.currencyCode ?: "Unknown Currency ($bcdKey)"
        } catch (e: Exception) {
            "Unknown Currency ($bcdKey)"
        }
    }

    fun decodeDate(bytes: List<String>): String {
        if (bytes.size < 3) return "Invalid Date"
        val (yy, mm, dd) = Triple(bytes[0], bytes[1], bytes[2])
        // Validate BCD: each component must be two decimal digits
        if (!yy.isBcdPair() || !mm.isBcdPair() || !dd.isBcdPair()) return "Invalid Date"
        val month = mm.toInt(); val day = dd.toInt()
        if (month !in 1..12 || day !in 1..31) return "Invalid Date"
        return "$dd.$mm.20$yy"
    }

    fun decodeTime(bytes: List<String>): String {
        if (bytes.size < 3) return "Invalid Time"
        val (hh, min, sec) = Triple(bytes[0], bytes[1], bytes[2])
        if (!hh.isBcdPair() || !min.isBcdPair() || !sec.isBcdPair()) return "Invalid Time"
        val h = hh.toInt(); val m = min.toInt(); val s = sec.toInt()
        if (h > 23 || m > 59 || s > 59) return "Invalid Time"
        return "$hh:$min:$sec"
    }

    /**
     * Decodes a BCD-encoded ISO 3166-1 numeric country code.
     * Bytes contain packed BCD digits (e.g., 08 40 → "0840" → "840" → "US").
     */
    fun decodeCountryCode(bytes: List<String>): String {
        val bcdCode = bytes.joinToString("").trimStart('0').ifEmpty { "0" }
        return iso3166NumericToAlpha2[bcdCode]
            ?.let { alpha2 -> Locale("", alpha2).displayCountry }
            ?: "Unknown Country ($bcdCode)"
    }

    fun decodeTransactionType(byte: String): String =
        transactionTypes[byte] ?: "Unknown Transaction Type ($byte)"

    fun decodeApplicationIdentifier(bytes: List<String>): String {
        val hex = bytes.joinToString("")
        return when {
            hex.startsWith("A000000003") -> "Visa"
            hex.startsWith("A000000004") -> "Mastercard"
            hex.startsWith("A000000025") -> "American Express"
            hex.startsWith("A000000065") -> "JCB"
            hex.startsWith("A000000152") -> "Discover/Diners Club"
            hex.startsWith("A000000324") -> "UnionPay"
            hex.startsWith("A000000677") -> "Mir"
            hex.contains("D276000025")   -> "Interac"
            else -> "Unknown Card ($hex)"
        }
    }

    fun decodeServiceCode(bytes: List<String>): String {
        if (bytes.size < 3) return "Incomplete Service Code"
        val code = bytes.joinToString("")
        if (code.length < 3) return "Incomplete Service Code"

        val interchange = when (code[0].toString()) {
            "1" -> "International interchange"
            "2" -> "International interchange, with IC"
            "5" -> "National interchange only"
            "6" -> "National interchange only, with IC"
            "7" -> "Private"
            "9" -> "Test"
            else -> "Unknown interchange"
        }
        val authorization = when (code[1].toString()) {
            "0" -> "Normal authorization"
            "2" -> "By issuer"
            "4" -> "By issuer unless explicit agreement"
            else -> "Unknown authorization"
        }
        val services = when (code[2].toString()) {
            "0" -> "No restrictions, PIN required"
            "1" -> "No restrictions"
            "2" -> "Goods and services only"
            "3" -> "ATM only, PIN required"
            "4" -> "Cash only"
            "5" -> "Goods and services only, PIN required"
            "6" -> "No restrictions, use PIN if feasible"
            "7" -> "Goods and services only, use PIN if feasible"
            else -> "Unknown services"
        }
        return "$interchange, $authorization, $services"
    }

    fun decodeCardholderVerificationMethodResult(bytes: List<String>): String {
        return when (bytes.joinToString("")) {
            "0000" -> "No CVM performed"
            "0001" -> "Plaintext PIN verified by ICC"
            "0002" -> "Enciphered PIN verified online"
            "0003" -> "Plaintext PIN verified by ICC and signature"
            "0004" -> "Enciphered PIN verified by ICC"
            "0005" -> "Enciphered PIN verified by ICC and signature"
            "0006" -> "Signature"
            "0007" -> "No CVM required"
            "0008" -> "Card CVM reference check failed"
            else   -> "Unknown CVM (${bytes.joinToString("")})"
        }
    }

    fun decodeFormFactorIndicator(bytes: List<String>): String {
        val code = bytes.firstOrNull()?.take(2) ?: return "Unknown form factor"
        return when (code) {
            "01" -> "Physical card with magnetic stripe"
            "02" -> "Physical card with magnetic stripe and contact chip"
            "03" -> "Physical card with contact chip only"
            "04" -> "Physical card with contact chip and contactless"
            "05" -> "Physical contactless card"
            "06" -> "Mobile phone"
            "07" -> "Smart watch"
            "08" -> "Smart card"
            "09" -> "Passive wearable (ring, bracelet, band)"
            "0A" -> "Battery-powered wearable"
            "41" -> "Physical card hosted a virtual card"
            "42" -> "Mobile phone hosted a virtual card"
            else -> "Unknown form factor"
        }
    }

    /** Returns true if the hex string is exactly 2 chars of decimal digits (valid BCD byte). */
    private fun String.isBcdPair(): Boolean = length == 2 && all { it.isDigit() }
}
