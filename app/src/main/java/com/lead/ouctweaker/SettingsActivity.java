package com.lead.ouctweaker;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    private SharedPreferences sp;
    private int pad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences(Prefs.FILE, MODE_PRIVATE);
        pad = (int) (16 * getResources().getDisplayMetrics().density);

        ScrollView sv = new ScrollView(this);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(pad, pad, pad, pad);

        ll.addView(title("OUC Tweaker"));
        ll.addView(note("作用域：荣耀「软件更新」com.hihonor.ouc。改完设置后到系统设置里强制停止「软件更新」，再打开才生效。"));

        ll.addView(section("拦截"));
        addSwitch(ll, "阻止定时检查更新", "block_scheduled", true,
                "NetworkCheckJobService.onStartJob -> false");
        addSwitch(ll, "阻止自动安装 / 重启", "block_install", true,
                "拦 PowerManager.reboot 与 RecoverySystem.installPackage。开着时「立即重启安装」也会被拦。");
        addSwitch(ll, "阻止自动检查服务", "block_autocheck", false,
                "AutoCheckService.onStartCommand，比较粗暴，手动检查可能一起失效。");
        addSwitch(ll, "拦截升级推送", "block_push", false,
                "HnOucMessageService 的推送处理，会一并丢其它 OUC 推送。");
        addSwitch(ll, "屏蔽应用推荐检查", "block_cota", false,
                "CotaCheckJobService.onStartJob，减少预装/推荐应用检查。");
        addSwitch(ll, "屏蔽使用情况上报", "block_romsurvey", false,
                "romsurvey.AutoCheckService，减少 rom 调查上报。");

        ll.addView(section("调参"));
        addSwitch(ll, "降低升级提醒优先级", "lower_priority", true,
                "getInstallRemindPriority / getUpgradePackagePriority -> 0");
        addSwitch(ll, "尝鲜通道", "trial_mode", false,
                "isTrialVersionsUpdate() -> true，让检查走尝鲜版本逻辑。");
        addSwitch(ll, "伪装上报版本", "spoof_version", false,
                "只改 getProductBaseVersion，其余版本字段保持真实。");

        TextView lv = new TextView(this);
        lv.setText("上报版本值（例如 10.0.0.175）");
        lv.setPadding(0, pad, 0, 0);
        ll.addView(lv);

        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setHint("留空则不启用");
        et.setText(sp.getString("version_value", ""));
        et.setSingleLine(true);
        ll.addView(et);

        Button save = new Button(this);
        save.setText("保存版本值");
        save.setOnClickListener(v -> sp.edit().putString("version_value", et.getText().toString().trim()).apply());
        ll.addView(save);

        ll.addView(section("诊断"));
        addSwitch(ll, "诊断日志", "debug", true,
                "打印 UpgradeInfo / DependCheckInfo 全部 getter、参数构造与版本列表页。");

        addSwitch(ll, "日志落盘", "dump_file", false,
                "没有 su 时用：把日志追加写到 Download/OucTweaker.log，用文件管理器拷出来。");

        Button diag = new Button(this);
        diag.setText("打开诊断采集（需 su）");
        diag.setOnClickListener(v -> startActivity(new android.content.Intent(
                this, DiagActivity.class)));
        ll.addView(diag);

        ll.addView(note("adb 方式：adb logcat -c && adb logcat | grep OucTweaker"));

        sv.addView(ll);
        setContentView(sv);
    }

    private TextView title(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(22);
        t.setPadding(0, 0, 0, pad);
        return t;
    }

    private TextView section(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(15);
        t.setPadding(0, pad, 0, pad / 2);
        return t;
    }

    private TextView note(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(12);
        t.setPadding(0, pad / 2, 0, pad);
        return t;
    }

    private void addSwitch(LinearLayout parent, String label, final String key, boolean def, String desc) {
        Switch sw = new Switch(this);
        sw.setText(label);
        sw.setChecked(sp.getBoolean(key, def));
        sw.setOnCheckedChangeListener((b, checked) -> sp.edit().putBoolean(key, checked).apply());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = pad / 2;
        parent.addView(sw, lp);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextSize(11);
        d.setPadding(pad, 0, 0, pad / 2);
        parent.addView(d);
    }
}
