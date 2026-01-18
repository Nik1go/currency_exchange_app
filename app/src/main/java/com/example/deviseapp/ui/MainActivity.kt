package com.example.deviseapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.FragmentActivity
import com.example.deviseapp.BuildConfig
import com.example.deviseapp.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * Activité principale de l'application
 * Gère la conversion de devises et la carte des bureaux de change
 */
class MainActivity : FragmentActivity(), OnMapReadyCallback {
    // ViewModels
    private val viewModel: MainViewModel by viewModels()
    private val mapViewModel: MapViewModel by viewModels()
    
    // Services Firebase et Google
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    
    // Carte Google Maps
    private var googleMap: GoogleMap? = null
    private lateinit var mapLoadingOverlay: LinearLayout
    private lateinit var searchInAreaButton: ExtendedFloatingActionButton
    private lateinit var mapContainer: FrameLayout
    
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

    // Catalogue de toutes les devises supportées (Frankfurter API)
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

    // Gestion des permissions de localisation
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                mapViewModel.setLocationPermissionGranted(true)
                enableUserLocation()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                mapViewModel.setLocationPermissionGranted(true)
                enableUserLocation()
            }
            else -> {
                mapViewModel.setLocationPermissionGranted(false)
                mapViewModel.setError("Permission de localisation requise")
                Toast.makeText(this, "La localisation est requise pour afficher les bureaux de change", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation de Firebase Auth
        auth = Firebase.auth

        // Initialisation de l'API Google Places
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
        placesClient = Places.createClient(this)
        
        // Initialisation du client de localisation
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Initialisation des éléments de la carte
        mapLoadingOverlay = findViewById(R.id.mapLoadingOverlay)
        searchInAreaButton = findViewById(R.id.searchInAreaButton)

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
        
        // MapContainer pour afficher/cacher la carte
        mapContainer = findViewById(R.id.mapContainer)
        
        // Bouton i18n (toggle FR/EN)
        val i18nButton: Button = findViewById(R.id.i18nButton)
        i18nButton.setOnClickListener {
            isEnglish = !isEnglish
            updateLanguage(i18nButton)
        }
        
        // Bouton pour afficher la carte
        val showMapButton: Button = findViewById(R.id.showMapButton)
        showMapButton.setOnClickListener {
            mapContainer.visibility = View.VISIBLE
            requestLocationPermission()
        }
        
        // Bouton pour fermer la carte
        val closeMapButton: Button = findViewById(R.id.closeMapButton)
        closeMapButton.setOnClickListener {
            mapContainer.visibility = View.GONE
        }
        
        // Initialisation de la carte Google Maps
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

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
        val closeMapButton: Button = findViewById(R.id.closeMapButton)
        
        if (isEnglish) {
            i18nButton.text = "🌐 FR"
            labelSource.text = "Source currency"
            labelTarget.text = "Target currency"
            historyButton.text = "View history"
            showMapButton.text = "Exchange offices nearby"
            logoutButton.text = "Logout"
            closeMapButton.text = "✕ Close"
        } else {
            i18nButton.text = "🌐 EN"
            labelSource.text = "Devise source"
            labelTarget.text = "Devise cible"
            historyButton.text = "Voir l'historique"
            showMapButton.text = "Bureaux de change à proximité"
            logoutButton.text = "Déconnexion"
            closeMapButton.text = "✕ Fermer"
        }
    }
    
    /**
     * Configure le convertisseur de devises
     * Initialise les champs de saisie et les observateurs
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
            // Swap the selected currencies
            val tempCode = selectedFromCode
            selectedFromCode = selectedToCode
            selectedToCode = tempCode
            
            // Swap the amounts
            val tempAmount = inputAmountFrom.text?.toString() ?: ""
            inputAmountFrom.setText(inputAmountTo.text?.toString() ?: "")
            inputAmountTo.setText(tempAmount)
            
            // Update the currency dropdowns
            currencyCatalog[selectedFromCode]?.let { from ->
                currencyFromInput.setText("${from.flag} ${from.code} - ${from.name}", false)
            }
            currencyCatalog[selectedToCode]?.let { to ->
                currencyToInput.setText("${to.flag} ${to.code} - ${to.name}", false)
            }
            
            // Trigger conversion
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
    
    // ========== MÉTHODES CARTE ET LOCALISATION ==========
    
    /**
     * Demande les permissions de localisation à l'utilisateur
     */
    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                mapViewModel.setLocationPermissionGranted(true)
                enableUserLocation()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
    
    /**
     * Callback appelé quand la carte Google Maps est prête
     * Configure l'interface et les interactions de la carte
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Configuration de l'interface de la carte
        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isCompassEnabled = true
            uiSettings.isMyLocationButtonEnabled = true
            
            // Adapter personnalisé pour l'info window des marqueurs
            setInfoWindowAdapter(CustomInfoWindowAdapter())
            
            // Affiche le bouton "Chercher dans cette zone" quand la caméra bouge
            setOnCameraIdleListener {
                if (mapViewModel.userLocation.value != null) {
                    searchInAreaButton.visibility = View.VISIBLE
                }
            }
        }
        
        // Clic sur le bouton "Chercher dans cette zone"
        searchInAreaButton.setOnClickListener {
            googleMap?.cameraPosition?.target?.let { centerLocation ->
                searchInAreaButton.visibility = View.GONE
                searchNearbyExchangeOffices(centerLocation)
            }
        }
        
        if (mapViewModel.locationPermissionGranted.value == true) {
            enableUserLocation()
        }
    }
    
    /**
     * Adapter personnalisé pour afficher les infos des bureaux de change
     */
    inner class CustomInfoWindowAdapter : GoogleMap.InfoWindowAdapter {
        private val view = layoutInflater.inflate(R.layout.custom_info_window, null)
        
        override fun getInfoWindow(marker: com.google.android.gms.maps.model.Marker): View {
            val office = marker.tag as? ExchangeOffice
            if (office != null) {
                view.findViewById<TextView>(R.id.infoWindowTitle).text = office.name
                view.findViewById<TextView>(R.id.infoWindowAddress).text = office.address
                view.findViewById<TextView>(R.id.infoWindowDistance).text = 
                    "Distance: ${String.format("%.2f", office.distance)} km"
            }
            return view
        }
        
        override fun getInfoContents(marker: com.google.android.gms.maps.model.Marker): View? {
            return null
        }
    }
    
    /**
     * Active la localisation de l'utilisateur sur la carte
     * Récupère et affiche la position actuelle
     */
    private fun enableUserLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        googleMap?.isMyLocationEnabled = true
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                mapViewModel.setUserLocation(userLatLng)
                
                // Déplace la caméra vers la position de l'utilisateur
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(userLatLng, 14f)
                )
                
                // Recherche les bureaux de change à proximité
                searchNearbyExchangeOffices(userLatLng)
            } else {
                Toast.makeText(this, "Impossible de récupérer votre position", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            mapViewModel.setError("Erreur de localisation: ${it.message}")
            Toast.makeText(this, "Erreur: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Recherche les bureaux de change à proximité d'une position donnée
     * Utilise l'API Google Places Nearby Search (rayon de 5km)
     */
    private fun searchNearbyExchangeOffices(userLocation: LatLng) {
        mapLoadingOverlay.visibility = View.VISIBLE
        mapViewModel.setLoading(true)
        
        // Paramètres de recherche
        val radius = 5000 // Rayon de 5km
        val keyword = "bureau de change"
        val apiKey = BuildConfig.MAPS_API_KEY
        
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${userLocation.latitude},${userLocation.longitude}" +
                "&radius=$radius" +
                "&keyword=$keyword" +
                "&key=$apiKey"
        
        // Requête HTTP avec OkHttp
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder()
            .url(url)
            .build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    mapViewModel.setError("Erreur de recherche: ${e.message}")
                    mapLoadingOverlay.visibility = View.GONE
                    mapViewModel.setLoading(false)
                    Toast.makeText(
                        this@MainActivity,
                        "Erreur: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val jsonData = response.body?.string()
                runOnUiThread {
                    try {
                        val exchangeOffices = mutableListOf<ExchangeOffice>()
                        
                        if (jsonData != null) {
                            val json = org.json.JSONObject(jsonData)
                            
                            // Log pour debug
                            val status = json.getString("status")
                            android.util.Log.d("PlacesAPI", "Status: $status")
                            android.util.Log.d("PlacesAPI", "Réponse complète: $jsonData")
                            
                            // Vérification si la requête est refusée
                            if (status == "REQUEST_DENIED") {
                                val errorMessage = json.optString("error_message", "Pas de détails")
                                Toast.makeText(
                                    this@MainActivity,
                                    "API refusée: $errorMessage",
                                    Toast.LENGTH_LONG
                                ).show()
                                mapLoadingOverlay.visibility = View.GONE
                                mapViewModel.setLoading(false)
                                return@runOnUiThread
                            }
                            
                            // Parsing des résultats (limité à 20)
                            val results = json.getJSONArray("results")
                            android.util.Log.d("PlacesAPI", "Résultats trouvés: ${results.length()}")
                            
                            for (i in 0 until minOf(results.length(), 20)) {
                                val place = results.getJSONObject(i)
                                val name = place.getString("name")
                                val location = place.getJSONObject("geometry").getJSONObject("location")
                                val lat = location.getDouble("lat")
                                val lng = location.getDouble("lng")
                                val address = place.optString("vicinity", "Adresse non disponible")
                                
                                val placeLatLng = LatLng(lat, lng)
                                val office = ExchangeOffice(
                                    id = place.optString("place_id", ""),
                                    name = name,
                                    address = address,
                                    location = placeLatLng,
                                    distance = mapViewModel.calculateDistance(
                                        ExchangeOffice("", "", "", placeLatLng),
                                        userLocation
                                    )
                                )
                                exchangeOffices.add(office)
                            }
                        }
                        
                        // Tri par distance et affichage
                        val sortedOffices = exchangeOffices.sortedBy { it.distance }
                        mapViewModel.setExchangeOffices(sortedOffices)
                        displayExchangeOfficesOnMap(sortedOffices)
                        
                        mapLoadingOverlay.visibility = View.GONE
                        mapViewModel.setLoading(false)
                        
                        if (sortedOffices.isEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Aucun bureau de change trouvé dans un rayon de 5km",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "${sortedOffices.size} bureau(x) trouvé(s)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PlacesAPI", "Erreur parsing: ${e.message}", e)
                        mapLoadingOverlay.visibility = View.GONE
                        mapViewModel.setLoading(false)
                        Toast.makeText(
                            this@MainActivity,
                            "Erreur: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }
    
    /**
     * Affiche les bureaux de change sur la carte avec des marqueurs
     */
    private fun displayExchangeOfficesOnMap(offices: List<ExchangeOffice>) {
        googleMap?.clear()
        
        for (office in offices) {
            val marker = googleMap?.addMarker(
                MarkerOptions()
                    .position(office.location)
                    .title(office.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            marker?.tag = office
        }
        
        // Ouvre l'itinéraire quand on clique sur l'info window
        googleMap?.setOnInfoWindowClickListener { marker ->
            val office = marker.tag as? ExchangeOffice
            if (office != null) {
                openGoogleMapsDirections(office)
            }
        }
    }
    
    /**
     * Ouvre Google Maps avec l'itinéraire vers le bureau de change
     * Utilise les transports en commun par défaut
     */
    private fun openGoogleMapsDirections(office: ExchangeOffice) {
        // Intent pour ouvrir Google Maps avec navigation
        val uri = "google.navigation:q=${office.location.latitude},${office.location.longitude}&mode=d"
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps")
        
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Si Google Maps n'est pas installé, ouvrir dans le navigateur
            val browserUri = "https://www.google.com/maps/dir/?api=1&destination=${office.location.latitude},${office.location.longitude}&travelmode=transit"
            val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(browserUri))
            startActivity(browserIntent)
        }
    }
}
