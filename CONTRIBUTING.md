# Organisation des branches

| Branche     | Rôle                                                        | Build ?                      |
|-------------|-------------------------------------------------------------|------------------------------|
| `develop`   | Travail au quotidien : autant de commits que nécessaire     | Non                          |
| `feature/…` | (optionnel) Une fonctionnalité isolée, partie de `develop`  | Non                          |
| `main`      | Versions publiées                                           | Oui → Release + mise à jour  |

## Au quotidien
```bash
git switch develop
# … modifications …
git commit -am "Ce que j'ai changé"
git push
```
Rien n'est construit, rien n'arrive sur les téléphones.

## Publier une version
1. Sur GitHub : **Pull requests › New pull request**, base `main` ← compare `develop`.
2. La vérification « Vérifier la compilation » tourne (sans rien publier).
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
