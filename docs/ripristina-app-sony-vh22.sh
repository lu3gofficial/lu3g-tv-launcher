#!/bin/sh
# Ripristino delle app Sony VH22 disattivate/disinstallate il 30/10/2026.
# Uso: sh ripristina-app-sony-vh22.sh   (TV collegata via adb)
ADB=${ADB:-adb}

# App di sistema disattivate: si riattivano così
$ADB shell pm enable com.sony.dtv.sonyselect
$ADB shell pm enable com.sony.dtv.promos
$ADB shell pm enable com.sony.dtv.demosupport
$ADB shell pm enable com.sony.dtv.demosystemsupport
$ADB shell pm enable com.sony.dtv.multiscreendemo
$ADB shell pm enable tv.samba.ssm
$ADB shell pm enable tv.samba.ssm.ui
$ADB shell pm enable com.qterics.da.product
$ADB shell pm enable com.google.android.feedback
$ADB shell pm enable com.google.android.play.games
$ADB shell pm enable com.google.android.youtube.tvmusic
$ADB shell pm enable com.sony.dtv.livingfit
$ADB shell pm enable com.sony.dtv.ecodashboard
$ADB shell pm enable com.google.android.videos
$ADB shell pm enable com.sony.dtv.seeds.iot
$ADB shell pm enable com.sony.dtv.seeds.iot.remotestart
$ADB shell pm enable com.sony.dtv.seeds.auth
$ADB shell pm enable com.sony.dtv.sensingcamera.settings
$ADB shell pm enable com.sony.dtv.sensingcamera.fwupdate
$ADB shell pm enable com.sony.dtv.sensingfusionservice
$ADB shell pm enable com.sony.dtv.gestureapp
$ADB shell pm enable com.sony.dtv.distancealert
$ADB shell pm enable com.sony.dtv.recapp
$ADB shell pm enable com.sony.dtv.recservice
$ADB shell pm enable com.sony.dtv.homenetwork
$ADB shell pm enable com.sony.huey.dlna.module
$ADB shell pm enable com.sony.huey.dlna.renderersettings
$ADB shell pm enable com.sonyericsson.dlna
$ADB shell pm enable com.sony.dtv.hbbtvplayer
$ADB shell pm enable com.sony.dtv.interactivetvutil
$ADB shell pm enable com.sony.dtv.interactivetvutil.output
$ADB shell pm enable com.sony.dtv.calibrationmonitor
$ADB shell pm enable com.sony.dtv.customersupport
$ADB shell pm enable com.sony.dtv.customersupport.provider.res
$ADB shell pm enable com.sony.dtv.smarthelp
$ADB shell pm enable com.sony.dtv.helpguidecontents
$ADB shell pm enable com.google.android.marvin.talkback
$ADB shell pm enable com.sony.dtv.accessibility.tutorial
$ADB shell pm enable com.sony.dtv.osat.music
$ADB shell pm enable com.sony.dtv.smartmediaapp
# App disinstallate per l'utente: erano preinstallate, si rimettono così (altrimenti dal Play Store)
$ADB shell cmd package install-existing --user 0 com.google.android.youtube.tvkids
$ADB shell cmd package install-existing --user 0 tunein.player
$ADB shell cmd package install-existing --user 0 com.dazn
$ADB shell cmd package install-existing --user 0 com.discovery.dplay
$ADB shell cmd package install-existing --user 0 com.crunchyroll.crunchyroid
$ADB shell cmd package install-existing --user 0 tv.wuaki.apptv
$ADB shell cmd package install-existing --user 0 it.telecomitalia.cubovision
$ADB shell cmd package install-existing --user 0 it.mediaset.infinitytv
$ADB shell cmd package install-existing --user 0 it.rainet.androidtv
