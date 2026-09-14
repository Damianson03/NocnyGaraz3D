package pl.nocnygaraz.game;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;
import java.util.Random;

public final class GameState {

    public static final String GAME_VERSION = "v0.3.1";

    public enum Screen {
        GARAGE,
        MODE_SELECT,
        COUNTDOWN,
        RACING,
        RESULT
    }

    public enum Mode {
        CAREER,
        CASH_RUN
    }

    public static final String[] UPGRADE_NAMES = {
            "SILNIK",
            "TURBO",
            "SKRZYNIA",
            "ECU",
            "OPONY",
            "MASA"
    };

    /*
     * v0.3.1
     *
     * 1 i 2 bieg zostają szybkie.
     * 3 nadal umiarkowany.
     * 4, 5 i 6 są wyraźnie dłuższe.
     */
    private static final float[] GEAR_RPM_RATE = {
            3600f,
            1900f,
            1250f,
            760f,
            430f,
            270f
    };

    /*
     * RPM po zmianie biegu.
     *
     * Nie podnosimy już późnych biegów
     * tak agresywnie wysoko po zmianie.
     */
    private static final float[] POST_SHIFT_RPM = {
            0f,
            3600f,
            3900f,
            4050f,
            4150f,
            4250f
    };

    private static final float[] GEAR_ACCEL_FACTOR = {
            1.00f,
            0.80f,
            0.66f,
            0.52f,
            0.40f,
            0.33f
    };

    private static final float[] BASE_GEAR_SPEED_CAP = {
            70f,
            116f,
            162f,
            208f,
            254f,
            300f
    };

    private final SharedPreferences prefs;
    private final Random random = new Random();

    public Screen screen = Screen.GARAGE;
    public Mode mode = Mode.CASH_RUN;

    public long money;
    public int careerStage;
    public int wins;

    public final int[] upgrades = new int[6];

    public boolean gasHeld = false;

    public float rpm = 950f;
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

    private float opponentSpeedKmh = 0f;
    private float opponentRating = 0f;

    private float shiftPenalty = 0f;
    private float launchPenalty = 0f;

    public GameState(Context context) {

        prefs = context.getSharedPreferences(
                "nocny_garaz_save",
                Context.MODE_PRIVATE
        );

        money = prefs.getLong(
                "money",
                1_000_000L
        );

        careerStage = prefs.getInt(
                "careerStage",
                1
        );

        wins = prefs.getInt(
                "wins",
                0
        );

        for (int i = 0; i < upgrades.length; i++) {
            upgrades[i] = prefs.getInt(
                    "upg" + i,
                    0
            );
        }
    }

    public void save() {

        SharedPreferences.Editor e =
                prefs.edit();

        e.putLong(
                "money",
                money
        );

        e.putInt(
                "careerStage",
                careerStage
        );

        e.putInt(
                "wins",
                wins
        );

        for (int i = 0; i < upgrades.length; i++) {
            e.putInt(
                    "upg" + i,
                    upgrades[i]
            );
        }

        e.apply();
    }

    public int rating() {

        return 250
                + upgrades[0] * 34
                + upgrades[1] * 42
                + upgrades[2] * 24
                + upgrades[3] * 22
                + upgrades[4] * 20
                + upgrades[5] * 18;
    }

    public float horsepower() {

        return 205f
                + upgrades[0] * 36f
                + upgrades[1] * 48f
                + upgrades[3] * 20f;
    }

    public float weightKg() {

        return 1340f
                - upgrades[5] * 42f;
    }

    public int upgradeCost(int idx) {

        int level =
                upgrades[idx];

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

        if (idx < 0
                || idx >= upgrades.length) {

            return false;
        }

        if (upgrades[idx] >= 5) {
            return false;
        }

        int cost =
                upgradeCost(idx);

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

        screen =
                Screen.MODE_SELECT;
    }

    public void goGarage() {

        gasHeld = false;

        screen =
                Screen.GARAGE;
    }

    public void startRace(
            Mode selected
    ) {

        mode = selected;

        screen =
                Screen.COUNTDOWN;

        gasHeld = false;

        rpm = 950f;

        gear = 1;

        speedKmh = 0f;

        opponentSpeedKmh = 0f;

        playerDistance = 0f;

        opponentDistance = 0f;

        raceTime = 0f;

        countdown = 3.2f;

        launchQuality = 0f;

        launchPenalty = 0f;

        shiftPenalty = 0f;

        shiftMessage = "";

        shiftMessageTime = 0f;

        playerFinished = false;

        opponentFinished = false;

        playerFinishTime = 0f;

        opponentFinishTime = 0f;

        lastReward = 0;

        float myRating =
                rating();

        if (mode == Mode.CAREER) {

            opponentRating =
                    235f
                    + careerStage * 18f;

        } else {

            opponentRating =
                    myRating
                    * (
                    0.90f
                    + random.nextFloat()
                    * 0.035f
            );
        }
    }

    public void setGas(
            boolean held
    ) {

        gasHeld = held;
    }

    public float maxRpm() {

        return 7100f
                + upgrades[3] * 120f;
    }

    public float greenLow() {

        return maxRpm()
                * 0.83f;
    }

    public float greenHigh() {

        return maxRpm()
                * 0.91f;
    }

    public float launchGreenLow() {

        return 4050f
                - upgrades[4] * 35f;
    }

    public float launchGreenHigh() {

        return 5050f
                + upgrades[4] * 55f;
    }

    public void shift() {

        if (screen != Screen.RACING
                || playerFinished
                || gear >= 6) {

            return;
        }

        float low =
                greenLow();

        float high =
                greenHigh();

        float center =
                (low + high) * 0.5f;

        float width =
                (high - low) * 0.5f;

        float diff =
                Math.abs(
                        rpm - center
                );

        if (diff <= width * 0.35f) {

            shiftMessage =
                    "PERFECT SHIFT";

            shiftPenalty =
                    Math.max(
                            0f,
                            shiftPenalty - 0.04f
                    );

        } else if (diff <= width) {

            shiftMessage =
                    "GOOD SHIFT";

            shiftPenalty += 0.05f;

        } else if (rpm < low) {

            shiftMessage =
                    "ZA WCZEŚNIE";

            shiftPenalty += 0.18f;

        } else {

            shiftMessage =
                    "ZA PÓŹNO";

            shiftPenalty += 0.22f;
        }

        shiftMessageTime =
                0.8f;

        gear++;

        rpm =
                POST_SHIFT_RPM[
                        gear - 1
                        ]
                + upgrades[2]
                * 25f;

        speedKmh *=
                0.988f
                + upgrades[2]
                * 0.0015f;
    }

    public void update(
            float dt
    ) {

        dt =
                Math.min(
                        dt,
                        0.05f
                );

        if (shiftMessageTime > 0f) {

            shiftMessageTime -= dt;
        }

        /*
         * =========================
         *          START
         * =========================
         *
         * Zostawiony bez zmian.
         *
         * Gaz:
         * RPM szybko idzie w górę.
         *
         * Odpuszczenie:
         * RPM wolniej opada.
         */
        if (screen == Screen.COUNTDOWN) {

            float riseRate =
                    3600f
                    + upgrades[3]
                    * 120f;

            float fallRate =
                    1080f
                    + upgrades[3]
                    * 25f;

            if (gasHeld) {

                rpm +=
                        riseRate
                        * dt;

            } else {

                rpm -=
                        fallRate
                        * dt;
            }

            rpm =
                    clamp(
                            rpm,
                            950f,
                            maxRpm() + 140f
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
                                        half * 2.0f
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

                shiftMessageTime =
                        1.0f;

                screen =
                        Screen.RACING;
            }

            return;
        }

        if (screen != Screen.RACING) {
            return;
        }

        raceTime += dt;

        /*
         * =========================
         *         GRACZ
         * =========================
         */

        if (!playerFinished) {

            float hp =
                    horsepower();

            float powerScale =
                    1f
                            + clamp(
                            (
                                    hp - 205f
                            ) / 520f,
                            0f,
                            1f
                    ) * 0.48f;

            float weightFactor =
                    (float)
                            Math.sqrt(
                                    1340f
                                            / weightKg()
                            );

            float grip =
                    1f
                            + upgrades[4]
                            * 0.025f;

            float gearbox =
                    1f
                            + upgrades[2]
                            * 0.010f;

            float rpmFactor =
                    0.86f
                            + 0.18f
                            * Math.min(
                            1f,
                            rpm
                                    / greenHigh()
                    );

            float launchFade =
                    Math.max(
                            0f,
                            1f
                                    - raceTime
                                    / 2.2f
                    );

            float penaltyFactor =
                    Math.max(
                            0.62f,
                            1f
                                    - shiftPenalty
                                    - launchPenalty
                                    * launchFade
                    );

            float accelKmhPerSec =
                    38f
                            * powerScale
                            * weightFactor
                            * grip
                            * gearbox
                            * GEAR_ACCEL_FACTOR[
                            gear - 1
                            ]
                            * rpmFactor
                            * penaltyFactor;

            float gearSpeedCap =
                    BASE_GEAR_SPEED_CAP[
                            gear - 1
                            ]
                            + upgrades[0]
                            * 2.0f
                            + upgrades[1]
                            * 3.0f
                            + upgrades[3]
                            * 1.0f
                            + upgrades[2]
                            * 1.5f;

            float capPressure =
                    clamp(
                            (
                                    gearSpeedCap
                                            + 12f
                                            - speedKmh
                            ) / 22f,
                            0.06f,
                            1f
                    );

            float aero =
                    Math.max(
                            0.35f,
                            1f
                                    - (float)
                                    Math.pow(
                                            speedKmh
                                                    / 355f,
                                            1.6f
                                    )
                                    * 0.68f
                    );

            speedKmh +=
                    accelKmhPerSec
                            * capPressure
                            * aero
                            * dt;

            speedKmh =
                    Math.min(
                            speedKmh,
                            345f
                    );

            /*
             * =========================
             *      BIEGI v0.3.1
             * =========================
             */

            float engineRateBonus =
                    1f
                            + clamp(
                            (
                                    hp - 205f
                            ) / 520f,
                            0f,
                            1f
                    ) * 0.07f;

            float gearboxRateBonus =
                    1f
                            + upgrades[2]
                            * 0.006f;

            float rpmRate =
                    GEAR_RPM_RATE[
                            gear - 1
                            ]
                            * engineRateBonus
                            * gearboxRateBonus;

            rpm +=
                    rpmRate
                            * dt;

            float limiter =
                    maxRpm()
                            + 320f;

            if (rpm > limiter) {

                rpm =
                        limiter
                                - 80f
                                + (float)
                                Math.sin(
                                        raceTime
                                                * 30f
                                )
                                * 55f;

                speedKmh *=
                        (
                                1f
                                        - 0.10f
                                        * dt
                        );
            }

            playerDistance +=
                    (
                            speedKmh
                                    / 3.6f
                    )
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
         * =========================
         *            AI
         * =========================
         */

        if (!opponentFinished) {

            float normalized =
                    clamp(
                            (
                                    opponentRating
                                            - 245f
                            ) / 805f,
                            0f,
                            1.15f
                    );

            float oppAccel =
                    28f
                            + normalized
                            * 34f;

            float oppAero =
                    Math.max(
                            0.34f,
                            1f
                                    - (float)
                                    Math.pow(
                                            opponentSpeedKmh
                                                    / 350f,
                                            1.55f
                                    )
                                    * 0.66f
                    );

            if (raceTime < 0.15f) {

                oppAccel *=
                        0.45f;
            }

            opponentSpeedKmh +=
                    oppAccel
                            * oppAero
                            * dt;

            opponentSpeedKmh =
                    Math.min(
                            opponentSpeedKmh,
                            340f
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

        if (
                playerFinished
                        && opponentFinished
        ) {

            finishRace();

        } else if (
                playerFinished
                        && raceTime
                        > playerFinishTime
                        + 1.2f
        ) {

            finishRace();

        } else if (
                opponentFinished
                        && raceTime
                        > opponentFinishTime
                        + 1.2f
        ) {

            finishRace();
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

    public String rewardPreview(
            Mode m
    ) {

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
