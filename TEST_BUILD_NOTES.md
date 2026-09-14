# Test build scope — v0.1

## Included
- 3D night drag-strip rendered natively with OpenGL ES 2.0.
- One fictional RWD coupe (player) plus an opponent car.
- One 1/4-mile industrial/night map.
- Launch mechanic: hold GAS during countdown, release/adjust to hit the green RPM band.
- Six-speed drag run with green shift zone and PERFECT / GOOD / EARLY / LATE feedback.
- Garage with six upgrade branches: engine, turbo, gearbox, ECU, tires, weight reduction.
- Five levels per branch.
- Career: opponent strength increases after every win; cash reward increases with stage.
- Cash Run: opponent scales near the player's current rating and remains slightly beatable for farming.
- Persistent local save.
- Deliberately generous test economy: $1,000,000 starting cash, large race payouts, and a DEV +$500,000 button.

## Intentionally not included yet
- Licensed/real-world vehicles or CSR assets.
- Multiple maps/cars.
- Online multiplayer, cloud saves, ads/IAP.
- Final art, audio, particles, car customization cosmetics, final economy balance.

## Tuning balance
The prototype favors fast iteration over realism. Upgrade numbers and payouts are exposed in `GameState.java` so they can be rebalanced quickly after phone testing.
