package com.example.deviseapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.FragmentActivity
import com.example.deviseapp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * Activité principale de l'application
 * Gère la conversion de devises
 */
class MainActivity : FragmentActivity() {
    // ViewModel
    private val viewModel: MainViewModel by viewModels()
    
    // Firebase Auth
    private lateinit var auth: FirebaseAuth
    
    // i18n state
    private var isEnglish = false
    
    // Gestion des changements de texte pour éviter les boucles infinies
    private var suppressFromChange = false
    private var suppressToChange = false
    
    // Devises sélectionnées par défaut
    private var selectedFromCode = "EUR"
    private var selectedToCode = "USD"
    
    // Liste des devises disponibles
    private var currencyDisplays: List<CurrencyDisplay> = emptyList()
    private var currencyAdapter: CurrencyAdapter? = null

    // Catalogue de toutes les devises supportées
    private val currencyCatalog = mapOf(
        "EUR" to CurrencyDisplay("EUR", "Euro", "🇪🇺"),
        "USD" to CurrencyDisplay("USD", "Dollar américain", "🇺🇸"),
        "GBP" to CurrencyDisplay("GBP", "Livre sterling", "🇬🇧"),
        "CHF" to CurrencyDisplay("CHF", "Franc suisse", "🇨🇭"),
        "CAD" to CurrencyDisplay("CAD", "Dollar canadien", "🇨🇦"),
        "AUD" to CurrencyDisplay("AUD", "Dollar australien", "🇦🇺"),
        "JPY" to CurrencyDisplay("JPY", "Yen japonais", "🇯🇵"),
        "CNY" to CurrencyDisplay("CNY", "Yuan chinois", "🇨🇳"),
        "BRL" to CurrencyDisplay("BRL", "Real brésilien", "🇧🇷"),
        "NOK" to CurrencyDisplay("NOK", "Couronne norvégienne", "🇳🇴"),
        "SEK" to CurrencyDisplay("SEK", "Couronne suédoise", "🇸🇪"),
        "DKK" to CurrencyDisplay("DKK", "Couronne danoise", "🇩🇰"),
        "THB" to CurrencyDisplay("THB", "Baht thaïlandais", "🇹🇭"),
        "INR" to CurrencyDisplay("INR", "Roupie indienne", "🇮🇳"),
        "KRW" to CurrencyDisplay("KRW", "Won sud-coréen", "🇰🇷"),
        "MXN" to CurrencyDisplay("MXN", "Peso mexicain", "🇲🇽"),
        "SGD" to CurrencyDisplay("SGD", "Dollar singapourien", "🇸🇬"),
        "HKD" to CurrencyDisplay("HKD", "Dollar hongkongais", "🇭🇰"),
        "NZD" to CurrencyDisplay("NZD", "Dollar néo-zélandais", "🇳🇿"),
        "ZAR" to CurrencyDisplay("ZAR", "Rand sud-africain", "🇿🇦"),
        "TRY" to CurrencyDisplay("TRY", "Livre turque", "🇹🇷"),
        "PLN" to CurrencyDisplay("PLN", "Złoty polonais", "🇵🇱"),
        "CZK" to CurrencyDisplay("CZK", "Couronne tchèque", "🇨🇿"),
        "HUF" to CurrencyDisplay("HUF", "Forint hongrois", "🇭🇺"),
        "RON" to CurrencyDisplay("RON", "Leu roumain", "🇷🇴"),
        "ILS" to CurrencyDisplay("ILS", "Shekel israélien", "🇮🇱"),
        "PHP" to CurrencyDisplay("PHP", "Peso philippin", "🇵🇭"),
        "MYR" to CurrencyDisplay("MYR", "Ringgit malaisien", "🇲🇾"),
        "IDR" to CurrencyDisplay("IDR", "Roupie indonésienne", "🇮🇩"),
        "ISK" to CurrencyDisplay("ISK", "Couronne islandaise", "🇮🇸")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation de Firebase Auth
        auth = Firebase.auth

        // Vérification si l'utilisateur est connecté
        val currentUser = auth.currentUser
        if (currentUser == null) {
            goToLoginActivity()
            return
        }

        // Affichage de l'email de l'utilisateur
        val userEmailText: TextView = findViewById(R.id.userEmail)
        userEmailText.text = currentUser.email ?: "Utilisateur"

        // Bouton de déconnexion
        val logoutButton: Button = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            auth.signOut()
            goToLoginActivity()
        }
        
        // Bouton i18n (toggle FR/EN)
        val i18nButton: Button = findViewById(R.id.i18nButton)
        i18nButton.setOnClickListener {
            isEnglish = !isEnglish
            updateLanguage(i18nButton)
        }
        
        // Bouton pour afficher la carte (lance MapActivity)
        val showMapButton: Button = findViewById(R.id.showMapButton)
        showMapButton.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        setupCurrencyConverter()
    }
    
    /**
     * Met à jour les textes selon la langue sélectionnée
     */
    private fun updateLanguage(i18nButton: Button) {
        val labelSource: TextView = findViewById(R.id.labelSource)
        val labelTarget: TextView = findViewById(R.id.labelTarget)
        val historyButton: Button = findViewById(R.id.historyButton)
        val showMapButton: Button = findViewById(R.id.showMapButton)
        val logoutButton: Button = findViewById(R.id.logoutButton)
        
        if (isEnglish) {
            i18nButton.text = "🌐 FR"
            labelSource.text = "Source currency"
            labelTarget.text = "Target currency"
            historyButton.text = "View history"
            showMapButton.text = "Exchange offices nearby"
            logoutButton.text = "Logout"
        } else {
            i18nButton.text = "🌐 EN"
            labelSource.text = "Devise source"
            labelTarget.text = "Devise cible"
            historyButton.text = "Voir l'historique"
            showMapButton.text = "Bureaux de change à proximité"
            logoutButton.text = "Déconnexion"
        }
    }
    
    /**
     * Configure le convertisseur de devises
     */
    private fun setupCurrencyConverter() {
        val inputAmountFrom: TextInputEditText = findViewById(R.id.inputAmountFrom)
        val inputAmountTo: TextInputEditText = findViewById(R.id.inputAmountTo)
        val currencyFromInput: AutoCompleteTextView = findViewById(R.id.inputCurrencyFrom)
        val currencyToInput: AutoCompleteTextView = findViewById(R.id.inputCurrencyTo)
        val textStatus: TextView = findViewById(R.id.textStatus)
        val historyButton: Button = findViewById(R.id.historyButton)
        val swapButton: Button = findViewById(R.id.swapButton)
        
        // Setup history button
        historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java).apply {
                putExtra(HistoryActivity.EXTRA_BASE_CURRENCY, selectedFromCode)
                putExtra(HistoryActivity.EXTRA_TARGET_CURRENCY, selectedToCode)
            }
            startActivity(intent)
        }
        
        // Setup swap button
        swapButton.setOnClickListener {
            val tempCode = selectedFromCode
            selectedFromCode = selectedToCode
            selectedToCode = tempCode
            
            val tempAmount = inputAmountFrom.text?.toString() ?: ""
            inputAmountFrom.setText(inputAmountTo.text?.toString() ?: "")
            inputAmountTo.setText(tempAmount)
            
            currencyCatalog[selectedFromCode]?.let { from ->
                currencyFromInput.setText("${from.flag} ${from.code} - ${from.name}", false)
            }
            currencyCatalog[selectedToCode]?.let { to ->
                currencyToInput.setText("${to.flag} ${to.code} - ${to.name}", false)
            }
            
            viewModel.onAmountChanged(
                MainViewModel.AmountField.FROM,
                inputAmountFrom.text?.toString() ?: "",
                selectedFromCode,
                selectedToCode
            )
        }

        // Observer les devises disponibles
        viewModel.currencies.observe(this) { codes ->
            currencyDisplays = codes.mapNotNull { currencyCatalog[it] }
            if (currencyDisplays.isEmpty()) return@observe
            currencyAdapter = CurrencyAdapter(this, currencyDisplays)
            currencyFromInput.setAdapter(currencyAdapter)
            currencyToInput.setAdapter(currencyAdapter)
            setDefaultCurrencySelections(currencyFromInput, currencyToInput)
        }

        // Changement de la devise source
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

        // Changement de la devise cible
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

        // Observer l'état de chargement
        viewModel.loading.observe(this) { loading ->
            if (loading) {
                textStatus.text = "Mise à jour des taux..."
            } else if (viewModel.error.value.isNullOrEmpty()) {
                textStatus.text = ""
            }
        }

        // Observer les erreurs
        viewModel.error.observe(this) { err ->
            if (err != null) {
                textStatus.text = "Erreur: $err"
            } else if (viewModel.loading.value != true) {
                textStatus.text = ""
            }
        }

        // Observer le montant source
        viewModel.fromAmount.observe(this) { value ->
            val current = inputAmountFrom.text.toString()
            if (value != current) {
                suppressFromChange = true
                inputAmountFrom.setText(value)
                inputAmountFrom.setSelection(value.length)
                suppressFromChange = false
            }
        }

        // Observer le montant cible
        viewModel.toAmount.observe(this) { value ->
            val current = inputAmountTo.text.toString()
            if (value != current) {
                suppressToChange = true
                inputAmountTo.setText(value)
                inputAmountTo.setSelection(value.length)
                suppressToChange = false
            }
        }

        // Écouter les changements de texte dans le champ source
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

        // Écouter les changements de texte dans le champ cible
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

    /**
     * Définit les devises par défaut dans les champs de sélection
     */
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

    /**
     * Trouve la position d'une devise dans la liste par son code
     */
    private fun findPositionByCode(code: String): Int {
        return currencyDisplays.indexOfFirst { it.code == code }.takeIf { it >= 0 } ?: 0
    }

    /**
     * Redirige vers l'écran de connexion
     */
    private fun goToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
