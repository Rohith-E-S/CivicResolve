package com.example.complaintportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.complaintportal.data.model.Message
import com.example.complaintportal.ui.viewmodel.ComplaintViewModel
import com.example.complaintportal.ui.viewmodel.MessageViewModel
import androidx.compose.ui.res.stringResource
import com.example.complaintportal.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messageViewModel: MessageViewModel,
    complaintViewModel: ComplaintViewModel,
    complaintId: String,
    currentUserId: String,
    isAdmin: Boolean,
    onNavigateBack: () -> Unit
) {
    val messageState by messageViewModel.state.collectAsState()
    val complaintState by complaintViewModel.state.collectAsState()
    var messageText by remember { mutableStateOf("") }

    LaunchedEffect(complaintId) {
        complaintViewModel.fetchComplaint(complaintId, currentUserId)
        messageViewModel.connectSocket(complaintId)
        messageViewModel.loadMessages(complaintId)
    }

    DisposableEffect(Unit) {
        onDispose {
            messageViewModel.disconnectSocket()
        }
    }

    val receiverId = remember(complaintState.currentComplaint, isAdmin) {
        if (isAdmin) {
            // Admin replying to user: send to the complaint owner
            complaintState.currentComplaint?.user?.id ?: ""
        } else {
            // User messaging admin: backend will route to the district admin
            // Pass "admin" as a sentinel — backend ignores this and uses district lookup
            complaintState.currentComplaint?.user?.id ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.primary)
                    }
                },
                title = {
                    Column {
                        Text("Case #${complaintId.takeLast(6).uppercase()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primaryContainer)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                            Text(if (isAdmin) "Citizen Online" else "Support Agent Online", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(R.string.info))
                    }
                }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text(stringResource(R.string.message_support), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.SentimentSatisfied, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable(enabled = messageText.isNotBlank()) {
                                messageViewModel.sendMessage(complaintId, receiverId, messageText)
                                messageText = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Send, contentDescription = stringResource(R.string.send), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                reverseLayout = true
            ) {
                items(messageState.messages.reversed()) { msg ->
                    MessageItem(msg, msg.fromUser?.id == currentUserId)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: Message, isMine: Boolean) {
    val alignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val shape = if (isMine) RoundedCornerShape(24.dp, 24.dp, 4.dp, 24.dp) else RoundedCornerShape(24.dp, 24.dp, 24.dp, 4.dp)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!isMine) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                Box(
                    modifier = Modifier
                        .clip(shape)
                        .background(bgColor)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(text = message.message ?: "", color = textColor, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.just_now), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (isMine) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}
