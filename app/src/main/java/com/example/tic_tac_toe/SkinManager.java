package com.example.tic_tac_toe;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.DisplayMetrics;

/**
 * Manages board-theme skins and symbol skins.
 * All drawables are built programmatically — no extra XML needed.
 */
public class SkinManager {

    // ── Board skin IDs ────────────────────────────────────────────────────────
    public static final String SKIN_DEFAULT  = "default";
    public static final String SKIN_INFERNO  = "inferno";
    public static final String SKIN_ARCTIC   = "arctic";
    public static final String SKIN_NEON     = "neon";
    public static final String SKIN_MIDNIGHT = "midnight";
    public static final String SKIN_GALAXY   = "galaxy";

    // ── Symbol skin IDs ───────────────────────────────────────────────────────
    public static final String SYM_DEFAULT = "default";
    public static final String SYM_FIRE    = "fire";
    public static final String SYM_ICE     = "ice";
    public static final String SYM_STARS   = "stars";

    private static final String PREFS_NAME = "skin_prefs";

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static SkinManager instance;

    public static SkinManager get(Context ctx) {
        if (instance == null)
            instance = new SkinManager(ctx.getApplicationContext());
        return instance;
    }

    // ── Data classes ──────────────────────────────────────────────────────────

    public static class BoardSkin {
        public final String id, emoji, name;
        /** Unlock condition: achievement ID, or null */
        public final String unlockAchId;
        /** Unlock condition: total wins needed (0 = not needed) */
        public final int    unlockWins;
        // Cell colors
        public final int cellBg, cellBgPressed, cellStroke, cellStrokePressed;
        // Win-cell gradient + border
        public final int winStart, winEnd, winStroke;

        public BoardSkin(String id, String emoji, String name,
                         String unlockAchId, int unlockWins,
                         int cellBg, int cellBgPressed, int cellStroke, int cellStrokePressed,
                         int winStart, int winEnd, int winStroke) {
            this.id = id; this.emoji = emoji; this.name = name;
            this.unlockAchId = unlockAchId; this.unlockWins = unlockWins;
            this.cellBg = cellBg; this.cellBgPressed = cellBgPressed;
            this.cellStroke = cellStroke; this.cellStrokePressed = cellStrokePressed;
            this.winStart = winStart; this.winEnd = winEnd; this.winStroke = winStroke;
        }
    }

    public static class SymbolSkin {
        public final String id, emoji, name;
        public final String playerSymbol, aiSymbol;
        public final int    playerColor, aiColor;
        /** Unlock condition: board skin ID that must be unlocked first */
        public final String unlockBoardId;
        /** Unlock condition: achievement ID (used instead of board skin) */
        public final String unlockAchId;

        public SymbolSkin(String id, String emoji, String name,
                          String playerSymbol, String aiSymbol,
                          int playerColor, int aiColor,
                          String unlockBoardId, String unlockAchId) {
            this.id = id; this.emoji = emoji; this.name = name;
            this.playerSymbol = playerSymbol; this.aiSymbol = aiSymbol;
            this.playerColor = playerColor; this.aiColor = aiColor;
            this.unlockBoardId = unlockBoardId; this.unlockAchId = unlockAchId;
        }
    }

    // ── Catalog ───────────────────────────────────────────────────────────────

    public BoardSkin[] getBoardSkins() {
        return new BoardSkin[]{
            new BoardSkin(SKIN_DEFAULT, "🌌", "Default",
                null, 0,
                0xFF160E35, 0xFF221A50, 0xFF3D2070, 0xFFFF2D78,
                0x2010B981, 0x1510B981, 0xFF10B981),

            new BoardSkin(SKIN_INFERNO, "🔥", "Inferno",
                null, 10,
                0xFF1A0800, 0xFF2E0F00, 0xFF8B2500, 0xFFFF5500,
                0x20FF4500, 0x15FF2200, 0xFFFF6B35),

            new BoardSkin(SKIN_ARCTIC, "❄️", "Arctic",
                AchievementManager.ON_FIRE, 0,
                0xFF001526, 0xFF002540, 0xFF005580, 0xFF00BFFF,
                0x2000BFFF, 0x1500D4FF, 0xFF00D4FF),

            new BoardSkin(SKIN_NEON, "💜", "Neon",
                null, 50,
                0xFF0D0020, 0xFF1A0040, 0xFF6600CC, 0xFFCC00FF,
                0x20AA00FF, 0x15BB00FF, 0xFFCC44FF),

            new BoardSkin(SKIN_MIDNIGHT, "🌙", "Midnight",
                AchievementManager.GENIUS, 0,
                0xFF050510, 0xFF0A0A25, 0xFF1A1A50, 0xFF4444AA,
                0x204444AA, 0x153344AA, 0xFF6666CC),

            new BoardSkin(SKIN_GALAXY, "⭐", "Galaxy",
                null, 100,
                0xFF080518, 0xFF120830, 0xFF2A1055, 0xFFFFD700,
                0x20FFD700, 0x15FFAA00, 0xFFFFDD44),
        };
    }

    public SymbolSkin[] getSymbolSkins() {
        return new SymbolSkin[]{
            new SymbolSkin(SYM_DEFAULT, "🎮", "Classic",
                "X", "O", 0xFFFBBF24, 0xFF38BDF8,
                null, null),

            new SymbolSkin(SYM_FIRE, "🔥", "Fire & Water",
                "🔥", "💧", 0xFFFF6B35, 0xFF00BFFF,
                SKIN_INFERNO, null),

            new SymbolSkin(SYM_ICE, "❄️", "Ice & Wave",
                "❄️", "🌊", 0xFF90E0FF, 0xFF005FA3,
                SKIN_ARCTIC, null),

            new SymbolSkin(SYM_STARS, "⭐", "Stars",
                "⭐", "🌙", 0xFFFFD700, 0xFFB0A0FF,
                null, AchievementManager.CHAMPION),
        };
    }

    // ── Unlock checks ─────────────────────────────────────────────────────────

    public boolean isBoardUnlocked(BoardSkin skin, AchievementManager am, int totalWins) {
        if (SKIN_DEFAULT.equals(skin.id)) return true;
        if (skin.unlockWins > 0 && totalWins < skin.unlockWins) return false;
        if (skin.unlockAchId != null) {
            Achievement a = am.get(skin.unlockAchId);
            if (a == null || !a.unlocked) return false;
        }
        return true;
    }

    public boolean isSymbolUnlocked(SymbolSkin sym, AchievementManager am, int totalWins) {
        if (SYM_DEFAULT.equals(sym.id)) return true;
        if (sym.unlockBoardId != null) {
            for (BoardSkin bs : getBoardSkins()) {
                if (bs.id.equals(sym.unlockBoardId)) {
                    return isBoardUnlocked(bs, am, totalWins);
                }
            }
        }
        if (sym.unlockAchId != null) {
            Achievement a = am.get(sym.unlockAchId);
            return a != null && a.unlocked;
        }
        return false;
    }

    // ── Unlock condition labels ───────────────────────────────────────────────

    public String getBoardUnlockLabel(BoardSkin skin, AchievementManager am) {
        if (SKIN_DEFAULT.equals(skin.id)) return "Free";
        if (skin.unlockWins > 0) return "Win " + skin.unlockWins + " games";
        if (skin.unlockAchId != null) {
            Achievement a = am.get(skin.unlockAchId);
            return a != null ? "Get: " + a.title : "Achievement";
        }
        return "";
    }

    public String getSymbolUnlockLabel(SymbolSkin sym, AchievementManager am) {
        if (SYM_DEFAULT.equals(sym.id)) return "Free";
        if (sym.unlockBoardId != null) return "Unlock " + sym.unlockBoardId + " skin";
        if (sym.unlockAchId != null) {
            Achievement a = am.get(sym.unlockAchId);
            return a != null ? "Get: " + a.title : "Achievement";
        }
        return "";
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private final SharedPreferences prefs;

    private SkinManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getCurrentBoardSkinId()   { return prefs.getString("board",  SKIN_DEFAULT); }
    public String getCurrentSymbolSkinId()  { return prefs.getString("symbol", SYM_DEFAULT); }
    public void   setBoardSkin(String id)   { prefs.edit().putString("board",  id).apply(); }
    public void   setSymbolSkin(String id)  { prefs.edit().putString("symbol", id).apply(); }

    public BoardSkin getCurrentBoardSkin() {
        String id = getCurrentBoardSkinId();
        for (BoardSkin s : getBoardSkins()) if (s.id.equals(id)) return s;
        return getBoardSkins()[0];
    }

    public SymbolSkin getCurrentSymbolSkin() {
        String id = getCurrentSymbolSkinId();
        for (SymbolSkin s : getSymbolSkins()) if (s.id.equals(id)) return s;
        return getSymbolSkins()[0];
    }

    // ── Drawable builders ─────────────────────────────────────────────────────

    /** Build a selector drawable for empty game cells using the given skin */
    public Drawable makeCellDrawable(BoardSkin skin, float density) {
        int r = dp(14, density);

        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(skin.cellBgPressed);
        pressed.setCornerRadius(r);
        pressed.setStroke(dp(2, density), skin.cellStrokePressed);

        GradientDrawable normal = new GradientDrawable();
        normal.setColor(skin.cellBg);
        normal.setCornerRadius(r);
        normal.setStroke(dp(1, density), skin.cellStroke);

        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed}, pressed);
        sld.addState(new int[]{}, normal);
        return sld;
    }

    /** Build win-highlight drawable for cells */
    public Drawable makeWinDrawable(BoardSkin skin, float density) {
        GradientDrawable gd = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{skin.winStart, skin.winEnd});
        gd.setCornerRadius(dp(14, density));
        gd.setStroke(dp(2, density), skin.winStroke);
        return gd;
    }

    private int dp(int dp, float density) { return Math.round(dp * density); }
}
