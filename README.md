# OUC Tweaker

LSPosed 模块，作用域只有一个：荣耀「软件更新」`com.hihonor.ouc`（分析基于 18.0.3.230）。

## 功能

| 分组 | 开关 | 实现 |
|---|---|---|
| 拦截 | 阻止定时检查更新 | `NetworkCheckJobService.onStartJob` → false |
| 拦截 | 阻止自动安装 / 重启 | `PowerManager.reboot` / `RecoverySystem.installPackage` |
| 拦截 | 阻止自动检查服务 | `AutoCheckService.onStartCommand` |
| 拦截 | 拦截升级推送 | `HnOucMessageService.a(DataMessage)` |
| 拦截 | 屏蔽应用推荐检查 | `newBiz.service.CotaCheckJobService.onStartJob` → false |
| 拦截 | 屏蔽使用情况上报 | `romsurvey.check.AutoCheckService.onStartCommand` |
| 调参 | 降低升级提醒优先级 | `UpgradeInfo.getInstallRemindPriority/getUpgradePackagePriority` → 0 |
| 调参 | 尝鲜通道 | `DependCheckInfo.isTrialVersionsUpdate()` → true |
| 调参 | 伪装上报版本 | `DependCheckInfo.getProductBaseVersion()` → 自定义 |
| 诊断 | 诊断日志 | getter 全量打点 + 参数构造 + 版本列表页 |

## 它不能做什么

**不能让 recovery 接受降级包。** 版本白名单、签名、versionCode、vrl 都在 recovery 的 `update-binary` 里，读 oeminfo / vrl 分区，与本进程无关。hook 重启即消失。

## 在 GitHub 上编译

1. 新建仓库，把本目录全部内容推上去（`.github/workflows/build.yml` 必须一起提交）；
2. Actions 里会自动跑 `build-apk`，也可以手动 `Run workflow`；
3. 跑完在 **Artifacts → ouc-tweaker-apk** 下载 `app-debug.apk`。

工作流要点：JDK 17 + `android-actions/setup-android` + `gradle/actions/setup-gradle`（Gradle 8.7）+ 预下载 Xposed API jar。预下载成功就用本地 jar，失败自动回退到 `https://api.xposed.info/` 仓库。

本地编译（需 JDK 17 + SDK 34）：

```bash
gradle wrapper --gradle-version 8.7
./gradlew :app:assembleDebug
```

## 安装

1. 装 APK；
2. LSPosed 启用模块，作用域勾选「软件更新」；
3. 打开 OUC Tweaker 配开关；
4. 系统设置 → 应用 → 软件更新 → 强制停止，再打开「软件更新」，配置生效。

## 抓日志

```bash
adb logcat -c && adb logcat | grep OucTweaker
```

点一次「检查更新」，重点看：

- `UpgradeInfo.getInstallStrategy / getRebootType / getInstallRemindPriority` 的真实值；
- `DependCheckInfo.isTrialVersionsUpdate` 原本返回什么；
- `ParamBuilder.getVersionPackageRules` 里的规则内容；
- `ParaVersionListActivity` 打开时打了什么。

## 已知限制

- `AutoCheckService`、`NightUpgradeSwitchActivity` 内部方法已被混淆（`a/b/c`），只钩生命周期与数据模型；
- `isEnableRollback` 只是 Gson 字段（`@SerializedName`），没有对应 accessor，钩不到；
- 平台签名 app：hook 崩溃会导致「软件更新」反复重启，出问题先在 LSPosed 里关模块。
