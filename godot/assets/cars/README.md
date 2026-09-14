# Modele samochodów

Godot automatycznie załaduje model gracza z:

`res://assets/cars/golf7.glb`

oraz model rywala z:

`res://assets/cars/rival.glb`

Jeśli plików jeszcze nie ma, gra używa tymczasowego proceduralnego auta 3D.

## Zalecenia dla docelowego modelu
- format: glTF 2.0 / `.glb`
- przód auta skierowany w stronę osi +Z lub -Z; orientację można skorygować w `world_builder.gd`
- realna skala w metrach
- 4 koła jako osobne węzły, jeśli później chcemy animować obrót
- materiały PBR: Base Color, Roughness, Metallic, Normal
- tekstury 1K–2K na Androidzie
- docelowo 60–120 tys. trójkątów dla auta gracza, mniej dla rywala/LOD
