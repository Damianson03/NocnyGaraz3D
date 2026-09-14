package pl.nocnygaraz.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

public final class HUDView extends View {

    private final GameState state;

    private final Paint p =
            new Paint(
                    Paint.ANTI_ALIAS_FLAG
            );

    private final RectF r =
            new RectF();

    private float density;

    private boolean gasTouch =
            false;

    public HUDView(
            Context context,
            GameState state
    ) {

        super(context);

        this.state =
                state;

        density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        p.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                )
        );

        setLayerType(
                View.LAYER_TYPE_SOFTWARE,
                null
        );
    }

    private float dp(float v) {

        return v * density;
    }

    private void txt(
            Canvas c,
            String s,
            float x,
            float y,
            float size,
            int color,
            Paint.Align align
    ) {

        p.setStyle(
                Paint.Style.FILL
        );

        p.setTextSize(
                dp(size)
        );

        p.setColor(
                color
        );

        p.setTextAlign(
                align
        );

        c.drawText(
                s,
                x,
                y,
                p
        );
    }

    private void box(
            Canvas c,
            float l,
            float t,
            float rr,
            float b,
            int color,
            float rad
    ) {

        p.setStyle(
                Paint.Style.FILL
        );

        p.setColor(
                color
        );

        r.set(
                l,
                t,
                rr,
                b
        );

        c.drawRoundRect(
                r,
                dp(rad),
                dp(rad),
                p
        );
    }

    private void strokeBox(
            Canvas c,
            float l,
            float t,
            float rr,
            float b,
            int color,
            float sw,
            float rad
    ) {

        p.setStyle(
                Paint.Style.STROKE
        );

        p.setStrokeWidth(
                dp(sw)
        );

        p.setColor(
                color
        );

        r.set(
                l,
                t,
                rr,
                b
        );

        c.drawRoundRect(
                r,
                dp(rad),
                dp(rad),
                p
        );

        p.setStyle(
                Paint.Style.FILL
        );
    }

    @Override
    protected void onDraw(
            Canvas c
    ) {

        super.onDraw(c);

        switch (state.screen) {

            case GARAGE:
                drawGarage(c);
                break;

            case MODE_SELECT:
                drawModeSelect(c);
                break;

            case COUNTDOWN:
            case RACING:
                drawRace(c);
                break;

            case RESULT:
                drawResult(c);
                break;
        }
    }

    private void drawTop(
            Canvas c,
            String title
    ) {

        int w =
                getWidth();

        box(
                c,
                0,
                0,
                w,
                dp(58),
                0xAA050810,
                0
        );

        txt(
                c,
                title,
                dp(20),
                dp(38),
                22,
                Color.WHITE,
                Paint.Align.LEFT
        );

        txt(
                c,
                state.moneyText(),
                w - dp(20),
                dp(37),
                20,
                0xFF58FF7A,
                Paint.Align.RIGHT
        );
    }

    private void drawGarage(
            Canvas c
    ) {

        int w =
                getWidth();

        int h =
                getHeight();

        drawTop(
                c,
                "NOCNY GARAŻ 3D"
        );

        /*
         * Numer wersji zawsze widoczny
         * w menu głównym.
         */
        txt(
                c,
                GameState.GAME_VERSION,
                dp(20),
                dp(70),
                11,
                0xFF8995A8,
                Paint.Align.LEFT
        );

        box(
                c,
                dp(18),
                dp(82),
                dp(300),
                h - dp(24),
                0xB5080C14,
                14
        );

        txt(
                c,
                "NG-X ONE",
                dp(38),
                dp(118),
                27,
                Color.WHITE,
                Paint.Align.LEFT
        );

        txt(
                c,
                "RATING  "
                        + state.rating(),
                dp(38),
                dp(149),
                16,
                0xFF59F47B,
                Paint.Align.LEFT
        );

        txt(
                c,
                String.format(
                        Locale.US,
                        "%.0f KM",
                        state.horsepower()
                ),
                dp(38),
                dp(176),
                15,
                0xFFCAD2E3,
                Paint.Align.LEFT
        );

        txt(
                c,
                String.format(
                        Locale.US,
                        "%.0f kg",
                        state.weightKg()
                ),
                dp(38),
                dp(201),
                15,
                0xFFCAD2E3,
                Paint.Align.LEFT
        );

        txt(
                c,
                "1/4 MILI  •  RWD",
                dp(38),
                dp(226),
                14,
                0xFF8F9AAF,
                Paint.Align.LEFT
        );

        txt(
                c,
                "Ekonomia testowa:",
                dp(38),
                h - dp(95),
                13,
                0xFF8F9AAF,
                Paint.Align.LEFT
        );

        txt(
                c,
                "duże wypłaty + DEV CASH",
                dp(38),
                h - dp(72),
                13,
                0xFF8F9AAF,
                Paint.Align.LEFT
        );

        button(
                c,
                dp(36),
                h - dp(58),
                dp(282),
                h - dp(20),
                "DEV +500 000 $",
                0xFF1B2432,
                0xFFCCD6E6
        );

        float gridL =
                w - dp(430);

        float gridR =
                w - dp(18);

        float top =
                dp(82);

        float gap =
                dp(10);

        float cellW =
                (
                        gridR
                                - gridL
                                - gap
                ) / 2f;

        float cellH =
                dp(88);

        for (
                int i = 0;
                i < 6;
                i++
        ) {

            int row =
                    i / 2;

            int col =
                    i % 2;

            float l =
                    gridL
                            + col
                            * (
                            cellW
                                    + gap
                    );

            float t =
                    top
                            + row
                            * (
                            cellH
                                    + gap
                    );

            int lv =
                    state.upgrades[i];

            box(
                    c,
                    l,
                    t,
                    l + cellW,
                    t + cellH,
                    0xD20A0F18,
                    12
            );

            strokeBox(
                    c,
                    l,
                    t,
                    l + cellW,
                    t + cellH,
                    lv >= 5
                            ? 0xFF58FF7A
                            : 0xFF293142,
                    1,
                    12
            );

            txt(
                    c,
                    GameState.UPGRADE_NAMES[i],
                    l + dp(14),
                    t + dp(25),
                    14,
                    Color.WHITE,
                    Paint.Align.LEFT
            );

            txt(
                    c,
                    "LV "
                            + lv
                            + "/5",
                    l + dp(14),
                    t + dp(49),
                    12,
                    0xFF8FA0B8,
                    Paint.Align.LEFT
            );

            String cost =
                    lv >= 5
                            ? "MAX"
                            : formatMoney(
                            state.upgradeCost(i)
                    );

            txt(
                    c,
                    cost,
                    l + cellW
                            - dp(12),
                    t + dp(68),
                    12,
                    lv >= 5
                            ? 0xFF58FF7A
                            : 0xFFFFD35A,
                    Paint.Align.RIGHT
            );
        }

        button(
                c,
                w - dp(430),
                h - dp(72),
                w - dp(18),
                h - dp(18),
                "WYŚCIG  →",
                0xFF19B653,
                Color.WHITE
        );
    }

    private void drawModeSelect(
            Canvas c
    ) {

        int w =
                getWidth();

        int h =
                getHeight();

        drawTop(
                c,
                "WYBIERZ TRYB"
        );

        float cw =
                Math.min(
                        dp(360),
                        w * 0.38f
                );

        float ch =
                Math.min(
                        dp(250),
                        h * 0.48f
                );

        float gap =
                dp(24);

        float total =
                cw * 2
                        + gap;

        float left =
                (
                        w - total
                ) / 2f;

        float top =
                dp(92);

        modeCard(
                c,
                left,
                top,
                cw,
                ch,
                true
        );

        modeCard(
                c,
                left + cw + gap,
                top,
                cw,
                ch,
                false
        );

        button(
                c,
                dp(18),
                h - dp(64),
                dp(170),
                h - dp(18),
                "← GARAŻ",
                0xFF1B2432,
                Color.WHITE
        );
    }

    private void modeCard(
            Canvas c,
            float l,
            float t,
            float cw,
            float ch,
            boolean career
    ) {

        box(
                c,
                l,
                t,
                l + cw,
                t + ch,
                0xD20A0F18,
                16
        );

        strokeBox(
                c,
                l,
                t,
                l + cw,
                t + ch,
                career
                        ? 0xFFFFB84D
                        : 0xFF58FF7A,
                2,
                16
        );

        txt(
                c,
                career
                        ? "KARIERA"
                        : "CASH RUN",
                l + dp(22),
                t + dp(42),
                24,
                Color.WHITE,
                Paint.Align.LEFT
        );

        txt(
                c,
                career
                        ? "STAGE "
                        + state.careerStage
                        : "FARMING",
                l + dp(22),
                t + dp(70),
                13,
                career
                        ? 0xFFFFC66B
                        : 0xFF77FF92,
                Paint.Align.LEFT
        );

        String[] lines =
                career
                        ? new String[]{
                        "Każdy wygrany rywal jest",
                        "mocniejszy. Nagroda rośnie",
                        "z każdym etapem."
                }
                        : new String[]{
                        "Rywal skaluje się blisko",
                        "Twojego auta. Tryb do",
                        "zarabiania na tuning."
                };

        for (
                int i = 0;
                i < lines.length;
                i++
        ) {

            txt(
                    c,
                    lines[i],
                    l + dp(22),
                    t + dp(
                            108
                                    + i * 23
                    ),
                    13,
                    0xFFB2BDCF,
                    Paint.Align.LEFT
            );
        }

        txt(
                c,
                "NAGRODA",
                l + dp(22),
                t + ch
                        - dp(54),
                11,
                0xFF77849B,
                Paint.Align.LEFT
        );

        txt(
                c,
                state.rewardPreview(
                        career
                                ? GameState.Mode.CAREER
                                : GameState.Mode.CASH_RUN
                ),
                l + dp(22),
                t + ch
                        - dp(25),
                18,
                0xFF58FF7A,
                Paint.Align.LEFT
        );
    }

    private void drawRace(
            Canvas c
    ) {

        int w =
                getWidth();

        int h =
                getHeight();

        box(
                c,
                0,
                0,
                w,
                dp(52),
                0x9905070D,
                0
        );

        txt(
                c,
                state.mode
                        == GameState.Mode.CAREER
                        ? "KARIERA • STAGE "
                        + state.careerStage
                        : "CASH RUN",
                dp(16),
                dp(34),
                16,
                Color.WHITE,
                Paint.Align.LEFT
        );

        txt(
                c,
                String.format(
                        Locale.US,
                        "%.0f / 402 m",
                        state.playerDistance
                ),
                w / 2f,
                dp(34),
                15,
                0xFFDCE4EF,
                Paint.Align.CENTER
        );

        txt(
                c,
                String.format(
                        Locale.US,
                        "%03.0f km/h",
                        state.speedKmh
                ),
                w - dp(16),
                dp(34),
                16,
                0xFF58FF7A,
                Paint.Align.RIGHT
        );

        if (
                state.screen
                        == GameState.Screen.COUNTDOWN
        ) {

            String count =
                    state.countdown > 2.2f
                            ? "3"
                            : state.countdown > 1.2f
                            ? "2"
                            : state.countdown > 0.2f
                            ? "1"
                            : "GO";

            int cc =
                    "GO".equals(count)
                            ? 0xFF58FF7A
                            : Color.WHITE;

            txt(
                    c,
                    count,
                    w / 2f,
                    h * 0.33f,
                    58,
                    cc,
                    Paint.Align.CENTER
            );

            txt(
                    c,
                    "TRZYMAJ GAZ I TRAF W ZIELONE",
                    w / 2f,
                    h * 0.43f,
                    14,
                    0xFFE9EEF7,
                    Paint.Align.CENTER
            );

        } else if (
                state.shiftMessageTime > 0f
        ) {

            txt(
                    c,
                    state.shiftMessage,
                    w / 2f,
                    h * 0.31f,
                    28,
                    state.shiftMessage.contains(
                            "PERFECT"
                    )
                            ? 0xFF58FF7A
                            : 0xFFFFD36A,
                    Paint.Align.CENTER
            );
        }

        drawTach(c);

        float bw =
                dp(170);

        float bh =
                dp(74);

        float margin =
                dp(22);

        int gasColor =
                gasTouch
                        ? 0xFF58FF7A
                        : 0xFF1B2432;

        button(
                c,
                margin,
                h - bh - margin,
                margin + bw,
                h - margin,
                "GAZ",
                gasColor,
                gasTouch
                        ? 0xFF06110A
                        : Color.WHITE
        );

        int shiftColor =
                state.screen
                        == GameState.Screen.RACING
                        ? 0xFF1487FF
                        : 0xFF202633;

        button(
                c,
                w - margin - bw,
                h - bh - margin,
                w - margin,
                h - margin,
                "SHIFT",
                shiftColor,
                Color.WHITE
        );

        txt(
                c,
                "BIEG "
                        + state.gear,
                w / 2f,
                h - dp(116),
                18,
                Color.WHITE,
                Paint.Align.CENTER
        );
    }

    private void drawTach(
            Canvas c
    ) {

        int w =
                getWidth();

        int h =
                getHeight();

        float left =
                w * 0.29f;

        float right =
                w * 0.71f;

        float top =
                h - dp(72);

        float bottom =
                top + dp(22);

        box(
                c,
                left,
                top,
                right,
                bottom,
                0xDD151B25,
                9
        );

        float max =
                state.maxRpm()
                        + 400f;

        float low =
                state.screen
                        == GameState.Screen.COUNTDOWN
                        ? state.launchGreenLow()
                        : state.greenLow();

        float high =
                state.screen
                        == GameState.Screen.COUNTDOWN
                        ? state.launchGreenHigh()
                        : state.greenHigh();

        float gx1 =
                left
                        + (
                        right - left
                )
                        * (
                        low / max
                );

        float gx2 =
                left
                        + (
                        right - left
                )
                        * (
                        high / max
                );

        box(
                c,
                gx1,
                top,
                gx2,
                bottom,
                0xFF36E765,
                8
        );

        float rx =
                left
                        + (
                        right - left
                )
                        * (
                        state.maxRpm()
                                / max
                );

        box(
                c,
                rx,
                top,
                right,
                bottom,
                0xFFEF3E42,
                8
        );

        float needle =
                left
                        + (
                        right - left
                )
                        * Math.max(
                        0f,
                        Math.min(
                                1f,
                                state.rpm
                                        / max
                        )
                );

        p.setColor(
                Color.WHITE
        );

        p.setStrokeWidth(
                dp(4)
        );

        c.drawLine(
                needle,
                top - dp(6),
                needle,
                bottom + dp(6),
                p
        );

        txt(
                c,
                String.format(
                        Locale.US,
                        "%.0f RPM",
                        state.rpm
                ),
                w / 2f,
                top - dp(11),
                13,
                Color.WHITE,
                Paint.Align.CENTER
        );
    }

    private void drawResult(
            Canvas c
    ) {

        int w =
                getWidth();

        int h =
                getHeight();

        box(
                c,
                0,
                0,
                w,
                h,
                0x8802060C,
                0
        );

        float pw =
                Math.min(
                        dp(520),
                        w * 0.68f
                );

        float ph =
                Math.min(
                        dp(330),
                        h * 0.70f
                );

        float l =
                (
                        w - pw
                ) / 2f;

        float t =
                (
                        h - ph
                ) / 2f;

        box(
                c,
                l,
                t,
                l + pw,
                t + ph,
                0xF00A0F18,
                18
        );

        strokeBox(
                c,
                l,
                t,
                l + pw,
                t + ph,
                state.lastWin
                        ? 0xFF58FF7A
                        : 0xFFFF5353,
                2,
                18
        );

        txt(
                c,
                state.lastWin
                        ? "WYGRANA"
                        : "PRZEGRANA",
                w / 2f,
                t + dp(58),
                32,
                state.lastWin
                        ? 0xFF58FF7A
                        : 0xFFFF6363,
                Paint.Align.CENTER
        );

        txt(
                c,
                String.format(
                        Locale.US,
                        "Twój czas: %.3f s",
                        state.playerFinishTime
                ),
                w / 2f,
                t + dp(102),
                16,
                Color.WHITE,
                Paint.Align.CENTER
        );

        txt(
                c,
                String.format(
                        Locale.US,
                        "Rywal: %.3f s",
                        state.opponentFinishTime
                ),
                w / 2f,
                t + dp(132),
                15,
                0xFFB3BED0,
                Paint.Align.CENTER
        );

        txt(
                c,
                "+ "
                        + formatMoney(
                        state.lastReward
                ),
                w / 2f,
                t + dp(176),
                24,
                0xFFFFD45C,
                Paint.Align.CENTER
        );

        float gap =
                dp(14);

        float bw =
                (
                        pw
                                - dp(54)
                                - gap
                ) / 2f;

        float by =
                t + ph
                        - dp(68);

        button(
                c,
                l + dp(20),
                by,
                l + dp(20)
                        + bw,
                by + dp(46),
                "GARAŻ",
                0xFF1B2432,
                Color.WHITE
        );

        button(
                c,
                l + dp(20)
                        + bw
                        + gap,
                by,
                l + pw
                        - dp(20),
                by + dp(46),
                "JESZCZE RAZ",
                0xFF19B653,
                Color.WHITE
        );
    }

    private void button(
            Canvas c,
            float l,
            float t,
            float rr,
            float b,
            String text,
            int bg,
            int fg
    ) {

        box(
                c,
                l,
                t,
                rr,
                b,
                bg,
                11
        );

        txt(
                c,
                text,
                (
                        l + rr
                ) / 2f,
                (
                        t + b
                ) / 2f
                        + dp(6),
                14,
                fg,
                Paint.Align.CENTER
        );
    }

    private static String formatMoney(
            long v
    ) {

        return String.format(
                Locale.US,
                "%,d $",
                v
        ).replace(
                ',',
                ' '
        );
    }

    private boolean hit(
            float x,
            float y,
            float l,
            float t,
            float rr,
            float b
    ) {

        return x >= l
                && x <= rr
                && y >= t
                && y <= b;
    }

    @Override
    public boolean onTouchEvent(
            MotionEvent e
    ) {

        float x =
                e.getX();

        float y =
                e.getY();

        int w =
                getWidth();

        int h =
                getHeight();

        if (
                e.getAction()
                        == MotionEvent.ACTION_DOWN
        ) {

            if (
                    state.screen
                            == GameState.Screen.COUNTDOWN
            ) {

                float margin =
                        dp(22);

                float bw =
                        dp(170);

                float bh =
                        dp(74);

                if (
                        hit(
                                x,
                                y,
                                margin,
                                h - bh - margin,
                                margin + bw,
                                h - margin
                        )
                ) {

                    gasTouch =
                            true;

                    state.setGas(
                            true
                    );

                    invalidate();
                }
            }

            return true;
        }

        if (
                e.getAction()
                        == MotionEvent.ACTION_UP
        ) {

            if (gasTouch) {

                gasTouch =
                        false;

                state.setGas(
                        false
                );
            }

            if (
                    state.screen
                            == GameState.Screen.GARAGE
            ) {

                handleGarageTap(
                        x,
                        y,
                        w,
                        h
                );

            } else if (
                    state.screen
                            == GameState.Screen.MODE_SELECT
            ) {

                handleModeTap(
                        x,
                        y,
                        w,
                        h
                );

            } else if (
                    state.screen
                            == GameState.Screen.RACING
            ) {

                float margin =
                        dp(22);

                float bw =
                        dp(170);

                float bh =
                        dp(74);

                if (
                        hit(
                                x,
                                y,
                                w - margin - bw,
                                h - bh - margin,
                                w - margin,
                                h - margin
                        )
                ) {

                    state.shift();
                }

            } else if (
                    state.screen
                            == GameState.Screen.RESULT
            ) {

                handleResultTap(
                        x,
                        y,
                        w,
                        h
                );
            }

            invalidate();

            return true;
        }

        if (
                e.getAction()
                        == MotionEvent.ACTION_CANCEL
        ) {

            gasTouch =
                    false;

            state.setGas(
                    false
            );

            invalidate();

            return true;
        }

        return true;
    }

    private void handleGarageTap(
            float x,
            float y,
            int w,
            int h
    ) {

        if (
                hit(
                        x,
                        y,
                        dp(36),
                        h - dp(58),
                        dp(282),
                        h - dp(20)
                )
        ) {

            state.addDevCash();

            return;
        }

        float gridL =
                w - dp(430);

        float gridR =
                w - dp(18);

        float top =
                dp(82);

        float gap =
                dp(10);

        float cellW =
                (
                        gridR
                                - gridL
                                - gap
                ) / 2f;

        float cellH =
                dp(88);

        for (
                int i = 0;
                i < 6;
                i++
        ) {

            int row =
                    i / 2;

            int col =
                    i % 2;

            float l =
                    gridL
                            + col
                            * (
                            cellW
                                    + gap
                    );

            float t =
                    top
                            + row
                            * (
                            cellH
                                    + gap
                    );

            if (
                    hit(
                            x,
                            y,
                            l,
                            t,
                            l + cellW,
                            t + cellH
                    )
            ) {

                state.buyUpgrade(i);

                return;
            }
        }

        if (
                hit(
                        x,
                        y,
                        w - dp(430),
                        h - dp(72),
                        w - dp(18),
                        h - dp(18)
                )
        ) {

            state.goModeSelect();
        }
    }

    private void handleModeTap(
            float x,
            float y,
            int w,
            int h
    ) {

        float cw =
                Math.min(
                        dp(360),
                        w * 0.38f
                );

        float ch =
                Math.min(
                        dp(250),
                        h * 0.48f
                );

        float gap =
                dp(24);

        float left =
                (
                        w
                                - (
                                cw * 2
                                        + gap
                        )
                ) / 2f;

        float top =
                dp(92);

        if (
                hit(
                        x,
                        y,
                        left,
                        top,
                        left + cw,
                        top + ch
                )
        ) {

            state.startRace(
                    GameState.Mode.CAREER
            );

        } else if (
                hit(
                        x,
                        y,
                        left + cw + gap,
                        top,
                        left + cw * 2 + gap,
                        top + ch
                )
        ) {

            state.startRace(
                    GameState.Mode.CASH_RUN
            );

        } else if (
                hit(
                        x,
                        y,
                        dp(18),
                        h - dp(64),
                        dp(170),
                        h - dp(18)
                )
        ) {

            state.goGarage();
        }
    }

    private void handleResultTap(
            float x,
            float y,
            int w,
            int h
    ) {

        float pw =
                Math.min(
                        dp(520),
                        w * 0.68f
                );

        float ph =
                Math.min(
                        dp(330),
                        h * 0.70f
                );

        float l =
                (
                        w - pw
                ) / 2f;

        float t =
                (
                        h - ph
                ) / 2f;

        float gap =
                dp(14);

        float bw =
                (
                        pw
                                - dp(54)
                                - gap
                ) / 2f;

        float by =
                t + ph
                        - dp(68);

        if (
                hit(
                        x,
                        y,
                        l + dp(20),
                        by,
                        l + dp(20) + bw,
                        by + dp(46)
                )
        ) {

            state.goGarage();

        } else if (
                hit(
                        x,
                        y,
                        l + dp(20) + bw + gap,
                        by,
                        l + pw - dp(20),
                        by + dp(46)
                )
        ) {

            state.startRace(
                    state.mode
            );
        }
    }
}
