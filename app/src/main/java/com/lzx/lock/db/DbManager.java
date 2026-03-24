package com.lzx.lock.db;

import com.lzx.lock.bean.CommLockInfo;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

/**
 * Singleton-Datenbankmanager für App-Sperr-Informationen
 */
public class DbManager {

    private static DbManager sInstance;

    private DbManager() {
    }

    public static DbManager get() {
        if (sInstance == null) {
            synchronized (DbManager.class) {
                if (sInstance == null) {
                    sInstance = new DbManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * Alle App-Informationen aus der Datenbank abrufen
     */
    public synchronized List<CommLockInfo> queryInfoList() {
        List<CommLockInfo> list = LitePal.findAll(CommLockInfo.class);
        Collections.sort(list, new Comparator<CommLockInfo>() {
            @Override
            public int compare(CommLockInfo lhs, CommLockInfo rhs) {
                if (lhs.isFaviterApp() && !rhs.isFaviterApp()) {
                    return -1;
                } else if (!lhs.isFaviterApp() && rhs.isFaviterApp()) {
                    return 1;
                }
                return 0;
            }
        });
        return list;
    }

    /**
     * App-Informationsliste asynchron speichern
     */
    public Observable<Boolean> saveLockAppInfoListAsync(final List<CommLockInfo> list) {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                try {
                    saveInfoList(list);
                    emitter.onNext(true);
                } catch (Exception e) {
                    emitter.onNext(false);
                }
            }
        });
    }

    /**
     * App-Informationsliste synchron speichern
     */
    public synchronized void saveInfoList(List<CommLockInfo> list) {
        List<CommLockInfo> unique = removeDuplicates(list);
        LitePal.saveAll(unique);
    }

    /**
     * Apps aus der Datenbank löschen
     */
    public synchronized void deleteInfoByList(List<CommLockInfo> list) {
        for (CommLockInfo info : list) {
            LitePal.deleteAll(CommLockInfo.class, "packageName = ?", info.getPackageName());
        }
    }

    /**
     * Doppelte Einträge entfernen
     */
    private List<CommLockInfo> removeDuplicates(List<CommLockInfo> list) {
        List<CommLockInfo> result = new ArrayList<>();
        List<String> packageNames = new ArrayList<>();
        for (CommLockInfo info : list) {
            if (!packageNames.contains(info.getPackageName())) {
                packageNames.add(info.getPackageName());
                result.add(info);
            }
        }
        return result;
    }
}
