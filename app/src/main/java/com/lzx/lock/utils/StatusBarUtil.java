package com.lzx.lock.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

/**
 * Created by Jaeger on 16/2/14.
 *
 * Email: chjie.jaeger@gmail.com
 * GitHub: https://github.com/laobie
 */
public class StatusBarUtil {

    public static final int DEFAULT_STATUS_BAR_ALPHA = 112;

    /**
     * Statusleistenfarbe setzen
     *
     * @param activity Die zu konfigurierende Activity
     * @param color    Farbwert der Statusleiste
     */
    public static void setColor(Activity activity, @ColorInt int color) {
        setColor(activity, color, DEFAULT_STATUS_BAR_ALPHA);
    }

    /**
     * Statusleistenfarbe setzen
     *
     * @param activity       Die zu konfigurierende Activity
     * @param color          Farbwert der Statusleiste
     * @param statusBarAlpha Transparenz der Statusleiste
     */

    public static void setColor(Activity activity, @ColorInt int color, int statusBarAlpha) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().setStatusBarColor(calculateStatusColor(color, statusBarAlpha));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            int count = decorView.getChildCount();
            if (count > 0 && decorView.getChildAt(count - 1) instanceof StatusBarView) {
                decorView.getChildAt(count - 1).setBackgroundColor(calculateStatusColor(color, statusBarAlpha));
            } else {
                StatusBarView statusView = createStatusBarView(activity, color, statusBarAlpha);
                decorView.addView(statusView);
            }
            setRootView(activity);
        }
    }

    /**
     * Statusleistenfarbe für Swipe-Back-Interface setzen
     *
     * @param activity Die zu konfigurierende Activity
     * @param color    Farbwert der Statusleiste
     */
    public static void setColorForSwipeBack(Activity activity, int color) {
        setColorForSwipeBack(activity, color, DEFAULT_STATUS_BAR_ALPHA);
    }

    /**
     * Statusleistenfarbe für Swipe-Back-Interface setzen
     *
     * @param activity       Die zu konfigurierende Activity
     * @param color          Farbwert der Statusleiste
     * @param statusBarAlpha Transparenz der Statusleiste
     */
    public static void setColorForSwipeBack(Activity activity, @ColorInt int color, int statusBarAlpha) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ViewGroup contentView = ((ViewGroup) activity.findViewById(android.R.id.content));
            contentView.setPadding(0, getStatusBarHeight(activity), 0, 0);
            contentView.setBackgroundColor(calculateStatusColor(color, statusBarAlpha));
            setTransparentForWindow(activity);
        }
    }

    /**
     * Statusleiste in Vollfarbe setzen, ohne Halbtransparenz-Effekt
     *
     * @param activity Die zu konfigurierende Activity
     * @param color    Farbwert der Statusleiste
     */
    public static void setColorNoTranslucent(Activity activity, @ColorInt int color) {
        setColor(activity, color, 0);
    }

    /**
     * Statusleistenfarbe setzen(unter Android 5.0 kein Halbtransparenz-Effekt, nicht empfohlen)
     *
     * @param activity Die zu konfigurierende Activity
     * @param color    Farbwert der Statusleiste
     */
    @Deprecated
    public static void setColorDiff(Activity activity, @ColorInt int color) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        transparentStatusBar(activity);
        ViewGroup contentView = (ViewGroup) activity.findViewById(android.R.id.content);
        // Halbtransparentes Rechteck entfernen, um Überlappungen zu vermeiden
        if (contentView.getChildCount() > 1) {
            contentView.getChildAt(1).setBackgroundColor(color);
        } else {
            contentView.addView(createStatusBarView(activity, color));
        }
        setRootView(activity);
    }

    /**
     * Statusleiste halbtransparent machen
     *
     * Geeignet für Oberflächen mit Bild als Hintergrund, das bis in die Statusleiste reicht
     *
     * @param activity Die zu konfigurierende Activity
     */
    public static void setTranslucent(Activity activity) {
        setTranslucent(activity, DEFAULT_STATUS_BAR_ALPHA);
    }

    /**
     * Statusleiste halbtransparent machen
     *
     * Geeignet für Oberflächen mit Bild als Hintergrund, das bis in die Statusleiste reicht
     *
     * @param activity       Die zu konfigurierende Activity
     * @param statusBarAlpha Transparenz der Statusleiste
     */
    public static void setTranslucent(Activity activity, int statusBarAlpha) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        setTransparent(activity);
        addTranslucentView(activity, statusBarAlpha);
    }

    /**
     * Für Root-Layout CoordinatorLayout: Statusleiste halbtransparent machen
     *
     * Geeignet für Oberflächen mit Bild als Hintergrund, das bis in die Statusleiste reicht
     *
     * @param activity       Die zu konfigurierende Activity
     * @param statusBarAlpha Transparenz der Statusleiste
     */
    public static void setTranslucentForCoordinatorLayout(Activity activity, int statusBarAlpha) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        transparentStatusBar(activity);
        addTranslucentView(activity, statusBarAlpha);
    }

    /**
     * Statusleiste vollständig transparent setzen
     *
     * @param activity Die zu konfigurierende Activity
     */
    public static void setTransparent(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        transparentStatusBar(activity);
        setRootView(activity);
    }

    /**
     * Statusleiste transparent machen (ab 5.0 Halbtransparenz-Effekt, nicht empfohlen)
     *
     * Geeignet für Oberflächen mit Bild als Hintergrund, das bis in die Statusleiste reicht
     *
     * @param activity Die zu konfigurierende Activity
     */
    @Deprecated
    public static void setTranslucentDiff(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Statusleiste transparent setzen
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            setRootView(activity);
        }
    }

    /**
     * Statusleistenfärbung für DrawerLayout setzen
     *
     * @param activity     Die zu konfigurierende Activity
     * @param drawerLayout DrawerLayout
     * @param color        Farbwert der Statusleiste
     */
    public static void setColorForDrawerLayout(Activity activity, DrawerLayout drawerLayout, @ColorInt int color) {
        setColorForDrawerLayout(activity, drawerLayout, color, DEFAULT_STATUS_BAR_ALPHA);
    }

    /**
     * Statusleiste für DrawerLayout in Vollfarbe setzen
     *
     * @param activity     Die zu konfigurierende Activity
     * @param drawerLayout DrawerLayout
     * @param color        Farbwert der Statusleiste
     */
    public static void setColorNoTranslucentForDrawerLayout(Activity activity, DrawerLayout drawerLayout, @ColorInt int color) {
        setColorForDrawerLayout(activity, drawerLayout, color, 0);
    }

    /**
     * Statusleistenfärbung für DrawerLayout setzen
     *
     * @param activity       Die zu konfigurierende Activity
     * @param drawerLayout   DrawerLayout
     * @param color          Farbwert der Statusleiste
     * @param statusBarAlpha Transparenz der Statusleiste
     */
    public static void setColorForDrawerLayout(Activity activity, DrawerLayout drawerLayout, @ColorInt int color,
                                               int statusBarAlpha) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        } else {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        // Rechteck in Größe der Statusleiste erzeugen
        // statusBarView zum Layout hinzufügen
        ViewGroup contentLayout = (ViewGroup) drawerLayout.getChildAt(0);
        if (contentLayout.getChildCount() > 0 && contentLayout.getChildAt(0) instanceof StatusBarView) {
            contentLayout.getChildAt(0).setBackgroundColor(color);
        } else {
            StatusBarView statusBarView = createStatusBarView(activity, color);
            contentLayout.addView(statusBarView, 0);
        }
        // Falls Content-Layout kein LinearLayout ist, paddingTop setzen
        if (!(contentLayout instanceof LinearLayout) && contentLayout.getChildAt(1) != null) {
            contentLayout.getChildAt(1)
                .setPadding(contentLayout.getPaddingLeft(), getStatusBarHeight(activity) + contentLayout.getPaddingTop(),
                    contentLayout.getPaddingRight(), contentLayout.getPaddingBottom());
        }
        // Eigenschaften setzen
        setDrawerLayoutProperty(drawerLayout, contentLayout);
        addTranslucentView(activity, statusBarAlpha);
    }

    /**
     * DrawerLayout-Eigenschaften setzen
     *
     * @param drawerLayout              DrawerLayout
     * @param drawerLayoutContentLayout Inhaltslayout des DrawerLayouts
     */
    private static void setDrawerLayoutProperty(DrawerLayout drawerLayout, ViewGroup drawerLayoutContentLayout) {
        ViewGroup drawer = (ViewGroup) drawerLayout.getChildAt(1);
        drawerLayout.setFitsSystemWindows(false);
        drawerLayoutContentLayout.setFitsSystemWindows(false);
        drawerLayoutContentLayout.setClipToPadding(true);
        drawer.setFitsSystemWindows(false);
    }

    /**
     * Statusleistenfärbung für DrawerLayout setzen(unter Android 5.0 kein Halbtransparenz-Effekt, nicht empfohlen)
     *
     * @param activity     Die zu konfigurierende Activity
     * @param drawerLayout DrawerLayout
     * @param color        Farbwert der Statusleiste
     */
    @Deprecated
    public static void setColorForDrawerLayoutDiff(Activity activity, DrawerLayout drawerLayout, @ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // Rechteck in Größe der Statusleiste erzeugen
            ViewGroup contentLayout = (ViewGroup) drawerLayout.getChildAt(0);
            if (contentLayout.getChildCount() > 0 && contentLayout.getChildAt(0) instanceof StatusBarView) {
                contentLayout.getChildAt(0).setBackgroundColor(calculateStatusColor(color, DEFAULT_STATUS_BAR_ALPHA));
            } else {
                // statusBarView zum Layout hinzufügen
                StatusBarView statusBarView = createStatusBarView(activity, color);
                contentLayout.addView(statusBarView, 0);
            }
            // Falls Content-Layout kein LinearLayout ist, paddingTop setzen
            if (!(contentLayout instanceof LinearLayout) && contentLayout.getChildAt(1) != null) {
                contentLayout.getChildAt(1).setPadding(0, getStatusBarHeight(activity), 0, 0);
            }
            // Eigenschaften setzen
            setDrawerLayoutProperty(drawerLayout, contentLayout);
        }
    }

    /**
     * Statusleiste für DrawerLayout transparent setzen
     *
     * @param activity     Die zu konfigurierende Activity
     * @param drawerLayout DrawerLayout
     */
    public static void setTranslucentForDrawerLayout(Activity activity, DrawerLayout drawerLayout) {
        setTranslucentForDrawerLayout(activity, drawerLayout, DEFAULT_STATUS_BAR_ALPHA);
    }

    /**
     * Statusleiste für DrawerLayout transparent setzen
     *
     * @param activity     Die zu konfigurierende Activity
     * @param drawerLayout DrawerLayout
     */
    public static void setTranslucentForDrawerLayout(Activity activity, DrawerLayout drawerLayout, int statusBarAlpha) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        setTransparentForDrawerLayout(activity, drawerLayout);
        addTranslucentView(activity, statusBarAlpha);
    }

    /**
     * Statusleiste für DrawerLayout transparent setzen
     *
     * @param activity     Die zu konfigurierende Activity
     * @param drawerLayout DrawerLayout
     */
    public static void setTransparentForDrawerLayout(Activity activity, DrawerLayout drawerLayout) {
        Log.e("TAG","SDK_INT="+ Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        } else {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        ViewGroup contentLayout = (ViewGroup) drawerLayout.getChildAt(0);
        // Falls Content-Layout kein LinearLayout ist, paddingTop setzen
        if (!(contentLayout instanceof LinearLayout) && contentLayout.getChildAt(1) != null) {
            contentLayout.getChildAt(1).setPadding(0, getStatusBarHeight(activity), 0, 0);
        }

        // Eigenschaften setzen
        setDrawerLayoutProperty(drawerLayout, contentLayout);
    }

    /**
     * Statusleiste für DrawerLayout transparent setzen(ab Android 5.0 Halbtransparenz-Effekt, nicht empfohlen)
     *
     * @param activity     Die zu konfigurierende Activity
     * @param drawerLayout DrawerLayout
     */
    @Deprecated
    public static void setTranslucentForDrawerLayoutDiff(Activity activity, DrawerLayout drawerLayout) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Statusleiste transparent setzen
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // Eigenschaften des Inhaltslayouts setzen
            ViewGroup contentLayout = (ViewGroup) drawerLayout.getChildAt(0);
            contentLayout.setFitsSystemWindows(true);
            contentLayout.setClipToPadding(true);
            // Eigenschaften des Drawer-Layouts setzen
            ViewGroup vg = (ViewGroup) drawerLayout.getChildAt(1);
            vg.setFitsSystemWindows(false);
            // DrawerLayout-Eigenschaften setzen
            drawerLayout.setFitsSystemWindows(false);
        }
    }

    /**
     * Statusleiste für Oberflächen mit ImageView-Kopfbereich vollständig transparent setzen
     *
     * @param activity       Die zu konfigurierende Activity
     * @param needOffsetView View, die nach unten versetzt werden soll
     */
    public static void setTransparentForImageView(Activity activity, View needOffsetView) {
        setTranslucentForImageView(activity, 0, needOffsetView);
    }

    /**
     * Statusleiste für Oberflächen mit ImageView-Kopfbereich transparent setzen (Standard-Transparenz)
     *
     * @param activity       Die zu konfigurierende Activity
     * @param needOffsetView View, die nach unten versetzt werden soll
     */
    public static void setTranslucentForImageView(Activity activity, View needOffsetView) {
        setTranslucentForImageView(activity, DEFAULT_STATUS_BAR_ALPHA, needOffsetView);
    }

    /**
     * Statusleiste für Oberflächen mit ImageView-Kopfbereich transparent setzen
     *
     * @param activity       Die zu konfigurierende Activity
     * @param statusBarAlpha Transparenz der Statusleiste
     * @param needOffsetView View, die nach unten versetzt werden soll
     */
    public static void setTranslucentForImageView(Activity activity, int statusBarAlpha, View needOffsetView) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        setTransparentForWindow(activity);
        addTranslucentView(activity, statusBarAlpha);
        if (needOffsetView != null) {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) needOffsetView.getLayoutParams();
            layoutParams.setMargins(layoutParams.leftMargin, layoutParams.topMargin + getStatusBarHeight(activity),
                layoutParams.rightMargin, layoutParams.bottomMargin);
        }
    }

    /**
     * Statusleiste für Fragment mit ImageView-Kopfbereich transparent setzen
     *
     * @param activity       Die dem Fragment zugehörige Activity
     * @param needOffsetView View, die nach unten versetzt werden soll
     */
    public static void setTranslucentForImageViewInFragment(Activity activity, View needOffsetView) {
        setTranslucentForImageViewInFragment(activity, DEFAULT_STATUS_BAR_ALPHA, needOffsetView);
    }

    /**
     * Statusleiste für Fragment mit ImageView-Kopfbereich transparent setzen
     *
     * @param activity       Die dem Fragment zugehörige Activity
     * @param needOffsetView View, die nach unten versetzt werden soll
     */
    public static void setTransparentForImageViewInFragment(Activity activity, View needOffsetView) {
        setTranslucentForImageViewInFragment(activity, 0, needOffsetView);
    }

    /**
     * Statusleiste für Fragment mit ImageView-Kopfbereich transparent setzen
     *
     * @param activity       Die dem Fragment zugehörige Activity
     * @param statusBarAlpha Transparenz der Statusleiste
     * @param needOffsetView View, die nach unten versetzt werden soll
     */
    public static void setTranslucentForImageViewInFragment(Activity activity, int statusBarAlpha, View needOffsetView) {
        setTranslucentForImageView(activity, statusBarAlpha, needOffsetView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            clearPreviousSetting(activity);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void clearPreviousSetting(Activity activity) {
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        int count = decorView.getChildCount();
        if (count > 0 && decorView.getChildAt(count - 1) instanceof StatusBarView) {
            decorView.removeViewAt(count - 1);
            ViewGroup rootView = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
            rootView.setPadding(0, 0, 0, 0);
        }
    }

    /**
     * Halbtransparenten Balken hinzufügen
     *
     * @param activity       Die zu konfigurierende Activity
     * @param statusBarAlpha Transparenzwert
     */
    private static void addTranslucentView(Activity activity, int statusBarAlpha) {
        ViewGroup contentView = (ViewGroup) activity.findViewById(android.R.id.content);
        if (contentView.getChildCount() > 1) {
            contentView.getChildAt(1).setBackgroundColor(Color.argb(statusBarAlpha, 0, 0, 0));
        } else {
            contentView.addView(createTranslucentStatusBarView(activity, statusBarAlpha));
        }
    }

    /**
     * Farbigen Balken in Größe der Statusleiste erzeugen
     *
     * @param activity Die zu konfigurierende Activity
     * @param color    Farbwert der Statusleiste
     * @return Statusleisten-Balken
     */
    private static StatusBarView createStatusBarView(Activity activity, @ColorInt int color) {
        // Rechteck in Höhe der Statusleiste zeichnen
        StatusBarView statusBarView = new StatusBarView(activity);
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight(activity));
        statusBarView.setLayoutParams(params);
        statusBarView.setBackgroundColor(color);
        return statusBarView;
    }

    /**
     * Halbtransparenten Balken in Größe der Statusleiste erzeugen
     *
     * @param activity Die zu konfigurierende Activity
     * @param color    Farbwert der Statusleiste
     * @param alpha    Transparenzwert
     * @return Statusleisten-Balken
     */
    private static StatusBarView createStatusBarView(Activity activity, @ColorInt int color, int alpha) {
        // Rechteck in Höhe der Statusleiste zeichnen
        StatusBarView statusBarView = new StatusBarView(activity);
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight(activity));
        statusBarView.setLayoutParams(params);
        statusBarView.setBackgroundColor(calculateStatusColor(color, alpha));
        return statusBarView;
    }

    /**
     * Root-Layout-Parameter setzen
     */
    private static void setRootView(Activity activity) {
        ViewGroup parent = (ViewGroup) activity.findViewById(android.R.id.content);
        for (int i = 0, count = parent.getChildCount(); i < count; i++) {
            View childView = parent.getChildAt(i);
            if (childView instanceof ViewGroup) {
                childView.setFitsSystemWindows(true);
                ((ViewGroup) childView).setClipToPadding(true);
            }
        }
    }

    /**
     * Transparenz setzen
     */
    private static void setTransparentForWindow(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
            activity.getWindow()
                .getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.getWindow()
                .setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    /**
     * Statusleiste transparent machen
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void transparentStatusBar(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        } else {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    /**
     * Halbtransparente rechteckige View erstellen
     *
     * @param alpha Transparenzwert
     * @return Halbtransparente View
     */
    private static StatusBarView createTranslucentStatusBarView(Activity activity, int alpha) {
        // Rechteck in Höhe der Statusleiste zeichnen
        StatusBarView statusBarView = new StatusBarView(activity);
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight(activity));
        statusBarView.setLayoutParams(params);
        statusBarView.setBackgroundColor(Color.argb(alpha, 0, 0, 0));
        return statusBarView;
    }

    /**
     * Höhe der Statusleiste ermitteln
     *
     * @param context context
     * @return Höhe der Statusleiste
     */
    private static int getStatusBarHeight(Context context) {
        // Höhe der Statusleiste abrufen
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return context.getResources().getDimensionPixelSize(resourceId);
    }

    /**
     * Statusleistenfarbe berechnen
     *
     * @param color Farbwert
     * @param alpha Alphawert
     * @return Resultierende Statusleistenfarbe
     */
    private static int calculateStatusColor(@ColorInt int color, int alpha) {
        float a = 1 - alpha / 255f;
        int red = color >> 16 & 0xff;
        int green = color >> 8 & 0xff;
        int blue = color & 0xff;
        red = (int) (red * a + 0.5);
        green = (int) (green * a + 0.5);
        blue = (int) (blue * a + 0.5);
        return 0xff << 24 | red << 16 | green << 8 | blue;
    }
}
