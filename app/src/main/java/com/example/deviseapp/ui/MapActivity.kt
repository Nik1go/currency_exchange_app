package com.example.deviseapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

/**
 * Activité dédiée à l'affichage de la carte des bureaux de change
 */
class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private val mapViewModel: MapViewModel by viewModels()

    // Services Google
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient

    // Carte Google Maps
    private var googleMap: GoogleMap? = null
    private lateinit var mapLoadingOverlay: LinearLayout
    private lateinit var searchInAreaButton: ExtendedFloatingActionButton
    private lateinit var permissionView: LinearLayout

    // Gestion des permissions de localisation
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                mapViewModel.setLocationPermissionGranted(true)
                permissionView.visibility = View.GONE
                enableUserLocation()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                mapViewModel.setLocationPermissionGranted(true)
                permissionView.visibility = View.GONE
                enableUserLocation()
            }
            else -> {
                mapViewModel.setLocationPermissionGranted(false)
                permissionView.visibility = View.VISIBLE
                Toast.makeText(
                    this,
                    "La localisation est requise pour afficher les bureaux de change",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Initialisation de l'API Google Places
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
        placesClient = Places.createClient(this)

        // Initialisation du client de localisation
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupViews()
    }

    private fun setupViews() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        mapLoadingOverlay = findViewById(R.id.mapLoadingOverlay)
        searchInAreaButton = findViewById(R.id.searchInAreaButton)
        permissionView = findViewById(R.id.permissionView)

        // Setup toolbar
        toolbar.setNavigationOnClickListener { finish() }

        // Setup permission button
        findViewById<MaterialButton>(R.id.grantPermissionButton).setOnClickListener {
            requestLocationPermission()
        }

        // Initialisation de la carte Google Maps
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

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
                permissionView.visibility = View.GONE
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

        // Demander les permissions au lancement
        requestLocationPermission()
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
     */
    private fun searchNearbyExchangeOffices(userLocation: LatLng) {
        mapLoadingOverlay.visibility = View.VISIBLE
        mapViewModel.setLoading(true)

        val radius = 5000 // Rayon de 5km
        val keyword = "bureau de change"
        val apiKey = BuildConfig.MAPS_API_KEY

        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${userLocation.latitude},${userLocation.longitude}" +
                "&radius=$radius" +
                "&keyword=$keyword" +
                "&key=$apiKey"

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
                        this@MapActivity,
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

                            val status = json.getString("status")
                            android.util.Log.d("PlacesAPI", "Status: $status")

                            if (status == "REQUEST_DENIED") {
                                val errorMessage = json.optString("error_message", "Pas de détails")
                                Toast.makeText(
                                    this@MapActivity,
                                    "API refusée: $errorMessage",
                                    Toast.LENGTH_LONG
                                ).show()
                                mapLoadingOverlay.visibility = View.GONE
                                mapViewModel.setLoading(false)
                                return@runOnUiThread
                            }

                            val results = json.getJSONArray("results")

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

                        val sortedOffices = exchangeOffices.sortedBy { it.distance }
                        mapViewModel.setExchangeOffices(sortedOffices)
                        displayExchangeOfficesOnMap(sortedOffices)

                        mapLoadingOverlay.visibility = View.GONE
                        mapViewModel.setLoading(false)

                        if (sortedOffices.isEmpty()) {
                            Toast.makeText(
                                this@MapActivity,
                                "Aucun bureau de change trouvé dans un rayon de 5km",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@MapActivity,
                                "${sortedOffices.size} bureau(x) trouvé(s)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PlacesAPI", "Erreur parsing: ${e.message}", e)
                        mapLoadingOverlay.visibility = View.GONE
                        mapViewModel.setLoading(false)
                        Toast.makeText(
                            this@MapActivity,
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
     */
    private fun openGoogleMapsDirections(office: ExchangeOffice) {
        val uri = "google.navigation:q=${office.location.latitude},${office.location.longitude}&mode=d"
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps")

        try {
            startActivity(intent)
        } catch (e: Exception) {
            val browserUri = "https://www.google.com/maps/dir/?api=1&destination=${office.location.latitude},${office.location.longitude}&travelmode=transit"
            val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(browserUri))
            startActivity(browserIntent)
        }
    }
}
