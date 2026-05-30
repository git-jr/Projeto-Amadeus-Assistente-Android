#!/bin/bash
# Concede permissão de acessibilidade ao Amadeus via ADB.
# Uso: ./grant_accessibility.sh (device/emulador deve estar conectado)
# Equivalente a: Configurações > Acessibilidade > Amadeus > Ativar
PKG="com.paradoxo.amadeus"
SVC="${PKG}/${PKG}.service.AmadeusAccessibilityService"

echo "Enabling accessibility service..."
adb shell settings put secure enabled_accessibility_services "$SVC"
adb shell settings put secure accessibility_enabled 1
echo "Done."
