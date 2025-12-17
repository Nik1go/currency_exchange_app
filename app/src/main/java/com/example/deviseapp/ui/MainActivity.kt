package com.example.deviseapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.widget.doOnTextChanged
import com.example.deviseapp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var auth: FirebaseAuth
    private var suppressFromChange = false
    private var suppressToChange = false
    private var selectedFromCode = "EUR"
    private var selectedToCode = "USD"
    private var currencyDisplays: List<CurrencyDisplay> = emptyList()
    private var currencyAdapter: CurrencyAdapter? = null

    private val currencyCatalog = mapOf(
        "EUR" to CurrencyDisplay("EUR", "Euro", "🇪🇺"),
        "USD" to CurrencyDisplay("USD", "Dollar américain", "🇺🇸"),
        "GBP" to CurrencyDisplay("GBP", "Livre sterling", "🇬🇧"),
        "CHF" to CurrencyDisplay("CHF", "Franc suisse", "🇨🇭"),
        "CAD" to CurrencyDisplay("CAD", "Dollar canadien", "🇨🇦"),
        "DZD" to CurrencyDisplay("DZD", "Dinar algérien", "🇩🇿"),
        "TND" to CurrencyDisplay("TND", "Dinar tunisien", "🇹🇳"),
        "MAD" to CurrencyDisplay("MAD", "Dirham marocain", "🇲🇦"),
        "THB" to CurrencyDisplay("THB", "Baht thaïlandais", "🇹🇭"),
        "JPY" to CurrencyDisplay("JPY", "Yen japonais", "🇯🇵"),
        "AUD" to CurrencyDisplay("AUD", "Dollar australien", "🇦🇺"),
        "CNY" to CurrencyDisplay("CNY", "Yuan chinois", "🇨🇳"),
        "RUB" to CurrencyDisplay("RUB", "Ruble russe", "🇷🇺"),
        "BRL" to CurrencyDisplay("BRL", "Real Brésil", "🇧🇷"),


    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Check if user is logged in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            goToLoginActivity()
            return
        }

        // Display user email
        val userEmailText: TextView = findViewById(R.id.userEmail)
        userEmailText.text = currentUser.email ?: "Utilisateur"

        // Logout button
        val logoutButton: Button = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            auth.signOut()
            goToLoginActivity()
        }

        val inputAmountFrom: TextInputEditText = findViewById(R.id.inputAmountFrom)
        val inputAmountTo: TextInputEditText = findViewById(R.id.inputAmountTo)
        val currencyFromInput: AutoCompleteTextView = findViewById(R.id.inputCurrencyFrom)
        val currencyToInput: AutoCompleteTextView = findViewById(R.id.inputCurrencyTo)
        val textStatus: TextView = findViewById(R.id.textStatus)

        viewModel.currencies.observe(this) { codes ->
            currencyDisplays = codes.mapNotNull { currencyCatalog[it] }
            if (currencyDisplays.isEmpty()) return@observe
            currencyAdapter = CurrencyAdapter(this, currencyDisplays)
            currencyFromInput.setAdapter(currencyAdapter)
            currencyToInput.setAdapter(currencyAdapter)
            setDefaultCurrencySelections(currencyFromInput, currencyToInput)
        }

        currencyFromInput.setOnItemClickListener { _, _, position, _ ->
            val selected = currencyAdapter?.getItem(position) ?: return@setOnItemClickListener
            selectedFromCode = selected.code
            viewModel.onAmountChanged(
                MainViewModel.AmountField.FROM,
                inputAmountFrom.text?.toString() ?: "",
                selectedFromCode,
                selectedToCode
            )
        }

        currencyToInput.setOnItemClickListener { _, _, position, _ ->
            val selected = currencyAdapter?.getItem(position) ?: return@setOnItemClickListener
            selectedToCode = selected.code
            viewModel.onAmountChanged(
                MainViewModel.AmountField.FROM,
                inputAmountFrom.text?.toString() ?: "",
                selectedFromCode,
                selectedToCode
            )
        }

        viewModel.loading.observe(this) { loading ->
            if (loading) {
                textStatus.text = "Mise à jour des taux..."
            } else if (viewModel.error.value.isNullOrEmpty()) {
                textStatus.text = ""
            }
        }

        viewModel.error.observe(this) { err ->
            if (err != null) {
                textStatus.text = "Erreur: $err"
            } else if (viewModel.loading.value != true) {
                textStatus.text = ""
            }
        }

        viewModel.fromAmount.observe(this) { value ->
            val current = inputAmountFrom.text.toString()
            if (value != current) {
                suppressFromChange = true
                inputAmountFrom.setText(value)
                inputAmountFrom.setSelection(value.length)
                suppressFromChange = false
            }
        }

        viewModel.toAmount.observe(this) { value ->
            val current = inputAmountTo.text.toString()
            if (value != current) {
                suppressToChange = true
                inputAmountTo.setText(value)
                inputAmountTo.setSelection(value.length)
                suppressToChange = false
            }
        }

        inputAmountFrom.doOnTextChanged { text, _, _, _ ->
            if (suppressFromChange) return@doOnTextChanged
            if (currencyDisplays.isEmpty()) return@doOnTextChanged
            viewModel.onAmountChanged(
                MainViewModel.AmountField.FROM,
                text?.toString() ?: "",
                selectedFromCode,
                selectedToCode
            )
        }

        inputAmountTo.doOnTextChanged { text, _, _, _ ->
            if (suppressToChange) return@doOnTextChanged
            if (currencyDisplays.isEmpty()) return@doOnTextChanged
            viewModel.onAmountChanged(
                MainViewModel.AmountField.TO,
                text?.toString() ?: "",
                selectedFromCode,
                selectedToCode
            )
        }
    }

    private fun setDefaultCurrencySelections(
        currencyFromInput: AutoCompleteTextView,
        currencyToInput: AutoCompleteTextView
    ) {
        val fromPos = findPositionByCode(selectedFromCode)
        val toPos = findPositionByCode(selectedToCode)
        currencyAdapter?.getItem(fromPos)?.let { from ->
            currencyFromInput.setText("${from.flag} ${from.code} - ${from.name}", false)
            selectedFromCode = from.code
        }
        currencyAdapter?.getItem(toPos)?.let { to ->
            currencyToInput.setText("${to.flag} ${to.code} - ${to.name}", false)
            selectedToCode = to.code
        }
        viewModel.onAmountChanged(
            MainViewModel.AmountField.FROM,
            "1",
            selectedFromCode,
            selectedToCode
        )
        findViewById<TextInputEditText>(R.id.inputAmountFrom).setText("1")
    }

    private fun findPositionByCode(code: String): Int {
        return currencyDisplays.indexOfFirst { it.code == code }.takeIf { it >= 0 } ?: 0
    }

    private fun goToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

