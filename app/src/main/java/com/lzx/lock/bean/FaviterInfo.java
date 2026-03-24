package com.lzx.lock.bean;

import org.litepal.crud.LitePalSupport;

/**
 * Created by xian on 2017/2/17.
 * Informationen zu empfohlenen zu sperrenden Apps
 */

public class FaviterInfo extends LitePalSupport {
    public String packageName;

    public FaviterInfo() {
    }

    public FaviterInfo(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
