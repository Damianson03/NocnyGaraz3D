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
            -0.5f,-0.5f, 0.5f,  0.5f,-0.5f, 0.5f,  0.5f, 0.5f, 0.5f,
            -0.5f,-0.5f, 0.5f,  0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
             0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f, -0.5f, 0.5f,-0.5f,
             0.5f,-0.5f,-0.5f, -0.5f, 0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
            -0.5f,-0.5f,-0.5f, -0.5f,-0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
            -0.5f,-0.5f,-0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f,-0.5f,
             0.5f,-0.5f, 0.5f,  0.5f,-0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
             0.5f,-0.5f, 0.5f,  0.5f, 0.5f,-0.5f,  0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f, 0.5f,  0.5f, 0.5f, 0.5f,  0.5f, 0.5f,-0.5f,
            -0.5f, 0.5f, 0.5f,  0.5f, 0.5f,-0.5f, -0.5f, 0.5f,-0.5f,
            -0.5f,-0.5f,-0.5f,  0.5f,-0.5f,-0.5f,  0.5f,-0.5f, 0.5f,
            -0.5f,-0.5f,-0.5f,  0.5f,-0.5f, 0.5f, -0.5f,-0.5f, 0.5f
    };

    public DragRenderer(GameState state) { this.state = state; }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.015f, 0.02f, 0.045f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        cube = ByteBuffer.allocateDirect(CUBE.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        cube.put(CUBE).position(0);
        program = createProgram(
                "uniform mat4 uMVP; attribute vec3 aPosition; void main(){ gl_Position=uMVP*vec4(aPosition,1.0); }",
                "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }"
        );
        aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        uMvp = GLES20.glGetUniformLocation(program, "uMVP");
        uColor = GLES20.glGetUniformLocation(program, "uColor");
        startNanos = System.nanoTime();
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        aspect = width / (float)Math.max(1, height);
        Matrix.perspectiveM(proj, 0, 52f, aspect, 0.1f, 900f);
    }

    @Override public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        float t = (System.nanoTime() - startNanos) / 1_000_000_000f;
        if (state.screen == GameState.Screen.GARAGE || state.screen == GameState.Screen.MODE_SELECT) {
            drawGarage(t);
        } else {
            drawTrack(t);
        }
    }

    private void drawGarage(float t) {
        float angle = 14f + (float)Math.sin(t * 0.22f) * 7f;
        float camX = 7.6f * (float)Math.sin(Math.toRadians(angle));
        float camZ = 7.6f * (float)Math.cos(Math.toRadians(angle));
        Matrix.setLookAtM(view, 0, camX, 2.7f, camZ, 0f, 0.65f, 0f, 0f, 1f, 0f);
        Matrix.multiplyMM(pv, 0, proj, 0, view, 0);

        // garage floor and back wall
        drawCube(0f, -0.16f, 0f, 16f, 0.25f, 14f, 0f, 0.045f,0.05f,0.07f,1f);
        drawCube(0f, 3.0f, -6.2f, 16f, 6f, 0.28f, 0f, 0.035f,0.04f,0.06f,1f);
        for (int i=-3;i<=3;i++) {
            drawCube(i*2.1f, 2.9f, -6.0f, 0.08f, 0.35f, 0.18f, 0f, 0.18f,1f,0.38f,1f);
            drawCube(i*2.1f, 2.35f, -6.0f, 1.15f, 0.05f, 0.18f, 0f, 0.12f,0.35f,0.18f,1f);
        }
        drawCar(0f, 0f, 0f, 0f, false, t);
    }

    private void drawTrack(float t) {
        float pd = state.playerDistance;
        float follow = Math.min(392f, pd);
        float cameraZ = 12f - follow;
        Matrix.setLookAtM(view, 0,
                8.5f, 4.0f, cameraZ,
                0f, 0.8f, -11f - follow,
                0f, 1f, 0f);
        Matrix.multiplyMM(pv, 0, proj, 0, view, 0);

        // road and shoulders
        drawCube(0f, -0.18f, -205f, 11f, 0.24f, 430f, 0f, 0.055f,0.06f,0.075f,1f);
        drawCube(-6.2f, -0.16f, -205f, 1.1f, 0.18f, 430f, 0f, 0.11f,0.11f,0.12f,1f);
        drawCube( 6.2f, -0.16f, -205f, 1.1f, 0.18f, 430f, 0f, 0.11f,0.11f,0.12f,1f);
        for (int z=0; z<=420; z+=16) {
            drawCube(0f, -0.02f, -z, 0.10f, 0.03f, 7.5f, 0f, 0.76f,0.72f,0.35f,0.85f);
        }
        // start & finish markers
        drawCube(0f, 0.01f, -2.5f, 10.5f, 0.04f, 0.25f, 0f, 0.9f,0.9f,0.9f,1f);
        drawCube(0f, 0.01f, -402.3f, 10.5f, 0.04f, 0.55f, 0f, 0.18f,1f,0.42f,1f);

        for (int z=5; z<=420; z+=30) {
            float zz = -z;
            drawLamp(-7.1f, zz);
            drawLamp(7.1f, zz);
        }
        for (int i=0; i<15; i++) {
            float z = -(i*31f + 15f);
            float h1 = 5f + (i%4)*1.4f;
            float h2 = 4f + ((i+2)%5)*1.1f;
            drawCube(-13.0f, h1*0.5f-0.1f, z, 7.2f, h1, 18f, 0f, 0.035f,0.045f,0.075f,1f);
            drawCube( 13.5f, h2*0.5f-0.1f, z-8f, 7.8f, h2, 20f, 0f, 0.045f,0.05f,0.08f,1f);
            // lit windows
            drawCube(-9.35f, 2.1f, z+2f, 0.08f, 0.45f, 3.0f, 0f, 0.16f,0.42f,0.28f,1f);
            drawCube( 9.55f, 2.7f, z-5f, 0.08f, 0.45f, 3.5f, 0f, 0.5f,0.28f,0.12f,1f);
        }

        drawCar(-1.75f, 0f, -state.playerDistance, 0f, false, t);
        drawCar( 1.75f, 0f, -state.opponentDistance, 0f, true, t);
    }

    private void drawLamp(float x, float z) {
        drawCube(x, 2.4f, z, 0.10f, 4.9f, 0.10f, 0f, 0.18f,0.20f,0.23f,1f);
        drawCube(x, 4.85f, z, 0.42f, 0.12f, 0.42f, 0f, 0.30f,0.95f,0.52f,1f);
    }

    private void drawCar(float x, float y, float z, float rotY, boolean opponent, float t) {
        float r = opponent ? 0.72f : 0.08f;
        float g = opponent ? 0.12f : 0.54f;
        float b = opponent ? 0.10f : 0.95f;
        float bounce = (state.screen == GameState.Screen.RACING && !opponent ? Math.min(0.035f, state.speedKmh/9000f) : 0f)
                * (float)Math.sin(t*40f);
        float yy = y + bounce;
        // lower chassis/body
        drawCube(x, yy+0.43f, z, 1.82f, 0.52f, 4.15f, rotY, r,g,b,1f);
        drawCube(x, yy+0.78f, z+0.22f, 1.66f, 0.34f, 2.95f, rotY, r*0.88f,g*0.88f,b*0.88f,1f);
        // cabin
        drawCube(x, yy+1.12f, z+0.35f, 1.42f, 0.56f, 1.75f, rotY, 0.055f,0.075f,0.11f,1f);
        // roof line
        drawCube(x, yy+1.43f, z+0.36f, 1.18f, 0.10f, 1.36f, rotY, r*0.72f,g*0.72f,b*0.72f,1f);
        // bumpers / diffuser
        drawCube(x, yy+0.30f, z+2.13f, 1.72f, 0.18f, 0.20f, rotY, 0.04f,0.04f,0.05f,1f);
        drawCube(x, yy+0.30f, z-2.13f, 1.72f, 0.18f, 0.20f, rotY, 0.04f,0.04f,0.05f,1f);
        // wheels (stylized as thick dark cuboids)
        float wzF = z-1.32f, wzR = z+1.32f;
        drawWheel(x-0.94f, yy+0.28f, wzF);
        drawWheel(x+0.94f, yy+0.28f, wzF);
        drawWheel(x-0.94f, yy+0.28f, wzR);
        drawWheel(x+0.94f, yy+0.28f, wzR);
        // lights: front is negative z (race direction)
        drawCube(x-0.53f, yy+0.55f, z-2.105f, 0.38f, 0.16f, 0.05f, rotY, 0.74f,0.90f,1f,1f);
        drawCube(x+0.53f, yy+0.55f, z-2.105f, 0.38f, 0.16f, 0.05f, rotY, 0.74f,0.90f,1f,1f);
        drawCube(x-0.56f, yy+0.52f, z+2.105f, 0.34f, 0.14f, 0.05f, rotY, 0.95f,0.04f,0.02f,1f);
        drawCube(x+0.56f, yy+0.52f, z+2.105f, 0.34f, 0.14f, 0.05f, rotY, 0.95f,0.04f,0.02f,1f);
        // spoiler
        drawCube(x, yy+1.05f, z+1.98f, 1.65f, 0.08f, 0.25f, rotY, 0.05f,0.05f,0.06f,1f);
        drawCube(x-0.55f, yy+0.82f, z+1.98f, 0.08f, 0.42f, 0.10f, rotY, 0.05f,0.05f,0.06f,1f);
        drawCube(x+0.55f, yy+0.82f, z+1.98f, 0.08f, 0.42f, 0.10f, rotY, 0.05f,0.05f,0.06f,1f);
    }

    private void drawWheel(float x, float y, float z) {
        drawCube(x, y, z, 0.28f, 0.58f, 0.72f, 0f, 0.018f,0.018f,0.022f,1f);
        drawCube(x + (x<0 ? -0.145f : 0.145f), y, z, 0.04f, 0.34f, 0.42f, 0f, 0.32f,0.34f,0.38f,1f);
    }

    private void drawCube(float x,float y,float z,float sx,float sy,float sz,float rotY,
                          float r,float g,float b,float a) {
        GLES20.glUseProgram(program);
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, x,y,z);
        if (rotY != 0f) Matrix.rotateM(model, 0, rotY, 0f,1f,0f);
        Matrix.scaleM(model, 0, sx,sy,sz);
        Matrix.multiplyMM(mvp, 0, pv, 0, model, 0);
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0);
        GLES20.glUniform4f(uColor, r,g,b,a);
        cube.position(0);
        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, cube);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        GLES20.glDisableVertexAttribArray(aPosition);
    }

    private static int createProgram(String vs, String fs) {
        int v = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(v, vs); GLES20.glCompileShader(v);
        int f = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(f, fs); GLES20.glCompileShader(f);
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, v); GLES20.glAttachShader(p, f); GLES20.glLinkProgram(p);
        GLES20.glDeleteShader(v); GLES20.glDeleteShader(f);
        return p;
    }
}
