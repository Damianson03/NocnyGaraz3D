package pl.nocnygaraz.game;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;
import java.util.Random;

public final class GameState {

    public static final String GAME_VERSION = "v0.4.0";

    public enum Screen { GARAGE, MODE_SELECT, COUNTDOWN, RACING, RESULT }
    public enum Mode { CAREER, CASH_RUN }

    public static final String[] UPGRADE_NAMES = {
            "SILNIK", "TURBO", "SKRZYNIA", "ECU", "OPONY", "MASA"
    };

    // Golf VII 1.2 TSI 85 KM, 5-biegowy manual.
    private static final float[] GEAR_RATIOS = {
            3.77f, 1.96f, 1.28f, 0.88f, 0.67f
    };

    private static final int MAX_GEARS = 5;
    private static final float FINAL_DRIVE = 4.06f;
    private static final float WHEEL_RADIUS_M = 0.31725f; // 195/65 R15

    private static final float BASE_HP = 85f;
    private static final float BASE_WEIGHT_KG = 1205f;

    private static final float IDLE_RPM = 950f;
    private static final float REV_LIMIT_RPM = 6000f;

    // Zielona strefa zmiany biegu. Środek = 5300 rpm.
    private static final float SHIFT_GREEN_LOW = 5150f;
    private static final float SHIFT_GREEN_HIGH = 5450f;

    // Kalibracja seryjnego auta:
    // ok. 11,9 s 0-100 i ok. 179 km/h vmax przy perfect launch/shift.
    private static final float DRIVETRAIN_EFFICIENCY = 0.884f;
    private static final float AIR_DENSITY = 1.225f;
    private static final float CDA = 0.63215f;
    private static final float ROLLING_RESISTANCE = 0.012f;
    private static final float GRAVITY = 9.81f;

    // Bazowa trakcja FWD. Opony dodają +5% / poziom.
    private static final float FIRST_GEAR_TRACTION = 0.56f;
    private static final float SECOND_GEAR_TRACTION = 0.32f;

    private final SharedPreferences prefs;
    private final Random random = new Random();

    public Screen screen = Screen.GARAGE;
    public Mode mode = Mode.CASH_RUN;

    public long money;
    public int careerStage;
    public int wins;

    public final int[] upgrades = new int[6];

    public boolean gasHeld = false;
    public float rpm = IDLE_RPM;
    public int gear = 1;
    public float speedKmh = 0f;

    public float playerDistance = 0f;
    public float opponentDistance = 0f;
    public float raceTime = 0f;
    public float countdown = 3.2f;

    public float launchQuality = 0f;
    public String shiftMessage = "";
    public float shiftMessageTime = 0f;

    public boolean playerFinished = false;
    public boolean opponentFinished = false;
    public float playerFinishTime = 0f;
    public float opponentFinishTime = 0f;

    public boolean lastWin = false;
    public long lastReward = 0;

    public boolean shifting = false;

    private int pendingGear = 1;
    private float shiftTimer = 0f;
    private float activeShiftDuration = 0f;
    private float shiftStartRpm = IDLE_RPM;

    private float launchPenalty = 0f;
    private float launchRpmAtGo = 4550f;

    private float opponentSpeedKmh = 0f;
    private float opponentPerformance = 1f;
    private float opponentMaxSpeed = 179f;

    public GameState(Context context) {
        prefs = context.getSharedPreferences(
                "nocny_garaz_save",
                Context.MODE_PRIVATE
        );

        money = prefs.getLong("money", 1_000_000L);
        careerStage = prefs.getInt("careerStage", 1);
        wins = prefs.getInt("wins", 0);

        for (int i = 0; i < upgrades.length; i++) {
            upgrades[i] = prefs.getInt("upg" + i, 0);
        }
    }

    public void save() {
        SharedPreferences.Editor e = prefs.edit();

        e.putLong("money", money);
        e.putInt("careerStage", careerStage);
        e.putInt("wins", wins);

        for (int i = 0; i < upgrades.length; i++) {
            e.putInt("upg" + i, upgrades[i]);
        }

        e.apply();
    }

    // Silnik +5 KM/lvl, turbo +10 KM/lvl, ECU +2 KM/lvl.
    public float horsepower() {
        return BASE_HP
                + upgrades[0] * 5f
                + upgrades[1] * 10f
                + upgrades[3] * 2f;
    }

    // 1205 kg seryjnie, -25 kg/lvl.
    public float weightKg() {
        return BASE_WEIGHT_KG - upgrades[5] * 25f;
    }

    // 0,60 s seryjnie, -0,05 s/lvl, minimum 0,35 s.
    public float shiftDuration() {
        return Math.max(
                0.35f,
                0.60f - upgrades[2] * 0.05f
        );
    }

    public int rating() {
        float hpGain = horsepower() - BASE_HP;
        float weightGain = BASE_WEIGHT_KG - weightKg();

        return Math.round(
                250f
                        + hpGain * 3.0f
                        + weightGain * 0.40f
                        + upgrades[2] * 18f
                        + upgrades[4] * 14f
        );
    }

    public int upgradeCost(int idx) {
        int level = upgrades[idx];

        if (level >= 5) {
            return 0;
        }

        int[] base = {
                22_000,
                28_000,
                18_000,
                16_000,
                14_000,
                17_000
        };

        return base[idx]
                * (level + 1)
                * (level + 1);
    }

    public boolean buyUpgrade(int idx) {
        if (idx < 0 || idx >= upgrades.length) {
            return false;
        }

        if (upgrades[idx] >= 5) {
            return false;
        }

        int cost = upgradeCost(idx);

        if (money < cost) {
            return false;
        }

        money -= cost;
        upgrades[idx]++;

        save();

        return true;
    }

    public void addDevCash() {
        money += 500_000L;
        save();
    }

    public void goModeSelect() {
        gasHeld = false;
        screen = Screen.MODE_SELECT;
    }

    public void goGarage() {
        gasHeld = false;
        screen = Screen.GARAGE;
    }

    public void startRace(Mode selected) {
        mode = selected;
        screen = Screen.COUNTDOWN;

        gasHeld = false;

        rpm = IDLE_RPM;
        gear = 1;
        pendingGear = 1;

        shifting = false;
        shiftTimer = 0f;
        activeShiftDuration = 0f;

        speedKmh = 0f;
        opponentSpeedKmh = 0f;

        playerDistance = 0f;
        opponentDistance = 0f;

        raceTime = 0f;
        countdown = 3.2f;

        launchQuality = 0f;
        launchPenalty = 0f;
        launchRpmAtGo = 4550f;

        shiftMessage = "";
        shiftMessageTime = 0f;

        playerFinished = false;
        opponentFinished = false;

        playerFinishTime = 0f;
        opponentFinishTime = 0f;

        lastReward = 0;

        float playerPerf = performanceIndex();

        if (mode == Mode.CAREER) {
            opponentPerformance =
                    0.95f
                            + Math.max(
                            0,
                            careerStage - 1
                    ) * 0.045f;
        } else {
            opponentPerformance =
                    playerPerf
                            * (
                            0.96f
                                    + random.nextFloat() * 0.08f
                    );
        }

        opponentMaxSpeed =
                179f
                        * (float)Math.pow(
                        opponentPerformance,
                        0.34f
                );
    }

    public void setGas(boolean held) {
        gasHeld = held;
    }

    public float maxRpm() {
        return REV_LIMIT_RPM;
    }

    public float greenLow() {
        return SHIFT_GREEN_LOW;
    }

    public float greenHigh() {
        return SHIFT_GREEN_HIGH;
    }

    // Start zostaje taki jak w zaakceptowanej v0.3.1.
    public float launchGreenLow() {
        return 4050f
                - upgrades[4] * 35f;
    }

    public float launchGreenHigh() {
        return 5050f
                + upgrades[4] * 55f;
    }

    public void shift() {
        if (screen != Screen.RACING) {
            return;
        }

        if (playerFinished) {
            return;
        }

        if (shifting) {
            return;
        }

        if (gear >= MAX_GEARS) {
            return;
        }

        float low = greenLow();
        float high = greenHigh();

        float center =
                (low + high) * 0.5f;

        float halfWidth =
                (high - low) * 0.5f;

        float diff =
                Math.abs(
                        rpm - center
                );

        float extraDelay;

        if (diff <= halfWidth * 0.35f) {
            shiftMessage =
                    "PERFECT SHIFT";

            extraDelay = 0f;

        } else if (diff <= halfWidth) {
            shiftMessage =
                    "GOOD SHIFT";

            extraDelay = 0.03f;

        } else if (rpm < low) {
            shiftMessage =
                    "ZA WCZEŚNIE";

            extraDelay = 0.12f;

        } else {
            shiftMessage =
                    "ZA PÓŹNO";

            extraDelay = 0.15f;
        }

        shiftMessageTime = 0.8f;

        shifting = true;
        pendingGear = gear + 1;

        activeShiftDuration =
                shiftDuration()
                        + extraDelay;

        shiftTimer =
                activeShiftDuration;

        shiftStartRpm =
                rpm;
    }

    public void update(float dt) {
        dt =
                Math.min(
                        dt,
                        0.05f
                );

        if (shiftMessageTime > 0f) {
            shiftMessageTime -= dt;
        }

        /*
         * START:
         * gaz = szybki wzrost RPM,
         * puszczenie = wolny spadek RPM.
         */
        if (screen == Screen.COUNTDOWN) {
            float riseRate = 3600f;
            float fallRate = 1080f;

            if (gasHeld) {
                rpm +=
                        riseRate * dt;
            } else {
                rpm -=
                        fallRate * dt;
            }

            rpm =
                    clamp(
                            rpm,
                            IDLE_RPM,
                            REV_LIMIT_RPM + 140f
                    );

            countdown -= dt;

            if (countdown <= 0f) {
                float lo =
                        launchGreenLow();

                float hi =
                        launchGreenHigh();

                float center =
                        (lo + hi) * 0.5f;

                float half =
                        (hi - lo) * 0.5f;

                float d =
                        Math.abs(
                                rpm - center
                        );

                launchQuality =
                        clamp(
                                1f
                                        - d
                                        / (
                                        half * 2f
                                ),
                                0f,
                                1f
                        );

                launchPenalty =
                        (
                                1f
                                        - launchQuality
                        )
                                * 0.30f;

                launchRpmAtGo =
                        rpm;

                if (d <= half * 0.35f) {
                    shiftMessage =
                            "PERFECT START";

                } else if (
                        rpm >= lo
                                && rpm <= hi
                ) {
                    shiftMessage =
                            "GOOD START";

                } else if (
                        launchQuality > 0.45f
                ) {
                    shiftMessage =
                            "OK START";

                } else {
                    shiftMessage =
                            "SŁABY START";
                }

                shiftMessageTime = 1.0f;

                screen =
                        Screen.RACING;
            }

            return;
        }

        if (screen != Screen.RACING) {
            return;
        }

        raceTime += dt;

        if (!playerFinished) {
            updatePlayerPhysics(dt);
        }

        if (!opponentFinished) {
            updateOpponent(dt);
        }

        if (
                playerFinished
                        && opponentFinished
        ) {
            finishRace();

        } else if (
                playerFinished
                        && raceTime
                        > playerFinishTime + 1.2f
        ) {
            finishRace();

        } else if (
                opponentFinished
                        && raceTime
                        > opponentFinishTime + 1.2f
        ) {
            finishRace();
        }
    }

    private void updatePlayerPhysics(float dt) {
        float mass =
                weightKg();

        float speedMs =
                speedKmh / 3.6f;

        float driveForce = 0f;

        /*
         * Podczas zmiany biegu napęd jest rozłączony.
         * Seryjnie 0,60 s, po lvl 5 skrzyni 0,35 s.
         */
        if (shifting) {
            shiftTimer -= dt;

            float progress =
                    activeShiftDuration <= 0f
                            ? 1f
                            : clamp(
                            1f
                                    - shiftTimer
                                    / activeShiftDuration,
                            0f,
                            1f
                    );

            float targetRpm =
                    Math.max(
                            IDLE_RPM,
                            rpmFromSpeed(
                                    speedKmh,
                                    pendingGear
                            )
                    );

            float smooth =
                    progress
                            * progress
                            * (
                            3f - 2f * progress
                    );

            rpm =
                    lerp(
                            shiftStartRpm,
                            targetRpm,
                            smooth
                    );

            if (shiftTimer <= 0f) {
                gear =
                        pendingGear;

                shifting =
                        false;

                rpm =
                        Math.max(
                                IDLE_RPM,
                                rpmFromSpeed(
                                        speedKmh,
                                        gear
                                )
                        );
            }

        } else {
            rpm =
                    calculateEngineRpm();

            float torque =
                    engineTorqueNm(rpm);

            // Więcej KM = większy moment i szybsze przyspieszenie.
            torque *=
                    horsepower()
                            / BASE_HP;

            driveForce =
                    torque
                            * GEAR_RATIOS[
                            gear - 1
                            ]
                            * FINAL_DRIVE
                            * DRIVETRAIN_EFFICIENCY
                            / WHEEL_RADIUS_M;

            /*
             * Opony:
             * +5% trakcji na poziom.
             * Najbardziej odczuwalne na 1. i 2. biegu.
             */
            float tireGrip =
                    1f
                            + upgrades[4]
                            * 0.05f;

            if (gear == 1) {
                float tractionLimit =
                        mass
                                * GRAVITY
                                * FIRST_GEAR_TRACTION
                                * tireGrip;

                driveForce =
                        Math.min(
                                driveForce,
                                tractionLimit
                        );

            } else if (gear == 2) {
                float tractionLimit =
                        mass
                                * GRAVITY
                                * SECOND_GEAR_TRACTION
                                * tireGrip;

                driveForce =
                        Math.min(
                                driveForce,
                                tractionLimit
                        );
            }

            // Gorszy launch wpływa głównie na pierwsze ~2,2 s.
            float launchFade =
                    Math.max(
                            0f,
                            1f
                                    - raceTime
                                    / 2.2f
                    );

            driveForce *=
                    Math.max(
                            0.70f,
                            1f
                                    - launchPenalty
                                    * launchFade
                    );

            // Odcięcie przy 6000 rpm.
            if (rpm >= REV_LIMIT_RPM) {
                driveForce *=
                        0.08f;

                rpm =
                        REV_LIMIT_RPM
                                - 70f
                                + (float)Math.sin(
                                raceTime * 32f
                        ) * 45f;
            }
        }

        float aeroForce =
                0.5f
                        * AIR_DENSITY
                        * CDA
                        * speedMs
                        * speedMs;

        float rollingForce =
                ROLLING_RESISTANCE
                        * mass
                        * GRAVITY;

        float netForce =
                driveForce
                        - aeroForce
                        - rollingForce;

        float accelMs2 =
                netForce
                        / mass;

        speedMs +=
                accelMs2
                        * dt;

        speedMs =
                Math.max(
                        0f,
                        speedMs
                );

        speedKmh =
                speedMs
                        * 3.6f;

        if (!shifting) {
            rpm =
                    calculateEngineRpm();
        }

        playerDistance +=
                speedMs
                        * dt;

        if (
                playerDistance
                        >= 402.336f
        ) {
            playerDistance =
                    402.336f;

            playerFinished =
                    true;

            playerFinishTime =
                    raceTime;
        }
    }

    /*
     * Krótki poślizg sprzęgła przy ruszaniu.
     * Dzięki temu po GO RPM nie spada od razu z ~4500 do 950.
     */
    private float calculateEngineRpm() {
        float coupledRpm =
                Math.max(
                        IDLE_RPM,
                        rpmFromSpeed(
                                speedKmh,
                                gear
                        )
                );

        if (
                gear == 1
                        && raceTime < 0.90f
        ) {
            float coupling =
                    clamp(
                            raceTime / 0.90f,
                            0f,
                            1f
                    );

            float slippingRpm =
                    lerp(
                            launchRpmAtGo,
                            1400f,
                            coupling
                    );

            return Math.max(
                    coupledRpm,
                    slippingRpm
            );
        }

        return coupledRpm;
    }

    private float rpmFromSpeed(
            float kmh,
            int selectedGear
    ) {
        int index =
                Math.max(
                        0,
                        Math.min(
                                MAX_GEARS - 1,
                                selectedGear - 1
                        )
                );

        float speedMs =
                kmh / 3.6f;

        float wheelRps =
                speedMs
                        / (
                        2f
                                * (float)Math.PI
                                * WHEEL_RADIUS_M
                );

        return wheelRps
                * 60f
                * GEAR_RATIOS[index]
                * FINAL_DRIVE;
    }

    /*
     * Przybliżona seryjna krzywa momentu 1.2 TSI 85 KM:
     * 160 Nm przy 1400-3500 rpm,
     * potem spadek momentu przy wyższych obrotach.
     */
    private float engineTorqueNm(float engineRpm) {
        float r =
                Math.max(
                        700f,
                        engineRpm
                );

        if (r < 800f) {
            return 70f;
        }

        if (r < 1400f) {
            return lerp(
                    90f,
                    160f,
                    (r - 800f) / 600f
            );
        }

        if (r <= 3500f) {
            return 160f;
        }

        if (r <= 4300f) {
            return lerp(
                    160f,
                    139f,
                    (r - 3500f) / 800f
            );
        }

        if (r <= 5300f) {
            return lerp(
                    139f,
                    113f,
                    (r - 4300f) / 1000f
            );
        }

        if (r <= 6000f) {
            return lerp(
                    113f,
                    80f,
                    (r - 5300f) / 700f
            );
        }

        return 75f;
    }

    private float performanceIndex() {
        float powerPart =
                horsepower()
                        / BASE_HP;

        float weightPart =
                (float)Math.pow(
                        BASE_WEIGHT_KG
                                / weightKg(),
                        0.65f
                );

        float gearboxPart =
                1f
                        + upgrades[2]
                        * 0.025f;

        float tiresPart =
                1f
                        + upgrades[4]
                        * 0.015f;

        return powerPart
                * weightPart
                * gearboxPart
                * tiresPart;
    }

    // Prosty model AI dopasowany do nowych osiągów seryjnego Golfa.
    private void updateOpponent(float dt) {
        float speedRatio =
                opponentMaxSpeed <= 1f
                        ? 0f
                        : opponentSpeedKmh
                        / opponentMaxSpeed;

        float aeroFade =
                Math.max(
                        0.18f,
                        1f
                                - speedRatio
                                * speedRatio
                                * 0.82f
                );

        float accelKmhPerSec =
                9.3f
                        * opponentPerformance
                        * aeroFade;

        if (raceTime < 0.25f) {
            accelKmhPerSec *=
                    0.75f;
        }

        opponentSpeedKmh +=
                accelKmhPerSec
                        * dt;

        opponentSpeedKmh =
                Math.min(
                        opponentSpeedKmh,
                        opponentMaxSpeed
                );

        opponentDistance +=
                (
                        opponentSpeedKmh
                                / 3.6f
                )
                        * dt;

        if (
                opponentDistance
                        >= 402.336f
        ) {
            opponentDistance =
                    402.336f;

            opponentFinished =
                    true;

            opponentFinishTime =
                    raceTime;
        }
    }

    private void finishRace() {
        if (screen == Screen.RESULT) {
            return;
        }

        if (!playerFinished) {
            playerFinishTime =
                    raceTime + 9f;
        }

        if (!opponentFinished) {
            opponentFinishTime =
                    raceTime + 9f;
        }

        lastWin =
                playerFinishTime
                        <= opponentFinishTime;

        if (lastWin) {
            wins++;

            if (mode == Mode.CAREER) {
                lastReward =
                        150_000L
                                + careerStage
                                * 45_000L;

                careerStage++;
            } else {
                lastReward =
                        120_000L
                                + rating()
                                * 180L;
            }

            money +=
                    lastReward;

            save();

        } else {
            lastReward =
                    20_000L;

            money +=
                    lastReward;

            save();
        }

        screen =
                Screen.RESULT;
    }

    private static float clamp(
            float value,
            float min,
            float max
    ) {
        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }

    private static float lerp(
            float a,
            float b,
            float t
    ) {
        return a
                + (
                b - a
        )
                * clamp(
                t,
                0f,
                1f
        );
    }

    public String moneyText() {
        return String.format(
                Locale.US,
                "%,d $",
                money
        ).replace(
                ',',
                ' '
        );
    }

    public String rewardPreview(Mode m) {
        long v;

        if (m == Mode.CAREER) {
            v =
                    150_000L
                            + careerStage
                            * 45_000L;
        } else {
            v =
                    120_000L
                            + rating()
                            * 180L;
        }

        return String.format(
                Locale.US,
                "%,d $",
                v
        ).replace(
                ',',
                ' '
        );
    }
}
