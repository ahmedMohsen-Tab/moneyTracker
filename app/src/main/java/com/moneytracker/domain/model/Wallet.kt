package com.moneytracker.domain.model

/**
 * Wallet types a transaction can be paid from or received into.
 * Credit Card has been removed; existing rows whose stored `wallet` reads
 * "Credit Card" are silently re-bucketed to CASH by the migration in
 * `MIGRATION_3_4` and by the fallback in [fromName].
 */
enum class Wallet(val displayName: String) {
    CASH("Cash"),
    BANK("Bank");

    companion object {
        fun fromName(value: String?): Wallet =
            entries.firstOrNull { it.displayName.equals(value, ignoreCase = true) || it.name == value }
                ?: CASH
    }
}
