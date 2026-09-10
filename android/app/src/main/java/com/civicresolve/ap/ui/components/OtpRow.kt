package com.civicresolve.ap.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SimpleOtpRow(
    values: List<String>,
    onChange: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEachIndexed { idx, v ->
            OutlinedOtpBox(value = v, onValueChange = { onChange(idx, it) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun OutlinedOtpBox(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= 1 && it.all { c -> c.isDigit() }) onValueChange(it.takeLast(1)) },
        textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
            .height(52.dp)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(0.dp),
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (value.isEmpty()) Text("", color = MaterialTheme.colorScheme.onSurfaceVariant)
                inner()
            }
        }
    )
}
