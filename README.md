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
| **Morceau précédent inconnu** | Bouton **grisé** (`Controls.kt`) — jamais un bouton sans effet |

## Fonctionnalités

- **Affichage matriciel ambré** reprenant la façade de la barre (source + volume).
- **Now Playing** : titre / artiste / pochette (via `CPM GetRadioInfo`), halo ambré.
- **VU-mètre style radio 90s** — *décoratif et assumé comme tel*. L'audio ne
  transite ni par le téléphone ni par un serveur : aucun spectre réel n'existe.
  L'animation est pilotée uniquement par des données réelles (volume, lecture/pause)
  et une légende le dit explicitement.
- **Transport** : play/pause, morceau suivant (précédent grisé).
- **Volume** (0–30) optimiste + réconcilié par relecture, **mute**.
- **Sources** : Wi-Fi · Bluetooth · AUX · HDMI · Optique (`d.in`) · TV SoundConnect.
- **Raccourci « Bluetooth (YouTube) »** : YouTube/YT Music ne sont pas supportés
  nativement ; la voie propre est le Bluetooth depuis le téléphone.
- **« Lire une URL »** : `CPM SetUrlPlayback` pour vos flux HTTP (NAS, webradio,
  TTS). *Ne pas* y injecter de flux YouTube extraits (contraire aux CGU YouTube).
- **Services musicaux** : Spotify, TuneIn, Deezer, Qobuz, TIDAL affichés ;
  Napster / JUKE / 7digital / Murfie masqués (morts/obsolètes).
- **Réglages** : IP/port éditables (défaut `192.168.0.107:55001`).

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
    Models.kt               # Source, MusicService, PlayStatus, SoundbarState…
    XmlParsing.kt           # lecture tolérante des réponses UIC/CPM
    SoundbarApi.kt          # client HTTP + commandes vérifiées
    SoundbarRepository.kt   # quirk notifications: écrire → attendre → relire
    Settings.kt             # persistance IP/port
  ui/
    SoundbarViewModel.kt    # état + sérialisation des commandes + polling
    MainScreen.kt           # écran principal + dialogues
    components/{MatrixDisplay,VuMeter,Controls}.kt
    theme/Theme.kt          # panneau ambré sur noir
tools/
  k650_discover.py          # sonde des commandes supportées (stdlib seule)
```

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

Plusieurs points restent ouverts (morceau précédent, niveau caisson, presets EQ,
LED, minuterie, `GetCurrentPlayTime`…). **Ne rien coder à l'aveugle** : sonder
d'abord avec la barre allumée.

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
