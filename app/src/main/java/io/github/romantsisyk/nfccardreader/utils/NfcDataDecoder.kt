package io.github.romantsisyk.nfccardreader.utils

import java.util.Currency
import java.util.Locale
import java.math.BigInteger

/**
 * Utility object for decoding NFC/EMV data values.
 *
 * Provides methods to convert raw hex byte values from EMV tags into
 * human-readable formats. Supports decoding of amounts, currencies,
 * dates, service codes, and various card-specific data.
 *
 * All methods handle invalid input gracefully, returning sensible
 * defaults or descriptive error strings rather than throwing exceptions.
 */
object NfcDataDecoder {

    /** ISO 4217 currency code mappings (numeric to alpha) */
    private val currencyCodes = mapOf(
        "0840" to "USD",
        "0978" to "EUR",
        "0980" to "UAH",
        "0826" to "GBP",
        "0392" to "JPY",
        "0124" to "CAD",
        "0036" to "AUD",
        "0756" to "CHF",
        "0156" to "CNY",
        "0643" to "RUB"
    )
    
    // Карта для типів транзакцій
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
    
    /**
     * Decodes a transaction amount from BCD-encoded hex bytes.
     *
     * @param bytes List of hex string bytes representing the amount
     * @return Formatted decimal amount string (e.g., "123.45")
     */
    fun decodeAmount(bytes: List<String>): String {
        try {
            val joinedHex = bytes.joinToString("")
            val amount = BigInteger(joinedHex, 16).toLong() / 100.0
            return String.format(Locale.US, "%.2f", amount)
        } catch (e: Exception) {
            return "0.00"
        }
    }

    fun decodeCurrency(bytes: List<String>): String {
        val hexValue = bytes.joinToString("")
        return currencyCodes[hexValue] ?: try {
            val numericCode = hexValue.toInt(16)
            val currencies = Currency.getAvailableCurrencies()
            val currency = currencies.find { it.numericCode == numericCode }
            currency?.currencyCode ?: "Unknown Currency ($hexValue)"
        } catch (e: Exception) {
            "Unknown Currency ($hexValue)"
        }
    }

    fun decodeDate(bytes: List<String>): String {
        val year = "20${bytes[0]}"
        val month = bytes[1]
        val day = bytes[2]
        return "$day.$month.$year"
    }
    
    fun decodeTime(bytes: List<String>): String {
        val hour = bytes[0]
        val minute = bytes[1]
        val second = bytes[2]
        return "$hour:$minute:$second"
    }
    
    fun decodeCountryCode(bytes: List<String>): String {
        val hexValue = bytes.joinToString("")
        val numericCode = hexValue.toInt(16)
        return try {
            val availableLocales = Locale.getAvailableLocales()
            val locale = availableLocales.find { 
                it.country.isNotEmpty() && it.country.hashCode() == numericCode
            }
            locale?.displayCountry ?: "Unknown Country ($hexValue)"
        } catch (e: Exception) {
            "Unknown Country ($hexValue)"
        }
    }
    
    fun decodeTransactionType(byte: String): String {
        return transactionTypes[byte] ?: "Unknown Transaction Type ($byte)"
    }
    
    fun decodeApplicationIdentifier(bytes: List<String>): String {
        val hexValue = bytes.joinToString("")
        return when {
            hexValue.startsWith("A000000003") -> "Visa"
            hexValue.startsWith("A000000004") -> "MasterCard"
            hexValue.startsWith("A000000025") -> "American Express"
            hexValue.startsWith("A000000065") -> "JCB"
            hexValue.startsWith("A000000152") -> "Discover/Diners Club"
            hexValue.startsWith("A000000324") -> "UnionPay"
            hexValue.startsWith("A000000677") -> "Mir"
            hexValue.contains("D276000025") -> "Interac"
            else -> "Unknown Card ($hexValue)"
        }
    }
    
    fun decodeServiceCode(bytes: List<String>): String {
        val serviceCode = bytes.joinToString("")
        val firstDigit = serviceCode.substring(0, 1)
        val secondDigit = serviceCode.substring(1, 2)
        val thirdDigit = serviceCode.substring(2, 3)
        
        val interchange = when (firstDigit) {
            "1" -> "International interchange"
            "2" -> "International interchange, with IC"
            "5" -> "National interchange only"
            "6" -> "National interchange only, with IC"
            "7" -> "Private"
            "9" -> "Test"
            else -> "Unknown interchange"
        }
        
        val authorization = when (secondDigit) {
            "0" -> "Normal authorization"
            "2" -> "By issuer"
            "4" -> "By issuer unless explicit agreement"
            else -> "Unknown authorization"
        }
        
        val services = when (thirdDigit) {
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
        val hexValue = bytes.joinToString("")
        
        return when (hexValue) {
            "0000" -> "No CVM performed"
            "0001" -> "Plaintext PIN verified by ICC"
            "0002" -> "Enciphered PIN verified online"
            "0003" -> "Plaintext PIN verified by ICC and signature"
            "0004" -> "Enciphered PIN verified by ICC"
            "0005" -> "Enciphered PIN verified by ICC and signature"
            "0006" -> "Signature"
            "0007" -> "No CVM required"
            "0008" -> "Card CVM reference check failed"
            else -> "Unknown CVM ($hexValue)"
        }
    }
    
    fun decodeFormFactorIndicator(bytes: List<String>): String {
        val hexValue = bytes.joinToString("")
        val firstByte = if (hexValue.length >= 2) hexValue.substring(0, 2) else "00"
        
        val formFactor = when (firstByte) {
            "00" -> "Unknown form factor"
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
        
        return formFactor
    }
}
