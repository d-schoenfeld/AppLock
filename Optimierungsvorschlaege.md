### Optimierungsvorschläge für das Projekt

Insgesamt gibt es 6 Verbesserungsvorschläge:

#### Vorschlag 1:

Optimierungsvorschlag für den LockService (App-Sperrdienst): Der Dienst soll nicht mehr von `IntentService` erben, sondern von `Service`, und intern einen eigenen Thread verwalten.

Die Abbruchbedingung der Schleife sollte nicht mit einem einfachen `boolean` realisiert werden, sondern mit dem thread-sicheren `AtomicBoolean`:
```java
public class AppLockService extends Service {
    private AtomicBoolean mIsServiceDestoryed = new AtomicBoolean(false);

    @Override
    public void onCreate() {
        super.onCreate();
        //...
        AsyncTask.SERIAL_EXECUTOR.execute(new ServiceWorker());
    }

    private class ServiceWorker implements Runnable {

      @Override
      public void run() {
          while (!mIsServiceDestoryed.get()) {
              //...
          }
      }
    }

    @Override
   public void onDestroy() {
       mIsServiceDestoryed.set(true);
       super.onDestroy();
   }
}
```

#### Vorschlag 2:
Aktuell werden in der Schleife zu häufig SharedPreferences-Dateien (SP) gelesen und geschrieben. Da das Lesen und Schreiben von SP-Dateien gepuffert und zudem ein Datei-I/O-Vorgang ist, kann dies bei einer so häufigen Schleife die Performance beeinträchtigen und zu Verlangsamungen führen.

Da sich Variablen wie die Sperrzeit in Echtzeit ändern müssen, können sie nicht als globale Variablen angelegt werden. Der Optimierungsvorschlag ist daher, globale statische Variablen statt der SP-Datei zu verwenden. Die SP-Datei speichert nur die zuletzt geänderte Zeit. Wenn die App beendet wird, werden die SP-Werte beim nächsten Start den statischen Variablen zugewiesen.

#### Vorschlag 3:
Die Entsperrungsanzeige ist aktuell als `Activity` implementiert. Es wird empfohlen, sie stattdessen als schwebende `Window`-Ansicht umzusetzen. Unten folgt ein Codebeispiel zur Referenz, das direkt verwendet oder als Grundlage eigener Implementierungen dienen kann:
```java
public class UnlockView extends FrameLayout {

    private int mFailedPatternAttemptsSinceLastTimeout = 0;

    private WindowManager.LayoutParams mLayoutParams;
    private WindowManager mWindowManager;
    private Context mContext;
    private View mUnLockView;
    private Drawable iconDrawable;
    private String appLabel;

    private ImageView mBgView, mUnLockIcon, mBtnMore;
    private TextView mUnLockAppName, mUnlockFailTip;
    private LockPatternView mPatternView;

    private LockPatternUtils mPatternUtils;
    private LockPatternViewPattern mPatternViewPattern;
    private LockAppInfo mLockAppInfo;
    private ApplicationInfo mApplicationInfo;
    private PackageManager mPackageManager;

    public UnlockView(@NonNull Context context) {
        super(context, null, 0);
        init();
    }

    private void init() {
        mContext = getContext();

        mPackageManager = mContext.getPackageManager();

        mUnLockView = LayoutInflater.from(mContext).inflate(R.layout.layout_unlock_view, this);
        mBgView = mUnLockView.findViewById(R.id.bg_view);
        mUnLockIcon = mUnLockView.findViewById(R.id.unlock_icon);
        mBtnMore = mUnLockView.findViewById(R.id.btn_more);
        mUnLockAppName = mUnLockView.findViewById(R.id.unlock_app_name);
        mPatternView = mUnLockView.findViewById(R.id.unlock_lock_view);
        mUnlockFailTip = mUnLockView.findViewById(R.id.unlock_fail_tip);

        //Schwebendes Fenster erstellen
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSPARENT
        );
        mLayoutParams.gravity = Gravity.CENTER;

        initLockPatternView();
    }

    public void setLockAppInfo(LockAppInfo lockAppInfo) {
        mLockAppInfo = lockAppInfo;
    }

    private final static int MSG_ADDVIEW = 100;
    private final static int MSG_GO_HOME = 200;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_ADDVIEW:
                    mWindowManager.addView(UnlockView.this, mLayoutParams);
                    break;
                case MSG_GO_HOME:
                    closeUnLockView();
                    break;
            }
        }
    };

    /**
     * Entsperrungsansicht öffnen
     */
    public void showUnLockView() {
        if (mLockAppInfo == null) {
            return;
        }
        initBgView();
        mHandler.obtainMessage(MSG_ADDVIEW).sendToTarget();
    }

    /**
     * Entsperrungsansicht schließen
     */
    private boolean closeUnLockView() {
        if (mWindowManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (isAttachedToWindow()) {
                    mWindowManager.removeViewImmediate(this);
                    return true;
                } else {
                    return false;
                }
            } else {
                try {
                    if (getParent() != null) {
                        mWindowManager.removeViewImmediate(this);
                    }
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    /**
     * Home-Taste
     */
    public void closeUnLockViewFormHomeAction() {
        if (getParent() != null && mHandler != null) {
            mHandler.sendEmptyMessageDelayed(MSG_GO_HOME, 500);
        }
    }

    /**
     * Hintergrundbild
     */
    private void initBgView() {
        mApplicationInfo = mLockAppInfo.getAppInfo();
        if (mApplicationInfo != null) {
            try {
                iconDrawable = mPackageManager.getApplicationIcon(mApplicationInfo);
                appLabel = mLockAppInfo.getAppName();
                mUnLockIcon.setImageDrawable(iconDrawable);
                mUnLockAppName.setText(appLabel);
                mUnlockFailTip.setText(mContext.getString(R.string.password_gestrue_tips));
                mBgView.setBackground(iconDrawable);
                mBgView.getViewTreeObserver().addOnPreDrawListener(
                        new ViewTreeObserver.OnPreDrawListener() {
                            @Override
                            public boolean onPreDraw() {
                                mBgView.getViewTreeObserver().removeOnPreDrawListener(this);
                                mBgView.buildDrawingCache();
                                Bitmap bmp = BlurUtil.drawableToBitmap(iconDrawable, mBgView);
                                BlurUtil.blur(mContext, BlurUtil.big(bmp), mBgView);  //Gaußscher Weichzeichner
                                return true;
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Entsperr-Widget initialisieren
     */
    private void initLockPatternView() {
        mPatternView.setLineColorRight(0x80ffffff);
        mPatternUtils = new LockPatternUtils(mContext);
        mPatternViewPattern = new LockPatternViewPattern(mPatternView);
        mPatternViewPattern.setPatternListener(new LockPatternViewPattern.onPatternListener() {
            @Override
            public void onPatternDetected(List<LockPatternView.Cell> pattern) {
                if (mPatternUtils.checkPattern(pattern)) { //Entsperrung erfolgreich, Datenbankstatus aktualisieren
                    mPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
                    //TODO
                    closeUnLockView();
                } else {
                    mPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    if (pattern.size() >= LockPatternUtils.MIN_PATTERN_REGISTER_FAIL) {
                        mFailedPatternAttemptsSinceLastTimeout++;
                        int retry = LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT - mFailedPatternAttemptsSinceLastTimeout;
                        if (retry >= 0) {
                            String format = mContext.getResources().getString(R.string.password_error_count);
                            mUnlockFailTip.setText(format);
                        }
                    } else {
                        mUnlockFailTip.setText(mContext.getResources().getString(R.string.password_short));
                    }
                    if (mFailedPatternAttemptsSinceLastTimeout >= 3) { //Fehlversuche > 3
                        mPatternView.postDelayed(mClearPatternRunnable, 500);
                    }
                    if (mFailedPatternAttemptsSinceLastTimeout >= LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT) { //Fehlversuche >= Maximum
                        mPatternView.postDelayed(mClearPatternRunnable, 500);
                    } else {
                        mPatternView.postDelayed(mClearPatternRunnable, 500);
                    }
                }
            }
        });
        mPatternView.setOnPatternListener(mPatternViewPattern);
        mPatternView.setTactileFeedbackEnabled(true);
    }

    private Runnable mClearPatternRunnable = new Runnable() {
        public void run() {
            mPatternView.clearPattern();
        }
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (getParent() != null) {
                LockUtil.launchHome(mContext);
                mHandler.sendEmptyMessageDelayed(MSG_GO_HOME, 500);
            }
            return true;
        }
        return false;
    }
}
```

#### Vorschlag 4:
Wenn RxJava im Projekt verwendet wird, lässt sich die App damit erheblich verbessern.

Zum Beispiel wird das Laden der App-Liste aktuell in einem Dienst ausgeführt – damals wurde dieser Ansatz gewählt, um die zeitaufwändige Operation in den Hintergrund zu verlagern und ANR/Ruckler zu vermeiden. Mit RxJava wird der Thread-Wechsel sehr einfach. Unten folgt ein Codebeispiel. `LoadAppHelper` ist eine Hilfsklasse zum Laden der App-Liste, `DbManager` eine Datenbankmanager-Klasse – beide sind einfach zu implementieren:
```Java
//Daten initialisieren
LoadAppHelper.loadAllLockAppInfoAsync(this)
   .subscribeOn(Schedulers.newThread())
   .observeOn(AndroidSchedulers.mainThread())
   .filter(new Predicate<List<LockAppInfo>>() {
       @Override
       public boolean test(List<LockAppInfo> lockAppInfos) throws Exception {
           if (isFirstTime) {
               DbManager.get()
                       .saveLockAppInfoListAsync(lockAppInfos)
                       .subscribeOn(Schedulers.newThread())
                       .observeOn(AndroidSchedulers.mainThread())
                       .subscribe(new Consumer<Boolean>() {
                           @Override
                           public void accept(Boolean aBoolean) throws Exception {
                               animator.start();
                           }
                       });
               return false;
           } else {
               return true;
           }
       }
   })
   .observeOn(Schedulers.newThread())
   .map(new Function<List<LockAppInfo>, List<LockAppInfo>>() {
       @Override
       public List<LockAppInfo> apply(List<LockAppInfo> appList) throws Exception {
           //Datenbankeinträge mit aktuellen App-Daten vergleichen
           List<LockAppInfo> dbList = DbManager.get().queryInfoList();
           if (appList.size() > dbList.size()) { //wenn neue App installiert wurde
               List<LockAppInfo> resultList = new ArrayList<>();
               HashMap<String, LockAppInfo> hashMap = new HashMap<>();
               for (LockAppInfo info : dbList) {
                   hashMap.put(info.getPackageName(), info);
               }
               for (LockAppInfo info : appList) {
                   if (!hashMap.containsKey(info.getPackageName())) {
                       resultList.add(info);
                   }
               }
               //neue Apps in Datenbank einfügen
               if (resultList.size() != 0) {
                   DbManager.get().saveInfoList(resultList);
               }
           } else if (appList.size() < dbList.size()) { //wenn App deinstalliert wurde
               List<LockAppInfo> resultList = new ArrayList<>();
               HashMap<String, LockAppInfo> hashMap = new HashMap<>();
               for (LockAppInfo info : appList) {
                   hashMap.put(info.getPackageName(), info);
               }
               for (LockAppInfo info : dbList) {
                   if (!hashMap.containsKey(info.getPackageName())) {
                       resultList.add(info);
                   }
               }
               //deinstallierte Apps aus Datenbank löschen
               if (resultList.size() != 0) {
                   DbManager.get().deleteInfoByList(resultList);
               }
           }
           return DbManager.get().queryInfoList();
       }
   })
   .observeOn(AndroidSchedulers.mainThread())
   .subscribe(new Consumer<List<LockAppInfo>>() {
       @Override
       public void accept(List<LockAppInfo> lockAppInfos) throws Exception {
           if (lockAppInfos.size() != 0) {
               animator.start();
           } else {
               Toast.makeText(mContext, "Fehler bei der Datenverarbeitung", Toast.LENGTH_SHORT).show();
           }
       }
   }, new Consumer<Throwable>() {
       @Override
       public void accept(Throwable throwable) throws Exception {
           Toast.makeText(mContext, "Fehler bei der Datenverarbeitung", Toast.LENGTH_SHORT).show();
       }
   });
```

LoadAppHelper:

```Java
/**
 * Hilfsklasse zum Laden der App-Liste
 */
public class LoadAppHelper {

    /**
     * Alle installierten Apps abrufen
     */
    private static List<ResolveInfo> loadPhoneAppList(PackageManager packageManager) {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        return packageManager.queryIntentActivities(intent, 0);
    }

    /**
     * Empfohlene zu sperrende Apps initialisieren
     */
    private static List<String> loadRecommendApps() {
        List<String> packages = new ArrayList<>();
        packages.add("com.android.gallery3d");       //Galerie
        packages.add("com.android.mms");             //SMS
        packages.add("com.tencent.mm");              //WeChat
        packages.add("com.android.contacts");        //Kontakte und Telefon
        packages.add("com.facebook.katana");         //Facebook
        packages.add("com.facebook.orca");           //Facebook Messenger
        packages.add("com.mediatek.filemanager");    //Dateimanager
        packages.add("com.sec.android.gallery3d");   //weitere Galerie-App
        packages.add("com.android.email");           //E-Mail
        packages.add("com.sec.android.app.myfiles"); //Samsung Dateien
        packages.add("com.android.vending");         //App-Store
        packages.add("com.google.android.youtube");  //YouTube
        packages.add("com.tencent.mobileqq");        //QQ
        packages.add("com.tencent.qq");              //QQ
        packages.add("com.android.dialer");          //Telefon
        packages.add("com.twitter.android");         //Twitter
        return packages;
    }

    /**
     * App-Informationen in benötigte Datenstruktur umwandeln
     */
    private static List<LockAppInfo> loadLockAppInfo(Activity activity) {
        List<LockAppInfo> list = new ArrayList<>();
        try {
            PackageManager mPackageManager = activity.getPackageManager();
            List<ResolveInfo> resolveInfos = loadPhoneAppList(mPackageManager);
            for (ResolveInfo resolveInfo : resolveInfos) {
                String packageName = resolveInfo.activityInfo.packageName;
                boolean isRecommend = isRecommendApp(packageName);
                LockAppInfo info = new LockAppInfo(packageName, false, isRecommend);
                ApplicationInfo appInfo = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
                String appName = mPackageManager.getApplicationLabel(appInfo).toString();
                if (!isFilterOutApps(packageName)) {
                    boolean isSysApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                    info.setLocked(isRecommend);
                    info.setAppName(appName);
                    info.setSysApp(isSysApp);
                    info.setAppType(isSysApp ? "SystemApp" : "OtherApp");
                    info.setSetUnLock(false);
                    info.setAppInfo(appInfo);
                    list.add(info);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Asynchron laden
     */
    public static Observable<List<LockAppInfo>> loadAllLockAppInfoAsync(final Activity activity) {
        return Observable.create(new ObservableOnSubscribe<List<LockAppInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<LockAppInfo>> emitter) throws Exception {
                emitter.onNext(loadLockAppInfo(activity));
            }
        });
    }

    public static Observable<List<LockAppInfo>> loadLockedAppInfoAsync(final Activity activity) {
        return Observable.create(new ObservableOnSubscribe<List<LockAppInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<LockAppInfo>> emitter) throws Exception {
                List<LockAppInfo> list = loadLockAppInfo(activity);
                List<LockAppInfo> lockAppInfos = new ArrayList<>();
                for (LockAppInfo info : list) {
                    if (info.isLocked()) {
                        lockAppInfos.add(info);
                    }
                }
                emitter.onNext(lockAppInfos);
            }
        });
    }

    public static Observable<List<LockAppInfo>> loadUnLockAppInfoAsync(final Activity activity) {
        return Observable.create(new ObservableOnSubscribe<List<LockAppInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<LockAppInfo>> emitter) throws Exception {
                List<LockAppInfo> list = loadLockAppInfo(activity);
                List<LockAppInfo> lockAppInfos = new ArrayList<>();
                for (LockAppInfo info : list) {
                    if (!info.isLocked()) {
                        lockAppInfos.add(info);
                    }
                }
                emitter.onNext(lockAppInfos);
            }
        });
    }

    public static Observable<List<LockAppInfo>> loadSystemAppInfoAsync(final Activity activity) {
        return Observable.create(new ObservableOnSubscribe<List<LockAppInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<LockAppInfo>> emitter) throws Exception {
                List<LockAppInfo> list = loadLockAppInfo(activity);
                List<LockAppInfo> lockAppInfos = new ArrayList<>();
                for (LockAppInfo info : list) {
                    if (info.isSysApp()) {
                        lockAppInfos.add(info);
                    }
                }
                emitter.onNext(lockAppInfos);
            }
        });
    }

    public static Observable<List<LockAppInfo>> loadUserAppInfoAsync(final Activity activity) {
        return Observable.create(new ObservableOnSubscribe<List<LockAppInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<LockAppInfo>> emitter) throws Exception {
                List<LockAppInfo> list = loadLockAppInfo(activity);
                List<LockAppInfo> lockAppInfos = new ArrayList<>();
                for (LockAppInfo info : list) {
                    if (!info.isSysApp()) {
                        lockAppInfos.add(info);
                    }
                }
                emitter.onNext(lockAppInfos);
            }
        });
    }

    /**
     * Prüfen ob empfohlene zu sperrende App
     */
    private static boolean isRecommendApp(String packageName) {
        List<String> packages = loadRecommendApps();
        return !TextUtils.isEmpty(packageName) && packages.contains(packageName);
    }

    /**
     * Whitelist für auszuschließende Apps
     */
    private static boolean isFilterOutApps(String packageName) {
        return packageName.equals(Constants.APP_PACKAGE_NAME) ||
                packageName.equals("com.android.settings") ||
                packageName.equals("com.google.android.googlequicksearchbox");
    }
}
```

#### Vorschlag 5:
Falls gewünscht, kann der Dienst als Remote-Dienst implementiert werden. Da ein IPC-basierter Dienst den Speicherbedarf der App reduziert, lassen sich damit OOM-Fehler vermeiden.

#### Vorschlag 6:
Was die Prozesserhaltung (Process Keep-Alive) betrifft: Es ist nicht möglich, deren Erfolg absolut zu garantieren. Nachfolgend einige gesammelte Ressourcen zur Referenz:

- [Prozesserhaltungslösungen](https://www.jianshu.com/p/845373586ac1)  
- [Android-Prozesserhaltung](http://geek.csdn.net/news/detail/95035)  
- [Überblick über Android-Prozesserhaltungsansätze](https://www.jianshu.com/p/c1a9e3e86666)  
- [Android-Prozesserhaltung mit JobScheduler und Vordergrunddienstll](https://www.jianshu.com/p/f9322c15579a)  
- [Lernmaterial: Prozesserhaltungslösungen](https://www.jianshu.com/p/da6efef407e9)  
- [Android-Prozesserhaltungslösung](https://www.jianshu.com/p/a407a9b1a3e6)
