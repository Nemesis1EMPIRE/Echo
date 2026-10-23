# ECHO

Assistant vocal Android personnel en Kotlin et Jetpack Compose. Min SDK 26, target SDK 34.

## Construire

Ouvrir le dossier dans Android Studio (JDK 17, Android SDK 35), synchroniser Gradle puis lancer `app`. Pour créer un APK debug :

```sh
./gradlew :app:assembleDebug
```

L'APK est généré dans `app/build/outputs/apk/debug/app-debug.apk`.

## Clés API

Le chat OpenAI est optionnel et peut aussi recevoir sa clé dans l'écran Réglages. Pour l'injecter au build, définir `OPENAI_API_KEY` dans `~/.gradle/gradle.properties`. La météo utilise OpenWeather : ajouter `OPENWEATHER_API_KEY` au même fichier. Les deux valeurs sont générées dans `BuildConfig` ; ne pas versionner de clé réelle. Le service OpenAI peut entraîner des frais, ce n'est pas une dépendance gratuite.

## Démarrer ECHO

Accorder le microphone au premier lancement, puis appuyer sur le grand bouton pour parler. « Démarrer le service » garde le service actif. « Activer la bulle flottante » ouvre les réglages système de superposition ; autoriser ECHO, puis revenir dans l'application. La bulle cyan reste au-dessus des autres applications : toucher pour écouter, maintenir pour revenir à l'application, faire glisser pour déplacer.

Les appels, SMS, contacts et torche demandent leurs permissions au premier usage correspondant.

## Limites du niveau 1

- La reconnaissance dépend du fournisseur vocal installé par la ROM et peut nécessiter Internet.
- La sélection féminine est heuristique : ECHO choisit un nom de voix française connu, puis la première voix française disponible. Le moteur TTS et la présence réelle de voix varient selon Samsung, Pixel, Xiaomi et les paquets de voix installés ; Android ne fournit pas de marqueur de genre fiable.
- Le chat sans clé est limité à quelques réponses locales. La clé OpenAI et la météo nécessitent Internet.
- Les actions d'appel et d'envoi SMS sont immédiates après la commande et l'autorisation correspondante. Vérifier la transcription avant toute commande sensible.
- Les constructeurs peuvent limiter ou arrêter les services en superposition selon leurs réglages d'économie d'énergie.
- La clé OpenAI saisie dans Réglages est stockée localement dans DataStore, sans chiffrement dédié.