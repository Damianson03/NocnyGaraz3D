package pl.nocnygaraz.game;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public final class DragRenderer implements GLSurfaceView.Renderer {

    private final GameState state;

    private int program;
    private int aPosition;
    private int uMvp;
    private int uColor;

    private final float[] proj = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] pv = new float[16];
    private final float[] mvp = new float[16];

    private FloatBuffer cube;

    private long startNanos;
    private float aspect = 1f;

    private static final float[] CUBE = {
            -0.5f,-0.5f, 0.5f,   0.5f,-0.5f, 0.5f,   0.5f, 0.5f, 0.5f,
            -0.5f,-0.5f, 0.5f,   0.5f, 0.5f, 0.5f,  -0.5f, 0.5f, 0.5f,

             0.5f,-0.5f,-0.5f,  -0.5f,-0.5f,-0.5f,  -0.5f, 0.5f,-0.5f,
             0.5f,-0.5f,-0.5f,  -0.5f, 0.5f,-0.5f,   0.5f, 0.5f,-0.5f,

            -0.5f,-0.5f,-0.5f,  -0.5f,-0.5f, 0.5f,  -0.5f, 0.5f, 0.5f,
            -0.5f,-0.5f,-0.5f,  -0.5f, 0.5f, 0.5f,  -0.5f, 0.5f,-0.5f,

             0.5f,-0.5f, 0.5f,   0.5f,-0.5f,-0.5f,   0.5f, 0.5f,-0.5f,
             0.5f,-0.5f, 0.5f,   0.5f, 0.5f,-0.5f,   0.5f, 0.5f, 0.5f,

            -0.5f, 0.5f, 0.5f,   0.5f, 0.5f, 0.5f,   0.5f, 0.5f,-0.5f,
            -0.5f, 0.5f, 0.5f,   0.5f, 0.5f,-0.5f,  -0.5f, 0.5f,-0.5f,

            -0.5f,-0.5f,-0.5f,   0.5f,-0.5f,-0.5f,   0.5f,-0.5f, 0.5f,
            -0.5f,-0.5f,-0.5f,   0.5f,-0.5f, 0.5f,  -0.5f,-0.5f, 0.5f
    };

    public DragRenderer(GameState state) {
        this.state = state;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.010f, 0.015f, 0.032f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        cube = ByteBuffer
                .allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        cube.put(CUBE).position(0);

        program = createProgram(
                "uniform mat4 uMVP; attribute vec3 aPosition; void main(){ gl_Position = uMVP * vec4(aPosition, 1.0); }",
                "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor = uColor; }"
        );

        aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        uMvp = GLES20.glGetUniformLocation(program, "uMVP");
        uColor = GLES20.glGetUniformLocation(program, "uColor");

        startNanos = System.nanoTime();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        aspect = width / (float)Math.max(1, height);
        Matrix.perspectiveM(proj, 0, 50f, aspect, 0.1f, 1200f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(
                GLES20.GL_COLOR_BUFFER_BIT
                        | GLES20.GL_DEPTH_BUFFER_BIT
        );

        float t =
                (System.nanoTime() - startNanos)
                        / 1_000_000_000f;

        if (state.screen == GameState.Screen.GARAGE
                || state.screen == GameState.Screen.MODE_SELECT) {

            drawGarage(t);

        } else {
            drawTrack(t);
        }
    }

    private void drawGarage(float t) {
        float camAngle = 18f + (float)Math.sin(t * 0.28f) * 8f;
        float camRadius = 8.6f;

        float camX =
                camRadius
                        * (float)Math.sin(Math.toRadians(camAngle));

        float camZ =
                camRadius
                        * (float)Math.cos(Math.toRadians(camAngle));

        Matrix.perspectiveM(proj, 0, 48f, aspect, 0.1f, 1200f);

        Matrix.setLookAtM(
                view, 0,
                camX, 2.75f, camZ,
                0f, 0.85f, 0f,
                0f, 1f, 0f
        );

        Matrix.multiplyMM(pv, 0, proj, 0, view, 0);

        // floor
        drawCube(0f, -0.18f, 0f, 18f, 0.22f, 16f, 0f, 0.035f, 0.040f, 0.055f, 1f);
        drawCube(0f, -0.02f, 0f, 14f, 0.01f, 12f, 0f, 0.090f, 0.100f, 0.125f, 0.20f);

        // walls
        drawCube(0f, 3.15f, -7.2f, 18f, 6.8f, 0.32f, 0f, 0.035f, 0.040f, 0.060f, 1f);
        drawCube(-8.8f, 2.6f, 0f, 0.28f, 5.2f, 15f, 0f, 0.028f, 0.033f, 0.045f, 1f);
        drawCube(8.8f, 2.6f, 0f, 0.28f, 5.2f, 15f, 0f, 0.028f, 0.033f, 0.045f, 1f);

        // light bars
        for (int i = -3; i <= 3; i++) {
            float x = i * 2.25f;
            drawCube(x, 3.0f, -7.0f, 0.08f, 0.32f, 0.18f, 0f, 0.20f, 1f, 0.45f, 1f);
            drawCube(x, 2.42f, -7.0f, 1.08f, 0.05f, 0.18f, 0f, 0.12f, 0.38f, 0.18f, 1f);
        }

        // side accent lights
        for (int i = -2; i <= 2; i++) {
            drawCube(-8.45f, 1.4f + i * 0.9f, -3.0f, 0.10f, 0.32f, 3.6f, 0f, 0.18f, 0.28f, 0.90f, 0.65f);
            drawCube(8.45f, 1.4f + i * 0.9f, -3.0f, 0.10f, 0.32f, 3.6f, 0f, 0.18f, 0.28f, 0.90f, 0.65f);
        }

        drawGolf7(0f, 0f, 0f, 0f, false, true, t);
    }

    private void drawTrack(float t) {
        float playerZ = -state.playerDistance;
        float opponentZ = -state.opponentDistance;

        float speedBlend =
                clamp(state.speedKmh / 180f, 0f, 1f);

        float cameraBlend;

        if (state.screen == GameState.Screen.COUNTDOWN) {
            cameraBlend = 0f;
        } else {
            cameraBlend = smoothstep(0f, 1.3f, state.raceTime);
        }

        float frontCamX = -2.7f;
        float frontCamY = 1.55f;
        float frontCamZ = playerZ - 7.8f;

        float sideCamX = 7.4f;
        float sideCamY = 2.05f;
        float sideCamZ = playerZ + 3.0f;

        float camX = lerp(frontCamX, sideCamX, cameraBlend);
        float camY = lerp(frontCamY, sideCamY, cameraBlend);
        float camZ = lerp(frontCamZ, sideCamZ, cameraBlend);

        float targetFrontX = -0.25f;
        float targetFrontY = 1.05f;
        float targetFrontZ = playerZ - 0.40f;

        float targetSideX = 0.15f;
        float targetSideY = 1.02f;
        float targetSideZ = playerZ - 18f;

        float targetX = lerp(targetFrontX, targetSideX, cameraBlend);
        float targetY = lerp(targetFrontY, targetSideY, cameraBlend);
        float targetZ = lerp(targetFrontZ, targetSideZ, cameraBlend);

        float speedShake =
                (0.0045f + speedBlend * 0.010f)
                        * (float)Math.sin(t * 34f);

        camY += speedShake;
        targetY += speedShake * 0.55f;

        float fov = lerp(47f, 57f, speedBlend);
        Matrix.perspectiveM(proj, 0, fov, aspect, 0.1f, 1200f);

        Matrix.setLookAtM(
                view, 0,
                camX, camY, camZ,
                targetX, targetY, targetZ,
                0f, 1f, 0f
        );

        Matrix.multiplyMM(pv, 0, proj, 0, view, 0);

        drawEnvironment();
        drawStartRig();
        drawGolf7(-1.72f, 0f, playerZ, 0f, false, false, t);
        drawOpponentCar(1.72f, 0f, opponentZ, t);
    }

    private void drawEnvironment() {
        // road base
        drawCube(0f, -0.22f, -205f, 11.8f, 0.26f, 430f, 0f, 0.050f, 0.055f, 0.065f, 1f);

        // wet road reflection layer
        drawCube(0f, -0.005f, -205f, 10.5f, 0.01f, 430f, 0f, 0.12f, 0.13f, 0.16f, 0.18f);

        // shoulder
        drawCube(-6.7f, -0.18f, -205f, 1.5f, 0.18f, 430f, 0f, 0.085f, 0.090f, 0.100f, 1f);
        drawCube( 6.7f, -0.18f, -205f, 1.5f, 0.18f, 430f, 0f, 0.085f, 0.090f, 0.100f, 1f);

        // side edges
        drawCube(-5.18f, -0.01f, -205f, 0.06f, 0.02f, 430f, 0f, 0.90f, 0.90f, 0.92f, 0.80f);
        drawCube( 5.18f, -0.01f, -205f, 0.06f, 0.02f, 430f, 0f, 0.90f, 0.90f, 0.92f, 0.80f);

        // lane separators
        for (int z = 8; z <= 404; z += 14) {
            float zz = -z;
            drawCube(0f, -0.01f, zz, 0.09f, 0.025f, 4.3f, 0f, 0.92f, 0.86f, 0.35f, 0.88f);
        }

        // start line and finish line
        drawCube(0f, 0.012f, -2.8f, 10.4f, 0.035f, 0.32f, 0f, 0.95f, 0.95f, 0.96f, 1f);
        drawCube(0f, 0.012f, -402.3f, 10.4f, 0.035f, 0.50f, 0f, 0.18f, 1f, 0.42f, 1f);

        // guardrails
        for (int z = 0; z <= 420; z += 12) {
            float zz = -z;
            drawGuardRail(-7.85f, zz, true);
            drawGuardRail(7.85f, zz, false);
        }

        // lamp posts
        for (int z = 8; z <= 420; z += 28) {
            float zz = -z;
            drawLamp(-7.15f, zz);
            drawLamp(7.15f, zz);
        }

        // city blocks
        for (int i = 0; i < 14; i++) {
            float z = -(i * 31f + 14f);
            float h1 = 4.2f + (i % 4) * 1.3f;
            float h2 = 3.8f + ((i + 2) % 5) * 1.15f;

            drawCityBlock(-14.2f, z, 6.8f, h1, 17f, 0.032f, 0.040f, 0.070f);
            drawCityBlock(14.8f, z - 8f, 7.2f, h2, 19f, 0.040f, 0.046f, 0.075f);
        }

        // distant skyline / horizon shapes
        for (int i = -4; i <= 4; i++) {
            float x = i * 7.5f;
            float h = 8f + ((i + 5) % 4) * 2.4f;
            drawCube(x, h * 0.5f - 0.5f, -470f, 2.0f, h, 9f, 0f, 0.025f, 0.030f, 0.050f, 1f);
        }

        // horizon glow
        drawCube(0f, 2.0f, -450f, 55f, 3.5f, 0.2f, 0f, 0.14f, 0.18f, 0.28f, 0.24f);
    }

    private void drawStartRig() {
        float z = -5.0f;

        // side columns
        drawCube(-4.8f, 2.3f, z, 0.22f, 4.6f, 0.22f, 0f, 0.18f, 0.19f, 0.23f, 1f);
        drawCube(4.8f, 2.3f, z, 0.22f, 4.6f, 0.22f, 0f, 0.18f, 0.19f, 0.23f, 1f);

        // top beam
        drawCube(0f, 4.45f, z, 5.05f, 0.16f, 0.24f, 0f, 0.18f, 0.19f, 0.23f, 1f);

        // signal tree
        drawCube(0f, 2.15f, z - 0.10f, 0.16f, 2.0f, 0.16f, 0f, 0.15f, 0.16f, 0.18f, 1f);

        float[] y = {3.00f, 2.60f, 2.20f, 1.80f, 1.40f};

        for (float yy : y) {
            drawCube(-0.16f, yy, z - 0.12f, 0.12f, 0.12f, 0.06f, 0f, 1.0f, 0.72f, 0.18f, 0.90f);
            drawCube(0.16f, yy, z - 0.12f, 0.12f, 0.12f, 0.06f, 0f, 1.0f, 0.72f, 0.18f, 0.90f);
        }

        drawCube(0f, 0.05f, z, 10.6f, 0.03f, 0.18f, 0f, 0.95f, 0.95f, 0.95f, 1f);
    }

    private void drawGuardRail(float x, float z, boolean left) {
        drawCube(x, 0.36f, z, 0.10f, 0.72f, 0.10f, 0f, 0.30f, 0.31f, 0.34f, 1f);

        float offset = left ? 0.35f : -0.35f;

        drawCube(
                x + offset, 0.55f, z,
                0.68f, 0.09f, 0.12f, 0f,
                0.68f, 0.70f, 0.74f, 1f
        );

        drawCube(
                x + offset, 0.28f, z,
                0.68f, 0.09f, 0.12f, 0f,
                0.68f, 0.70f, 0.74f, 1f
        );
    }

    private void drawLamp(float x, float z) {
        drawCube(x, 2.55f, z, 0.11f, 5.05f, 0.11f, 0f, 0.17f, 0.18f, 0.21f, 1f);
        drawCube(x, 5.02f, z, 0.48f, 0.12f, 0.48f, 0f, 0.35f, 0.95f, 0.58f, 1f);

        // lamp glow
        drawCube(x, 4.90f, z, 1.35f, 0.08f, 1.35f, 0f, 0.50f, 0.95f, 0.60f, 0.18f);

        // ground reflection
        drawCube(x * 0.75f, 0.01f, z, 0.95f, 0.01f, 1.90f, 0f, 0.26f, 0.35f, 0.22f, 0.12f);
    }

    private void drawCityBlock(float x, float z, float w, float h, float d,
                               float r, float g, float b) {
        drawCube(x, h * 0.5f - 0.1f, z, w, h, d, 0f, r, g, b, 1f);

        // windows
        for (int i = 0; i < 4; i++) {
            float yy = 1.2f + i * 1.1f;

            if (yy > h - 0.6f) {
                break;
            }

            drawCube(
                    x + (x < 0 ? 2.4f : -2.4f),
                    yy,
                    z + 2.5f,
                    0.08f,
                    0.28f,
                    2.5f,
                    0f,
                    0.18f,
                    0.48f,
                    0.30f,
                    1f
            );

            drawCube(
                    x + (x < 0 ? 2.4f : -2.4f),
                    yy,
                    z - 2.7f,
                    0.08f,
                    0.28f,
                    2.1f,
                    0f,
                    0.55f,
                    0.32f,
                    0.15f,
                    1f
            );
        }

        // rooftop accent
        drawCube(x, h + 0.05f, z, w * 0.72f, 0.10f, d * 0.72f, 0f, r + 0.03f, g + 0.03f, b + 0.03f, 1f);
    }

    private void drawOpponentCar(float x, float y, float z, float t) {
        drawGolf7(x, y, z, 0f, true, false, t);
    }

    private void drawGolf7(float x, float y, float z, float rotY,
                           boolean opponent, boolean garage, float t) {

        float bodyR = opponent ? 0.18f : 0.78f;
        float bodyG = opponent ? 0.22f : 0.08f;
        float bodyB = opponent ? 0.30f : 0.09f;

        float highlightR = opponent ? 0.26f : 0.92f;
        float highlightG = opponent ? 0.30f : 0.12f;
        float highlightB = opponent ? 0.38f : 0.12f;

        float bounce = 0f;

        if (!garage && !opponent) {
            bounce =
                    Math.min(0.030f, state.speedKmh / 8500f)
                            * (float)Math.sin(t * 42f);
        }

        float yy = y + bounce;

        // lower main body
        drawCube(x, yy + 0.40f, z, 1.78f, 0.44f, 3.95f, rotY, bodyR, bodyG, bodyB, 1f);

        // bonnet
        drawCube(x, yy + 0.67f, z - 1.12f, 1.62f, 0.22f, 0.92f, rotY, highlightR, highlightG, highlightB, 1f);

        // side shoulder
        drawCube(x, yy + 0.73f, z + 0.15f, 1.70f, 0.18f, 2.55f, rotY, highlightR * 0.92f, highlightG * 0.92f, highlightB * 0.92f, 1f);

        // cabin lower
        drawCube(x, yy + 0.98f, z + 0.10f, 1.34f, 0.34f, 1.95f, rotY, 0.08f, 0.09f, 0.11f, 1f);

        // roof
        drawCube(x, yy + 1.28f, z + 0.28f, 1.10f, 0.12f, 1.25f, rotY, bodyR * 0.82f, bodyG * 0.82f, bodyB * 0.82f, 1f);

        // rear hatch top
        drawCube(x, yy + 1.03f, z + 1.44f, 1.18f, 0.34f, 0.62f, rotY, bodyR * 0.92f, bodyG * 0.92f, bodyB * 0.92f, 1f);

        // rear hatch lower
        drawCube(x, yy + 0.73f, z + 1.80f, 1.32f, 0.34f, 0.28f, rotY, highlightR * 0.90f, highlightG * 0.90f, highlightB * 0.90f, 1f);

        // front bumper / grille
        drawCube(x, yy + 0.28f, z - 1.98f, 1.65f, 0.16f, 0.18f, rotY, 0.05f, 0.05f, 0.06f, 1f);
        drawCube(x, yy + 0.45f, z - 1.96f, 1.45f, 0.13f, 0.05f, rotY, 0.03f, 0.03f, 0.04f, 1f);

        // rear bumper
        drawCube(x, yy + 0.28f, z + 1.98f, 1.65f, 0.16f, 0.18f, rotY, 0.05f, 0.05f, 0.06f, 1f);

        // front lights
        drawCube(x - 0.54f, yy + 0.56f, z - 1.96f, 0.34f, 0.12f, 0.06f, rotY, 0.72f, 0.90f, 1f, 1f);
        drawCube(x + 0.54f, yy + 0.56f, z - 1.96f, 0.34f, 0.12f, 0.06f, rotY, 0.72f, 0.90f, 1f, 1f);

        // rear lights
        drawCube(x - 0.56f, yy + 0.58f, z + 1.95f, 0.28f, 0.14f, 0.06f, rotY, 0.95f, 0.05f, 0.04f, 1f);
        drawCube(x + 0.56f, yy + 0.58f, z + 1.95f, 0.28f, 0.14f, 0.06f, rotY, 0.95f, 0.05f, 0.04f, 1f);

        // mirrors
        drawCube(x - 1.04f, yy + 0.88f, z - 0.38f, 0.12f, 0.10f, 0.18f, rotY, 0.06f, 0.06f, 0.07f, 1f);
        drawCube(x + 1.04f, yy + 0.88f, z - 0.38f, 0.12f, 0.10f, 0.18f, rotY, 0.06f, 0.06f, 0.07f, 1f);

        // wheels
        float frontWheelZ = z - 1.20f;
        float rearWheelZ = z + 1.26f;

        drawWheel(x - 0.95f, yy + 0.24f, frontWheelZ);
        drawWheel(x + 0.95f, yy + 0.24f, frontWheelZ);
        drawWheel(x - 0.95f, yy + 0.24f, rearWheelZ);
        drawWheel(x + 0.95f, yy + 0.24f, rearWheelZ);

        // windshield
        drawCube(x, yy + 1.02f, z - 0.55f, 1.18f, 0.16f, 0.62f, rotY, 0.10f, 0.14f, 0.18f, 1f);

        // rear glass
        drawCube(x, yy + 1.00f, z + 1.16f, 1.08f, 0.16f, 0.40f, rotY, 0.10f, 0.14f, 0.18f, 1f);

        // roof spoiler lip
        drawCube(x, yy + 1.34f, z + 1.64f, 1.06f, 0.05f, 0.14f, rotY, 0.04f, 0.04f, 0.05f, 1f);

        // small underglow reflection on wet road
        if (!garage) {
            drawCube(x, 0.01f, z, 1.65f, 0.01f, 3.25f, 0f, bodyR, bodyG, bodyB, 0.10f);
        }
    }

    private void drawWheel(float x, float y, float z) {
        drawCube(x, y, z, 0.26f, 0.54f, 0.64f, 0f, 0.018f, 0.018f, 0.022f, 1f);

        float hubOffset = x < 0f ? -0.135f : 0.135f;

        drawCube(x + hubOffset, y, z, 0.04f, 0.28f, 0.34f, 0f, 0.40f, 0.42f, 0.46f, 1f);
    }

    private void drawCube(float x, float y, float z,
                          float sx, float sy, float sz,
                          float rotY,
                          float r, float g, float b, float a) {

        GLES20.glUseProgram(program);

        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, x, y, z);

        if (rotY != 0f) {
            Matrix.rotateM(model, 0, rotY, 0f, 1f, 0f);
        }

        Matrix.scaleM(model, 0, sx, sy, sz);
        Matrix.multiplyMM(mvp, 0, pv, 0, model, 0);

        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0);
        GLES20.glUniform4f(uColor, r, g, b, a);

        cube.position(0);
        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(
                aPosition,
                3,
                GLES20.GL_FLOAT,
                false,
                0,
                cube
        );

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        GLES20.glDisableVertexAttribArray(aPosition);
    }

    private static int createProgram(String vs, String fs) {
        int v = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(v, vs);
        GLES20.glCompileShader(v);

        int f = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(f, fs);
        GLES20.glCompileShader(f);

        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, v);
        GLES20.glAttachShader(p, f);
        GLES20.glLinkProgram(p);

        GLES20.glDeleteShader(v);
        GLES20.glDeleteShader(f);

        return p;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp(t, 0f, 1f);
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = clamp((x - edge0) / Math.max(0.0001f, edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }
}
