package com.civicresolve.ap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civicresolve.ap.data.model.Message
import com.civicresolve.ap.ui.utils.formatTime

@Composable
fun ChatBox(
    messages: List<Message>,
    currentUserId: String?,
    chattingWith: String,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
        ) {
            Text("Chat with $chattingWith", style = MaterialTheme.typography.titleSmall)
        }
        Divider(color = MaterialTheme.colorScheme.outline)
        LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (messages.isEmpty()) {
                item { Text("No messages yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(24.dp)) }
            } else {
                items(messages) { msg ->
                    val isOwn = msg.fromUser?.id == currentUserId
                    val senderName = if (isOwn) "You" else if (msg.fromUser?.isAdmin == true) "Admin" else msg.fromUser?.fullName ?: "User"
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .widthIn(max = 280.dp)
                        ) {
                            Text(msg.message ?: "", color = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "$senderName • ${formatTime(msg.createdAt)}${if (isOwn) if (msg.hasSeen == true) " • Seen" else " • Sent" else ""}",
                                fontSize = 10.sp,
                                color = if (isOwn) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline)
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type your message", fontSize = 13.sp) },
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { if (input.isNotBlank()) { onSendMessage(input.trim()); input = "" } }, enabled = input.isNotBlank()) {
                Text("Send")
            }
        }
    }
}
