package com.lzy.edit;

import android.app.Application;
import android.os.Handler;
import android.view.ViewConfiguration;
import java.lang.reflect.Field;

/*
 * Application
 */
public class MyApplication extends Application {

	private Handler mHandler;

	public void setHandler(Handler mHandler) {
		this.mHandler = mHandler;
	}

	public Handler getHandler() {
		return mHandler;
	}

    @Override
    public void onCreate() {
        // TODO: Implement this method
        super.onCreate();
        menuKey();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());

    }

    public void menuKey() {
        try {
            ViewConfiguration mconfig = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(mconfig, false);
            }
        } catch (Exception e) {
        }
    }

}
