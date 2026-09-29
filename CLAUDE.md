# Portail Keo — consignes pour Claude

App Android (Kotlin, Views + Material 3) qui appelle le numéro du portail à l'arrivée.
Package : `fr.julesdupont.portail`. minSdk 26, targetSdk 34, compileSdk 35.

## Branches
- Travailler et committer sur `develop` (ou `feature/…`). **Ne jamais pousser sur `main`** :
  `main` = versions publiées (Release GitHub → mise à jour sur les téléphones), via pull request.
- Ne jamais committer `github-token.txt`, `signing/`, `*.jks`, `apk/`.

## Tests (obligatoires)
- Chaque nouvelle fonctionnalité ou correction de bug arrive **avec ses tests** dans `app/src/test/`.
- Logique pure hors Android dès que possible (`Rules.evaluate`, `Arrival`, `Coords`, `Holidays`,
  `Updater.parseRelease`) ; Robolectric pour ce qui touche Android (Prefs, CallHelper, AutoCall,
  GeofenceReceiver.handleTransition, PortalService).
- Heure via `AppClock` ; appels via `CallHelper.placer` ; utiliser `TestSupport` (setUp/tearDown).
- Vérifier que le workflow **Tests** passe sur GitHub Actions avant d'annoncer que c'est fini.
- Mettre à jour le tableau des tests dans `CONTRIBUTING.md` et les README (EN + FR) si besoin.

## Version de test
- Build type `beta` : `fr.julesdupont.portail.test`, nom « Portail Keo (test) », icône orange
  (`app/src/beta/res`), `BuildConfig.TEST_BUILD = true` → `TestTools` visibles ; mises à jour sur demande
  depuis la pré-release `test` (nom « Test 1.N » = versionCode), jamais automatiques.
- Publiée à chaque push sur develop en pré-release `test` (asset `PortailKeo-test.apk`).
- Nouvelle fonctionnalité → ajouter si utile un outil de simulation dans `TestTools` + une ligne
  dans la checklist de tests fonctionnels de `CONTRIBUTING.md`.

## Écrans
- Premier lancement : `SetupActivity` (étapes dans `SetupWizard`, logique pure) si `!prefs.setupDone`.
- Partage : `ShareActivity` (QR via `QrCode`/ZXing), import `ConfigImport` (scanner Play Services) →
  lien `portailkeo://config?...` (`ConfigShare`, schéma `portailkeo-test` pour la version de test).

## Build
- Pas de SDK Android sur le PC : la compilation et les tests tournent sur GitHub Actions.
- `check.yml` (Tests) : push sur develop/feature, PR. `build.yml` (Release APK) : push sur main.
