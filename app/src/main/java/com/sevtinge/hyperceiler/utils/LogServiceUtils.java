/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils;


import static com.sevtinge.hyperceiler.Application.isModuleActivated;
import static com.sevtinge.hyperceiler.common.utils.api.ProjectApi.isRelease;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.sevtinge.hyperceiler.common.log.LogStatusManager;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;

public class LogServiceUtils {

    public static void init(Context context) {
        LogStatusManager.onHealthCheckDone(() -> {
            if (shouldShowLogServiceWarn()) {
                new Handler(Looper.getMainLooper()).post(() ->
                    DialogHelper.showLogServiceWarnDialog(context)
                );
            }
        });
    }

    private static boolean shouldShowLogServiceWarn() {
        // HyperCeiler-Android13-Backport:
        // 原逻辑为 !isRelease()，即 debug/canary 才弹。但社区自建构建基本都是 debug，
        // 而 Android 13 上 logcat 管道检查常读不到自身标记（15% 属误报），
        // 每次启动都弹会干扰使用。这里改为仅 release 构建提示，
        // 该功能不影响任何 hook（IS_LOGGER_ALIVE 只用于 UI 展示与日志采集）。
        if (!isRelease()) return false;
        return !LogStatusManager.IS_LOGGER_ALIVE && isModuleActivated &&
            !PrefsBridge.getBoolean("prefs_key_development_close_log_alert_dialog", false);
    }
}
