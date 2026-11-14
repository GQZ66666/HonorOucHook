package com.lead.ouctweaker;

import android.app.Service;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TARGET = "com.hihonor.ouc";
    private static final String T = "OucTweaker | ";

    private static final String CLS_DEPEND = "com.hihonor.android.hnouc.check.model.checkinfo.DependCheckInfo";
    private static final String CLS_PARAM = "com.hihonor.android.hnouc.check.model.checkinfo.Request$ParamBuilder";
    private static final String CLS_UPGRADE = "com.hihonor.android.hnouc.install.info.UpgradeInfo";
    private static final String CLS_NETJOB = "com.hihonor.android.hnouc.check.auto.NetworkCheckJobService";
    private static final String CLS_AUTOCHECK = "com.hihonor.android.hnouc.check.auto.AutoCheckService";
    private static final String CLS_PUSH = "com.hihonor.android.hnouc.para.push.HnOucMessageService";
    private static final String CLS_COTA = "com.hihonor.android.hnouc.newBiz.service.CotaCheckJobService";
    private static final String CLS_SURVEY = "com.hihonor.android.hnouc.romsurvey.check.AutoCheckService";
    private static final String CLS_VERSIONLIST = "com.hihonor.android.hnouc.para.ui.versionlist.ParaVersionListActivity";

    private static Prefs sCfg;

    private static void log(String s) {
        XposedBridge.log(T + s);
        try {
            Log.i("OucTweaker", s);
        } catch (Throwable ignored) {
        }
        if (sCfg != null && sCfg.dumpFile) {
            appendToFile(s);
        }
    }

    /** 无 root 时的兜底：直接落盘，用文件管理器就能拷出来。 */
    private static void appendToFile(String s) {
        try {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File f = new File(dir, "OucTweaker.log");
            FileOutputStream fos = new FileOutputStream(f, true);
            try {
                fos.write((s + "\n").getBytes("UTF-8"));
            } finally {
                fos.close();
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!TARGET.equals(lp.packageName)) return;

        Prefs cfg = Prefs.load();
        sCfg = cfg;
        log("loaded pid=" + android.os.Process.myPid() + " " + cfg);

        ClassLoader cl = lp.classLoader;

        if (cfg.blockScheduled) hookJobService(cl, CLS_NETJOB, "定时检查");
        if (cfg.blockAutoCheck) hookService(cl, CLS_AUTOCHECK, "自动检查服务");
        if (cfg.blockCota) hookJobService(cl, CLS_COTA, "应用推荐检查");
        if (cfg.blockRomSurvey) hookService(cl, CLS_SURVEY, "使用情况上报");
        if (cfg.blockPush) hookPush(cl);
        if (cfg.blockInstall) hookInstall(cl);
        if (cfg.lowerPriority) hookPriority(cl);
        if (cfg.spoofVersion && cfg.version != null && !cfg.version.isEmpty()) hookSpoof(cl, cfg.version);
        if (cfg.trialMode) hookTrial(cl);
        if (cfg.debug) hookDiagnostics(cl);
    }

    // ---------- 定时 / 后台服务 ----------

    private void hookJobService(ClassLoader cl, String cls, String label) {
        try {
            Class<?> c = XposedHelpers.findClass(cls, cl);
            XposedBridge.hookAllMethods(c, "onStartJob", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    log("blocked " + cls.substring(cls.lastIndexOf('.') + 1) + ".onStartJob (" + label + ")");
                    param.setResult(Boolean.FALSE);
                }
            });
            log("hook ok: " + label);
        } catch (Throwable t) {
            log("hook fail " + label + ": " + t);
        }
    }

    private void hookService(ClassLoader cl, String cls, String label) {
        try {
            Class<?> c = XposedHelpers.findClass(cls, cl);
            XposedBridge.hookAllMethods(c, "onStartCommand", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    log("blocked " + cls.substring(cls.lastIndexOf('.') + 1) + ".onStartCommand (" + label + ")");
                    param.setResult(Service.START_NOT_STICKY);
                }
            });
            log("hook ok: " + label);
        } catch (Throwable t) {
            log("hook fail " + label + ": " + t);
        }
    }

    // ---------- 推送 ----------

    private void hookPush(ClassLoader cl) {
        try {
            Class<?> c = XposedHelpers.findClass(CLS_PUSH, cl);
            XposedBridge.hookAllMethods(c, "a", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object msg = param.args.length > 0 ? param.args[0] : null;
                    log("blocked push: " + msg);
                    param.setResult(null);
                }
            });
            log("hook ok: push");
        } catch (Throwable t) {
            log("hook fail push: " + t);
        }
    }

    // ---------- 阻止自动安装 / 重启 ----------

    private void hookInstall(ClassLoader cl) {
        try {
            XposedBridge.hookAllMethods(PowerManager.class, "reboot", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String reason = "";
                    if (param.args.length > 0 && param.args[0] instanceof String) {
                        reason = (String) param.args[0];
                    }
                    log("PowerManager.reboot(" + reason + ")");
                    String r = reason == null ? "" : reason.toLowerCase();
                    if (r.isEmpty() || r.contains("recover") || r.contains("ota")
                            || r.contains("update") || r.contains("upgrade")) {
                        log("  -> blocked");
                        param.setResult(null);
                    }
                }
            });
            log("hook ok: PowerManager.reboot");
        } catch (Throwable t) {
            log("hook fail PowerManager.reboot: " + t);
        }

        try {
            Class<?> rs = XposedHelpers.findClass("android.os.RecoverySystem", cl);
            XposedBridge.hookAllMethods(rs, "installPackage", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object file = param.args.length > 1 ? param.args[1] : null;
                    log("blocked RecoverySystem.installPackage(" + file + ")");
                    param.setResult(null);
                }
            });
            log("hook ok: RecoverySystem.installPackage");
        } catch (Throwable t) {
            log("hook skip RecoverySystem: " + t);
        }
    }

    // ---------- 提醒优先级 ----------

    private void hookPriority(ClassLoader cl) {
        try {
            Class<?> ui = XposedHelpers.findClass(CLS_UPGRADE, cl);
            hookIntGetter(ui, "getInstallRemindPriority", 0);
            hookIntGetter(ui, "getUpgradePackagePriority", 0);
            log("hook ok: priority");
        } catch (Throwable t) {
            log("hook fail priority: " + t);
        }
    }

    // ---------- 伪装上报版本 ----------

    private void hookSpoof(ClassLoader cl, final String value) {
        try {
            Class<?> dci = XposedHelpers.findClass(CLS_DEPEND, cl);
            hookStringGetter(dci, "getProductBaseVersion", value);
            log("hook ok: spoof " + value);
        } catch (Throwable t) {
            log("hook fail spoof: " + t);
        }
    }

    // ---------- 尝鲜通道 ----------

    private void hookTrial(ClassLoader cl) {
        try {
            Class<?> dci = XposedHelpers.findClass(CLS_DEPEND, cl);
            Method m = XposedHelpers.findMethodExactIfExists(dci, "isTrialVersionsUpdate");
            if (m != null && m.getReturnType() == boolean.class) {
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        log("isTrialVersionsUpdate -> true");
                        param.setResult(Boolean.TRUE);
                    }
                });
                log("hook ok: trial");
            } else {
                log("hook skip trial: method missing");
            }
        } catch (Throwable t) {
            log("hook fail trial: " + t);
        }
    }

    // ---------- 诊断 ----------

    private void hookDiagnostics(ClassLoader cl) {
        try {
            logGetters(XposedHelpers.findClass(CLS_UPGRADE, cl), "UpgradeInfo");
        } catch (Throwable t) {
            log("diag skip UpgradeInfo: " + t);
        }
        try {
            logGetters(XposedHelpers.findClass(CLS_DEPEND, cl), "DependCheckInfo");
        } catch (Throwable t) {
            log("diag skip DependCheckInfo: " + t);
        }
        try {
            Class<?> pb = XposedHelpers.findClass(CLS_PARAM, cl);
            hookLogMethod(pb, "getVersionPackageRules", "ParamBuilder");
            hookLogMethod(pb, "getDeviceCertificate", "ParamBuilder");
            hookLogMethod(pb, "getKeyAttestation", "ParamBuilder");
        } catch (Throwable t) {
            log("diag skip ParamBuilder: " + t);
        }
        try {
            Class<?> vl = XposedHelpers.findClass(CLS_VERSIONLIST, cl);
            hookLogMethod(vl, "onCreate", new Class<?>[]{Bundle.class}, "ParaVersionList");
            hookLogMethod(vl, "onResume", "ParaVersionList");
        } catch (Throwable t) {
            log("diag skip ParaVersionList: " + t);
        }
    }

    // ---------- helpers ----------

    private static void hookLogMethod(Class<?> c, String name, final String tag) {
        hookLogMethod(c, name, new Class<?>[0], tag);
    }

    private static void hookLogMethod(Class<?> c, String name, Class<?>[] types, final String tag) {
        try {
            Method m = XposedHelpers.findMethodExactIfExists(c, name, (Object[]) types);
            if (m == null) {
                log("  log skip " + tag + "." + name + " (missing)");
                return;
            }
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object r = param.getResult();
                    if (r instanceof String && ((String) r).length() > 80) {
                        r = "<" + ((String) r).length() + " chars>";
                    }
                    log(tag + "." + param.method.getName() + " -> " + r);
                }
            });
        } catch (Throwable t) {
            log("  log fail " + tag + "." + name + ": " + t);
        }
    }

    private static void hookIntGetter(Class<?> c, String name, final int value) {
        try {
            XposedHelpers.findAndHookMethod(c, name, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    log(param.method.getName() + " -> " + value);
                    param.setResult(value);
                }
            });
        } catch (Throwable t) {
            log("  int hook fail " + name + ": " + t);
        }
    }

    private static void hookStringGetter(Class<?> c, String name, final String value) {
        try {
            Method m = XposedHelpers.findMethodExactIfExists(c, name);
            if (m == null || m.getReturnType() != String.class) {
                log("  string hook skip " + name);
                return;
            }
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    log(param.method.getName() + " -> " + value);
                    param.setResult(value);
                }
            });
        } catch (Throwable t) {
            log("  string hook fail " + name + ": " + t);
        }
    }

    private static void logGetters(Class<?> c, final String tag) {
        for (final Method m : c.getDeclaredMethods()) {
            if (m.getParameterCount() != 0 || m.isSynthetic()) continue;
            Class<?> rt = m.getReturnType();
            if (rt != String.class && rt != int.class && rt != boolean.class && rt != long.class) continue;
            try {
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        log(tag + "." + m.getName() + " = " + param.getResult());
                    }
                });
            } catch (Throwable ignored) {
            }
        }
    }
}
