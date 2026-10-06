# Organisation des branches

| Branche     | Rôle                                                        | Build ?                      |
|-------------|-------------------------------------------------------------|------------------------------|
| `develop`   | Travail au quotidien : autant de commits que nécessaire     | Tests seulement              |
| `feature/…` | (optionnel) Une fonctionnalité isolée, partie de `develop`  | Non                          |
| `main`      | Versions publiées                                           | Oui → Release + mise à jour  |

## Au quotidien
```bash
git switch develop
# … modifications …
git commit -am "Ce que j'ai changé"
git push
```
Les tests se lancent automatiquement (onglet **Actions › Tests**) ; rien n'arrive sur les téléphones.

## Tester sur le téléphone (version de test)
Chaque envoi sur `develop` publie **Portail Keo (test)** (icône orange) en pré-release :
<https://github.com/JulesD29/PortailKeo/releases/download/test/PortailKeo-test.apk>

- Elle s'installe **à côté** de la vraie app, avec ses propres réglages.
- **Rechercher une mise à jour** installe la dernière version de `develop` (sur demande uniquement,
  pas de vérification automatique).
- **Outils de test** en bas de l'écran : simuler l'arrivée, simuler l'approche, réinitialiser l'appel du jour.
- Pour tester l'appel automatique en conditions réelles, désactiver l'automatisation dans la vraie app
  ce jour-là (sinon les deux appellent) et mettre de préférence un autre numéro que le portail.

### Checklist de tests fonctionnels avant de fusionner dans `main`
- [ ] Écran principal : la carte d'état change selon la situation (actif, pause, appelé aujourd'hui,
      désactivé, autorisations manquantes) ; clair et sombre lisibles
- [ ] Réglages : modifier, fermer, rouvrir → tout est conservé sans bouton « Enregistrer » ; « ✓ Zone active »
- [ ] Autorisations : les ✓ s'affichent après les avoir accordées
- [ ] « Choisir sur la carte » : la carte s'ouvre sur la position enregistrée, les 2 cercles suivent
      le viseur et les curseurs ; « Valider » enregistre coordonnées et rayons
- [ ] Assistant (Outils de test › Réinitialiser l'assistant) : les 6 étapes s'enchaînent, « Suivant »
      reste grisé tant que le numéro / la position / les autorisations manquent ; « Terminer » active l'automatisation
- [ ] QR code : « Partager ma configuration » affiche le QR ; sur un 2e téléphone (ou l'app de test),
      « Scanner la configuration d'un collègue » affiche le résumé puis remplit tout
- [ ] « Ouvrir le portail maintenant » appelle bien le numéro
- [ ] Téléphone verrouillé : « Simuler l'arrivée dans 20 s », verrouiller ; l'écran s'allume, l'appel sonne ;
      journal « Téléphone verrouillé… » puis « Appel lancé depuis l'écran verrouillé » puis « Appel en cours (essai 1) »
- [ ] Retour en poche : « Tester le retour » vibre deux fois ; avec écouteurs, annonce « Portail appelé » ;
      en vrai : vibration à l'appel, double vibration + annonce après le raccrochage
- [ ] Appel automatique (Simuler l'arrivée) : notification « Appel du portail en cours », raccrochage après le délai
      réglé ; journal « Appel en cours (essai 1) » puis « Raccroché automatiquement… »
- [ ] Widget : appui long sur l'écran d'accueil › Widgets › Portail Keo ; l'état s'affiche, le bouton appelle,
      l'état change après une pause / un appel
- [ ] Commande vocale : activer l'interrupteur → icône « Portail test » dans les applis ;
      « Ok Google, ouvre Portail test » appelle ; désactiver → l'icône disparaît
- [ ] Historique : après une arrivée simulée, « Cette semaine : 1 appel » et la ligne du jour apparaissent
- [ ] « Quoi de neuf » (Outils de test › Revoir « Quoi de neuf ») : s'affiche une fois, puis plus
- [ ] Simuler l'arrivée : notification de compte à rebours, puis appel
- [ ] Simuler l'arrivée puis **Annuler** : pas d'appel, journal « Appel annulé »
- [ ] Simuler l'arrivée hors plage horaire : pas d'appel, raison dans le journal
- [ ] Simuler l'approche : notification « Approche du portail » avec la distance réelle ; « Arrêter » la coupe
- [ ] Tuile « Garage (test) » et raccourci « Portail (test) » appellent sans délai
- [ ] Pause jusqu'à demain : simuler l'arrivée → pas d'appel ; « Reprendre » → appel possible
- [ ] Ce qui a été modifié dans la pull request

## Publier une version
1. Sur GitHub : **Pull requests › New pull request**, base `main` ← compare `develop`.
2. Les **Tests** tournent (sans rien publier).
3. Si elle est verte : **Merge pull request**.
4. Le merge sur `main` lance « Release APK » → nouvelle Release → l'app propose la mise à jour.

Un commit qui ne touche que la doc (`*.md`) sur `main` ne déclenche pas de Release.

## Fonctionnalité isolée (optionnel)
```bash
git switch develop && git pull
git switch -c feature/ma-fonction
# … commits …
git push -u origin feature/ma-fonction
```
Puis pull request `feature/ma-fonction` → `develop`, et plus tard `develop` → `main`.

## Tests

Les tests sont dans `app/src/test/java/fr/julesdupont/portail/` et tournent sur la JVM
(pas besoin de téléphone). Robolectric simule Android pour les réglages, les appels et le service.

```bash
./gradlew testDebugUnitTest        # rapport : app/build/reports/tests/testDebugUnitTest/index.html
```

| Fichier | Ce qui est vérifié |
|---|---|
| `HolidaysTest` | Pâques et jours fériés fixes / mobiles |
| `RulesTest` | Activation, numéro, jours, plage horaire (y compris après minuit), pause, fériés, 1 appel/jour |
| `CoordsTest` | Lecture des coordonnées collées depuis Google Maps |
| `ArrivalTest` | Arrivée GPS : distance et précision |
| `PrefsTest` | Valeurs par défaut, sauvegarde, journal limité à 40 lignes |
| `CallHelperTest` | Appel auto / manuel, permission manquante, erreur du téléphone |
| `AutoCallTest` | Appel direct ou compte à rebours, refus, pas de double appel |
| `GeofenceLogicTest` | Entrée / sortie des zones portail et approche |
| `PortalServiceTest` | Compte à rebours, Annuler, Appeler maintenant ; suivi d'appel (rappel, raccrochage auto) |
| `UpdaterTest` | Lecture des Releases GitHub, comparaison des versions |
| `ZonesTest` | Limites des rayons, écart de 300 m, arrondi des curseurs de la carte |
| `MapPickerTest` | Aller-retour des valeurs entre la carte et l'écran de réglages |
| `ConfigShareTest` | Format du lien de configuration (QR), aller-retour, liens invalides refusés |
| `QrCodeTest` | Le QR code généré se relit à l'identique |
| `ConfigImportTest` | Import : réglages appliqués, activation/pause conservées ; assistant ; réinitialisation |
| `SetupWizardTest` | Ordre des étapes, étapes obligatoires, reprise après import |
| `DashboardTest` | Carte d'état : priorité des états, prochain créneau (fériés, pause, week-end, minuit), dernier événement |
| `HistoryTest` | Historique des arrivées : limite, semaine en cours, heure moyenne, résumé |
| `WhatsNewTest` | « Quoi de neuf » : nouveautés non vues, édition courante |
| `PortalWidgetTest` | Widget : contenu selon l'état, mise à jour sans plantage |
| `VoiceCommandTest` | Commande vocale : désactivée par défaut, activation, icône qui lance l'appel |
| `CallMonitorTest` | Suivi d'appel : rappel si l'appel se coupe sans sonner, abandon après 3 essais, délai de raccrochage |
| `CallLaunchTest` | Appel direct ou via l'écran verrouillé selon l'état du téléphone |
| `CallActivityTest` | Écran d'appel : appel automatique (verrouillé), pas de double appel, appel manuel |
| `FeedbackTest` | Retour en poche : vibrations, annonce seulement avec écouteurs et hors appel |
| `TestToolsTest` | Outils de la version de test : arrivée / approche simulées, réinitialisation |

### Règles pour les prochains développements
1. **Toute nouvelle fonctionnalité arrive avec ses tests**, dans le même commit ou la même pull request.
2. **Tout bug corrigé** reçoit d'abord un test qui le reproduit.
3. La logique (règles, calculs, décisions) va dans des fonctions **sans dépendance à Android**
   (comme `Rules.evaluate`, `Arrival`, `Coords`) : c'est ce qui se teste le mieux.
4. L'heure passe par `AppClock` et les appels par `CallHelper.placer` : les tests les remplacent
   (voir `TestSupport`) pour ne jamais dépendre de l'heure réelle ni passer de vrai appel.
5. On ne fusionne dans `main` que si **Tests** est vert.

## Maintenance
- **Dependabot** (`.github/dependabot.yml`) ouvre chaque mois une pull request vers `develop` quand une
  action GitHub (checkout, setup-java…) a une nouvelle version : vérifier que **Tests** passe, puis fusionner.
- Les constructions tournent sur `ubuntu-24.04` (image figée) avec le cache Gradle.
