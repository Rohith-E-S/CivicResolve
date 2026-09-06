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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OtpInput(
    otp: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(6) { idx ->
            val char = otp.getOrNull(idx)?.toString() ?: ""
            val focus = remember { FocusRequester() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .focusRequester(focus),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = char,
                    onValueChange = { v ->
                        if (v.length <= 1 && v.all { it.isDigit() }) {
                            val newOtp = buildString {
                                for (i in 0 until 6) {
                                    when {
                                        i == idx -> append(v)
                                        i < otp.length -> append(otp[i])
                                        else -> append("")
                                    }
                                }
                            }.take(6)
                            // simple: replace idx position
                            val arr = otp.toMutableList()
                            while (arr.size < 6) arr.add(' ')
                            // rebuild
                            val chars = MutableList(6) { i -> otp.getOrNull(i)?.toString() ?: "" }
                            chars[idx] = v.takeLast(1)
                            onOtpChange(chars.joinToString("").trim())
                        }
                    },
                    textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center, fontSize = 20.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (char.isEmpty()) {
                    // placeholder handled by empty
                }
            }
        }
    }
}

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
