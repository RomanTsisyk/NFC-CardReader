package io.github.romantsisyk.nfccardreader.domain

enum class EmvTag(val tag: String, val description: String = "") {
    // Tags for TLV parsing
    CARDHOLDER_NAME("5F20", "Cardholder Name"),
    APPLICATION_PAN("5A", "Primary Account Number"),
    TRACK2_EQUIVALENT_DATA("57", "Track 2 Equivalent Data"),
    EXPIRATION_DATE("5F24", "Expiration Date"),
    APPLICATION_PREFERRED_NAME("9F12", "Application Preferred Name"),

    // Additional tags for interpreting NFC data
    CARD_TYPE("6F", "File Control Information"),
    APPLICATION_LABEL("50", "Application Label"),
    TRANSACTION_AMOUNT("9F02", "Transaction Amount"),
    AMOUNT_OTHER("9F03", "Amount, Other"), // Added missing tag
    CURRENCY_CODE("5F2A", "Transaction Currency Code"),
    TRANSACTION_DATE("9A", "Transaction Date"),
    TRANSACTION_STATUS("90", "Transaction Status"),
    
    // Additional EMV tags
    APPLICATION_IDENTIFIER("4F", "Application Identifier"),
    APPLICATION_TEMPLATE("61", "Application Template"),
    DEDICATED_FILE_NAME("84", "Dedicated File Name"),
    ISSUER_COUNTRY_CODE("5F28", "Issuer Country Code"),
    TRANSACTION_CURRENCY_EXPONENT("5F36", "Transaction Currency Exponent"),
    SERVICE_CODE("5F30", "Service Code"),
    ISSUER_URL("5F50", "Issuer URL"),
    PAYMENT_ACCOUNT_REFERENCE("9F24", "Payment Account Reference"),
    APPLICATION_CRYPTOGRAM("9F26", "Application Cryptogram"),
    APPLICATION_TRANSACTION_COUNTER("9F36", "Application Transaction Counter"),
    APPLICATION_INTERCHANGE_PROFILE("82", "Application Interchange Profile"),
    TERMINAL_VERIFICATION_RESULTS("95", "Terminal Verification Results"),
    TRANSACTION_TYPE("9C", "Transaction Type"),
    ISSUER_APPLICATION_DATA("9F10", "Issuer Application Data"),
    TERMINAL_COUNTRY_CODE("9F1A", "Terminal Country Code"),
    INTERFACE_DEVICE_SERIAL_NUMBER("9F1E", "Interface Device Serial Number"),
    UNPREDICTABLE_NUMBER("9F37", "Unpredictable Number"),
    CARDHOLDER_VERIFICATION_METHOD_RESULTS("9F34", "Cardholder Verification Method Results"),
    ISSUER_SCRIPT_RESULTS("9F5B", "Issuer Script Results"),
    APPLICATION_CURRENCY_CODE("9F42", "Application Currency Code"),
    TRANSACTION_CATEGORY_CODE("9F53", "Transaction Category Code"),
    ISSUER_SCRIPT_TEMPLATE("9F7F", "Issuer Script Template"),
    FORM_FACTOR_INDICATOR("9F6E", "Form Factor Indicator"),
    CARD_AUTHENTICATION_METHOD("9F6C", "Card Authentication Method"),
    CERTIFICATION_AUTHORITY_PUBLIC_KEY_INDEX("9F22", "Certification Authority Public Key Index"),
    MASTERCARD_SPECIFIC_DATA("A0", "MasterCard Specific Data"),
    VISA_SPECIFIC_DATA("A5", "Visa Specific Data"),
    APPLICATION_IDENTIFIER_ADDITIONAL("9F06", "Application Identifier (AID)"), // Added missing tag

    // EMV protocol-flow tags (used by GPO / READ RECORD)
    APPLICATION_FILE_LOCATOR("94", "Application File Locator"),
    PDOL("9F38", "Processing Options Data Object List"),
    RESPONSE_MESSAGE_TEMPLATE_FORMAT_1("80", "Response Message Template Format 1"),
    RESPONSE_MESSAGE_TEMPLATE_FORMAT_2("77", "Response Message Template Format 2"),
    FCI_PROPRIETARY_TEMPLATE("A5", "FCI Proprietary Template"),
    FCI_ISSUER_DISCRETIONARY_DATA("BF0C", "FCI Issuer Discretionary Data"),

    // Fallback for unknown tags
    UNKNOWN("FFFF", "Unknown Tag");

    companion object {
        fun fromTag(tag: String): EmvTag {
            return entries.find { it.tag == tag } ?: UNKNOWN
        }
        
        fun getDescription(tag: String): String {
            return fromTag(tag).description
        }
    }
}