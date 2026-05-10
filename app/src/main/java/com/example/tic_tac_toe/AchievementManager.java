package com.example.tic_tac_toe;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton that owns all achievement definitions, persists state,
 * and exposes a single checkAfterGame() call.
 */
public class AchievementManager {

    // ── IDs ──────────────────────────────────────────────────────────────────
    public static final String FIRST_BLOOD  = "first_blood";
    public static final String ON_FIRE      = "on_fire";
    public static final String DESTROYER    = "destroyer";
    public static final String GENIUS       = "genius";
    public static final String SPEED_DEMON  = "speed_demon";
    public static final String DEDICATED    = "dedicated";
    public static final String VETERAN      = "veteran";
    public static final String CHAMPION     = "champion";
    public static final String COMEBACK     = "comeback";
    public static final String DIAMOND      = "diamond";

    private static final String PREFS_NAME = "ach_prefs";

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static AchievementManager instance;

    public static AchievementManager get(Context ctx) {
        if (instance == null)
            instance = new AchievementManager(ctx.getApplicationContext());
        return instance;
    }

    // ── Listener ──────────────────────────────────────────────────────────────
    public interface AchievementListener {
        void onAchievementUnlocked(Achievement a);
    }

    private AchievementListener listener;
    public void setListener(AchievementListener l)  { listener = l; }
    public void clearListener()                      { listener = null; }

    // ── State ─────────────────────────────────────────────────────────────────
    private final SharedPreferences prefs;
    private final List<Achievement>  achievements = new ArrayList<>();

    private AchievementManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        buildAchievements();
        loadState();
    }

    // ── Definitions ───────────────────────────────────────────────────────────
    private void buildAchievements() {
        achievements.add(new Achievement(FIRST_BLOOD,  R.drawable.ic_ach_target,
                "First Blood",    "Win your first game", 1));
        achievements.add(new Achievement(ON_FIRE,       R.drawable.ic_ach_fire,
                "On Fire",        "Win 3 games in a row", 1));
        achievements.add(new Achievement(DESTROYER,     R.drawable.ic_ach_skull,
                "Destroyer",      "Win 10 games in a row", 1));
        achievements.add(new Achievement(GENIUS,        R.drawable.ic_ach_brain,
                "Genius",         "Beat Hard AI in ≤7 total moves", 1));
        achievements.add(new Achievement(SPEED_DEMON,   R.drawable.ic_ach_lightning,
                "Speed Demon",    "Win in under 8 seconds of your own moves", 1));
        achievements.add(new Achievement(DEDICATED,     R.drawable.ic_ach_gamepad,
                "Dedicated",      "Play 50 total games", 50));
        achievements.add(new Achievement(VETERAN,       R.drawable.ic_ach_trophy,
                "Veteran",        "Play 100 total games", 100));
        achievements.add(new Achievement(CHAMPION,      R.drawable.ic_ach_crown,
                "Champion",       "Win 50 total games", 50));
        achievements.add(new Achievement(COMEBACK,      R.drawable.ic_ach_comeback,
                "Comeback",       "Win after 3 consecutive losses", 1));
        achievements.add(new Achievement(DIAMOND,       R.drawable.ic_ach_diamond,
                "Diamond Master", "Unlock all other achievements", 1));
    }

    // ── Persistence ───────────────────────────────────────────────────────────
    private void loadState() {
        for (Achievement a : achievements) {
            a.unlocked = prefs.getBoolean("u_" + a.id, false);
            a.progress = prefs.getInt("p_" + a.id, 0);
        }
    }

    private void saveState() {
        SharedPreferences.Editor ed = prefs.edit();
        for (Achievement a : achievements) {
            ed.putBoolean("u_" + a.id, a.unlocked);
            ed.putInt("p_" + a.id, a.progress);
        }
        ed.apply();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public List<Achievement> getAll() { return achievements; }

    public Achievement get(String id) {
        for (Achievement a : achievements) if (a.id.equals(id)) return a;
        return null;
    }

    public int getUnlockedCount() {
        int n = 0;
        for (Achievement a : achievements) if (a.unlocked) n++;
        return n;
    }

    /**
     * Call after every game completes.
     *
     * @param playerWon           true if the local player won
     * @param totalMoves          total moves made in this game (both sides)
     * @param playerThinkTimeMs   milliseconds the player spent thinking (excludes AI delay)
     * @param currentStreak       current consecutive-win streak (already updated)
     * @param totalWins           cumulative wins across all games
     * @param totalGames          cumulative games played
     * @param consecutiveLosses   consecutive losses BEFORE this game
     * @param isHardDifficulty    true if difficulty was HARD
     * @param playerMoves         moves made only by the player this game
     * @return list of newly unlocked achievements (may be empty)
     */
    public List<Achievement> checkAfterGame(
            boolean playerWon,
            int totalMoves,
            long playerThinkTimeMs,
            int currentStreak,
            int totalWins,
            int totalGames,
            int consecutiveLosses,
            boolean isHardDifficulty,
            int playerMoves) {

        List<Achievement> newlyUnlocked = new ArrayList<>();

        if (playerWon) {
            tryUnlock(FIRST_BLOOD, newlyUnlocked);
            if (currentStreak >= 3)  tryUnlock(ON_FIRE, newlyUnlocked);
            if (currentStreak >= 10) tryUnlock(DESTROYER, newlyUnlocked);
            // Genius: beat Hard AI in ≤7 total moves (achievable on 4x4/5x5 with limited-depth AI)
            if (isHardDifficulty && totalMoves <= 7) tryUnlock(GENIUS, newlyUnlocked);
            // Speed Demon: player's own think time under 8 seconds (AI delay excluded)
            if (playerThinkTimeMs < 8000L) tryUnlock(SPEED_DEMON, newlyUnlocked);
            if (consecutiveLosses >= 3) tryUnlock(COMEBACK, newlyUnlocked);
        }

        // Progress-based
        updateProgress(DEDICATED, totalGames, newlyUnlocked);
        updateProgress(VETERAN,   totalGames, newlyUnlocked);
        updateProgress(CHAMPION,  totalWins,  newlyUnlocked);

        // Meta-achievement
        checkDiamond(newlyUnlocked);

        saveState();

        // Notify listener
        for (Achievement a : newlyUnlocked) {
            if (listener != null) listener.onAchievementUnlocked(a);
        }

        return newlyUnlocked;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void tryUnlock(String id, List<Achievement> list) {
        Achievement a = get(id);
        if (a != null && !a.unlocked) {
            a.unlocked = true;
            a.progress = a.maxProgress;
            list.add(a);
        }
    }

    private void updateProgress(String id, int value, List<Achievement> list) {
        Achievement a = get(id);
        if (a == null || a.unlocked) return;
        a.progress = Math.min(value, a.maxProgress);
        if (a.progress >= a.maxProgress) {
            a.unlocked = true;
            list.add(a);
        }
    }

    private void checkDiamond(List<Achievement> newlyUnlocked) {
        Achievement diamond = get(DIAMOND);
        if (diamond == null || diamond.unlocked) return;
        for (Achievement a : achievements) {
            if (!a.id.equals(DIAMOND) && !a.unlocked) return;
        }
        diamond.unlocked = true;
        diamond.progress = 1;
        newlyUnlocked.add(diamond);
    }
}
