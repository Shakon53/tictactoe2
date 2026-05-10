package com.example.tic_tac_toe;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;

/**
 * Singleton background music manager.
 *
 * Usage (from BaseGameActivity):
 *   onResume  → MusicManager.get(this).resume()
 *   onPause   → MusicManager.get(this).pause()
 *   onDestroy → MusicManager.get(this).release()  [only from Application or last activity]
 *
 * Setting toggle: "setting_music" boolean in "tictactoe_prefs" (true = on, default on).
 */
public class MusicManager {

    private static final String PREFS_NAME  = "tictactoe_prefs";
    private static final String KEY_MUSIC   = "pref_bgm"; // совпадает с SettingsActivity

    private static MusicManager instance;

    private MediaPlayer  player;
    private final Context appCtx;
    private boolean isEnabled;
    private int     activeActivities = 0; // count activities in foreground

    private MusicManager(Context ctx) {
        appCtx    = ctx.getApplicationContext();
        SharedPreferences prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isEnabled = prefs.getBoolean(KEY_MUSIC, false); // по умолчанию выключена, как в SettingsActivity
    }

    public static MusicManager get(Context ctx) {
        if (instance == null) instance = new MusicManager(ctx);
        return instance;
    }

    // ── Called from BaseGameActivity.onResume() ───────────────────────────────

    public void onActivityResume() {
        activeActivities++;
        if (activeActivities == 1) {
            // First activity entering foreground
            resume();
        }
    }

    // ── Called from BaseGameActivity.onPause() ────────────────────────────────

    public void onActivityPause() {
        activeActivities = Math.max(0, activeActivities - 1);
        if (activeActivities == 0) {
            // No activities visible — pause music
            pause();
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void resume() {
        if (!isEnabled) return;
        if (player == null) {
            player = MediaPlayer.create(appCtx, R.raw.bg_music);
            if (player == null) return;
            player.setLooping(true);
            player.setVolume(0.45f, 0.45f);
        }
        if (!player.isPlaying()) {
            player.start();
        }
    }

    private void pause() {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Enable or disable music. Persists the setting. */
    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
              .edit().putBoolean(KEY_MUSIC, enabled).apply();
        if (enabled) {
            resume();
        } else {
            pause();
        }
    }

    public boolean isEnabled() { return isEnabled; }

    /** Fade volume to 0 and pause — call before sound effects that need focus */
    public void duck() {
        if (player != null && player.isPlaying()) {
            player.setVolume(0.1f, 0.1f);
        }
    }

    /** Restore normal volume */
    public void unduck() {
        if (player != null) {
            player.setVolume(0.45f, 0.45f);
        }
    }

    /** Release resources — call only when the app is fully exiting */
    public void release() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        instance = null;
    }
}
