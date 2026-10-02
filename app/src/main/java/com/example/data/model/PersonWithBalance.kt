package com.example.data.model

data class PersonWithBalance(
    val person: Person,
    val totalGiven: Double = 0.0, // Maine Diye (Outgoing to person)
    val totalGot: Double = 0.0,   // Mujhe Mile (Incoming from person)
    val netBalance: Double = 0.0, // totalGiven - totalGot (> 0: Lena hai, < 0: Dena hai)
    val lastTransactionTimestamp: Long? = null,
    val transactionCount: Int = 0
) {
    // When netBalance > 0: Person owes user (User will get / Lena Hai)
    // When netBalance < 0: User owes person (User will give / Dena Hai)
    // When netBalance == 0: Settled (Hisab Barabar)
    val isReceivable: Boolean get() = netBalance > 0.009
    val isPayable: Boolean get() = netBalance < -0.009
    val isSettled: Boolean get() = !isReceivable && !isPayable
}
