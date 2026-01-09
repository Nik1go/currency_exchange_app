#  deviseAPP - Convertisseur de Devises avec Géolocalisation

Application Android pour tout vos voyages. Cette appli fait la conversion de devises en temps réel et  recherche de bureaux de change à proximité.

### Authentification Firebase
-  Connexion par email/mot de passe
-  Connexion Google Sign-In
-  Gestion sécurisée des sessions
-  Déconnexion

### Géolocalisation & Carte
-  Carte Google Maps intégrée
-  L'utilisateur doit accepter la géolocalisation pour que l'application marche. 
-  Recherche de bureaux de change à proximité (rayon 5km)
-  Affichage des résultats avec distance et propose un itineraire. 


## Stack Utilisées

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

##  Installation

### Prérequis

1. **Android Studio** Arctic Fox ou plus récent
2. **JDK 11** ou supérieur
3. **Compte Firebase** (gratuit)
4. **Clé API Google Maps/Places**


## 📂PS C:\Users\leoja\AndroidStudioProjects\deviseAPP> git commit -m "nvll devise et maj readme"                             
On branch main
Your branch is ahead of 'origin/main' by 1 commit.
  (use "git push" to publish your local commits)

Changes not staged for commit:
  (use "git add <file>..." to update what will be committed)
  (use "git restore <file>..." to discard changes in working directory)
        modified:   .idea/deploymentTargetSelector.xml
        modified:   .idea/gradle.xml
        modified:   .idea/misc.xml

Untracked files:
  (use "git add <file>..." to include in what will be committed)
        .bundle/
        .idea/deviceManager.xml
        .kotlin/
        Gemfile
        Gemfile.lock
        ersleojaAndroidStudioProjectsdeviseAPP
        et --hard HEAD~1
        fastlane/
        tatus
        vendor/

no changes added to commit (use "git add" and/or "git commit -a")
PS C:\Users\leoja\AndroidStudioProjects\deviseAPP> git add README.md app/src/main/java/com/example/deviseapp/ui/MainActivity.kt Structure du Projet

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
