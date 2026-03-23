package com.lzx.lock.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;


//import com.squareup.leakcanary.RefWatcher;

/**
 * description: Fragment-Basisklasse
 * author: xiaodifu
 * date: 2016/7/8.
 */
public abstract class BaseFragment extends Fragment {

    private View view;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (getOptionsMenuId() != -1) {
            setHasOptionsMenu(true);
        }

        initBefore(inflater, container, savedInstanceState);
        view = inflater.inflate(getContentViewId(), container, false);
        init(view);
        return view;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (getOptionsMenuId() != -1) {
            inflater.inflate(getOptionsMenuId(), menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Vor der Initialisierung
     */
    protected void initBefore(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    }

    /**
     * Aktuelle Layout-Datei
     *
     * @return
     */
    protected abstract int getContentViewId();

    /**
     * Initialisieren
     */
    protected abstract void init(View rootView);


    public View findViewById(int id) {
        return view.findViewById(id);
    }


    /**
     * Menüressource der aktuellen Seite
     *
     * @return
     */
    protected int getOptionsMenuId() {
        return -1;
    }


}


