#!/bin/sh
# Pubblica una release del launcher su GitHub (lu3gofficial/lu3g-tv-launcher).
# Il launcher sulla TV controlla queste release e propone l'aggiornamento da solo.
#
# Uso:  tools/release.sh "Note di versione"
# Prima: aggiorna versionCode e versionName in app/build.gradle.kts e fai il commit.
set -e
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
VERSION=$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -1)
NOTES="${1:-Aggiornamento $VERSION}"
[ -n "$VERSION" ] || { echo "versionName non trovata"; exit 1; }
[ -f keystore.properties ] || { echo "manca keystore.properties: la release va firmata con la chiave LU3G"; exit 1; }
echo "Compilo la versione $VERSION..."
./gradlew assembleRelease -q
APK="lu3g-tv-launcher-$VERSION.apk"
cp app/build/outputs/apk/release/app-release.apk "$APK"
git tag -a "v$VERSION" -m "LU3G TV $VERSION" 2>/dev/null || echo "tag v$VERSION già presente"
git push origin "v$VERSION" 2>/dev/null || true
gh release create "v$VERSION" "$APK" --repo lu3gofficial/lu3g-tv-launcher --title "LU3G TV $VERSION" --notes "$NOTES"
rm -f "$APK"
echo "Release v$VERSION pubblicata: le TV la vedranno al prossimo controllo (all'avvio o entro 12 ore)."
