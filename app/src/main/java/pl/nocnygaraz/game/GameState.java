package pl.nocnygaraz.game;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;
import java.util.Random;

public final class GameState {
    public enum Screen { GARAGE, MODE_SELECT, COUNTDOWN, RACING, RESULT }
    public enum Mode { CAREER, CASH_RUN }

    public static final String[] UPGRADE_NAMES = {
            "SILNIK", "TURBO", "SKRZYNIA", "ECU", "OPONY", "MASA"
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
    private float countdownPulse = 0f;

    public GameState(Context context) {
        prefs = context.getSharedPreferences("nocny_garaz_save", Context.MODE_PRIVATE);
        money = prefs.getLong("money", 1_000_000L); // intentionally generous test economy
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
        for (int i = 0; i < upgrades.length; i++) e.putInt("upg" + i, upgrades[i]);
        e.apply();
    }

    public int rating() {
        return 250 + upgrades[0] * 34 + upgrades[1] * 42 + upgrades[2] * 24
                + upgrades[3] * 22 + upgrades[4] * 20 + upgrades[5] * 18;
    }

    public float horsepower() {
        return 205f + upgrades[0] * 36f + upgrades[1] * 48f + upgrades[3] * 20f;
    }

    public float weightKg() {
        return 1340f - upgrades[5] * 42f;
    }

    public int upgradeCost(int idx) {
        int level = upgrades[idx];
        if (level >= 5) return 0;
        int[] base = { 22_000, 28_000, 18_000, 16_000, 14_000, 17_000 };
        return base[idx] * (level + 1) * (level + 1);
    }

    public boolean buyUpgrade(int idx) {
        if (idx < 0 || idx >= upgrades.length || upgrades[idx] >= 5) return false;
        int cost = upgradeCost(idx);
        if (money < cost) return false;
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
        rpm = 950f;
        gear = 1;
        speedKmh = 0f;
        opponentSpeedKmh = 0f;
        playerDistance = 0f;
        opponentDistance = 0f;
        raceTime = 0f;
        countdown = 3.2f;
        countdownPulse = 0f;
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

        float myRating = rating();
        if (mode == Mode.CAREER) {
            // Absolute progression: every won stage is meaningfully tougher.
            opponentRating = 245f + careerStage * 22f;
        } else {
            // Farming opponent tracks the player's car and stays slightly beatable.
            opponentRating = myRating * (0.93f + random.nextFloat() * 0.045f);
        }
    }

    public void setGas(boolean held) {
        gasHeld = held;
    }

    public float maxRpm() {
        return 7100f + upgrades[3] * 120f;
    }

    public float greenLow() { return maxRpm() * 0.83f; }
    public float greenHigh() { return maxRpm() * 0.91f; }
    public float launchGreenLow() { return 3900f + upgrades[4] * 90f; }
    public float launchGreenHigh() { return 4750f + upgrades[4] * 100f; }

    public void shift() {
        if (screen != Screen.RACING || playerFinished || gear >= 6) return;
        float low = greenLow();
        float high = greenHigh();
        float center = (low + high) * 0.5f;
        float width = (high - low) * 0.5f;
        float diff = Math.abs(rpm - center);
        if (diff <= width * 0.35f) {
            shiftMessage = "PERFECT SHIFT";
            shiftPenalty = Math.max(0f, shiftPenalty - 0.04f);
        } else if (diff <= width) {
            shiftMessage = "GOOD SHIFT";
            shiftPenalty += 0.05f;
        } else if (rpm < low) {
            shiftMessage = "ZA WCZEŚNIE";
            shiftPenalty += 0.18f;
        } else {
            shiftMessage = "ZA PÓŹNO";
            shiftPenalty += 0.22f;
        }
        shiftMessageTime = 0.8f;
        gear++;
        rpm = Math.max(3200f, rpm * (0.62f + upgrades[2] * 0.012f));
        speedKmh *= (0.985f + upgrades[2] * 0.0025f);
    }

    public void update(float dt) {
        dt = Math.min(dt, 0.05f);
        if (shiftMessageTime > 0f) shiftMessageTime -= dt;

        if (screen == Screen.COUNTDOWN) {
            countdownPulse += dt;
            if (gasHeld) {
                // Holding gas makes RPM sweep/oscillate so player must catch the green band.
                float target = 4300f + (float)Math.sin(countdownPulse * 5.2f) * 1900f;
                rpm += (target - rpm) * Math.min(1f, dt * 6.2f);
            } else {
                rpm += (950f - rpm) * Math.min(1f, dt * 5.5f);
            }
            countdown -= dt;
            if (countdown <= 0f) {
                float lo = launchGreenLow(), hi = launchGreenHigh();
                float center = (lo + hi) * 0.5f;
                float half = (hi - lo) * 0.5f;
                float d = Math.abs(rpm - center);
                launchQuality = Math.max(0f, 1f - d / (half * 2.4f));
                launchPenalty = (1f - launchQuality) * 0.42f;
                shiftMessage = launchQuality > 0.82f ? "PERFECT START" :
                        (launchQuality > 0.52f ? "GOOD START" : "SŁABY START");
                shiftMessageTime = 1.0f;
                screen = Screen.RACING;
            }
            return;
        }

        if (screen != Screen.RACING) return;
        raceTime += dt;

        if (!playerFinished) {
            float powerToWeight = horsepower() / weightKg();
            float grip = 0.90f + upgrades[4] * 0.045f;
            float gearbox = 1f + upgrades[2] * 0.025f;
            float gearFactor = 1.08f - (gear - 1) * 0.065f;
            float rpmFactor = 0.72f + 0.35f * Math.min(1f, rpm / greenHigh());
            float penaltyFactor = Math.max(0.65f, 1f - shiftPenalty - launchPenalty * Math.max(0f, 1f - raceTime / 2f));
            float accelKmhPerSec = (34f + powerToWeight * 155f) * grip * gearbox * gearFactor * rpmFactor * penaltyFactor;
            float aero = Math.max(0.55f, 1f - speedKmh / 520f);
            speedKmh += accelKmhPerSec * aero * dt;

            float rpmRate = (2450f + speedKmh * 4.2f) * (1.0f - upgrades[2] * 0.012f);
            rpm += rpmRate * dt;
            float limiter = maxRpm() + 380f;
            if (rpm > limiter) {
                rpm = limiter - 160f + (float)Math.sin(raceTime * 36f) * 120f;
                speedKmh *= (1f - 0.16f * dt);
            }

            playerDistance += (speedKmh / 3.6f) * dt;
            if (playerDistance >= 402.336f) {
                playerDistance = 402.336f;
                playerFinished = true;
                playerFinishTime = raceTime;
            }
        }

        if (!opponentFinished) {
            float ratingRatio = opponentRating / 300f;
            float oppAccel = (22f + ratingRatio * 26f) * Math.max(0.60f, 1f - opponentSpeedKmh / 465f);
            // small staged launch delay makes starts readable visually
            if (raceTime < 0.12f) oppAccel *= 0.4f;
            opponentSpeedKmh += oppAccel * dt;
            opponentDistance += (opponentSpeedKmh / 3.6f) * dt;
            if (opponentDistance >= 402.336f) {
                opponentDistance = 402.336f;
                opponentFinished = true;
                opponentFinishTime = raceTime;
            }
        }

        if (playerFinished && opponentFinished) finishRace();
        else if (playerFinished && raceTime > playerFinishTime + 1.2f) finishRace();
        else if (opponentFinished && raceTime > opponentFinishTime + 1.2f) finishRace();
    }

    private void finishRace() {
        if (screen == Screen.RESULT) return;
        if (!playerFinished) playerFinishTime = raceTime + 9f;
        if (!opponentFinished) opponentFinishTime = raceTime + 9f;
        lastWin = playerFinishTime <= opponentFinishTime;
        if (lastWin) {
            wins++;
            if (mode == Mode.CAREER) {
                lastReward = 150_000L + careerStage * 45_000L;
                careerStage++;
            } else {
                lastReward = 120_000L + rating() * 180L;
            }
            money += lastReward;
            save();
        } else {
            lastReward = 20_000L; // test-build consolation to keep iteration fast
            money += lastReward;
            save();
        }
        screen = Screen.RESULT;
    }

    public String moneyText() {
        return String.format(Locale.US, "%,d $", money).replace(',', ' ');
    }

    public String rewardPreview(Mode m) {
        long v = (m == Mode.CAREER) ? 150_000L + careerStage * 45_000L : 120_000L + rating() * 180L;
        return String.format(Locale.US, "%,d $", v).replace(',', ' ');
    }
}
