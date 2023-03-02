package me.kbai.mfinstaller.tool;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author sean 2023/3/2
 */
public class StringUtils {

    @NonNull
    public static String nonNull(@Nullable String s) {
        return s == null ? "" : s;
    }

    public static String nonEmpty(@Nullable String s, @NonNull String def) {
        return TextUtils.isEmpty(s) ? def : s;
    }
}
