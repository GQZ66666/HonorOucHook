package com.lead.ouctweaker;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/** 采集器：su -c 跑 logcat / dumpsys，结果可直接复制或存文件。 */
public class DiagActivity extends Activity {

    private TextView out;
    private int pad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pad = (int) (16 * getResources().getDisplayMetrics().density);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView tip = new TextView(this);
        tip.setTextSize(12);
        tip.setText("需要一个可用的 su（Magisk / KernelSU / APatch）。"
                + "\n「采集」= logcat -s OucTweaker + 版本信息；点「复制」后发我。"
                + "\n临时 root（漏洞提权、无 su 二进制）会失败，此时改用设置里的「日志落盘」。");
        root.addView(tip);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(btn("采集", v -> collect()));
        row1.addView(btn("清空", v -> simple("logcat -c", "已清空日志")));
        root.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(btn("强停更新", v -> simple("am force-stop com.hihonor.ouc", "已强停软件更新")));
        row2.addView(btn("启动更新", v -> simple("monkey -p com.hihonor.ouc -c android.intent.category.LAUNCHER 1", "已启动")));
        root.addView(row2);

        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        row3.addView(btn("复制", v -> copy()));
        row3.addView(btn("存文件", v -> save()));
        root.addView(row3);

        out = new TextView(this);
        out.setTextSize(9);
        out.setPadding(0, pad, 0, 0);
        out.setMovementMethod(new ScrollingMovementMethod());
        out.setText("(空) 点「采集」开始");

        ScrollView sv = new ScrollView(this);
        sv.addView(out, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private Button btn(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        b.setLayoutParams(lp);
        return b;
    }

    private void collect() {
        String cmd = "echo '## id'; id; "
                + "echo '## build'; getprop ro.build.display.id; getprop ro.build.version.release; "
                + "echo '## ouc'; dumpsys package com.hihonor.ouc | grep -E 'versionName|versionCode|lastUpdateTime'; "
                + "echo '## log'; logcat -d -v time -s OucTweaker | tail -n 400";
        String r = su(cmd);
        if (r == null) {
            out.setText("su 不可用：临时 root 没有 su 二进制，无法读 logcat。\n"
                    + "请在 OUC Tweaker 设置里打开「日志落盘」，然后强停并重开软件更新、点一次检查更新，"
                    + "日志会写到 Download/OucTweaker.log。");
            return;
        }
        out.setText(r);
    }

    private void simple(String cmd, String ok) {
        String r = su(cmd);
        Toast.makeText(this, r == null ? "su 不可用" : ok, Toast.LENGTH_SHORT).show();
    }

    /** 返回 null 表示 su 不可用。 */
    private String su(String cmd) {
        StringBuilder sb = new StringBuilder();
        boolean gotOutput = false;
        try {
            ProcessBuilder pb = new ProcessBuilder("su", "-c", cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
                gotOutput = true;
            }
            p.waitFor(30, TimeUnit.SECONDS);
            if (!gotOutput) return null;
            return sb.toString();
        } catch (Throwable e) {
            return null;
        }
    }

    private void copy() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("ouc-diag", out.getText().toString()));
            Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
        }
    }

    private void save() {
        try {
            File dir = getExternalFilesDir(null);
            File f = new File(dir, "ouc-diag.txt");
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(out.getText().toString().getBytes("UTF-8"));
            fos.close();
            Toast.makeText(this, "已保存 " + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Throwable e) {
            Toast.makeText(this, "保存失败: " + e, Toast.LENGTH_SHORT).show();
        }
    }
}
