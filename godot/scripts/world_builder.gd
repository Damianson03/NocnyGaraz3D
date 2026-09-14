class_name WorldBuilder
extends Node3D

const PLAYER_MODEL_PATH := "res://assets/cars/golf7.glb"
const RIVAL_MODEL_PATH := "res://assets/cars/rival.glb"

var state: GameState
var player_car: Node3D
var rival_car: Node3D
var camera: Camera3D

var _camera_pos := Vector3.ZERO
var _camera_target := Vector3.ZERO
var _player_model_loaded := false

func setup(game_state: GameState) -> void:
    state = game_state
    _create_environment()
    _create_track()
    player_car = _create_car(false)
    rival_car = _create_car(true)
    add_child(player_car)
    add_child(rival_car)
    player_car.position = Vector3(-1.72, 0.0, 0.0)
    rival_car.position = Vector3(1.72, 0.0, 0.0)

    camera = Camera3D.new()
    camera.current = true
    camera.fov = 48.0
    camera.near = 0.08
    camera.far = 900.0
    add_child(camera)

    _camera_pos = Vector3(-3.2, 1.55, -7.6)
    _camera_target = Vector3(-0.2, 0.85, 0.0)
    camera.position = _camera_pos
    camera.look_at(_camera_target, Vector3.UP)

func update_visuals(delta: float) -> void:
    if state == null or player_car == null:
        return

    if state.screen == GameState.Screen.GARAGE or state.screen == GameState.Screen.MODE_SELECT:
        _update_garage_camera(delta)
        player_car.position = Vector3(0.0, 0.0, 0.0)
        rival_car.visible = false
        return

    rival_car.visible = true
    player_car.position = Vector3(-1.72, 0.0, -state.player_distance)
    rival_car.position = Vector3(1.72, 0.0, -state.opponent_distance)
    _update_race_camera(delta)

func _create_environment() -> void:
    var world := WorldEnvironment.new()
    var env := Environment.new()
    env.background_mode = Environment.BG_COLOR
    env.background_color = Color(0.006, 0.009, 0.020)
    env.background_energy_multiplier = 0.55
    env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    env.ambient_light_color = Color(0.18, 0.22, 0.32)
    env.ambient_light_energy = 0.55
    env.tonemap_mode = Environment.TONE_MAPPER_FILMIC
    env.glow_enabled = true
    env.glow_intensity = 0.7
    env.fog_enabled = true
    env.fog_light_color = Color(0.08, 0.10, 0.16)
    env.fog_density = 0.0028
    env.fog_sky_affect = 0.55
    world.environment = env
    add_child(world)

    var moon := DirectionalLight3D.new()
    moon.rotation_degrees = Vector3(-48.0, -32.0, 0.0)
    moon.light_color = Color(0.55, 0.65, 1.0)
    moon.light_energy = 0.75
    moon.shadow_enabled = true
    moon.directional_shadow_max_distance = 120.0
    add_child(moon)

func _create_track() -> void:
    var road_mat := _material(Color(0.035, 0.040, 0.052), 0.28, 0.82)
    var wet_mat := _material(Color(0.085, 0.095, 0.12), 0.15, 0.28)
    var concrete := _material(Color(0.18, 0.19, 0.21), 0.0, 0.92)
    var stripe := _material(Color(0.82, 0.78, 0.34), 0.0, 0.72)

    add_child(_box("Road", Vector3(11.8, 0.18, 430.0), Vector3(0.0, -0.13, -205.0), road_mat))
    add_child(_box("WetLayer", Vector3(10.5, 0.012, 430.0), Vector3(0.0, -0.025, -205.0), wet_mat))
    add_child(_box("LeftShoulder", Vector3(1.5, 0.18, 430.0), Vector3(-6.7, -0.12, -205.0), concrete))
    add_child(_box("RightShoulder", Vector3(1.5, 0.18, 430.0), Vector3(6.7, -0.12, -205.0), concrete))

    for z in range(8, 421, 14):
        add_child(_box("LaneMark", Vector3(0.10, 0.025, 4.3), Vector3(0.0, -0.002, -float(z)), stripe))

    var white := _material(Color(0.92, 0.92, 0.94), 0.0, 0.6)
    var green := _material(Color(0.12, 0.95, 0.35), 0.0, 0.45)
    add_child(_box("StartLine", Vector3(10.4, 0.025, 0.32), Vector3(0.0, 0.0, -2.8), white))
    add_child(_box("FinishLine", Vector3(10.4, 0.025, 0.50), Vector3(0.0, 0.0, -402.336), green))

    _create_barriers(concrete)
    _create_lamps()
    _create_city()
    _create_start_tree()

func _create_barriers(mat: StandardMaterial3D) -> void:
    for z in range(0, 421, 12):
        for side in [-1.0, 1.0]:
            var x := side * 7.55
            add_child(_box("Barrier", Vector3(0.55, 0.80, 5.6), Vector3(x, 0.36, -float(z)), mat))

func _create_lamps() -> void:
    var pole_mat := _material(Color(0.12, 0.13, 0.16), 0.62, 0.48)
    var lamp_mat := _material(Color(1.0, 0.72, 0.38), 0.1, 0.2, Color(1.0, 0.48, 0.12), 2.8)

    for z in range(10, 421, 38):
        for side in [-1.0, 1.0]:
            var x := side * 7.0
            add_child(_box("LampPole", Vector3(0.10, 4.8, 0.10), Vector3(x, 2.4, -float(z)), pole_mat))
            add_child(_box("LampHead", Vector3(0.44, 0.12, 0.44), Vector3(x, 4.82, -float(z)), lamp_mat))

            if z <= 200:
                var light := OmniLight3D.new()
                light.position = Vector3(x, 4.65, -float(z))
                light.omni_range = 11.0
                light.light_energy = 2.0
                light.light_color = Color(1.0, 0.62, 0.32)
                light.shadow_enabled = false
                add_child(light)

func _create_city() -> void:
    var dark_a := _material(Color(0.024, 0.030, 0.052), 0.0, 0.95)
    var dark_b := _material(Color(0.034, 0.040, 0.062), 0.0, 0.94)
    var window_warm := _material(Color(0.5, 0.28, 0.08), 0.0, 0.5, Color(1.0, 0.35, 0.08), 1.8)
    var window_cool := _material(Color(0.06, 0.18, 0.24), 0.0, 0.45, Color(0.08, 0.48, 0.72), 1.2)

    for i in range(14):
        var z := -(float(i) * 31.0 + 18.0)
        var h_left := 5.0 + float(i % 4) * 1.5
        var h_right := 4.5 + float((i + 2) % 5) * 1.2
        add_child(_box("BuildingL", Vector3(7.0, h_left, 17.0), Vector3(-14.0, h_left * 0.5 - 0.1, z), dark_a))
        add_child(_box("BuildingR", Vector3(7.5, h_right, 18.0), Vector3(14.5, h_right * 0.5 - 0.1, z - 8.0), dark_b))

        for row in range(3):
            var wy := 1.3 + float(row) * 1.45
            if wy < h_left - 0.4:
                add_child(_box("WindowL", Vector3(0.08, 0.28, 2.4), Vector3(-10.45, wy, z + 2.0), window_cool if row % 2 == 0 else window_warm))
            if wy < h_right - 0.4:
                add_child(_box("WindowR", Vector3(0.08, 0.28, 2.4), Vector3(10.70, wy, z - 4.0), window_warm if row % 2 == 0 else window_cool))

func _create_start_tree() -> void:
    var metal := _material(Color(0.10, 0.11, 0.13), 0.65, 0.42)
    var amber := _material(Color(0.9, 0.48, 0.05), 0.1, 0.3, Color(1.0, 0.30, 0.02), 2.2)
    var green := _material(Color(0.05, 0.65, 0.18), 0.1, 0.3, Color(0.0, 1.0, 0.24), 2.8)

    var root := Node3D.new()
    root.name = "StartTree"
    root.position = Vector3(4.7, 0.0, -4.8)
    add_child(root)
    root.add_child(_box("Post", Vector3(0.14, 3.6, 0.14), Vector3(0.0, 1.8, 0.0), metal))

    for i in range(4):
        var mat := green if i == 3 else amber
        root.add_child(_sphere("Signal", 0.13, Vector3(0.0, 2.65 - i * 0.42, -0.04), mat))

func _create_car(opponent: bool) -> Node3D:
    var path := RIVAL_MODEL_PATH if opponent else PLAYER_MODEL_PATH
    if ResourceLoader.exists(path):
        var resource = load(path)
        if resource is PackedScene:
            var instance := (resource as PackedScene).instantiate()
            if instance is Node3D:
                var model := instance as Node3D
                model.name = "RivalCar" if opponent else "PlayerGolf7"
                model.scale = Vector3.ONE
                model.rotation_degrees.y = 180.0
                if not opponent:
                    _player_model_loaded = true
                return model

    return _create_placeholder_car(opponent)

func _create_placeholder_car(opponent: bool) -> Node3D:
    var car := Node3D.new()
    car.name = "RivalPlaceholder" if opponent else "Golf7Placeholder"

    var paint_color := Color(0.20, 0.24, 0.30) if opponent else Color(0.72, 0.018, 0.028)
    var paint := _material(paint_color, 0.62, 0.22)
    var paint_hi := _material(paint_color.lightened(0.12), 0.62, 0.20)
    var glass := _material(Color(0.025, 0.045, 0.070), 0.25, 0.12)
    var black := _material(Color(0.018, 0.018, 0.022), 0.15, 0.55)
    var chrome := _material(Color(0.38, 0.40, 0.44), 0.9, 0.22)
    var headlight := _material(Color(0.72, 0.86, 1.0), 0.0, 0.12, Color(0.48, 0.72, 1.0), 2.2)
    var taillight := _material(Color(0.82, 0.02, 0.025), 0.0, 0.18, Color(1.0, 0.0, 0.0), 1.8)

    car.add_child(_box("Body", Vector3(1.78, 0.44, 3.95), Vector3(0.0, 0.42, 0.0), paint))
    car.add_child(_box("Shoulder", Vector3(1.68, 0.20, 2.65), Vector3(0.0, 0.73, 0.12), paint_hi))
    car.add_child(_box("Bonnet", Vector3(1.62, 0.20, 0.90), Vector3(0.0, 0.68, -1.14), paint_hi))
    car.add_child(_box("Cabin", Vector3(1.34, 0.40, 1.95), Vector3(0.0, 1.00, 0.12), glass))
    car.add_child(_box("Roof", Vector3(1.12, 0.12, 1.24), Vector3(0.0, 1.27, 0.28), paint))
    car.add_child(_box("FrontBumper", Vector3(1.66, 0.18, 0.18), Vector3(0.0, 0.29, -1.98), black))
    car.add_child(_box("RearBumper", Vector3(1.66, 0.18, 0.18), Vector3(0.0, 0.29, 1.98), black))
    car.add_child(_box("Grille", Vector3(1.34, 0.14, 0.04), Vector3(0.0, 0.46, -2.00), black))
    car.add_child(_box("HeadL", Vector3(0.35, 0.13, 0.06), Vector3(-0.53, 0.58, -2.00), headlight))
    car.add_child(_box("HeadR", Vector3(0.35, 0.13, 0.06), Vector3(0.53, 0.58, -2.00), headlight))
    car.add_child(_box("TailL", Vector3(0.30, 0.14, 0.06), Vector3(-0.54, 0.59, 2.00), taillight))
    car.add_child(_box("TailR", Vector3(0.30, 0.14, 0.06), Vector3(0.54, 0.59, 2.00), taillight))

    for wheel_z in [-1.20, 1.26]:
        for wheel_x in [-0.91, 0.91]:
            car.add_child(_wheel(Vector3(wheel_x, 0.28, wheel_z), black, chrome))

    return car

func _wheel(pos: Vector3, tire_mat: StandardMaterial3D, rim_mat: StandardMaterial3D) -> Node3D:
    var root := Node3D.new()
    root.position = pos

    var tire_mesh := CylinderMesh.new()
    tire_mesh.top_radius = 0.31
    tire_mesh.bottom_radius = 0.31
    tire_mesh.height = 0.22
    tire_mesh.radial_segments = 18
    var tire := MeshInstance3D.new()
    tire.mesh = tire_mesh
    tire.material_override = tire_mat
    tire.rotation_degrees.z = 90.0
    root.add_child(tire)

    var rim_mesh := CylinderMesh.new()
    rim_mesh.top_radius = 0.18
    rim_mesh.bottom_radius = 0.18
    rim_mesh.height = 0.235
    rim_mesh.radial_segments = 14
    var rim := MeshInstance3D.new()
    rim.mesh = rim_mesh
    rim.material_override = rim_mat
    rim.rotation_degrees.z = 90.0
    root.add_child(rim)
    return root

func _update_garage_camera(delta: float) -> void:
    var time := Time.get_ticks_msec() * 0.001
    var angle := deg_to_rad(18.0 + sin(time * 0.23) * 7.0)
    var desired := Vector3(sin(angle) * 7.8, 2.45, cos(angle) * 7.8)
    var target := Vector3(0.0, 0.72, 0.05)
    _apply_camera(delta, desired, target, 48.0, 4.0)

func _update_race_camera(delta: float) -> void:
    var player_z := -state.player_distance
    var speed_blend := clampf(state.speed_kmh / 180.0, 0.0, 1.0)
    var blend := 0.0 if state.screen == GameState.Screen.COUNTDOWN else _smoothstep(0.0, 1.35, state.race_time)

    # Kamera startowa: nisko z przodu / 3/4. Po starcie płynne przejście do bocznego śledzenia.
    var front_pos := Vector3(-3.0, 1.55, player_z - 7.3)
    var side_pos := Vector3(7.6, 2.05, player_z + 1.9)
    var front_target := Vector3(-0.2, 0.82, player_z - 0.2)
    var side_target := Vector3(0.0, 0.88, player_z - 8.5)

    var desired_pos := front_pos.lerp(side_pos, blend)
    var desired_target := front_target.lerp(side_target, blend)
    var desired_fov := lerpf(47.0, 58.0, speed_blend)

    var shake := sin(Time.get_ticks_msec() * 0.036) * speed_blend * 0.018
    desired_pos.y += shake
    desired_target.y += shake * 0.5
    _apply_camera(delta, desired_pos, desired_target, desired_fov, 7.5)

func _apply_camera(delta: float, desired_pos: Vector3, desired_target: Vector3, desired_fov: float, speed: float) -> void:
    var alpha := 1.0 - exp(-speed * delta)
    _camera_pos = _camera_pos.lerp(desired_pos, alpha)
    _camera_target = _camera_target.lerp(desired_target, alpha)
    camera.position = _camera_pos
    camera.fov = lerpf(camera.fov, desired_fov, alpha)
    camera.look_at(_camera_target, Vector3.UP)

func _box(name_value: String, size: Vector3, pos: Vector3, mat: Material) -> MeshInstance3D:
    var mesh := BoxMesh.new()
    mesh.size = size
    var node := MeshInstance3D.new()
    node.name = name_value
    node.mesh = mesh
    node.position = pos
    node.material_override = mat
    return node

func _sphere(name_value: String, radius: float, pos: Vector3, mat: Material) -> MeshInstance3D:
    var mesh := SphereMesh.new()
    mesh.radius = radius
    mesh.height = radius * 2.0
    mesh.radial_segments = 16
    mesh.rings = 8
    var node := MeshInstance3D.new()
    node.name = name_value
    node.mesh = mesh
    node.position = pos
    node.material_override = mat
    return node

func _material(base: Color, metallic := 0.0, roughness := 0.6, emission := Color.BLACK, emission_energy := 0.0) -> StandardMaterial3D:
    var mat := StandardMaterial3D.new()
    mat.albedo_color = base
    mat.metallic = metallic
    mat.roughness = roughness
    if emission_energy > 0.0:
        mat.emission_enabled = true
        mat.emission = emission
        mat.emission_energy_multiplier = emission_energy
    return mat

func _smoothstep(edge0: float, edge1: float, x: float) -> float:
    var t := clampf((x - edge0) / max(0.0001, edge1 - edge0), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)
