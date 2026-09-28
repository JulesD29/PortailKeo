# Portail Keo

Appelle automatiquement le numéro du portail quand vous entrez dans la zone
(jours et plage horaire réglables, 1 appel par jour maximum).

## Obtenir l'APK

**Option A — Android Studio (le plus simple)**
1. Ouvrir le dossier `PortailAuto` dans Android Studio (File › Open).
2. Attendre la synchronisation Gradle.
3. Menu **Build › Build App Bundle(s) / APK(s) › Build APK(s)**.
4. L'APK est dans `app/build/outputs/apk/debug/app-debug.apk`.
   Ou branchez le téléphone en USB (débogage USB activé) et cliquez ▶ Run.

**Option B — GitHub, sans rien installer**
1. Créer un dépôt **privé** sur GitHub et y pousser ce dossier.
2. Onglet **Actions** › « Build APK » tourne automatiquement (~5 min).
3. Télécharger l'artefact `PortailAuto-apk` (zip contenant l'APK).
   (À chaque nouvelle build GitHub, désinstaller l'ancienne version avant d'installer la nouvelle : la clé de signature change.)

## Installer sur le téléphone
1. Copier l'APK sur le téléphone et l'ouvrir.
2. Autoriser « Installer des applis inconnues » pour l'app qui ouvre le fichier.
3. Si Play Protect avertit : « Installer quand même ».

## Configuration (une seule fois)
1. **Numéro du portail**.
2. **Coordonnées** : sur Google Maps, appui long sur le portail → les coordonnées
   s'affichent en haut, touchez-les pour les copier, puis collez. Ou bien
   « Utiliser ma position actuelle » quand vous êtes devant.
3. **Rayon** : 200–400 m. Plus petit = détection plus tardive / moins fiable.
4. **Jours / horaires** (par défaut lun–ven, 7h00–10h00).
5. Activer l'interrupteur puis **Enregistrer**.
6. **Accorder les autorisations** : localisation → *Toujours autoriser*, appels, notifications.
7. **Désactiver l'optimisation batterie** (fortement conseillé, surtout Xiaomi,
   Huawei, Oppo, Samsung : mettre aussi l'app en « Sans restriction » dans
   Paramètres › Applis › Portail Keo › Batterie).

Le bouton **Tester l'appel** vérifie que l'appel direct fonctionne.
Le **Journal** montre chaque entrée dans la zone et pourquoi un appel a été
passé ou non.

## Bon à savoir
- La détection utilise le géorepérage de Google Play Services : économe en
  batterie, mais le déclenchement peut prendre de quelques secondes à ~2 min.
  Si c'est trop tard, augmentez le rayon.
- La localisation du téléphone doit rester activée.
- L'appel reste ouvert : c'est vous qui raccrochez.

## Ouvrir le portail à la main
- **Tuile des réglages rapides** : tirer le volet du haut › crayon ✏️ › glisser la tuile
  **Portail** dans les tuiles actives. Un appui = un appel.
- **Raccourci** : appui long sur l'icône de l'app › « Ouvrir le portail ».
  On peut le glisser sur l'écran d'accueil pour en faire un bouton.

## Pause et jours fériés
- « Mettre en pause… » : pas d'appel automatique jusqu'à la date choisie (incluse).
- « Pas d'appel les jours fériés » (activé par défaut) : jours fériés de France
  métropolitaine, lundi de Pâques, Ascension et lundi de Pentecôte inclus.
- La tuile et le raccourci fonctionnent toujours, même en pause.

## Signature fixe (mises à jour sans désinstaller)
La clé est dans `signing/` sur le PC (jamais envoyée sur Git — **à sauvegarder**,
sa perte oblige à désinstaller l'app). GitHub Actions l'utilise via les secrets
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## Mises à jour automatiques
Chaque commit sur `main` publie une *Release* GitHub (si la clé fixe est configurée).
L'app vérifie 2 fois par jour et à l'ouverture : une notification ou une fenêtre
propose d'installer la nouvelle version, sans passer par l'ordinateur.
Bouton « Rechercher une mise à jour » pour vérifier à la main.
La première fois, Android demande d'autoriser Portail Keo à installer des applis.
