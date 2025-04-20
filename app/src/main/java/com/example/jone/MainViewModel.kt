package com.example.jone

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ✅ NEW: Holds subject + timestamp
data class EmailData(
    val subject: String,
    val dateMillis: Long
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _subjects = MutableStateFlow<List<EmailData>>(emptyList())
    val subjects = _subjects.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun fetchGmailSubjects(account: GoogleSignInAccount) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val token = withContext(Dispatchers.IO) {
                    account.account?.let {
                        GoogleAuthUtil.getToken(
                            getApplication(),
                            it,
                            "oauth2:https://www.googleapis.com/auth/gmail.readonly"
                        )
                    }
                }

                val fetchedSubjects = token?.let { fetchSubjectsFromGmailApi(it) } ?: emptyList()
                _subjects.value = fetchedSubjects

            } catch (e: Exception) {
                Log.e("ViewModel", "Error fetching token or emails", e)
                _errorMessage.value = "Failed to fetch emails"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ UPDATED: Returns subject + timestamp
    private suspend fun fetchSubjectsFromGmailApi(token: String): List<EmailData> = withContext(Dispatchers.IO) {
        val subjects = mutableListOf<EmailData>()
        try {
            val listUrl = URL("https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=5")
            val listConn = listUrl.openConnection() as HttpURLConnection
            listConn.setRequestProperty("Authorization", "Bearer $token")
            listConn.connect()

            val listResponse = listConn.inputStream.bufferedReader().readText()
            val messageIds = JSONObject(listResponse).optJSONArray("messages") ?: return@withContext subjects

            for (i in 0 until messageIds.length()) {
                val messageId = messageIds.getJSONObject(i).getString("id")
                val messageUrl = URL("https://gmail.googleapis.com/gmail/v1/users/me/messages/$messageId?format=metadata&metadataHeaders=Subject")
                val messageConn = messageUrl.openConnection() as HttpURLConnection
                messageConn.setRequestProperty("Authorization", "Bearer $token")
                messageConn.connect()

                val messageResponse = messageConn.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(messageResponse)
                val payload = jsonResponse.getJSONObject("payload")
                val headers = payload.getJSONArray("headers")
                val internalDate = jsonResponse.optLong("internalDate", 0L)

                var subjectFound = false
                for (j in 0 until headers.length()) {
                    val header = headers.getJSONObject(j)
                    if (header.getString("name") == "Subject") {
                        val subject = header.getString("value")
                        subjects.add(EmailData(subject, internalDate))
                        subjectFound = true
                        break
                    }
                }

                if (!subjectFound) {
                    subjects.add(EmailData("(No Subject)", internalDate))
                }
            }
        } catch (e: Exception) {
            Log.e("ViewModel", "Error during Gmail API call", e)
        }

        return@withContext subjects
    }

    fun clearState() {
        _subjects.value = emptyList()
        _isLoading.value = false
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
