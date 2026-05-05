package pt.ua.EventManager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ua.EventManager.data.Event
import pt.ua.EventManager.ui.viewmodels.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantsListScreen(
    event: Event?,
    onBack: () -> Unit,
    userViewModel: UserViewModel
) {
    if (event == null) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Participants", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Verifica se a lista de IDs está vazia
            if (event.participantsUids.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No participants yet.", color = Color.Gray)
                    }
                }
            }

            items(event.participantsUids) { uid ->
                ParticipantRow(
                    uid = uid,
                    // Verifica se o UID está na lista de confirmados
                    isPresent = event.checkedInUids.contains(uid),
                    userViewModel = userViewModel
                )
            }
        }
    }
}

@Composable
fun ParticipantRow(uid: String, isPresent: Boolean, userViewModel: UserViewModel) {
    // Estado local para armazenar o nome recuperado do ViewModel
    var userName by remember { mutableStateOf("Loading...") }

    // Dispara a busca do nome quando o componente é montado ou o UID muda
    LaunchedEffect(uid) {
        userViewModel.getUserName(uid) { name ->
            userName = name ?: "Unknown User"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Usa o seu componente CommonAvatar com a inicial do nome
            CommonAvatar(
                text = if (userName != "Loading...") userName.take(1).uppercase() else "?",
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = userName,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isPresent) {
                // Badge de presença com a cor verde
                Surface(
                    color = Color(0xFF10B981),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Present",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    "Pending",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
