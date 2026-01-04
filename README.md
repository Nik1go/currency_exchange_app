# 💱 deviseAPP - Convertisseur de Devises avec Géolocalisation

Application Android de conversion de devises en temps réel avec recherche de bureaux de change à proximité.

## 🎯 Fonctionnalités

### Conversion de Devises
- ✅ Conversion en temps réel avec taux de change actualisés
- ✅ Support de 14 devises majeures (EUR, USD, GBP, CHF, CAD, JPY, CNY, RUB, BRL, etc.)
- ✅ Interface intuitive avec sélection rapide des devises
- ✅ Conversion bidirectionnelle instantanée

### Authentification Firebase
- ✅ Connexion par email/mot de passe
- ✅ Connexion Google Sign-In
- ✅ Gestion sécurisée des sessions
- ✅ Déconnexion

### Géolocalisation & Carte
- ✅ Carte Google Maps intégrée
- ✅ Géolocalisation automatique de l'utilisateur
- ✅ Recherche de bureaux de change à proximité (rayon 5km)
- ✅ Affichage des résultats avec distance
- ✅ Bouton "Chercher dans cette zone" pour relancer la recherche
- ✅ Itinéraire vers les bureaux via Google Maps (métro, bus, voiture, à pied)

---

## 🛠️ Technologies Utilisées

### Frontend
- **Kotlin** - Langage principal
- **Android SDK** (minSdk 24, targetSdk 36)
- **Material Design Components** - Interface moderne

### Backend & Services
- **Firebase Authentication** - Authentification utilisateur
- **Firebase Firestore** - Base de données (si nécessaire)
- **Google Maps SDK** - Affichage de la carte
- **Places API** - Recherche de bureaux de change
- **Location Services** - Géolocalisation

### APIs & Networking
- **Retrofit** - Requêtes HTTP
- **Moshi** - Parsing JSON
- **OkHttp** - Client HTTP
- **Coroutines** - Programmation asynchrone

### CI/CD
- **GitHub Actions** - Pipeline d'intégration continue
- **Firebase App Distribution** - Distribution aux testeurs

---

## 📦 Installation

### Prérequis

1. **Android Studio** Arctic Fox ou plus récent
2. **JDK 11** ou supérieur
3. **Compte Firebase** (gratuit)
4. **Clé API Google Maps/Places**

### Configuration

#### 1. Cloner le repository

```bash
git clone https://github.com/Nik1go/II.3510_2526_G1_CICD_MOBILE.git
cd deviseAPP
```

#### 2. Configurer Firebase

1. Crée un projet sur [Firebase Console](https://console.firebase.google.com/)
2. Télécharge `google-services.json`
3. Place-le dans `app/google-services.json`

#### 3. Obtenir une clé API Google Maps

1. Va sur [Google Cloud Console](https://console.cloud.google.com/)
2. Active **Maps SDK for Android** et **Places API**
3. Crée une clé API
4. **IMPORTANT** : Dans les restrictions, sélectionne **"Aucun"** pour les restrictions d'application

#### 4. Configurer la clé API localement

Crée/édite `local.properties` à la racine du projet :

```properties
sdk.dir=C\:\\Users\\TON_USER\\AppData\\Local\\Android\\Sdk
MAPS_API_KEY=TA_CLE_API_GOOGLE_MAPS
```

⚠️ **Ne commit JAMAIS ce fichier** (déjà dans `.gitignore`)

#### 5. Obtenir le SHA-1 pour Google Sign-In

```bash
./gradlew signingReport
```

Copie le SHA-1 (Debug) et ajoute-le dans :
- Firebase Console → Paramètres du projet → Empreintes de certificat

#### 6. Build & Run

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Ou via Android Studio : **Run ▶️**

---

## 🚀 CI/CD avec GitHub Actions

### Secrets à configurer

Va sur `Settings → Secrets → Actions` et ajoute :

| Secret | Description | Exemple |
|--------|-------------|---------|
| `GOOGLE_SERVICES_JSON_BASE64` | Fichier Firebase encodé en base64 | `base64 app/google-services.json` |
| `FIREBASE_APP_ID_ANDROID` | ID de l'app Firebase Android | `1:123456789:android:abc123` |
| `FIREBASE_TOKEN` | Token Firebase CLI | `firebase login:ci` |
| `FIREBASE_TESTERS` | Emails des testeurs | `test1@mail.com,test2@mail.com` |
| `GOOGLE_MAPS_API_KEY` | Clé API Google Maps | `AIzaSyAbc123...` |

### Workflow

Le pipeline GitHub Actions :
1. ✅ Build l'APK debug
2. ✅ Exécute les linters
3. ✅ Lance les tests unitaires
4. ✅ Distribue l'APK via Firebase App Distribution (sur push `main`)

---

## 📱 Utilisation

### 1. Connexion

- Crée un compte avec email/mot de passe
- Ou connecte-toi avec Google

### 2. Conversion de devises

- Entre un montant dans le champ "Devise source"
- Sélectionne les devises source et cible
- Le résultat s'affiche instantanément

### 3. Trouver des bureaux de change

- Accepte la permission de localisation
- La carte affiche ta position
- Les bureaux de change apparaissent automatiquement (marqueurs bleus)
- Déplace la carte et clique "Chercher dans cette zone" pour relancer

### 4. Obtenir un itinéraire

- Clique sur un marqueur bleu
- Une info window s'affiche avec les détails
- Clique sur le bouton **"📍 ITINÉRAIRE"**
- Google Maps s'ouvre avec l'itinéraire (métro, bus, voiture, vélo)

---

## 💰 Coûts & Quotas

### Google Cloud Platform

**Budget recommandé** : 10-20$/mois

| Service | Prix | Usage typique | Coût/mois |
|---------|------|---------------|-----------|
| Maps SDK for Android | **GRATUIT** | Affichage carte | 0$ |
| Places API (Nearby Search) | 0.032$/requête | 300 recherches | ~10$ |
| **FREE TIER** | 200$/mois offerts | - | **-200$** |
| **Total estimé** | - | Usage modéré | **0$** ✅ |

### Firebase

- **Authentication** : Gratuit jusqu'à 50,000 users
- **App Distribution** : Gratuit illimité
- **Firestore** : 50,000 lectures/jour gratuites

⚠️ Configure une **alerte budget** à 10$/mois sur Google Cloud pour surveiller !

---

## 🔒 Sécurité

### Protection des clés API

- ✅ `local.properties` ignoré par Git
- ✅ Clés injectées via GitHub Secrets (CI/CD)
- ✅ `google-services.json` ignoré par Git
- ✅ Clé API Maps sans restrictions d'application (nécessaire pour Web Services)
- ✅ Surveillance des quotas et budget alerts

### Bonnes pratiques

- Ne jamais commit les clés API en dur dans le code
- Utiliser BuildConfig pour les clés
- Configurer des alertes budgétaires
- Désactiver les clés API compromises immédiatement

---

## 📂 Structure du Projet

```
deviseAPP/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/deviseapp/
│   │   │   │   ├── data/          # Repositories & Data sources
│   │   │   │   │   └── RateRepository.kt
│   │   │   │   └── ui/            # Activities & ViewModels
│   │   │   │       ├── LoginActivity.kt
│   │   │   │       ├── MainActivity.kt
│   │   │   │       ├── MainViewModel.kt
│   │   │   │       └── MapViewModel.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/        # XML Layouts
│   │   │   │   ├── values/        # Strings, colors, themes
│   │   │   │   └── drawable/      # Icons & images
│   │   │   └── AndroidManifest.xml
│   │   └── test/                  # Unit tests
│   └── build.gradle.kts           # App-level Gradle
├── .github/
│   └── workflows/
│       └── android-ci.yml         # CI/CD Pipeline
├── local.properties               # Clés API locales (ignoré)
├── build.gradle.kts               # Project-level Gradle
└── README.md                      # Ce fichier
```

---

## 🧪 Tests

### Lancer les tests

```bash
./gradlew test                    # Tests unitaires
./gradlew connectedAndroidTest   # Tests instrumentés
./gradlew lint                    # Linter
```

---

## 🐛 Problèmes Connus

### La carte ne trouve pas de bureaux de change

**Solution** : 
- Vérifie que Places API est activée sur Google Cloud
- Vérifie que la clé API n'a **PAS** de restrictions "Applications Android"
- Les restrictions Android bloquent les Web Services (Nearby Search)
- → Mets "Aucun" dans les restrictions d'application

### Google Sign-In erreur 10

**Solution** :
- Ajoute le SHA-1 de ton keystore dans Firebase Console
- Télécharge un nouveau `google-services.json`

### Build échoue : `MAPS_API_KEY` not found

**Solution** :
- Vérifie que `local.properties` contient `MAPS_API_KEY=...`
- Lance `./gradlew clean`

---

## 📝 Roadmap

### Version actuelle : 2.3

- [x] Conversion de devises en temps réel
- [x] Firebase Auth (Email + Google)
- [x] Carte Google Maps
- [x] Recherche bureaux de change
- [x] Itinéraire Google Maps
- [x] CI/CD GitHub Actions

### Futures améliorations

- [ ] Mode hors-ligne avec cache
- [ ] Historique des conversions
- [ ] Favoris bureaux de change
- [ ] Notifications alertes de taux
- [ ] Support plus de devises
- [ ] Dark mode

---

## 👥 Contributeurs

- **Nik1go** - Développement principal

---

## 📄 Licence

Ce projet est un projet étudiant dans le cadre du cours de CI/CD Mobile.

---

## 📞 Support

Pour toute question ou problème :
- Ouvre une **Issue** sur GitHub
- Contacte : leojava.34@gmail.com

---

## 🙏 Remerciements

- [Firebase](https://firebase.google.com/) pour l'authentification
- [Google Maps](https://developers.google.com/maps) pour la cartographie
- [ExchangeRate-API](https://www.exchangerate-api.com/) pour les taux de change
- GitHub Actions pour le CI/CD

---

**⭐ N'oublie pas de star le repo si tu trouves ce projet utile !**

