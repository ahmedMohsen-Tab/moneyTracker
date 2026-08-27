/**
 * Read-only text field that opens a Material 3 TimePicker dialog on tap.
 * Same hoisting pattern as [DatePickerField].
 */
package com.moneytracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalTime

@Composable
fun TimePickerField(
    time: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    OutlinedTextField(
        value = time.toString(),
        onValueChange = {},
        readOnly = true,
        label = { Text("Time") },
        trailingIcon = {
            IconButton(onClick = {
                android.app.TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        onTimeSelected(LocalTime.of(hour, minute))
                    },
                    time.hour,
                    time.minute,
                    true
                ).show()
            }) {
                Icon(Icons.Default.AccessTime, contentDescription = "Select time")
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}
