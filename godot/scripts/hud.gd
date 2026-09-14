class_name GameHUD
extends CanvasLayer

var state: GameState
var on_start_race: Callable
var on_go_garage: Callable
var on_go_modes: Callable
var on_shift: Callable
var on_gas: Callable

var garage_panel: Control
var mode_panel: Control
var race_panel: Control
var result_panel: Control

var money_label: Label
var garage_stats: Label
var version_label: Label
var mode_stage_label: Label
var race_top: Label
var countdown_label: Label
var shift_feedback: Label
var rpm_label: Label
var speed_label: Label
var gear_label: Label
var progress_bar: ProgressBar
var tach_bar: ProgressBar
var result_title: Label
var result_stats: Label
var gas_button: Button
var shift_button: Button
var upgrade_buttons: Array[Button] = []

func setup(game_state: GameState) -> void:
    state = game_state
    layer = 10
    _build_ui()
    refresh()

func refresh() -> void:
    if state == null:
        return

    garage_panel.visible = state.screen == GameState.Screen.GARAGE
    mode_panel.visible = state.screen == GameState.Screen.MODE_SELECT
    race_panel.visible = state.screen == GameState.Screen.COUNTDOWN or state.screen == GameState.Screen.RACING
    result_panel.visible = state.screen == GameState.Screen.RESULT

    money_label.text = state.money_text()
    version_label.text = GameState.GAME_VERSION + "  •  GODOT 4"
    garage_stats.text = "Volkswagen Golf VII 1.2 TSI\n%d KM  •  %.0f kg\nRATING %d\n5-biegowy manual" % [roundi(state.horsepower()), state.weight_kg(), state.rating()]

    for i in range(upgrade_buttons.size()):
        var level := state.upgrades[i]
        var suffix := "MAX" if level >= 5 else "%s\n%s" % ["LV %d/5" % level, state._format_money(state.upgrade_cost(i))]
        upgrade_buttons[i].text = "%s\n%s" % [GameState.UPGRADE_NAMES[i], suffix]

    mode_stage_label.text = "KARIERA — STAGE %d\nNagroda: %s\n\nCASH RUN\nNagroda: %s" % [
        state.career_stage,
        state.reward_preview(GameState.Mode.CAREER),
        state.reward_preview(GameState.Mode.CASH_RUN)
    ]

    if race_panel.visible:
        race_top.text = "%s    •    %.0f / 402 m" % [
            "KARIERA" if state.mode == GameState.Mode.CAREER else "CASH RUN",
            state.player_distance
        ]
        speed_label.text = "%03d\nkm/h" % roundi(state.speed_kmh)
        rpm_label.text = "%d RPM" % roundi(state.rpm)
        gear_label.text = "%d\nBIEG" % state.gear
        progress_bar.value = clampf(state.player_distance / GameState.RACE_DISTANCE_M * 100.0, 0.0, 100.0)
        tach_bar.max_value = state.max_rpm() + 300.0
        tach_bar.value = state.rpm

        if state.screen == GameState.Screen.COUNTDOWN:
            if state.countdown > 2.2:
                countdown_label.text = "3"
            elif state.countdown > 1.2:
                countdown_label.text = "2"
            elif state.countdown > 0.2:
                countdown_label.text = "1"
            else:
                countdown_label.text = "GO"
            countdown_label.visible = true
            shift_button.disabled = true
        else:
            countdown_label.visible = false
            shift_button.disabled = state.shifting

        shift_feedback.text = state.shift_message if state.shift_message_time > 0.0 else ""
        gas_button.modulate = Color(0.55, 1.0, 0.62) if state.gas_held else Color.WHITE

    if result_panel.visible:
        result_title.text = "WYGRANA" if state.last_win else "PRZEGRANA"
        var hundred := "—" if state.time_to_100 < 0.0 else "%.2f s" % state.time_to_100
        result_stats.text = "1/4 mili: %.3f s\n0–100 km/h: %s\nRywal: %.3f s\nNagroda: %s" % [
            state.player_finish_time,
            hundred,
            state.opponent_finish_time,
            state._format_money(state.last_reward)
        ]

func _build_ui() -> void:
    var root := Control.new()
    root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    add_child(root)

    var top_bar := ColorRect.new()
    top_bar.color = Color(0.015, 0.022, 0.035, 0.86)
    top_bar.set_anchors_preset(Control.PRESET_TOP_WIDE)
    top_bar.offset_bottom = 72.0
    root.add_child(top_bar)

    var title := _label("NOCNY GARAŻ 3D", 28, Color.WHITE, HORIZONTAL_ALIGNMENT_LEFT)
    title.position = Vector2(28, 16)
    title.size = Vector2(500, 48)
    top_bar.add_child(title)

    money_label = _label("", 24, Color(0.38, 1.0, 0.52), HORIZONTAL_ALIGNMENT_RIGHT)
    money_label.set_anchors_preset(Control.PRESET_TOP_RIGHT)
    money_label.position = Vector2(-430, 18)
    money_label.size = Vector2(400, 42)
    top_bar.add_child(money_label)

    garage_panel = Control.new()
    garage_panel.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    root.add_child(garage_panel)
    _build_garage(garage_panel)

    mode_panel = Control.new()
    mode_panel.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    root.add_child(mode_panel)
    _build_modes(mode_panel)

    race_panel = Control.new()
    race_panel.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    root.add_child(race_panel)
    _build_race(race_panel)

    result_panel = Control.new()
    result_panel.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    root.add_child(result_panel)
    _build_result(result_panel)

func _build_garage(parent: Control) -> void:
    var left := _panel(Color(0.020, 0.030, 0.047, 0.88))
    left.position = Vector2(28, 108)
    left.size = Vector2(430, 780)
    parent.add_child(left)

    var car_name := _label("GOLF VII 1.2 TSI", 36, Color.WHITE)
    car_name.position = Vector2(28, 28)
    car_name.size = Vector2(370, 60)
    left.add_child(car_name)

    garage_stats = _label("", 23, Color(0.80, 0.85, 0.92))
    garage_stats.position = Vector2(28, 105)
    garage_stats.size = Vector2(370, 180)
    left.add_child(garage_stats)

    version_label = _label("", 18, Color(0.52, 0.60, 0.72))
    version_label.position = Vector2(28, 300)
    version_label.size = Vector2(370, 44)
    left.add_child(version_label)

    var cash := _button("DEV +500 000 $", Vector2(28, 690), Vector2(370, 58))
    cash.pressed.connect(_dev_cash_pressed)
    left.add_child(cash)

    var grid := GridContainer.new()
    grid.columns = 2
    grid.position = Vector2(520, 116)
    grid.size = Vector2(870, 540)
    grid.add_theme_constant_override("h_separation", 18)
    grid.add_theme_constant_override("v_separation", 18)
    parent.add_child(grid)

    for i in range(6):
        var b := Button.new()
        b.custom_minimum_size = Vector2(420, 150)
        b.add_theme_font_size_override("font_size", 22)
        b.pressed.connect(_upgrade_pressed.bind(i))
        grid.add_child(b)
        upgrade_buttons.append(b)

    var race := _button("WYŚCIG  →", Vector2(1415, 870), Vector2(470, 118))
    race.add_theme_font_size_override("font_size", 32)
    race.pressed.connect(_go_modes_pressed)
    parent.add_child(race)

func _build_modes(parent: Control) -> void:
    mode_stage_label = _label("", 28, Color.WHITE, HORIZONTAL_ALIGNMENT_CENTER)
    mode_stage_label.position = Vector2(600, 170)
    mode_stage_label.size = Vector2(720, 300)
    parent.add_child(mode_stage_label)

    var career := _button("KARIERA", Vector2(500, 520), Vector2(430, 130))
    career.add_theme_font_size_override("font_size", 34)
    career.pressed.connect(_career_pressed)
    parent.add_child(career)

    var cash := _button("CASH RUN", Vector2(990, 520), Vector2(430, 130))
    cash.add_theme_font_size_override("font_size", 34)
    cash.pressed.connect(_cash_run_pressed)
    parent.add_child(cash)

    var back := _button("← GARAŻ", Vector2(40, 920), Vector2(270, 74))
    back.pressed.connect(_go_garage_pressed)
    parent.add_child(back)

func _build_race(parent: Control) -> void:
    race_top = _label("", 22, Color.WHITE, HORIZONTAL_ALIGNMENT_CENTER)
    race_top.position = Vector2(560, 18)
    race_top.size = Vector2(800, 52)
    parent.add_child(race_top)

    progress_bar = ProgressBar.new()
    progress_bar.position = Vector2(520, 76)
    progress_bar.size = Vector2(880, 12)
    progress_bar.show_percentage = false
    parent.add_child(progress_bar)

    countdown_label = _label("3", 98, Color(0.35, 1.0, 0.42), HORIZONTAL_ALIGNMENT_CENTER)
    countdown_label.position = Vector2(710, 145)
    countdown_label.size = Vector2(500, 130)
    parent.add_child(countdown_label)

    shift_feedback = _label("", 44, Color(0.35, 1.0, 0.42), HORIZONTAL_ALIGNMENT_CENTER)
    shift_feedback.position = Vector2(560, 235)
    shift_feedback.size = Vector2(800, 80)
    parent.add_child(shift_feedback)

    speed_label = _label("0\nkm/h", 34, Color.WHITE, HORIZONTAL_ALIGNMENT_CENTER)
    speed_label.position = Vector2(530, 830)
    speed_label.size = Vector2(220, 130)
    parent.add_child(speed_label)

    rpm_label = _label("950 RPM", 36, Color.WHITE, HORIZONTAL_ALIGNMENT_CENTER)
    rpm_label.position = Vector2(760, 815)
    rpm_label.size = Vector2(400, 70)
    parent.add_child(rpm_label)

    tach_bar = ProgressBar.new()
    tach_bar.position = Vector2(720, 892)
    tach_bar.size = Vector2(480, 30)
    tach_bar.show_percentage = false
    parent.add_child(tach_bar)

    gear_label = _label("1\nBIEG", 34, Color.WHITE, HORIZONTAL_ALIGNMENT_CENTER)
    gear_label.position = Vector2(1180, 830)
    gear_label.size = Vector2(220, 130)
    parent.add_child(gear_label)

    gas_button = _button("GAZ", Vector2(55, 805), Vector2(260, 190))
    gas_button.add_theme_font_size_override("font_size", 36)
    gas_button.button_down.connect(_gas_down)
    gas_button.button_up.connect(_gas_up)
    parent.add_child(gas_button)

    shift_button = _button("SHIFT", Vector2(1600, 805), Vector2(260, 190))
    shift_button.add_theme_font_size_override("font_size", 36)
    shift_button.pressed.connect(_shift_pressed)
    parent.add_child(shift_button)

func _build_result(parent: Control) -> void:
    var card := _panel(Color(0.015, 0.025, 0.040, 0.94))
    card.position = Vector2(610, 215)
    card.size = Vector2(700, 610)
    parent.add_child(card)

    result_title = _label("WYGRANA", 52, Color(0.35, 1.0, 0.42), HORIZONTAL_ALIGNMENT_CENTER)
    result_title.position = Vector2(50, 45)
    result_title.size = Vector2(600, 80)
    card.add_child(result_title)

    result_stats = _label("", 28, Color.WHITE, HORIZONTAL_ALIGNMENT_CENTER)
    result_stats.position = Vector2(60, 150)
    result_stats.size = Vector2(580, 250)
    card.add_child(result_stats)

    var garage := _button("GARAŻ", Vector2(55, 470), Vector2(270, 85))
    garage.pressed.connect(_go_garage_pressed)
    card.add_child(garage)

    var again := _button("JESZCZE RAZ", Vector2(375, 470), Vector2(270, 85))
    again.pressed.connect(_again_pressed)
    card.add_child(again)


func _dev_cash_pressed() -> void:
    state.add_dev_cash()
    refresh()

func _go_modes_pressed() -> void:
    if on_go_modes.is_valid():
        on_go_modes.call()

func _career_pressed() -> void:
    if on_start_race.is_valid():
        on_start_race.call(GameState.Mode.CAREER)

func _cash_run_pressed() -> void:
    if on_start_race.is_valid():
        on_start_race.call(GameState.Mode.CASH_RUN)

func _go_garage_pressed() -> void:
    if on_go_garage.is_valid():
        on_go_garage.call()

func _again_pressed() -> void:
    if on_start_race.is_valid():
        on_start_race.call(state.mode)

func _gas_down() -> void:
    if on_gas.is_valid():
        on_gas.call(true)

func _gas_up() -> void:
    if on_gas.is_valid():
        on_gas.call(false)

func _shift_pressed() -> void:
    if on_shift.is_valid():
        on_shift.call()

func _upgrade_pressed(index: int) -> void:
    state.buy_upgrade(index)
    refresh()

func _panel(color: Color) -> ColorRect:
    var panel := ColorRect.new()
    panel.color = color
    return panel

func _label(text_value: String, font_size: int, color: Color, align := HORIZONTAL_ALIGNMENT_LEFT) -> Label:
    var label := Label.new()
    label.text = text_value
    label.add_theme_font_size_override("font_size", font_size)
    label.add_theme_color_override("font_color", color)
    label.horizontal_alignment = align
    label.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
    label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    return label

func _button(text_value: String, pos: Vector2, size_value: Vector2) -> Button:
    var button := Button.new()
    button.text = text_value
    button.position = pos
    button.size = size_value
    button.add_theme_font_size_override("font_size", 24)
    return button
