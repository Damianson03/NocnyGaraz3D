# Nocny Garaż 3D — Godot v0.6.0

To jest migracja gry z natywnego Android/Java/OpenGL ES do Godot 4.

## Co już przeniesiono
- garaż i 6 kategorii tuningu
- lokalny zapis
- Career i Cash Run
- start z kontrolą RPM
- Perfect/Good/Early/Late Shift
- realny 5-biegowy Golf VII 1.2 TSI 85 KM
- 1205 kg, 11,9 s 0–100 jako cel kalibracji, 179 km/h
- realne przełożenia 3.77 / 1.96 / 1.28 / 0.88 / 0.67
- opóźnienie zmiany biegu 0,60 s → 0,35 s po tuningu
- kamera startowa przód/3⁄4 → płynnie boczna
- pełna scena Node3D z oświetleniem i materiałami PBR
- automatyczne ładowanie `.glb` dla samochodu

## Najważniejsze
Wersja v0.6.0 ma przede wszystkim potwierdzić, że nowy silnik działa na telefonie i buduje się z GitHub Actions. Proceduralny model auta jest fallbackiem — docelową grafikę uzyskamy po dodaniu prawdziwego modelu `golf7.glb`, tekstur i assetów otoczenia.
