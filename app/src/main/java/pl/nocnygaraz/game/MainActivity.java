package pl.nocnygaraz.game;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public final class MainActivity extends Activity {
    private GameState state;
    private GameGLSurfaceView glView;
    private HUDView hud;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastNanos;
    private boolean running;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!running) return;
            long now = System.nanoTime();
            float dt = lastNanos == 0 ? 1f/60f : (now-lastNanos)/1_000_000_000f;
            lastNanos = now;
            state.update(dt);
            hud.invalidate();
            handler.postDelayed(this, 16);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        state = new GameState(this);
        glView = new GameGLSurfaceView(this, state);
        hud = new HUDView(this, state);
        FrameLayout root = new FrameLayout(this);
        root.addView(glView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(hud, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume(); glView.onResume(); running=true; lastNanos=0; handler.post(ticker);
    }
    @Override protected void onPause() {
        running=false; handler.removeCallbacks(ticker); state.save(); glView.onPause(); super.onPause();
    }
    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
}
