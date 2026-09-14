# Migracja v0.6.0

1. Nie usuwaj starego katalogu `app/` podczas pierwszego testu.
2. Dodaj katalog `godot/` i plik `.github/workflows/build-godot-apk.yml` do repozytorium.
3. GitHub Actions uruchomi workflow `Build Godot Android APK`.
4. Po zielonym buildzie pobierz artifact `NocnyGaraz3D-Godot-v0.6.0`.
5. Zainstaluj APK i sprawdź start, biegi, tuning, kamerę i stabilność.
6. Dopiero po potwierdzeniu nowego APK możemy usunąć stary renderer Java/OpenGL z aktywnego builda.
