package com.lzx.lock.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.lzx.lock.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * Hilfklasse für die Statusleiste
 * Zwei Modi der Statusleiste (ab Android 4.4)
 * 1. Immersiver Vollbildmodus
 * 2. Einfärbungsmodus der Statusleiste
 */
public class SystemBarHelper {

  private static float DEFAULT_ALPHA = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                                       ? 0.2f
                                       : 0.3f;


  /**
   * Statusleisteneinfärbung ab Android 4.4
   *
   * @param activity Activity-Objekt
   * @param statusBarColor Statusleistenfarbe
   */
  public static void tintStatusBar(Activity activity, @ColorInt int statusBarColor) {

    tintStatusBar(activity, statusBarColor, DEFAULT_ALPHA);
  }


  /**
   * Statusleisteneinfärbung ab Android 4.4
   *
   * @param activity Activity-Objekt
   * @param statusBarColor Statusleistenfarbe
   * @param alpha Transparenz der Statusleiste [0.0-1.0]
   */
  public static void tintStatusBar(Activity activity,
                                   @ColorInt int statusBarColor,
                                   @FloatRange(from = 0.0, to = 1.0) float alpha) {

    tintStatusBar(activity.getWindow(), statusBarColor, alpha);
  }


  /**
   * Statusleisteneinfärbung ab Android 4.4
   *
   * @param window Normalerweise das Activity-Fenster, kann aber auch Dialog oder DialogFragment sein
   * @param statusBarColor Statusleistenfarbe
   */
  public static void tintStatusBar(Window window, @ColorInt int statusBarColor) {

    tintStatusBar(window, statusBarColor, DEFAULT_ALPHA);
  }


  /**
   * Statusleisteneinfärbung ab Android 4.4
   *
   * @param window Normalerweise das Activity-Fenster, kann aber auch Dialog oder DialogFragment sein
   * @param statusBarColor Statusleistenfarbe
   * @param alpha Transparenz der Statusleiste [0.0-1.0]
   */
  public static void tintStatusBar(Window window,
                                   @ColorInt int statusBarColor,
                                   @FloatRange(from = 0.0, to = 1.0) float alpha) {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
      return;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      window.setStatusBarColor(Color.TRANSPARENT);
    } else {
      window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }

    ViewGroup decorView = (ViewGroup) window.getDecorView();
    ViewGroup contentView = (ViewGroup) window.getDecorView()
        .findViewById(Window.ID_ANDROID_CONTENT);
    View rootView = contentView.getChildAt(0);
    if (rootView != null) {
      ViewCompat.setFitsSystemWindows(rootView, true);
    }

    setStatusBar(decorView, statusBarColor, true);
    setTranslucentView(decorView, alpha);
  }


  /**
   * Statusleisteneinfärbung ab Android 4.4(für DrawerLayout)
   * Hinweis:
   * 1. Bei falscher Darstellung alle fitsSystemWindows-Attribute im Layout entfernen, insbesondere beim DrawerLayout
   * 2. Ab Android 5.0 kann stattdessen die systemeigene Methode verwendet werden
   *
   * @param activity Activity-Objekt
   * @param drawerLayout DrawerLayout-Objekt
   * @param statusBarColor Statusleistenfarbe
   */
  public static void tintStatusBarForDrawer(Activity activity, DrawerLayout drawerLayout,
                                            @ColorInt int statusBarColor) {

    tintStatusBarForDrawer(activity, drawerLayout, statusBarColor, DEFAULT_ALPHA);
  }


  /**
   * Statusleisteneinfärbung ab Android 4.4(für DrawerLayout)
   * Hinweis:
   * 1. Bei falscher Darstellung alle fitsSystemWindows-Attribute im Layout entfernen, insbesondere beim DrawerLayout
   * 2. Ab Android 5.0 kann stattdessen die systemeigene Methode verwendet werden
   *
   * @param activity Activity-Objekt
   * @param drawerLayout DrawerLayout-Objekt
   * @param statusBarColor Statusleistenfarbe
   * @param alpha Transparenz der Statusleiste [0.0-1.0]
   */
  public static void tintStatusBarForDrawer(Activity activity, DrawerLayout drawerLayout,
                                            @ColorInt int statusBarColor,
                                            @FloatRange(from = 0.0, to = 1.0) float alpha) {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
      return;
    }

    Window window = activity.getWindow();
    ViewGroup decorView = (ViewGroup) window.getDecorView();
    ViewGroup drawContent = (ViewGroup) drawerLayout.getChildAt(0);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      window.setStatusBarColor(Color.TRANSPARENT);
      drawerLayout.setStatusBarBackgroundColor(statusBarColor);

      int systemUiVisibility = window.getDecorView().getSystemUiVisibility();
      systemUiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
      systemUiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
      window.getDecorView().setSystemUiVisibility(systemUiVisibility);
    } else {
      window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }

    setStatusBar(decorView, statusBarColor, true, true);
    setTranslucentView(decorView, alpha);

    drawerLayout.setFitsSystemWindows(false);
    drawContent.setFitsSystemWindows(true);
    ViewGroup drawer = (ViewGroup) drawerLayout.getChildAt(1);
    drawer.setFitsSystemWindows(false);
  }


  /**
   * Immersiver Vollbildmodus ab Android 4.4
   * Hinweis:
   * 1. fitsSystemWindows-Attribut entfernen: Falls die Darstellung ab Android 5.0 fehlerhaft ist, alle fitsSystemWindows-Attribute im Layout entfernen
   * oder die Methode forceFitsSystemWindows aufrufen
   * 2. fitsSystemWindows-Attribut behalten: Ab Android 5.0 eigene Implementierung verwenden, diese Methode nicht aufrufen
   *
   * @param activity Activity-Objekt
   */
  public static void immersiveStatusBar(Activity activity) {

    immersiveStatusBar(activity, DEFAULT_ALPHA);
  }


  /**
   * Immersiver Vollbildmodus ab Android 4.4
   * Hinweis:
   * 1. fitsSystemWindows-Attribut entfernen: Falls die Darstellung ab Android 5.0 fehlerhaft ist, alle fitsSystemWindows-Attribute im Layout entfernen
   * oder die Methode forceFitsSystemWindows aufrufen
   * 2. fitsSystemWindows-Attribut behalten: Ab Android 5.0 eigene Implementierung verwenden, diese Methode nicht aufrufen
   *
   * @param activity Activity-Objekt
   * @param alpha Transparenz der Statusleiste [0.0-1.0]
   */
  public static void immersiveStatusBar(Activity activity,
                                        @FloatRange(from = 0.0, to = 1.0) float alpha) {

    immersiveStatusBar(activity.getWindow(), alpha);
  }


  /**
   * Immersiver Vollbildmodus ab Android 4.4
   * Hinweis:
   * 1. fitsSystemWindows-Attribut entfernen: Falls die Darstellung ab Android 5.0 fehlerhaft ist, alle fitsSystemWindows-Attribute im Layout entfernen
   * oder die Methode forceFitsSystemWindows aufrufen
   * 2. fitsSystemWindows-Attribut behalten: Ab Android 5.0 eigene Implementierung verwenden, diese Methode nicht aufrufen
   *
   * @param window Normalerweise das Activity-Fenster, kann aber auch Dialog oder DialogFragment sein
   */
  public static void immersiveStatusBar(Window window) {

    immersiveStatusBar(window, DEFAULT_ALPHA);
  }


  /**
   * Immersiver Vollbildmodus ab Android 4.4
   * Hinweis:
   * 1. fitsSystemWindows-Attribut entfernen: Falls die Darstellung ab Android 5.0 fehlerhaft ist, alle fitsSystemWindows-Attribute im Layout entfernen
   * oder die Methode forceFitsSystemWindows aufrufen
   * 2. fitsSystemWindows-Attribut behalten: Ab Android 5.0 eigene Implementierung verwenden, diese Methode nicht aufrufen
   *
   * @param window Normalerweise das Activity-Fenster, kann aber auch Dialog oder DialogFragment sein
   * @param alpha Transparenz der Statusleiste [0.0-1.0]
   */
  public static void immersiveStatusBar(Window window,
                                        @FloatRange(from = 0.0, to = 1.0) float alpha) {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
      return;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      window.setStatusBarColor(Color.TRANSPARENT);

      int systemUiVisibility = window.getDecorView().getSystemUiVisibility();
      systemUiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
      systemUiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
      window.getDecorView().setSystemUiVisibility(systemUiVisibility);
    } else {
      window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }

    ViewGroup decorView = (ViewGroup) window.getDecorView();
    ViewGroup contentView = (ViewGroup) window.getDecorView()
        .findViewById(Window.ID_ANDROID_CONTENT);
    View rootView = contentView.getChildAt(0);
    int statusBarHeight = getStatusBarHeight(window.getContext());
    if (rootView != null) {
      FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) rootView.getLayoutParams();
      ViewCompat.setFitsSystemWindows(rootView, true);
      lp.topMargin = -statusBarHeight;
      rootView.setLayoutParams(lp);
    }

    setTranslucentView(decorView, alpha);
  }


  /**
   * StatusBar-DarkMode setzen, Schriftfarbe und Icons schwärzen (unterstützt MIUI6+, Flyme4+, Android M+)
   */
  public static void setStatusBarDarkMode(Activity activity) {

    setStatusBarDarkMode(activity.getWindow());
  }


  /**
   * StatusBar-DarkMode setzen, Schriftfarbe und Icons schwärzen (unterstützt MIUI6+, Flyme4+, Android M+)
   */
  public static void setStatusBarDarkMode(Window window) {

    if (isFlyme4Later()) {
      setStatusBarDarkModeForFlyme4(window, true);
    } else if (isMIUI6Later()) {
      setStatusBarDarkModeForMIUI6(window, true);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      setStatusBarDarkModeForM(window);
    }
  }

  //------------------------->


  /**
   * Android 6.0: Schriftfarbe setzen
   */
  @TargetApi(Build.VERSION_CODES.M)
  public static void setStatusBarDarkModeForM(Window window) {

    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    window.setStatusBarColor(Color.TRANSPARENT);

    int systemUiVisibility = window.getDecorView().getSystemUiVisibility();
    systemUiVisibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
    window.getDecorView().setSystemUiVisibility(systemUiVisibility);
  }


  /**
   * DarkMode für Flyme4+ setzen, im DarkMode werden Schriftfarbe und Icons schwarz
   * http://open-wiki.flyme.cn/index.php?title=Flyme%E7%B3%BB%E7%BB%9FAPI
   */
  public static boolean setStatusBarDarkModeForFlyme4(Window window, boolean dark) {

    boolean result = false;
    if (window != null) {
      try {
        WindowManager.LayoutParams e = window.getAttributes();
        Field darkFlag = WindowManager.LayoutParams.class.getDeclaredField(
            "MEIZU_FLAG_DARK_STATUS_BAR_ICON");
        Field meizuFlags = WindowManager.LayoutParams.class.getDeclaredField("meizuFlags");
        darkFlag.setAccessible(true);
        meizuFlags.setAccessible(true);
        int bit = darkFlag.getInt(null);
        int value = meizuFlags.getInt(e);
        if (dark) {
          value |= bit;
        } else {
          value &= ~bit;
        }

        meizuFlags.setInt(e, value);
        window.setAttributes(e);
        result = true;
      } catch (Exception var8) {
        Log.e("StatusBar", "setStatusBarDarkIcon: failed");
      }
    }

    return result;
  }


  /**
   * DarkMode der Statusleiste für MIUI6+ setzen, im DarkMode werden Schriftfarbe und Icons schwarz
   * http://dev.xiaomi.com/doc/p=4769/
   */
  public static void setStatusBarDarkModeForMIUI6(Window window, boolean darkmode) {

    Class<? extends Window> clazz = window.getClass();
    try {
      int darkModeFlag = 0;
      Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
      Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
      darkModeFlag = field.getInt(layoutParams);
      Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
      extraFlagField.invoke(window, darkmode ? darkModeFlag : 0, darkModeFlag);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  /**
   * Gefälschte Statusleisten-View erstellen
   */
  private static void setStatusBar(ViewGroup container, @ColorInt
      int statusBarColor, boolean visible, boolean addToFirst) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      View statusBarView = container.findViewById(R.id.statusbar_view);
      if (statusBarView == null) {
        statusBarView = new View(container.getContext());
        statusBarView.setId(R.id.statusbar_view);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight(container.getContext()));
        if (addToFirst) {
          container.addView(statusBarView, 0, lp);
        } else {
          container.addView(statusBarView, lp);
        }
      }

      statusBarView.setBackgroundColor(statusBarColor);
      statusBarView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
  }


  /**
   * Gefälschte Statusleisten-View erstellen
   */
  private static void setStatusBar(ViewGroup container,
                                   @ColorInt int statusBarColor, boolean visible) {

    setStatusBar(container, statusBarColor, visible, false);
  }


  /**
   * Gefälschte transparente Leiste erstellen
   */
  private static void setTranslucentView(ViewGroup container,
                                         @FloatRange(from = 0.0, to = 1.0) float alpha) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      View translucentView = container.findViewById(R.id.translucent_view);
      if (translucentView == null) {
        translucentView = new View(container.getContext());
        translucentView.setId(R.id.translucent_view);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, getStatusBarHeight(container.getContext()));
        container.addView(translucentView, lp);
      }

      translucentView.setBackgroundColor(Color.argb((int) (alpha * 255), 0, 0, 0));
    }
  }


  /**
   * Höhe der Statusleiste ermitteln
   */
  public static int getStatusBarHeight(Context context) {

    int result = 0;
    int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
    if (resId > 0) {
      result = context.getResources().getDimensionPixelSize(resId);
    }
    return result;
  }


  /**
   * Prüfen ob Flyme4 oder höher
   */
  public static boolean isFlyme4Later() {

    return Build.FINGERPRINT.contains("Flyme_OS_4")
        || Build.VERSION.INCREMENTAL.contains("Flyme_OS_4")
        ||
        Pattern.compile("Flyme OS [4|5]", Pattern.CASE_INSENSITIVE).matcher(Build.DISPLAY).find();
  }


  /**
   * Prüfen ob MIUI6 oder höher
   */
  public static boolean isMIUI6Later() {

    try {
      Class<?> clz = Class.forName("android.os.SystemProperties");
      Method mtd = clz.getMethod("get", String.class);
      String val = (String) mtd.invoke(null, "ro.miui.ui.version.name");
      val = val.replaceAll("[vV]", "");
      int version = Integer.parseInt(val);
      return version >= 6;
    } catch (Exception e) {
      return false;
    }
  }


  /**
   * Höhe und paddingTop der View um die Statusleistenhöhe erhöhen. Wird typischerweise für die Toolbar im Vollbildmodus verwendet
   */
  public static void setHeightAndPadding(Context context, View view) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      ViewGroup.LayoutParams lp = view.getLayoutParams();
      lp.height += getStatusBarHeight(context);//Höhe erhöhen
      view.setPadding(view.getPaddingLeft(), view.getPaddingTop() + getStatusBarHeight(context),
          view.getPaddingRight(), view.getPaddingBottom());
    }
  }


  /**
   * paddingTop der View um die Statusleistenhöhe erhöhen
   */
  public static void setPadding(Context context, View view) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      view.setPadding(view.getPaddingLeft(), view.getPaddingTop() + getStatusBarHeight(context),
          view.getPaddingRight(), view.getPaddingBottom());
    }
  }


  /**
   * Alle FitsSystemWindows-Attribute der untergeordneten Views von rootView auf false setzen
   */
  public static void forceFitsSystemWindows(Activity activity) {

    forceFitsSystemWindows(activity.getWindow());
  }


  /**
   * Alle FitsSystemWindows-Attribute der untergeordneten Views von rootView auf false setzen
   */
  public static void forceFitsSystemWindows(Window window) {

    forceFitsSystemWindows(
        (ViewGroup) window.getDecorView().findViewById(Window.ID_ANDROID_CONTENT));
  }


  /**
   * Alle FitsSystemWindows-Attribute der untergeordneten Views von rootView auf false setzen
   */
  public static void forceFitsSystemWindows(ViewGroup viewGroup) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      int count = viewGroup.getChildCount();
      for (int i = 0; i < count; i++) {
        View view = viewGroup.getChildAt(i);
        if (view instanceof ViewGroup) {
          forceFitsSystemWindows((ViewGroup) view);
        } else {
          if (ViewCompat.getFitsSystemWindows(view)) {
            ViewCompat.setFitsSystemWindows(view, false);
          }
        }
      }
    }
  }
}