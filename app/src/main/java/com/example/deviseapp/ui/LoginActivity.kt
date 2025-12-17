package com.example.deviseapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.deviseapp.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : ComponentActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var errorText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var googleSignInButton: Button

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            setLoading(false)
            showError("Erreur Google Sign-In: ${e.message}")
            Log.w(TAG, "Google sign in failed", e)
        }
    }

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Check if user is already logged in
        if (auth.currentUser != null) {
            goToMainActivity()
            return
        }

        // Initialize views
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        errorText = findViewById(R.id.errorText)
        progressBar = findViewById(R.id.progressBar)
        loginButton = findViewById(R.id.loginButton)
        registerButton = findViewById(R.id.registerButton)
        googleSignInButton = findViewById(R.id.googleSignInButton)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            
            if (validateInputs(email, password)) {
                loginUser(email, password)
            }
        }

        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            
            if (validateInputs(email, password)) {
                registerUser(email, password)
            }
        }

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        setLoading(true)
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    goToMainActivity()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    showError("Erreur d'authentification: ${task.exception?.message}")
                }
            }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        errorText.visibility = View.GONE
        
        if (email.isEmpty()) {
            showError("Veuillez entrer votre email")
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Email invalide")
            return false
        }
        
        if (password.isEmpty()) {
            showError("Veuillez entrer votre mot de passe")
            return false
        }
        
        if (password.length < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères")
            return false
        }
        
        return true
    }

    private fun loginUser(email: String, password: String) {
        setLoading(true)
        
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                
                if (task.isSuccessful) {
                    goToMainActivity()
                } else {
                    showError("Erreur de connexion: ${task.exception?.message}")
                }
            }
    }

    private fun registerUser(email: String, password: String) {
        setLoading(true)
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                
                if (task.isSuccessful) {
                    goToMainActivity()
                } else {
                    showError("Erreur d'inscription: ${task.exception?.message}")
                }
            }
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        loginButton.isEnabled = !loading
        registerButton.isEnabled = !loading
        emailInput.isEnabled = !loading
        passwordInput.isEnabled = !loading
    }
}

