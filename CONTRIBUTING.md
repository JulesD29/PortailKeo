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

### Règles pour les prochains développements
1. **Toute nouvelle fonctionnalité arrive avec ses tests**, dans le même commit ou la même pull request.
2. **Tout bug corrigé** reçoit d'abord un test qui le reproduit.
3. La logique (règles, calculs, décisions) va dans des fonctions **sans dépendance à Android**
   (comme `Rules.evaluate`, `Arrival`, `Coords`) : c'est ce qui se teste le mieux.
4. L'heure passe par `AppClock` et les appels par `CallHelper.placer` : les tests les remplacent
   (voir `TestSupport`) pour ne jamais dépendre de l'heure réelle ni passer de vrai appel.
5. On ne fusionne dans `main` que si **Tests** est vert.
