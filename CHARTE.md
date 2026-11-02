# Charte graphique de Portail Keo

Version 2 — construite autour du logo « le K d'un trait ».
Tout nouvel écran ou composant suit cette charte. Maquette de référence : canevas « Portail Keo — nouvelle interface »
(page « Logo » : planche du K d'un trait ; page « App » : « Charte graphique » et « Accueil aux couleurs de la charte »).

## Logo : le K d'un trait

Le K de Keo dessiné comme un portail, en trois traits arrondis de même épaisseur et un point :

| Élément | Couleur | Sens |
|---|---|---|
| Pilier (trait vertical) | Blanc pilier `#FFFFFF` | le pilier du portail |
| Battant haut | Battant haut `#F884C9` | un battant qui vient de s'ouvrir |
| Battant bas (dessiné par-dessus) | Battant bas `#FCB1EC` | l'autre battant |
| Point à droite | `#F884C9` | l'appel qui part |

- Toujours sur le **vert portail** `#0B3722` (carré arrondi, cercle) ; les battants sont **légèrement décollés** du
  pilier.
- Logo complet : le K dans son carré vert + « **Portail** » (Bold, `colorOnSurface`) « **Keo** » (`colorSecondary`),
  avec la phrase « Arrivez, c'est ouvert » en dessous si la place le permet.
- Déclinaisons (`res/drawable`) :
  - `ic_logo_k` : le logo en couleurs, fond transparent (en-tête de l'accueil, gros bouton « Ouvrir le portail ») ;
  - `ic_launcher_fg` + `ic_bg` : icône de l'app (cercle ou carré arrondi selon le téléphone) ;
  - `ic_launcher_mono` : icône à thème d'Android 13+ (une couleur) ;
  - `ic_notif` : silhouette blanche pour les notifications et la barre d'état ;
  - `splash_icon` : animation d'ouverture (les battants fermés s'ouvrent, puis le point apparaît) ;
  - version claire (fond rose pâle `#FDCFF4`, pilier vert) pour un fond clair ou un fond d'écran sombre ;
  - version de test : fond orange `#E8710A`, pilier marron `#5A2A00`, battants blancs et crème.
- Lisible jusqu'à **20 px** : en dessous, utiliser `ic_notif`.
- À ne pas faire : changer les couleurs des traits, les espacer davantage, ajouter un contour ou une ombre sur le K,
  le poser sur une photo.

## Couleurs de la marque

| Nom | Code | Usage |
|---|---|---|
| Vert portail | `#0B3722` (`@color/brand_green`) | Fond du logo, gros bouton « Ouvrir le portail », boutons principaux |
| Battant haut | `#F884C9` (`@color/brand_pink`) | Accent : halo, pastille « Actif », points de la frise, point du logo |
| Battant bas | `#FCB1EC` (`@color/brand_pink_light`) | Accent doux ; texte secondaire sur fond vert |
| Blanc pilier | `#FFFFFF` | Texte et pilier du logo sur fond vert |

Les couleurs sont **fixes** : les couleurs Material You du fond d'écran ne sont pas appliquées (`PortalApp`).

## Rôles Material 3

Dans le code, on utilise **toujours les rôles** (`?attr/colorPrimary`, `?attr/colorSecondaryContainer`…), jamais une
couleur écrite en dur, sauf les trois couleurs de la marque ci-dessus.
Valeurs : `res/values/colors.xml` (clair) et `res/values-night/colors.xml` (sombre).

| Rôle | Clair | Sombre |
|---|---|---|
| primary / onPrimary | `#0B3722` / `#FFFFFF` | `#8FD9A8` / `#00391F` |
| primaryContainer / on… | `#CDEFD8` / `#0B3722` | `#0B3722` / `#B7F1C9` |
| secondary / onSecondary | `#8E3A6E` / `#FFFFFF` | `#F884C9` / `#5C1144` |
| secondaryContainer / on… | `#FDCFF4` / `#4A1038` | `#7A2A5F` / `#FDCFF4` |
| tertiary (pause) container / on… | `#F2F28A` / `#1D1D00` | `#4A4A00` / `#F2F28A` |
| background, surface | `#F7FAF6` | `#0E1511` |
| surfaceContainer (low → highest) | `#F1F5F0` → `#DFE4DE` | `#16201A` → `#2F3B33` |
| onSurface / onSurfaceVariant | `#181D19` / `#414942` | `#DDE5DE` / `#C0C9C1` |

Thème : **clair, sombre ou comme le système** (par défaut), au choix dans Réglages › Apparence (`AppTheme`).
Chaque écran doit être vérifié dans les deux thèmes.

## Typographie

**Be Vietnam Pro** partout (`res/font/be_vietnam_pro.xml`, licence SIL OFL dans `licences/`), appliquée par le thème
à tous les niveaux de texte Material 3 (`TextAppearance.PortailKeo.*`).

| Niveau | Taille / graisse | Exemple |
|---|---|---|
| Titre de page | HeadlineMedium, Bold | « Portail Keo », « Réglages » |
| Titre | TitleLarge, SemiBold | phrase de la carte d'état |
| Libellé | TitleMedium / LabelLarge, SemiBold | interrupteurs, boutons |
| Texte | BodyLarge / BodyMedium, Regular | explications, journal |
| En-tête de groupe | LabelMedium, Bold, majuscules | « AUJOURD'HUI » |

## Formes et composants

- Rayons : cartes **28 dp**, groupes **24 dp**, cases et champs **16 dp**, boutons et pastilles **arrondis complets**.
- En-tête de l'accueil : le logo complet (K dans son carré vert + « Portail Keo »).
- Gros bouton « Ouvrir le portail » : rond vert portail, le K en couleurs, halo `colorSecondaryContainer`.
- Pastille d'état : fond `colorSurface`, point de couleur (rose = actif, `colorTertiary` = pause, `colorError` = problème).
- Barre du bas : indicateur de l'onglet actif en `colorSecondaryContainer` (rose clair).

## Règles d'usage

1. **Le vert domine** ; le **rose est l'accent**, environ 10 % de l'écran.
2. **Pas de texte rose clair sur fond blanc** (illisible) : en clair, le texte rose est `colorSecondary` (`#8E3A6E`).
3. **Sur fond vert** : texte blanc, rose clair pour une phrase secondaire.
4. **États** : actif = vert / point rose, pause = jaune olive (tertiary), problème = rouge (error). Le rose ne signale
   jamais une erreur.
5. **Version de test** : mêmes règles ; l'orange `#E8710A` remplace le vert seulement pour l'icône et l'animation
   d'ouverture.
6. **Icônes** : traits arrondis Material, comme le K d'un trait, qui est l'élément graphique de référence.
7. **Titres** : courts, en Bold ; un sur-titre en majuscules espacées (`colorSecondary`) peut les précéder.
