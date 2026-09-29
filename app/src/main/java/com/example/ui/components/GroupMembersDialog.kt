package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.ChatEntity
import com.example.data.local.GroupMemberWithUser
import com.example.data.local.UserEntity
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.NexusGold
import com.example.ui.theme.NexusPrimary
import com.example.ui.theme.NexusViolet
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealPrimary

@Composable
fun GroupInfoDialog(
    chat: ChatEntity,
    members: List<GroupMemberWithUser>,
    currentUser: UserEntity?,
    onDismiss: () -> Unit,
    onInspectMember: (UserEntity) -> Unit,
    onAddMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onChangeRole: (String, String) -> Unit,
    onLoadDirectoryUsers: suspend () -> List<UserEntity>
) {
    val currentUsername = currentUser?.username ?: ""
    val currentUserRole = members.firstOrNull { it.member.username.equals(currentUsername, ignoreCase = true) }?.member?.role
    val canManage = currentUserRole == "OWNER" || currentUserRole == "ADMIN" || currentUsername.equals("Eliel_21", ignoreCase = true)
    val isDark = LocalIsDarkTheme.current

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var availableUsers by remember { mutableStateOf<List<UserEntity>>(emptyList()) }

    LaunchedEffect(showAddMemberDialog) {
        if (showAddMemberDialog) {
            val all = onLoadDirectoryUsers()
            val existingUsernames = members.map { it.member.username.lowercase() }.toSet()
            availableUsers = all.filter { it.username.lowercase() !in existingUsernames }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(TealPrimary, TealAccent, NexusViolet)
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .testTag("group_info_dialog"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INFORMACIÓN DEL GRUPO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary,
                        letterSpacing = 1.sp
                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Group Identity
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TelegramAvatar(
                        imageUrl = chat.avatarUrl,
                        name = chat.title,
                        size = 54.dp,
                        isGroup = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chat.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${members.size} miembro${if (members.size > 1) "s" else ""}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!chat.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = chat.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))

                // Members Header + Add Member Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Miembros del Grupo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (canManage) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = TealPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { showAddMemberDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Añadir", fontSize = 11.sp, color = TealPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Member List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(members) { item ->
                        val user = item.user
                        val member = item.member
                        val isOwner = member.role == "OWNER" || member.username.equals("Eliel_21", ignoreCase = true)
                        val isAdmin = member.role == "ADMIN"
                        val isSelf = member.username.equals(currentUsername, ignoreCase = true)

                        var menuOpen by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (user != null) onInspectMember(user)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TelegramAvatar(
                                    imageUrl = user?.avatarUrl,
                                    name = user?.displayName ?: member.username,
                                    size = 38.dp,
                                    showOnlineDot = (user?.isCurrentUser == true)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = (user?.displayName ?: member.username) + if (isSelf) " (Tú)" else "",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (isOwner) {
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = NexusGold.copy(alpha = 0.15f),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, NexusGold.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "creador",
                                                    color = if (isDark) NexusGold else Color(0xFFB45309),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
                                                )
                                            }
                                        } else if (isAdmin) {
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = TealPrimary.copy(alpha = 0.15f),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, TealPrimary.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "admin",
                                                    color = TealPrimary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "@${member.username}",
                                        fontSize = 11.sp,
                                        color = TealPrimary
                                    )
                                }

                                if (canManage && !isOwner && !isSelf) {
                                    Box {
                                        IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Opciones",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = menuOpen,
                                            onDismissRequest = { menuOpen = false }
                                        ) {
                                            if (!isAdmin) {
                                                DropdownMenuItem(
                                                    text = { Text("Promover a Administrador") },
                                                    onClick = {
                                                        menuOpen = false
                                                        onChangeRole(member.username, "ADMIN")
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) }
                                                )
                                            } else {
                                                DropdownMenuItem(
                                                    text = { Text("Degradar a Miembro") },
                                                    onClick = {
                                                        menuOpen = false
                                                        onChangeRole(member.username, "MEMBER")
                                                    }
                                                )
                                            }

                                            DropdownMenuItem(
                                                text = { Text("Expulsar del grupo", color = ErrorRed) },
                                                onClick = {
                                                    menuOpen = false
                                                    onRemoveMember(member.username)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Member Sub-dialog
    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = {
                Text("Añadir Miembro", fontWeight = FontWeight.Bold)
            },
            text = {
                if (availableUsers.isEmpty()) {
                    Text(
                        "Todos los usuarios registrados en ChatPro ya forman parte de este grupo.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(availableUsers) { u ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        onAddMember(u.username)
                                        showAddMemberDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TelegramAvatar(imageUrl = u.avatarUrl, name = u.displayName, size = 32.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(u.displayName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("@${u.username}", fontSize = 11.sp, color = TealPrimary)
                                    }
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
