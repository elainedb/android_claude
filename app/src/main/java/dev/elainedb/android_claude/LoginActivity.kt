package dev.elainedb.android_claude

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import dev.elainedb.android_claude.utils.ConfigHelper

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInButton: Button
    private lateinit var errorTextView: TextView
    private lateinit var authorizedEmails: List<String>

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loadConfiguration()
        initializeViews()
        configureGoogleSignIn()
        setupClickListeners()
    }

    private fun loadConfiguration() {
        authorizedEmails = ConfigHelper.getAuthorizedEmails(this)
        Log.d(TAG, "Loaded configuration with ${authorizedEmails.size} authorized emails")
    }

    private fun initializeViews() {
        signInButton = findViewById(R.id.sign_in_button)
        errorTextView = findViewById(R.id.error_text_view)
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        signInButton.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)

        // Clear any previous error messages
        errorTextView.text = ""
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val email = account.email

            if (email != null && authorizedEmails.contains(email)) {
                Log.d(TAG, "Access granted to $email")
                navigateToMainActivity()
            } else {
                showError("Access denied. Your email is not authorized.")
                signOut()
            }
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
            showError("Sign-in failed. Please try again.")
        }
    }

    private fun showError(message: String) {
        errorTextView.text = message
    }

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener(this) {
            Log.d(TAG, "User signed out due to unauthorized access")
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}