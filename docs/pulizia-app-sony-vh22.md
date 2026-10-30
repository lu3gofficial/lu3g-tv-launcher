# Pulizia app · Sony BRAVIA 4K VH22 (Android 12, Google TV)

Inventario del 30 ottobre 2026 (`sony-vh22-pacchetti.txt`, 267 pacchetti).
Metodo: le app di sistema si **disattivano** (`pm disable-user --user 0 PACCHETTO`, si riattivano con
`pm enable PACCHETTO`); le app installate dal Play Store si possono disinstallare e reinstallare dallo store.
Nessuna delle operazioni richiede reset o root. I nomi leggibili sono dedotti dal pacchetto: (?) = incerto.

## Gruppo 1 · Consigliate da spegnere (nessuna funzione persa nell'uso domestico)

| Nome | Pacchetto | Cos'è |
|---|---|---|
| Sony Select | com.sony.dtv.sonyselect | consigli e promozioni di app |
| Promozioni Sony | com.sony.dtv.promos | banner promozionali |
| Modalità demo negozio | com.sony.dtv.demosupport, com.sony.dtv.demosystemsupport, com.sony.dtv.multiscreendemo | demo da esposizione |
| Samba TV | tv.samba.ssm, tv.samba.ssm.ui | riconoscimento automatico dei contenuti: raccolta dati sulla visione a fini pubblicitari |
| Qterics (?) | com.qterics.da.product | analisi dati di utilizzo (probabile) |
| Google Feedback | com.google.android.feedback | invio segnalazioni a Google |
| Google Play Giochi | com.google.android.play.games | |
| YouTube Kids | com.google.android.youtube.tvkids | |
| YouTube Music | com.google.android.youtube.tvmusic | |
| TuneIn Radio | tunein.player | |
| Living Decor (?) | com.sony.dtv.livingfit | schermo "ambientale" decorativo |
| Eco dashboard | com.sony.dtv.ecodashboard | pannello consumi |

## Gruppo 2 · Decidi tu (dipende da cosa usi)

- **Streaming con tasto sul telecomando**: Netflix (com.netflix.ninja + com.netflix.partner.ncm), Prime Video (com.amazon.amazonvideo.livingroom), YouTube (com.google.android.youtube.tv). Spegnendoli il tasto dedicato non fa più nulla.
- **Streaming che hai in uso**: Apple TV (com.apple.atve.sony.appletv, nei preferiti), Disney+ (com.disney.disneyplus, nei preferiti), Spotify (com.spotify.tv.android).
- **Altri streaming**: DAZN (com.dazn), discovery+ (com.discovery.dplay), Crunchyroll (com.crunchyroll.crunchyroid), Rakuten TV (tv.wuaki.apptv), TIMVISION (it.telecomitalia.cubovision), Mediaset Infinity (it.mediaset.infinitytv), RaiPlay (it.rainet.androidtv), Google TV Film e serie (com.google.android.videos), BRAVIA CORE (com.sonypicturescore).
- **Apple**: AirPlay (com.sony.dtv.airplayapp), HomeKit (com.sony.dtv.homekit). Utili solo con iPhone/iPad/Mac.
- **Smart speaker**: abbinamento con Alexa/Google Home (com.sony.dtv.seeds.iot, com.sony.dtv.seeds.iot.remotestart, com.sony.dtv.seeds.auth).
- **BRAVIA CAM**: com.sony.dtv.sensingcamera.settings, com.sony.dtv.sensingcamera.fwupdate, com.sony.dtv.sensingfusionservice, com.sony.dtv.gestureapp, com.sony.dtv.distancealert. Inutili senza la telecamera opzionale.
- **Registrazione su USB**: com.sony.dtv.recapp, com.sony.dtv.recservice.
- **Rete domestica / DLNA**: com.sony.dtv.homenetwork, com.sony.huey.dlna.module, com.sony.huey.dlna.renderersettings, com.sonyericsson.dlna.
- **HbbTV** (servizi interattivi dei canali, es. tasto rosso Rai/Mediaset): com.sony.dtv.hbbtvplayer, com.sony.dtv.interactivetvutil, com.sony.dtv.interactivetvutil.output.
- **Browser internet**: com.sony.dtv.browser.webappplatform, .webappruntime, .webappruntimeextension, .webappservice (+ com.vewd.core.integration.dia, com.vewd.core.service).
- **Calibrazione**: Calman for BRAVIA (com.sony.dtv.calibrationmonitor).
- **Assistenza e guida**: com.sony.dtv.customersupport, com.sony.dtv.customersupport.provider.res, com.sony.dtv.smarthelp, com.sony.dtv.helpguidecontents.
- **Accessibilità**: TalkBack (com.google.android.marvin.talkback), tutorial accessibilità (com.sony.dtv.accessibility.tutorial).
- **Musica Sony (?)**: com.sony.dtv.osat.music.
- **Lettore multimediale USB**: com.sony.dtv.smartmediaapp (serve per foto/video da chiavetta).

## Gruppo 3 · Non toccare

Sintonizzatore e ingressi (com.sony.dtv.tvinput.*, com.sony.dtv.tvx*, com.sony.dtv.broadcast*, com.sony.dtv.citool),
audio (com.sony.dtv.audio*, acousticautocalibration, com.dolby.android.audio.service), HDMI e Bravia Sync
(com.sony.dtv.braviasync*), telecomando e tasti (remotecontrolsettings, globalkeyoverlay, irblaster, irbservice,
com.uei.quicksetsdk.sony), impostazioni e aggiornamenti (com.sony.dtv.settings*, systemupdate, update, upgradesetup,
oadservice, ondservice), Chromecast integrato (com.google.android.apps.mediashell), Assistente e ricerca vocale
(com.google.android.katniss, com.sony.dtv.voicesearch, com.sony.dtv.assistantadapter), Play Store e servizi Google
(com.android.vending, com.google.android.gms, ...), piattaforma MediaTek (com.mediatek.*, mtktvapi.agent),
modalità hotel/business (com.sony.dtv.b2b.*: dormienti, non pesano), tutto com.android.*.

Già disattivati dalla TV stessa (launcher regionali esteri): ITV, My5, All4, ARD, iPlayer, HD+, YouView.

## Eseguito il 30 ottobre 2026

Su indicazione dell'utente: tutto il gruppo 1 e il gruppo 2, **tenendo** BRAVIA CORE, AirPlay, HomeKit, Netflix,
Prime Video, YouTube, Apple TV, Disney+, Spotify e il browser internet.

- 40 app di sistema disattivate (`pm disable-user --user 0`).
- 9 app disinstallate per l'utente (`pm uninstall --user 0`): YouTube Kids, TuneIn, DAZN, discovery+, Crunchyroll,
  Rakuten TV, TIMVISION, Mediaset Infinity, RaiPlay.
- Ripristino completo: `ripristina-app-sony-vh22.sh` (riattiva tutto, rimette le app preinstallate).

Funzioni che non ci sono più finché non si ripristinano: tasto rosso HbbTV dei canali, registrazione su USB,
lettore multimediale USB, DLNA, abbinamento smart speaker, BRAVIA CAM, Calman, guida e assistenza Sony, TalkBack.
