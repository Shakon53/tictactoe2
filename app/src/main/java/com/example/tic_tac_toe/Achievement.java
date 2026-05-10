package com.example.tic_tac_toe;

public class Achievement {
    public final String id;
    public final int    iconRes;   // vector drawable resource ID
    public final String title;
    public final String desc;
    public final int    maxProgress;

    public boolean unlocked;
    public int     progress;

    public Achievement(String id, int iconRes, String title, String desc, int maxProgress) {
        this.id          = id;
        this.iconRes     = iconRes;
        this.title       = title;
        this.desc        = desc;
        this.maxProgress = maxProgress;
    }
}
