package com.moneytracker.domain.usecase

import com.moneytracker.domain.model.Transaction
import com.moneytracker.domain.model.Wallet
import javax.inject.Inject

/**
 * Aggregates a list of transactions into per-wallet balances plus a total.
 * Centralises the smart-cast logic that previously lived in DashboardViewModel
 * (where the second branch (`tx as Transaction.IncomeTransaction`) would
 * ClassCastException when reached from the ExpenseTransaction arm).
 */
class CalculateWalletBalancesUseCase @Inject constructor() {

    data class Balances(
        val total: Double,
        val cash: Double,
        val bank: Double
    )

    operator fun invoke(transactions: List<Transaction>): Balances {
        var cash = 0.0
        var bank = 0.0

        for (tx in transactions) {
            val wallet = when (tx) {
                is Transaction.ExpenseTransaction -> Wallet.fromName(tx.wallet)
                is Transaction.IncomeTransaction -> Wallet.fromName(tx.wallet)
            }
            // Expenses subtract from the wallet, income adds to it.
            val signedAmount = when (tx) {
                is Transaction.ExpenseTransaction -> -tx.amount
                is Transaction.IncomeTransaction -> tx.amount
            }
            when (wallet) {
                Wallet.CASH -> cash += signedAmount
                Wallet.BANK -> bank += signedAmount
                // Any unknown wallet label (e.g. legacy "Credit Card" rows)
                // falls through to CASH via Wallet.fromName.
            }
        }

        return Balances(
            total = cash + bank,
            cash = cash,
            bank = bank
        )
    }
}
