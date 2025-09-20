package dev.elainedb.android_claude

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dev.elainedb.android_claude.ui.VideoListScreen
import dev.elainedb.android_claude.ui.theme.AndroidClaudeTheme
import dev.elainedb.android_claude.viewmodel.VideoListViewModel

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            AndroidClaudeTheme {
                MainContent(
                    onLogout = { performLogout() }
                )
            }
        }
    }

    private fun performLogout() {
        googleSignInClient.signOut()
            .addOnCompleteListener(this) {
                Log.d(TAG, "User signed out successfully")
                navigateToLogin()
            }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

@Composable
fun MainContent(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { VideoListViewModel(context) }

    VideoListScreen(
        viewModel = viewModel,
        onLogout = onLogout,
        onViewMap = {
            val mapIntent = MapActivity.newIntent(context)
            context.startActivity(mapIntent)
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    AndroidClaudeTheme {
        MainContent(onLogout = {})
    }
}