package com.thomas;

import android.app.Application;
import android.view.Display;

public class ApplicationInit extends Application {
    private static ApplicationInit Application;

    public static ApplicationInit get(){
        if (Application == null){
            synchronized (ApplicationInit.class){
                if (Application == null){
                    Application = new ApplicationInit();
                }
            }
        }
        return Application;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Application = this;
    }

}
