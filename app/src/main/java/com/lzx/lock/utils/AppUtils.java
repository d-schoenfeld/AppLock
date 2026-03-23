package com.lzx.lock.utils;

import android.view.Window;
import android.view.WindowManager;

/**
 * Created by xian on 2017/2/17.
 */

public class AppUtils {

    public static void hideStatusBar(Window window, boolean enable) {
        WindowManager.LayoutParams p = window.getAttributes();
        if (enable)
            //|=: ODER-Zuweisung, entweder oder
            p.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        else
            //&=: UND-Zuweisung, beide müssen erfüllt sein, ~ : bitweise Negation
            p.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);

        window.setAttributes(p);
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

}
