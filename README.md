# LU3G TV Launcher

Launcher per Android TV / Google TV con il brand LU3G: veloce, animato con misura,
pensato per TV con Android 12 e SoC economici.

- Kotlin + Jetpack Compose for TV (`androidx.tv:tv-material`), nessun'altra libreria.
- Android minimo 7.0 (API 24), compilato con API 35. APK di rilascio ~1,2 MB.
- Intestazione "ricca": l'app a fuoco in grande (nome, ultimo utilizzo, banner grande a destra),
  sfondo che prende la tinta dominante dell'icona.
- Splash animata di 4 secondi con jingle: i triangoli entrano in ventaglio con rotazione e rimbalzo, le lettere
  L U 3 G si scrivono una alla volta, passa un riflesso, il logo respira, poi zoom sulla home. Parte all'avvio
  e a ogni risveglio della TV dallo standby se la TV si riaccende sul launcher: al risveglio resta nera finché il
  sistema non segnala lo schermo acceso (più 0,5 s di margine per il pannello), poi animazione e suono partono insieme (la boot animation di sistema non è
  modificabile senza root). Il jingle è generato da `tools/gen_splash_sound.py` (Python puro + `afconvert`), sincronizzato con le
  fasi dell'animazione, con una variante per ogni tema stagionale. Log diagnostici con tag `Lu3gSplash`.
- Righe: Preferiti, Usate di recente, Sorgenti (HDMI), Giochi, Tutte le app.
- Menu app (OK premuto a lungo o tasto Menu): apri, preferiti, informazioni, disinstalla.
- Splash animata sul logo: i quattro triangoli entrano in ventaglio, compaiono le lettere, un riflesso
  attraversa il marchio e un alone ambra pulsa; poi dissolvenza con zoom sulla home. Nessuna barra.
- Sfondo con aloni in movimento (12 aggiornamenti al secondo, livelli pre-rasterizzati),
  fermo quando il launcher non è in primo piano. Icone e banner limitati in dimensione.

## Temi stagionali

Il launcher si veste da solo in base al calendario (pulsante **Tema** in alto per spegnere gli effetti o forzare un tema):

| Tema | Quando | Decorazioni |
|---|---|---|
| Halloween | 24 ottobre – 2 novembre | zucche e pipistrelli che fluttuano, ragnatela con ragnetto in alto a destra, alone arancio |
| Natale | 1 dicembre – 6 gennaio | neve, filo di luci lampeggianti sul bordo superiore, alone rosso |
| San Valentino | 7 – 15 febbraio | cuori che salgono, alone rosa |
| Pasqua | da una settimana prima a Pasquetta (data calcolata) | uova colorate, fiori in basso a sinistra, alone verde |
| Estate | 15 giugno – 15 settembre | scintille che salgono, sole con raggi in alto a destra, alone giallo |
| Autunno | 16 settembre – 30 novembre (fuori Halloween) | foglie che cadono, alone ambra scuro |

Tutto in `theme/Seasons.kt`: per aggiungere una ricorrenza basta un nuovo `SeasonTheme` nella lista `all`
(date, saluto, tinta, particelle, decorazione ai bordi). Le decorazioni stanno dietro ai contenuti
(`ui/components/SeasonalLayer.kt`): poche forme semplici su un solo Canvas, 20 aggiornamenti al secondo,
ferme quando il launcher non è in primo piano. Preferenze: `seasonal_enabled`, `season_choice`.
Il tema vale anche sulla splash: tinta dell'alone, decorazioni che sfumano dentro mentre il logo si compone e saluto
sotto "LU3G TV" (il jingle non cambia).

## Salvaschermo LU3G

Il launcher include un salvaschermo (Daydream): sfondo LU3G fermo, decorazioni del tema stagionale, logo +
orologio grande + data + saluto che ogni 40 secondi scivolano in un punto diverso (contro il burn-in). Qualsiasi
tasto lo chiude. Si sceglie da Impostazioni → Sistema → Salvaschermo, oppure via ADB:

```bash
adb shell settings put secure screensaver_components it.lu3g.tvlauncher/.dream.Lu3gDreamService
```

Per tornare a quello di Google: `settings put secure screensaver_components com.google.android.apps.tv.dreamx/.service.Backdrop`
(il valore precedente è annotato in `docs/`).

## Jingle per tema

`tools/gen_splash_sound.py` genera il jingle di base e una variante per ogni tema (`res/raw/splash_*.m4a`):
Halloween in minore con bordone e glissando "da theremin", Natale con sonagli e rintocchi, San Valentino con arpa,
Pasqua con cinguettii, Estate con scintillio e un'onda, Autunno più grave e ventoso. Rigenerare:

```bash
python3 -I tools/gen_splash_sound.py /tmp/wav && for f in /tmp/wav/*.wav; do afconvert -f m4af -d aac -b 128000 "$f" app/src/main/res/raw/$(basename "$f" .wav).m4a; done
```

## Compilare

Servono JDK 17 e l'SDK Android (`local.properties` → `sdk.dir`).

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`.

La firma di rilascio legge `keystore.properties` (fuori dal repository). Senza quel file
si usa la chiave di debug. **Conserva `keystore/lu3g-tv.jks` e `keystore.properties`**:
un APK firmato con un'altra chiave non si installa sopra quello già presente sulla TV.

## Installare sulla TV (stessa rete Wi‑Fi)

Sulla TV: Impostazioni → Preferenze dispositivo → Informazioni → 7 tocchi su "Build",
poi Opzioni sviluppatore → Debug USB (e Debug di rete, se c'è).

```bash
adb connect IP_DELLA_TV:5555
adb install -r app/build/outputs/apk/release/app-release.apk
```

Alla prima connessione la TV chiede di autorizzare il debug: spunta "Consenti sempre".
Il launcher compare nella lista app con il nome "LU3G TV": si può provare da lì.

## Impostarlo come schermata Home

Il tasto Home è legato al launcher di serie: va disattivato (reversibile, nessun reset).

```bash
adb shell cmd package set-home-activity it.lu3g.tvlauncher/.MainActivity
adb shell pm disable-user --user 0 com.google.android.tvlauncher
```

Su Google TV il launcher è `com.google.android.apps.tv.launcherx` e va disattivato
anche `com.google.android.tungsten.setupwraith`, altrimenti la TV ripropone la
configurazione iniziale.

Per tornare al launcher originale:

```bash
adb shell pm enable com.google.android.tvlauncher
```

## Struttura

- `app/src/main/java/it/lu3g/tvlauncher/data` — lettura app dal PackageManager (con cache), preferiti e recenti.
- `app/src/main/java/it/lu3g/tvlauncher/ui` — schermata, card, intestazione, sfondo, splash, menu.
- `app/src/main/res/font` — Manrope e DM Sans (istanze statiche).
- `app/src/main/res/drawable/ic_lu3g_logo.xml` — logo vettoriale LU3G.
