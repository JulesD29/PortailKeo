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

- Elle s'installe **à côté** de la vraie app, avec ses propres réglages ; elle ne se met pas à jour seule
  (retélécharger le lien pour avoir la dernière version de `develop`).
- **Outils de test** en bas de l'écran : simuler l'arrivée, simuler l'approche, réinitialiser l'appel du jour.
- Pour tester l'appel automatique en conditions réelles, désactiver l'automatisation dans la vraie app
  ce jour-là (sinon les deux appellent) et mettre de préférence un autre numéro que le portail.

### Checklist de tests fonctionnels avant de fusionner dans `main`
- [ ] Réglages : enregistrer, fermer, rouvrir → tout est conservé ; « ✓ Zone active »
- [ ] Autorisations : les ✓ s'affichent après les avoir accordées
- [ ] « Tester l'appel maintenant » appelle bien le numéro
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
| `PortalServiceTest` | Compte à rebours, Annuler, Appeler maintenant |
| `UpdaterTest` | Lecture des Releases GitHub, comparaison des versions |
| `TestToolsTest` | Outils de la version de test : arrivée / approche simulées, réinitialisation |

### Règles pour les prochains développements
1. **Toute nouvelle fonctionnalité arrive avec ses tests**, dans le même commit ou la même pull request.
2. **Tout bug corrigé** reçoit d'abord un test qui le reproduit.
3. La logique (règles, calculs, décisions) va dans des fonctions **sans dépendance à Android**
   (comme `Rules.evaluate`, `Arrival`, `Coords`) : c'est ce qui se teste le mieux.
4. L'heure passe par `AppClock` et les appels par `CallHelper.placer` : les tests les remplacent
   (voir `TestSupport`) pour ne jamais dépendre de l'heure réelle ni passer de vrai appel.
5. On ne fusionne dans `main` que si **Tests** est vert.
