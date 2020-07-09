package cn.time24.kezhu.application;

import android.app.Application;

/**
 * 自定义Application
 * Created by wcy on 2015/11/27.
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCache.get().init(this);
    }
}
