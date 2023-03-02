package me.kbai.mfinstaller.tool;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.Nullable;

/**
 * @author Sean
 */
public class AppUtils {

    @Nullable
    public static PackageInfo getPackageInfo(Context context, String apkPath) {
        if (context == null || TextUtils.isEmpty(apkPath)) return null;
        PackageManager pm = context.getPackageManager();
        return pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
    }

    @Nullable
    public static ApplicationInfo getAppInfo(PackageInfo packageInfo, String apkPath) {
        if (packageInfo != null) {
            ApplicationInfo appInfo = packageInfo.applicationInfo;
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            return appInfo;
        }
        return null;
    }

    @Nullable
    public static Drawable getIcon(Context context, ApplicationInfo appInfo) {
        return appInfo == null ? null : appInfo.loadIcon(context.getPackageManager());
    }

    public static void copyToClipboard(Context context, CharSequence label, CharSequence content) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, content);
        clipboard.setPrimaryClip(clip);
    }
}
