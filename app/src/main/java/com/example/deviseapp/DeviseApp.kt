package com.example.deviseapp

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Application class pour l'initialisation globale
 * Active la persistence Firestore pour le mode hors ligne
 */
class DeviseApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialiser Firebase
        FirebaseApp.initializeApp(this)
        
        // Activer la persistence Firestore pour le mode hors ligne
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }
}
