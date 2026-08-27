/**
 * Wrapper for strings that may be either a literal or a `stringResource`
 * reference. Lets ViewModels surface user-visible messages without
 * importing Android `Context`.
 */
package com.moneytracker.ui.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

sealed class UiText {
    data class Literal(val value: String) : UiText()
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText()

    fun resolve(context: Context): String = when (this) {
        is Literal -> value
        is Resource -> if (args.isEmpty()) context.getString(id)
                       else context.getString(id, *args.toTypedArray())
    }

    @Composable
    fun asString(): String {
        val context = LocalContext.current
        return resolve(context)
    }
}