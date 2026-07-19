# Accessibility Manager

一个通过 Magisk 管理 Android 无障碍服务的模块。它提供 `a11yctl` 命令，并可在开机后恢复指定的无障碍服务。

仓库同时包含一个 Android 管理应用。应用首页列出设备中已安装的无障碍服务，并通过 Root 调用模块的 `a11yctl` 命令切换及保存服务状态。

默认采用 `ensure` 模式：只确保配置中的服务已启用，不会关闭用户手动启用的其他服务。模块不会绕过应用自身的无障碍服务声明，也不能让未安装或声明错误的服务生效。

## 安装

在项目目录执行：

```powershell
.\build.ps1
```

然后在 Magisk 应用中安装 `dist/magisk-accessibility-manager-v1.0.2.zip` 并重启。不要在 Magisk 中选择 Android 应用的 APK。模块要求 Magisk 20.4 或更高版本，建议 Android 8.0 或更高版本。

### Android 管理应用

使用 Android Studio 打开仓库，或在已配置 Android SDK 和 JDK 17 的环境中执行：

```powershell
.\gradlew.bat assembleDebug
```

APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。先安装并启用 Magisk 模块，再安装应用；第一次切换服务时需要在 Root 管理器中授权。

## 使用

先在系统设置中手动启用目标无障碍服务，再捕获当前状态，是最简单可靠的初始配置方式：

```sh
su
a11yctl enabled
a11yctl capture
a11yctl configured
```

也可以用完整组件名逐项管理：

```sh
su -c 'a11yctl add com.example.app/com.example.app.MyAccessibilityService'
su -c 'a11yctl apply'
su -c 'a11yctl status com.example.app/com.example.app.MyAccessibilityService'
```

常用命令：

```text
a11yctl enabled              列出当前已启用的服务
a11yctl configured           列出模块配置的服务
a11yctl enable COMPONENT     立即启用，但不写入持久配置
a11yctl disable COMPONENT    立即停用，但不修改持久配置
a11yctl add COMPONENT        加入持久配置
a11yctl remove COMPONENT     移出持久配置，但不立即停用
a11yctl apply                立即应用配置
a11yctl capture              用当前已启用服务覆盖持久配置
```

在 Magisk 模块页面点击模块的“操作”按钮，也会执行一次 `apply` 并显示结果。

## 配置

持久配置位于：

```text
/data/adb/accessibility-manager/config.conf
/data/adb/accessibility-manager/services.list
```

`services.list` 每行填写一个完整的无障碍服务组件名。空行和以 `#` 开头的注释会被忽略。

`config.conf` 支持：

- `USER_ID=auto`：管理前台 Android 用户，也可以指定数字用户 ID。
- `BOOT_DELAY=10`：系统报告启动完成后延迟多少秒再应用配置。
- `WATCH_INTERVAL=0`：周期性恢复间隔，单位为秒；`0` 表示仅开机应用一次。
- `MODE=ensure`：保留其他服务，仅补齐配置项。
- `MODE=exact`：只保留配置项。为避免误操作，空列表默认不能用于此模式。
- `ALLOW_EMPTY_EXACT=0`：设为 `1` 后允许 `exact` 模式关闭所有无障碍服务。

修改配置后执行 `su -c 'a11yctl apply'`，无需重启。

## 注意事项

- Android 或厂商系统仍可能要求用户首次手动确认无障碍权限；本模块主要用于已安装服务的状态管理与恢复。
- 应用被卸载、停用或其服务组件名发生变化时，对应配置不会生效。
- `exact` 模式会关闭列表外的所有无障碍服务，除非确实需要白名单策略，否则保留默认的 `ensure`。
- 多用户设备默认操作当前前台用户。若要固定用户，请设置数字形式的 `USER_ID`。
- 卸载模块不会关闭已经启用的无障碍服务，但会删除 `/data/adb/accessibility-manager` 下的模块配置。
