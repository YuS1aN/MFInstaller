package me.kbai.mfinstaller.tool;

import android.content.Context;
import android.util.TypedValue;

public class SizeUtils {

    public static float dp2px(Context context, float dpValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue,
                context.getResources().getDisplayMetrics());
    }

    public static float sp2px(Context context, float spValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue,
                context.getResources().getDisplayMetrics());
    }

    public static float px2dp(Context context, float pxValue) {
        float density = context.getResources().getDisplayMetrics().density;
        return px2dp(density, pxValue);
    }

    public static float px2dp(float density, float pxValue) {
        return (float) (pxValue / density + 0.5);
    }
}
