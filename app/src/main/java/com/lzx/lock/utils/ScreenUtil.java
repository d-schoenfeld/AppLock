package com.lzx.lock.utils;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by xian on 2017/2/17.
 */

public class ScreenUtil {
    /**
     * Bildschirmhöhe ermitteln
     *
     * @param context
     * @return
     */
    public static int getPhoneHeight(Context context) {
        DisplayMetrics dm = getDisplayMetrics(context);
        return dm.heightPixels;
    }

    /**
     * Bildschirmbreite ermitteln
     *
     * @param context
     * @return
     */
    public static int getPhoneWidth(Context context) {
        DisplayMetrics dm = getDisplayMetrics(context);
        return dm.widthPixels;
    }

    /**
     * Bildschirmauflösung ermitteln
     *
     * @param context
     * @return
     */
    public static DisplayMetrics getDisplayMetrics(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm;
    }

    /**
     * Bildschirmausrichtung ermitteln
     * @param context Kontext
     * @return Bildschirmausrichtung ORIENTATION_LANDSCAPE, ORIENTATION_PORTRAIT.
     */
    public static int getDisplayOrient (Context context) {
        return context.getResources().getConfiguration().orientation;
    }

}
