package com.example.data.model

enum class TransactionType(val label: String, val hindiLabel: String) {
    INCOMING("Cash In", "Aaya / Mile"),
    OUTGOING("Cash Out", "Gaya / Diye")
}

enum class PaymentMode(val label: String) {
    CASH("Cash"),
    UPI("UPI / Online"),
    BANK("Bank Transfer"),
    CHEQUE("Cheque")
}

enum class PersonType(val label: String, val hindiLabel: String) {
    CUSTOMER("Customer", "Grahak"),
    SUPPLIER("Supplier", "Vyapari"),
    FRIEND("Friend", "Dost"),
    STAFF("Staff", "Worker"),
    OTHER("Other", "Anya")
}
