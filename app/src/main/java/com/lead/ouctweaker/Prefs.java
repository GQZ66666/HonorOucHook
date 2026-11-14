package com.lead.ouctweaker;

import de.robv.android.xposed.XSharedPreferences;

/** 配置快照：进程启动时读一次，改完要到系统设置里强停「软件更新」再开。 */
public final class Prefs {

    public static final String PKG = "com.lead.ouctweaker";
    public static final String FILE = "cfg";

    public boolean blockScheduled;
    public boolean blockAutoCheck;
    public boolean blockInstall;
    public boolean lowerPriority;
    public boolean spoofVersion;
    public boolean trialMode;
    public boolean blockPush;
    public boolean blockCota;
    public boolean blockRomSurvey;
    public boolean debug;
    public boolean dumpFile;
    public String version = "";

    public static Prefs load() {
        Prefs p = new Prefs();
        try {
            XSharedPreferences sp = new XSharedPreferences(PKG, FILE);
            sp.reload();
            p.blockScheduled = sp.getBoolean("block_scheduled", true);
            p.blockAutoCheck = sp.getBoolean("block_autocheck", false);
            p.blockInstall = sp.getBoolean("block_install", true);
            p.lowerPriority = sp.getBoolean("lower_priority", true);
            p.spoofVersion = sp.getBoolean("spoof_version", false);
            p.trialMode = sp.getBoolean("trial_mode", false);
            p.blockPush = sp.getBoolean("block_push", false);
            p.blockCota = sp.getBoolean("block_cota", false);
            p.blockRomSurvey = sp.getBoolean("block_romsurvey", false);
            p.debug = sp.getBoolean("debug", true);
            p.dumpFile = sp.getBoolean("dump_file", false);
            p.version = sp.getString("version_value", "");
        } catch (Throwable ignored) {
        }
        return p;
    }

    @Override
    public String toString() {
        return "sched=" + blockScheduled + " auto=" + blockAutoCheck + " inst=" + blockInstall
                + " prio=" + lowerPriority + " spoof=" + spoofVersion + "(" + version + ")"
                + " trial=" + trialMode + " push=" + blockPush + " cota=" + blockCota
                + " survey=" + blockRomSurvey + " debug=" + debug;
    }
}
