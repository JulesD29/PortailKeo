<div align="center">

# 📞 Portail Keo

**Le portail de l'entreprise s'ouvre tout seul à votre arrivée : plus besoin d'appeler à la main chaque matin.**

[![Release APK](https://github.com/JulesD29/PortailKeo/actions/workflows/build.yml/badge.svg)](https://github.com/JulesD29/PortailKeo/actions/workflows/build.yml)
[![Dernière version](https://img.shields.io/github/v/release/JulesD29/PortailKeo?label=version)](https://github.com/JulesD29/PortailKeo/releases/latest)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)

[English](README.md) · Français

</div>

---

Beaucoup de portails d'entreprise s'ouvrent quand on appelle un numéro depuis un téléphone
autorisé. **Portail Keo** passe cet appel pour vous dès que vous approchez du portail,
les jours et aux heures que vous choisissez.

## Sommaire
- [Fonctionnalités](#fonctionnalités)
- [Installation](#installation)
- [Configuration](#configuration)
- [Utilisation au quotidien](#utilisation-au-quotidien)
- [Fonctionnement](#fonctionnement)
- [Autorisations](#autorisations)
- [Confidentialité](#confidentialité)
- [Dépannage](#dépannage)
- [Compiler depuis les sources](#compiler-depuis-les-sources)
- [Contribuer](#contribuer)

## Fonctionnalités

| | |
|---|---|
| 🗺️ **Choix sur la carte** | Placer le portail sur une carte OpenStreetMap et voir les deux zones sous forme de cercles en les réglant. |
| 🧭 **Assistant de configuration** | Au premier lancement : numéro, autorisations, carte, batterie et appel de test, étape par étape. |
| 🔗 **Partage par QR code** | Affichez votre configuration en QR code ; un collègue le scanne et tout est rempli. |
| 📍 **Appel automatique à l'arrivée** | Détection en deux temps : une grande zone d'approche (2 km par défaut) active un GPS précis, la zone du portail déclenche l'appel. |
| ⏱️ **Compte à rebours** | Notification « Appel du portail dans 5 s » avec **Annuler** et **Appeler maintenant**. |
| 📅 **Horaires** | Jours actifs, plage horaire, un seul appel automatique par jour. |
| 🏖️ **Pause et jours fériés** | Pause jusqu'à une date ; pas d'appel les jours fériés français. |
| 📲 **Appel manuel** | Tuile **Appeler Garage** dans les réglages rapides et raccourci (appui long sur l'icône). |
| 🔄 **Mises à jour automatiques** | L'app consulte les Releases GitHub et installe elle-même les nouvelles versions. |
| 📝 **Journal** | Chaque entrée dans une zone, chaque appel et chaque raison de ne pas appeler sont notés dans l'app. |

## Installation

> Nécessite Android 8.0 ou plus récent. L'app n'est pas sur le Play Store : on installe directement l'APK.

1. Sur le téléphone, ouvrez la [**dernière version**](https://github.com/JulesD29/PortailKeo/releases/latest)
   et téléchargez `PortailKeo-1.x.apk`.
2. Ouvrez le fichier téléchargé. Si Android le demande, autorisez votre navigateur ou
   gestionnaire de fichiers à *installer des applis inconnues*.
3. Si Play Protect affiche un avertissement : **Plus de détails › Installer quand même**.

> [!TIP]
> Si le téléchargement reste bloqué à 100 % dans Chrome, tirez le volet de notifications et
> touchez **Télécharger quand même**.

Après cette première installation, les mises à jour sont proposées **dans l'app**
(voir [Mises à jour automatiques](#mises-à-jour-automatiques)).

## Configuration

Au premier lancement, un **assistant** vous guide étape par étape. Un collègue utilise déjà l'app ?
Sur son téléphone : *Partager ma configuration (QR code)* ; sur le vôtre : **Scanner la configuration
d'un collègue** — il ne reste que les autorisations à accorder.

Les mêmes réglages restent ensuite disponibles sur l'écran principal :

1. **Numéro du portail** : le numéro qui ouvre le portail.
2. **Choisir sur la carte** : déplacez la carte pour mettre le viseur sur le portail et réglez les
   deux cercles avec les curseurs : bleu = zone du portail (appel, **200 à 400 m** conseillés),
   orange = zone d'approche (GPS précis, **2 km** par défaut). Touchez **Valider**.
   *Autres possibilités :* coller des coordonnées copiées dans Google Maps, ou **Utiliser ma position actuelle** devant le portail.
3. **GPS précis à l'approche** : laissez-le activé.
4. *(les rayons peuvent aussi être saisis directement)*
5. **Compte à rebours** : secondes avant l'appel (**0** = appel immédiat).
6. **Quand** : jours et plage horaire (lun–ven, 7h00–10h00 par défaut).
7. Activez **Automatisation activée**, puis touchez **Enregistrer**.
   La ligne en dessous doit afficher **✓ Zone active**.
8. **Accorder les autorisations** : appels, notifications, puis localisation › **Toujours autoriser**.
9. **Désactiver l'optimisation batterie** : fortement conseillé. Sur Samsung, Xiaomi, Huawei ou
   Oppo, mettez aussi *Paramètres › Applis › Portail Keo › Batterie* sur **Sans restriction**.
10. **Tester l'appel maintenant** pour vérifier que l'appel part bien.

> [!IMPORTANT]
> Le portail doit reconnaître **votre** numéro. S'il n'est pas enregistré auprès du système
> du portail, l'appel partira mais le portail ne s'ouvrira pas.

## Utilisation au quotidien

### Appel automatique
Rien à faire : en arrivant, la notification **Approche du portail** affiche la distance restante,
puis **Appel du portail dans 5 s**. Touchez **Annuler** pour annuler ; aucun autre appel
automatique ne sera passé ce jour-là.

### Appel manuel
- **Tuile des réglages rapides** : tirez le volet deux fois › ✏️ › glissez **Appeler Garage**
  dans vos tuiles. Sur Android 16, appui long en mode édition pour l'agrandir et afficher son nom.
- **Raccourci** : appui long sur l'icône de l'app › **Ouvrir le portail** (vous pouvez le glisser
  sur l'écran d'accueil).

La tuile et le raccourci fonctionnent toujours, même en pause ou hors de la plage horaire.

### Pause et jours fériés
- **Mettre en pause…** : aucun appel automatique jusqu'à la date choisie (incluse).
  **Reprendre** annule la pause.
- **Pas d'appel les jours fériés** (activé par défaut) : jours fériés de France métropolitaine,
  y compris le lundi de Pâques, l'Ascension et le lundi de Pentecôte.

### Mises à jour automatiques
L'app cherche une nouvelle version à chaque ouverture et deux fois par jour. Une notification
ou une fenêtre propose de l'installer ; vos réglages sont conservés. Vous pouvez aussi toucher
**Rechercher une mise à jour**. La première fois, Android demande d'autoriser Portail Keo à
installer des applis.

## Fonctionnement

```
      Zone d'approche (2 km)               Zone du portail (200–400 m)
 ─────────────●──────────────────────────────────●──────────────▶ Portail
              │                                  │
   règles OK ? → GPS précis              compte à rebours → appel
   toutes les 3 s (20 min max)           (Annuler / Appeler maintenant)
```

- Les zones utilisent le **géorepérage** de Google Play Services, économe en batterie.
- Dans la zone d'approche, un service de premier plan lit le GPS précis jusqu'à l'arrivée,
  la sortie de la zone, ou au bout de 20 min.
- La zone du portail reste active en secours si le GPS précis ne peut pas démarrer.
- Règles vérifiées avant tout appel automatique : activé, pas en pause, pas un jour férié,
  jour actif, dans la plage horaire, pas déjà appelé aujourd'hui.
- Les zones sont réenregistrées après un redémarrage, une mise à jour de l'app et toutes les 6 heures.

## Autorisations

| Autorisation | Pourquoi |
|---|---|
| Localisation — *Toujours autoriser* | Détecter l'arrivée même quand l'app est fermée |
| Appels téléphoniques | Passer l'appel directement, sans ouvrir le clavier |
| Notifications | Approche, compte à rebours, mises à jour et erreurs |
| Exemption d'optimisation batterie | Empêcher Android de mettre l'app en veille |
| Installation d'applis | Installer les mises à jour téléchargées depuis GitHub |
| Internet | Rechercher et télécharger les mises à jour, afficher la carte |

## Confidentialité

- Votre numéro, vos coordonnées, vos réglages et le journal **restent sur votre téléphone** ;
  rien n'est envoyé nulle part.
- Accès réseau : l'API GitHub (mises à jour) et les fonds de carte OpenStreetMap, seulement quand la carte est ouverte.
  Votre position n'est jamais envoyée ; seule la zone de carte affichée est téléchargée.
- Pas de compte, pas de statistiques, pas de publicité.

## Dépannage

| Problème | Solution |
|---|---|
| Pas d'appel à l'arrivée | Regardez la section **Journal** dans l'app : elle indique si la zone a été détectée et pourquoi l'appel n'est pas parti. |
| Appel trop tardif | Augmentez le **rayon** de la zone du portail ou la **zone d'approche**. |
| « ✗ Localisation « Toujours autoriser » manquante » | *Paramètres › Applis › Portail Keo › Autorisations › Position › Toujours autoriser*. |
| Rien ne se passe en arrière-plan | Désactivez l'optimisation batterie / mettez l'app en **Sans restriction**. |
| Appel passé mais portail fermé | Votre numéro n'est pas autorisé par le système du portail. |
| Tuile sans texte | Ouvrez le volet complet, ou agrandissez la tuile (Android 16). |
| « Mise à jour refusée : signature différente » | Désinstallez l'app, puis installez le dernier APK à la main. |

## Compiler depuis les sources

Prérequis : JDK 17 ou plus et le SDK Android (ou Android Studio).

```bash
git clone https://github.com/JulesD29/PortailKeo.git
cd PortailKeo
./gradlew assembleDebug      # → app/build/outputs/apk/debug/
```

Ou ouvrez le dossier dans Android Studio et cliquez sur ▶ **Run**.

<details>
<summary><b>Structure du projet</b></summary>

```
app/src/main/java/fr/julesdupont/portail/
├── MainActivity.kt        Écran de réglages
├── MapPickerActivity.kt   Choix sur la carte (OpenStreetMap / osmdroid)
├── Zones.kt               Règles de taille des zones
├── SetupActivity.kt       Assistant de premier lancement (étapes dans SetupWizard.kt)
├── ShareActivity.kt       QR code de la configuration
├── ConfigShare.kt         Format du lien QR, import / export
├── GeofenceManager.kt     Enregistre la zone du portail et la zone d'approche
├── GeofenceReceiver.kt    Gère les entrées / sorties de zone
├── PortalService.kt       GPS précis à l'approche + compte à rebours (service de premier plan)
├── AutoCall.kt            Point d'entrée unique des appels automatiques
├── Rules.kt               Jours, plage horaire, pause, fériés, une fois par jour
├── Holidays.kt            Jours fériés français (calcul de Pâques)
├── CallHelper.kt          Passe l'appel (TelecomManager)
├── PortalTileService.kt   Tuile « Appeler Garage » des réglages rapides
├── CallActivity.kt        Écran invisible utilisé par la tuile et le raccourci
├── Updater.kt             Mises à jour depuis les Releases GitHub
├── InstallReceiver.kt     Résultat de l'installation d'une mise à jour
├── RegisterReceiver.kt    Redémarrage, mise à jour de l'app, rafraîchissement périodique
├── Notifier.kt            Notifications
├── Prefs.kt / Perms.kt    Réglages et autorisations
```
</details>

<details>
<summary><b>Versions et signature</b></summary>

- Chaque fusion dans `main` lance le workflow **Release APK** : il compile, signe et publie une
  Release GitHub `v<N>`, que les apps installées récupèrent comme mise à jour.
- L'APK est signé avec une clé fixe stockée dans les **secrets** du dépôt
  (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Sans ces secrets,
  aucune Release n'est publiée.
- Les compilations locales utilisent `signing/keystore.properties` s'il existe, sinon la clé debug.
  Le dossier `signing/` est ignoré par Git et ne doit jamais être commité.
</details>

<details>
<summary><b>Version de test</b></summary>

Chaque envoi sur `develop` publie **Portail Keo (test)** (icône orange) en pré-release `test` :
[PortailKeo-test.apk](https://github.com/JulesD29/PortailKeo/releases/download/test/PortailKeo-test.apk).
Elle s'installe à côté de la vraie app, a ses propres réglages, se met à jour depuis `develop` sur demande
(**Rechercher une mise à jour**), et une
section **Outils de test** (simuler l'arrivée / l'approche, réinitialiser l'appel du jour).
Voir [CONTRIBUTING.md](CONTRIBUTING.md).
</details>

## Contribuer

On travaille sur `develop` ; la fusion dans `main` publie une nouvelle version.
Voir [CONTRIBUTING.md](CONTRIBUTING.md) pour l'organisation des branches.
