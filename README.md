# K650 Remote

Application Android native de pilotage de la barre de son **Samsung HW-K650**
(Wireless Multiroom, 2016) via son **API HTTP locale non authentifiée**
(port `55001`, protocole WAM / UIC / CPM).

L'app officielle *Samsung Wireless Audio – Multiroom* est abandonnée et plante
sur Android récent ; SmartThings ne gère quasiment rien sur la série K. Cette
app rétablit un contrôle complet : volume, mute, sources, transport, lecture
d'URL audio, affichage titre/artiste/pochette (Spotify Connect).

## Pourquoi c'est fiable là où c'est délicat

Toute la connaissance de terrain sur ce firmware est encodée dans la couche
données, pas éparpillée dans l'UI :

| Piège firmware | Où c'est traité |
| --- | --- |
| **Espaces à encoder en `%20`** (le `+` d'un encodage formulaire → errcode 53) | `data/UrlEncoding.kt` (percent-encoding RFC 3986) |
| **Notifications empilées** : la barre renvoie parfois la réponse d'une requête précédente | `data/SoundbarRepository.kt` : chaque écriture attend ~600 ms puis **relit l'état réel** ; l'accusé n'est jamais cru |
| **Timeout normal** (SetFunc vers une source déjà active ne répond pas) | `data/SoundbarApi.kt` : `ApiResult.Timeout` distinct d'`Error`, jamais traité comme une panne |
| **Saut de piste** accepté uniquement **sans paramètre** | `SetSkipCurrentTrack` (les variantes `SetTrickMode` / `SetPlaybackControl val="next"` sont volontairement absentes) |
| **Morceau précédent : aucune commande dédiée n'existe** (confirmé par pywam, krygal, bacl) | Le bouton **redémarre le morceau courant** via `SetSearchTime playtime=0` (comportement d'une vraie télécommande) + tente `SetTrickMode previous` en secours — un effet réel, honnêtement libellé |
| **Login des services en clair** sur le LAN (`SetSignIn`) | L'app **avertit explicitement** avant toute saisie d'identifiants |

## Interface — 5 onglets (look moderne, Material 3)

**Lecture** — afficheur matriciel ambré (source + volume), pochette + titre/artiste
(`GetRadioInfo`), **barre de progression + seek** (`GetCurrentPlayTime` /
`SetSearchTime`), transport complet (précédent / play-pause / suivant + **répétition**
et **aléatoire**), volume 0–30 et mute, et le **VU-mètre 90s décoratif** (piloté
uniquement par volume + lecture/pause, légende explicite — l'API ne renvoie aucun
niveau réel).

**Sources** — Wi-Fi · Bluetooth · AUX · HDMI · Optique (`d.in`) · TV SoundConnect,
raccourci **Bluetooth**, **« Lire une URL »** (`SetUrlPlayback` UIC, pour NAS /
webradio / TTS), et un encart honnête sur YouTube / Amazon Music (non natifs → BT).

**Son** — **égaliseur** : presets 7 bandes lus du firmware (`Get7bandEQList` /
`Set7bandEQMode`) ; **niveau du caisson** −6…+6 (`Set/GetWooferLevel`) ; note sur ce
qui n'existe qu'en cloud SmartThings (mode nuit, DRC, dialogues, synchro audio).

**Services** — liste réelle des services du firmware (`GetCpList`) avec statut de
connexion ; **liaison de compte** Deezer / TIDAL / Qobuz (`SetCpService` + `SetSignIn`,
avec **avertissement clair : identifiants en clair sur le LAN**) ; **navigation +
lecture** (`GetCpSubmenu` / `SetSelectCpSubmenu` / `BrowseMain` / `SetPlaySelect`) ;
Spotify identifié comme **renderer Spotify Connect** (piloté depuis l'app Spotify).

**Réglages** — **vraie vérification réseau** (`GetMainInfo` → modèle + MAC,
`GetApInfo` → SSID / RSSI / canal, `GetSoftwareVersion`), IP/port éditables,
**alimentation & veille** (`SetSleepTimer` + minuteries 15/30/60 min ; l'allumage
réseau n'est pas exposé par le firmware, l'app le dit), renommage de la barre.

## Réseau

- HTTP simple → `usesCleartextTraffic` + `res/xml/network_security_config.xml`.
  (Aucun souci de CORS en natif, contrairement à une web app.)
- **Réservation DHCP statique fortement recommandée** pour la barre : l'app pointe
  une IP fixe.
- Découverte mDNS possible (`_spotify-connect._tcp.local`) — non requise ici.

## Structure

```
app/src/main/java/com/k650/remote/
  MainActivity.kt
  data/
    UrlEncoding.kt          # %20 encoding (le piège principal)
    Models.kt               # Source, MusicService, EqPreset, CpService, DeviceInfo, SoundbarState…
    XmlParsing.kt           # lecture tolérante UIC/CPM : scalaires + listes (items())
    SoundbarApi.kt          # client HTTP + toutes les commandes vérifiées (transport, EQ, CP, réseau)
    SoundbarRepository.kt   # quirk notifications: écrire → attendre → relire ; parsing métier
    Settings.kt             # persistance IP/port
  ui/
    SoundbarViewModel.kt    # état + services + sérialisation des commandes + polling (core/statique)
    MainScaffold.kt         # navigation à 5 onglets
    screens/{NowPlaying,Sources,Sound,Services,Settings}Screen.kt, Dialogs.kt
    components/{MatrixDisplay,VuMeter,Atoms}.kt
    theme/{Theme,Color}.kt  # Material 3 sombre, ambre en accent
tools/
  k650_discover.py          # sonde des commandes supportées (stdlib seule)
```

## Bases de la recherche (reverse engineering vérifié)

Toutes les commandes proviennent de bibliothèques open-source de reverse
engineering, pas de suppositions : **Strixx76/pywam**, **krygal/samsung_multiroom**,
**bacl/WAM_API_DOC**, **snowriderau/SamsungSoundbar**. Faits marquants :
aucune vraie commande « précédent » n'existe ; aucune commande d'allumage réseau ;
mode nuit/DRC/synchro sont côté cloud SmartThings seulement ; YouTube/Amazon absents
du firmware.

## Récupérer l'APK (CI)

Le plus simple, sans rien installer : l'APK **debug** est construit
automatiquement par GitHub Actions (`.github/workflows/build-apk.yml`) à chaque
push sur la branche.

1. Onglet **Actions** du dépôt → run « Build APK » le plus récent.
2. Section **Artifacts** en bas → télécharger **`k650-remote-debug-apk`**.
3. Dézipper, transférer `app-debug.apk` sur le téléphone et l'installer
   (autoriser « sources inconnues »).

Le workflow se lance aussi à la main (bouton *Run workflow*). Sur un tag
`vX.Y.Z`, l'APK est en plus attaché à une **Release** (lien de téléchargement
direct et stable).

## Compiler localement

Le wrapper Gradle est fourni (`./gradlew`, Gradle 8.9) :

```bash
./gradlew assembleDebug
# APK -> app/build/outputs/apk/debug/app-debug.apk
```

Ou ouvrir le dossier dans **Android Studio** (Giraffe+), laisser Gradle
synchroniser, puis *Run*.

- `compileSdk` 34, `minSdk` 24, JDK 17, Kotlin + Jetpack Compose.

## Sonder le firmware avant d'ajouter des fonctions

Les commandes sont désormais implémentées d'après le reverse engineering, mais leur
support exact dépend du firmware de *votre* unité. **Ne rien supposer à l'aveugle** :
confirmez sur la barre allumée (presets EQ réellement renvoyés, login d'un service,
comportement du « précédent », etc.).

```bash
python3 tools/k650_discover.py                 # lectures + tous les probes
python3 tools/k650_discover.py --prev          # candidats "morceau précédent"
python3 tools/k650_discover.py --advanced      # réglages avancés (lecture seule)
```

Le script juge sur l'**état réel** (diff titre/artiste, `result="ok"`), jamais
sur l'accusé de réception. Dès qu'une commande « précédent » est confirmée,
câbler le bouton correspondant dans `Controls.kt` / `SoundbarViewModel.kt`.

## Bug matériel connu (hors app)

La barre perd sa config Wi-Fi tous les 2–3 jours. Pistes côté réseau : activer
« Veille du réseau Soundbar » (= garder le Wi-Fi **actif** en veille, libellé FR
trompeur), bail DHCP statique et long, PMF/802.11w désactivé sur le SSID 2,4 GHz,
délai d'inactivité du point d'accès désactivé. L'app signale simplement la barre
comme *injoignable* quand ça se produit.
