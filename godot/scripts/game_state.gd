class_name GameState
extends RefCounted

const GAME_VERSION := "v0.6.0"
const RACE_DISTANCE_M := 402.336

enum Screen { GARAGE, MODE_SELECT, COUNTDOWN, RACING, RESULT }
enum Mode { CAREER, CASH_RUN }

const UPGRADE_NAMES := ["SILNIK", "TURBO", "SKRZYNIA", "ECU", "OPONY", "MASA"]

# Golf VII 1.2 TSI 85 KM, 5-biegowy manual.
const GEAR_RATIOS := [3.77, 1.96, 1.28, 0.88, 0.67]
const MAX_GEARS := 5
const FINAL_DRIVE := 4.06
const WHEEL_RADIUS_M := 0.31725

const BASE_HP := 85.0
const BASE_WEIGHT_KG := 1205.0
const IDLE_RPM := 950.0
const REV_LIMIT_RPM := 6000.0
const SHIFT_GREEN_LOW := 5150.0
const SHIFT_GREEN_HIGH := 5450.0

# Kalibracja przeniesiona z zaakceptowanego prototypu v0.4.x.
const DRIVETRAIN_EFFICIENCY := 0.884
const AIR_DENSITY := 1.225
const CDA := 0.63215
const ROLLING_RESISTANCE := 0.012
const GRAVITY := 9.81
const FIRST_GEAR_TRACTION := 0.56
const SECOND_GEAR_TRACTION := 0.32

var screen: int = Screen.GARAGE
var mode: int = Mode.CASH_RUN

var money: int = 1_000_000
var career_stage: int = 1
var wins: int = 0
var upgrades: Array[int] = [0, 0, 0, 0, 0, 0]

var gas_held := false
var rpm := IDLE_RPM
var gear := 1
var speed_kmh := 0.0
var player_distance := 0.0
var opponent_distance := 0.0
var race_time := 0.0
var countdown := 3.2
var launch_quality := 0.0
var shift_message := ""
var shift_message_time := 0.0
var player_finished := false
var opponent_finished := false
var player_finish_time := 0.0
var opponent_finish_time := 0.0
var last_win := false
var last_reward: int = 0
var shifting := false
var time_to_100 := -1.0

var _pending_gear := 1
var _shift_timer := 0.0
var _active_shift_duration := 0.0
var _shift_start_rpm := IDLE_RPM
var _launch_penalty := 0.0
var _launch_rpm_at_go := 4550.0
var _opponent_speed_kmh := 0.0
var _opponent_performance := 1.0
var _opponent_max_speed := 179.0

func _init() -> void:
    load_save()

func horsepower() -> float:
    return BASE_HP + upgrades[0] * 5.0 + upgrades[1] * 10.0 + upgrades[3] * 2.0

func weight_kg() -> float:
    return BASE_WEIGHT_KG - upgrades[5] * 25.0

func shift_duration() -> float:
    return max(0.35, 0.60 - upgrades[2] * 0.05)

func rating() -> int:
    var hp_gain := horsepower() - BASE_HP
    var weight_gain := BASE_WEIGHT_KG - weight_kg()
    return roundi(250.0 + hp_gain * 3.0 + weight_gain * 0.40 + upgrades[2] * 18.0 + upgrades[4] * 14.0)

func upgrade_cost(index: int) -> int:
    if index < 0 or index >= upgrades.size() or upgrades[index] >= 5:
        return 0
    var base := [22000, 28000, 18000, 16000, 14000, 17000]
    var level := upgrades[index] + 1
    return base[index] * level * level

func buy_upgrade(index: int) -> bool:
    if index < 0 or index >= upgrades.size() or upgrades[index] >= 5:
        return false
    var cost := upgrade_cost(index)
    if money < cost:
        return false
    money -= cost
    upgrades[index] += 1
    save()
    return true

func add_dev_cash() -> void:
    money += 500_000
    save()

func go_mode_select() -> void:
    gas_held = false
    screen = Screen.MODE_SELECT

func go_garage() -> void:
    gas_held = false
    screen = Screen.GARAGE

func start_race(selected: int) -> void:
    mode = selected
    screen = Screen.COUNTDOWN
    gas_held = false
    rpm = IDLE_RPM
    gear = 1
    _pending_gear = 1
    shifting = false
    _shift_timer = 0.0
    _active_shift_duration = 0.0
    speed_kmh = 0.0
    _opponent_speed_kmh = 0.0
    player_distance = 0.0
    opponent_distance = 0.0
    race_time = 0.0
    countdown = 3.2
    launch_quality = 0.0
    _launch_penalty = 0.0
    _launch_rpm_at_go = 4550.0
    shift_message = ""
    shift_message_time = 0.0
    player_finished = false
    opponent_finished = false
    player_finish_time = 0.0
    opponent_finish_time = 0.0
    last_reward = 0
    time_to_100 = -1.0

    var player_perf := performance_index()
    if mode == Mode.CAREER:
        _opponent_performance = 0.95 + max(0, career_stage - 1) * 0.045
    else:
        _opponent_performance = player_perf * randf_range(0.96, 1.04)
    _opponent_max_speed = 179.0 * pow(_opponent_performance, 0.34)

func set_gas(held: bool) -> void:
    gas_held = held

func max_rpm() -> float:
    return REV_LIMIT_RPM

func green_low() -> float:
    return SHIFT_GREEN_LOW

func green_high() -> float:
    return SHIFT_GREEN_HIGH

func launch_green_low() -> float:
    return 4050.0 - upgrades[4] * 35.0

func launch_green_high() -> float:
    return 5050.0 + upgrades[4] * 55.0

func shift() -> void:
    if screen != Screen.RACING or player_finished or shifting or gear >= MAX_GEARS:
        return

    var center := (green_low() + green_high()) * 0.5
    var half_width := (green_high() - green_low()) * 0.5
    var diff := abs(rpm - center)
    var extra_delay := 0.0

    if diff <= half_width * 0.35:
        shift_message = "PERFECT SHIFT"
    elif diff <= half_width:
        shift_message = "GOOD SHIFT"
        extra_delay = 0.03
    elif rpm < green_low():
        shift_message = "ZA WCZEŚNIE"
        extra_delay = 0.12
    else:
        shift_message = "ZA PÓŹNO"
        extra_delay = 0.15

    shift_message_time = 0.8
    shifting = true
    _pending_gear = gear + 1
    _active_shift_duration = shift_duration() + extra_delay
    _shift_timer = _active_shift_duration
    _shift_start_rpm = rpm

func update(delta: float) -> void:
    var dt := min(delta, 0.05)

    if shift_message_time > 0.0:
        shift_message_time = max(0.0, shift_message_time - dt)

    if screen == Screen.COUNTDOWN:
        _update_countdown(dt)
        return

    if screen != Screen.RACING:
        return

    race_time += dt

    if not player_finished:
        _update_player_physics(dt)
    if not opponent_finished:
        _update_opponent(dt)

    if player_finished and opponent_finished:
        _finish_race()
    elif player_finished and race_time > player_finish_time + 1.2:
        _finish_race()
    elif opponent_finished and race_time > opponent_finish_time + 1.2:
        _finish_race()

func _update_countdown(dt: float) -> void:
    # Start zaakceptowany przez użytkownika: gaz szybko podnosi RPM, puszczenie wolno je obniża.
    var rise_rate := 3600.0
    var fall_rate := 1080.0
    rpm += (rise_rate if gas_held else -fall_rate) * dt
    rpm = clampf(rpm, IDLE_RPM, REV_LIMIT_RPM + 140.0)
    countdown -= dt

    if countdown > 0.0:
        return

    var lo := launch_green_low()
    var hi := launch_green_high()
    var center := (lo + hi) * 0.5
    var half := (hi - lo) * 0.5
    var d := abs(rpm - center)

    launch_quality = clampf(1.0 - d / (half * 2.0), 0.0, 1.0)
    _launch_penalty = (1.0 - launch_quality) * 0.30
    _launch_rpm_at_go = rpm

    if d <= half * 0.35:
        shift_message = "PERFECT START"
    elif rpm >= lo and rpm <= hi:
        shift_message = "GOOD START"
    elif launch_quality > 0.45:
        shift_message = "OK START"
    else:
        shift_message = "SŁABY START"

    shift_message_time = 1.0
    screen = Screen.RACING

func _update_player_physics(dt: float) -> void:
    var mass := weight_kg()
    var speed_ms := speed_kmh / 3.6
    var drive_force := 0.0

    if shifting:
        _shift_timer -= dt
        var progress := 1.0 if _active_shift_duration <= 0.0 else clampf(1.0 - _shift_timer / _active_shift_duration, 0.0, 1.0)
        var target_rpm := max(IDLE_RPM, rpm_from_speed(speed_kmh, _pending_gear))
        var smooth := progress * progress * (3.0 - 2.0 * progress)
        rpm = lerpf(_shift_start_rpm, target_rpm, smooth)

        if _shift_timer <= 0.0:
            gear = _pending_gear
            shifting = false
            rpm = max(IDLE_RPM, rpm_from_speed(speed_kmh, gear))
    else:
        rpm = calculate_engine_rpm()
        var torque := engine_torque_nm(rpm) * horsepower() / BASE_HP
        drive_force = torque * GEAR_RATIOS[gear - 1] * FINAL_DRIVE * DRIVETRAIN_EFFICIENCY / WHEEL_RADIUS_M

        var tire_grip := 1.0 + upgrades[4] * 0.05
        if gear == 1:
            drive_force = min(drive_force, mass * GRAVITY * FIRST_GEAR_TRACTION * tire_grip)
        elif gear == 2:
            drive_force = min(drive_force, mass * GRAVITY * SECOND_GEAR_TRACTION * tire_grip)

        var launch_fade := max(0.0, 1.0 - race_time / 2.2)
        drive_force *= max(0.70, 1.0 - _launch_penalty * launch_fade)

        if rpm >= REV_LIMIT_RPM:
            drive_force *= 0.08
            rpm = REV_LIMIT_RPM - 70.0 + sin(race_time * 32.0) * 45.0

    var aero_force := 0.5 * AIR_DENSITY * CDA * speed_ms * speed_ms
    var rolling_force := ROLLING_RESISTANCE * mass * GRAVITY
    var accel_ms2 := (drive_force - aero_force - rolling_force) / mass

    speed_ms = max(0.0, speed_ms + accel_ms2 * dt)
    speed_kmh = speed_ms * 3.6

    if not shifting:
        rpm = calculate_engine_rpm()

    if time_to_100 < 0.0 and speed_kmh >= 100.0:
        time_to_100 = race_time

    player_distance += speed_ms * dt
    if player_distance >= RACE_DISTANCE_M:
        player_distance = RACE_DISTANCE_M
        player_finished = true
        player_finish_time = race_time

func calculate_engine_rpm() -> float:
    var coupled_rpm := max(IDLE_RPM, rpm_from_speed(speed_kmh, gear))

    if gear == 1 and race_time < 0.90:
        var coupling := clampf(race_time / 0.90, 0.0, 1.0)
        var slipping_rpm := lerpf(_launch_rpm_at_go, 1400.0, coupling)
        return max(coupled_rpm, slipping_rpm)

    return coupled_rpm

func rpm_from_speed(kmh: float, selected_gear: int) -> float:
    var index := clampi(selected_gear - 1, 0, MAX_GEARS - 1)
    var speed_ms := kmh / 3.6
    var wheel_rps := speed_ms / (TAU * WHEEL_RADIUS_M)
    return wheel_rps * 60.0 * GEAR_RATIOS[index] * FINAL_DRIVE

func engine_torque_nm(engine_rpm: float) -> float:
    var r := max(700.0, engine_rpm)
    if r < 800.0:
        return 70.0
    if r < 1400.0:
        return lerpf(90.0, 160.0, (r - 800.0) / 600.0)
    if r <= 3500.0:
        return 160.0
    if r <= 4300.0:
        return lerpf(160.0, 139.0, (r - 3500.0) / 800.0)
    if r <= 5300.0:
        return lerpf(139.0, 113.0, (r - 4300.0) / 1000.0)
    if r <= 6000.0:
        return lerpf(113.0, 80.0, (r - 5300.0) / 700.0)
    return 75.0

func performance_index() -> float:
    var power_part := horsepower() / BASE_HP
    var weight_part := pow(BASE_WEIGHT_KG / weight_kg(), 0.65)
    var gearbox_part := 1.0 + upgrades[2] * 0.025
    var tires_part := 1.0 + upgrades[4] * 0.015
    return power_part * weight_part * gearbox_part * tires_part

func _update_opponent(dt: float) -> void:
    var speed_ratio := 0.0 if _opponent_max_speed <= 1.0 else _opponent_speed_kmh / _opponent_max_speed
    var aero_fade := max(0.18, 1.0 - speed_ratio * speed_ratio * 0.82)
    var accel_kmh_per_sec := 9.3 * _opponent_performance * aero_fade
    if race_time < 0.25:
        accel_kmh_per_sec *= 0.75

    _opponent_speed_kmh = min(_opponent_max_speed, _opponent_speed_kmh + accel_kmh_per_sec * dt)
    opponent_distance += (_opponent_speed_kmh / 3.6) * dt

    if opponent_distance >= RACE_DISTANCE_M:
        opponent_distance = RACE_DISTANCE_M
        opponent_finished = true
        opponent_finish_time = race_time

func _finish_race() -> void:
    if screen == Screen.RESULT:
        return

    if not player_finished:
        player_finish_time = race_time + 9.0
    if not opponent_finished:
        opponent_finish_time = race_time + 9.0

    last_win = player_finish_time <= opponent_finish_time
    if last_win:
        wins += 1
        if mode == Mode.CAREER:
            last_reward = 150_000 + career_stage * 45_000
            career_stage += 1
        else:
            last_reward = 120_000 + rating() * 180
    else:
        last_reward = 20_000

    money += last_reward
    save()
    screen = Screen.RESULT

func money_text() -> String:
    return _format_money(money)

func reward_preview(which_mode: int) -> String:
    var value: int
    if which_mode == Mode.CAREER:
        value = 150_000 + career_stage * 45_000
    else:
        value = 120_000 + rating() * 180
    return _format_money(value)

func _format_money(value: int) -> String:
    var raw := str(value)
    var output := ""
    var count := 0
    for i in range(raw.length() - 1, -1, -1):
        if count > 0 and count % 3 == 0:
            output = " " + output
        output = raw[i] + output
        count += 1
    return output + " $"

func save() -> void:
    var cfg := ConfigFile.new()
    cfg.set_value("save", "money", money)
    cfg.set_value("save", "career_stage", career_stage)
    cfg.set_value("save", "wins", wins)
    cfg.set_value("save", "upgrades", upgrades)
    cfg.save("user://nocny_garaz_save.cfg")

func load_save() -> void:
    var cfg := ConfigFile.new()
    if cfg.load("user://nocny_garaz_save.cfg") != OK:
        return
    money = int(cfg.get_value("save", "money", 1_000_000))
    career_stage = int(cfg.get_value("save", "career_stage", 1))
    wins = int(cfg.get_value("save", "wins", 0))
    var loaded = cfg.get_value("save", "upgrades", upgrades)
    if loaded is Array and loaded.size() == 6:
        for i in range(6):
            upgrades[i] = clampi(int(loaded[i]), 0, 5)
