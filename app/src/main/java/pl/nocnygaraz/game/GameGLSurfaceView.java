package pl.nocnygaraz.game;

import android.content.Context;
import android.opengl.GLSurfaceView;

public final class GameGLSurfaceView extends GLSurfaceView {
    public GameGLSurfaceView(Context context, GameState state) {
        super(context);
        setEGLContextClientVersion(2);
        setRenderer(new DragRenderer(state));
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        setPreserveEGLContextOnPause(true);
    }
}
