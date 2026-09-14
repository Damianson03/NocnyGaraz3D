extends Node

var state: GameState
var world: WorldBuilder
var hud: GameHUD

func _ready() -> void:
    randomize()
    state = GameState.new()

    world = WorldBuilder.new()
    add_child(world)
    world.setup(state)

    hud = GameHUD.new()
    add_child(hud)
    hud.on_start_race = Callable(self, "_start_race")
    hud.on_go_garage = Callable(self, "_go_garage")
    hud.on_go_modes = Callable(self, "_go_modes")
    hud.on_shift = Callable(self, "_shift")
    hud.on_gas = Callable(self, "_set_gas")
    hud.setup(state)

func _process(delta: float) -> void:
    state.update(delta)
    world.update_visuals(delta)
    hud.refresh()

    if Input.is_action_just_pressed("shift"):
        state.shift()
    state.set_gas(Input.is_action_pressed("gas") or state.gas_held)

func _notification(what: int) -> void:
    if what == NOTIFICATION_WM_CLOSE_REQUEST:
        if state != null:
            state.save()

func _start_race(which_mode: int) -> void:
    state.start_race(which_mode)
    hud.refresh()

func _go_garage() -> void:
    state.go_garage()
    hud.refresh()

func _go_modes() -> void:
    state.go_mode_select()
    hud.refresh()

func _shift() -> void:
    state.shift()

func _set_gas(held: bool) -> void:
    state.set_gas(held)
