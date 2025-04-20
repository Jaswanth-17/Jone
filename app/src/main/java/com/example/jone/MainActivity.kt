package com.example.jone

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.jone.ui.theme.JoneTheme
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Google Sign-In with Gmail read access
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope("https://www.googleapis.com/auth/gmail.readonly"))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            JoneTheme {
                val viewModel: MainViewModel = viewModel()
                var user by remember { mutableStateOf<GoogleSignInAccount?>(null) }

                val emailSubjects by viewModel.subjects.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        user = account
                        account?.let { viewModel.fetchGmailSubjects(it) }
                    } catch (e: ApiException) {
                        Log.e("SIGN_IN", "Sign in failed", e)
                    }
                }

                // Auto login if user already signed in
                LaunchedEffect(Unit) {
                    GoogleSignIn.getLastSignedInAccount(this@MainActivity)?.let {
                        user = it
                        viewModel.fetchGmailSubjects(it)
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Jone - Gmail Viewer") },
                            actions = {
                                if (user != null) {
                                    IconButton(onClick = {
                                        googleSignInClient.signOut().addOnCompleteListener {
                                            user = null
                                            viewModel.clearState()
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.Logout,
                                            contentDescription = "Sign Out"
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (user == null) {
                                // Sign-in UI
                                Spacer(modifier = Modifier.height(64.dp))
                                Text("Welcome to Jone", style = MaterialTheme.typography.headlineMedium)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "View your 5 most recent Gmail messages in style!",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Button(
                                    onClick = {
                                        val signInIntent = googleSignInClient.signInIntent
                                        launcher.launch(signInIntent)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Sign in with Google")
                                }
                            } else {
                                // User Info
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(user?.photoUrl),
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier
                                            .size(60.dp)
                                            .padding(end = 12.dp)
                                    )
                                    Column {
                                        Text(
                                            "Welcome, ${user?.displayName}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            user?.email ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Recent Emails", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))

                                val formatter = remember {
                                    SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                                }

                                SwipeRefresh(
                                    state = rememberSwipeRefreshState(isRefreshing = isLoading),
                                    onRefresh = { user?.let { viewModel.fetchGmailSubjects(it) } }
                                ) {
                                    LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                                        items(emailSubjects) { email ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        text = email.subject,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        overflow = TextOverflow.Ellipsis,
                                                        maxLines = 1
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    if (email.dateMillis > 0) {
                                                        Text(
                                                            text = formatter.format(Date(email.dateMillis)),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.outline
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Error Message
                                if (errorMessage != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        color = MaterialTheme.colorScheme.error
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
