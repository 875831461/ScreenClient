package com.thomas.client;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Display;
import android.widget.Chronometer;
import android.widget.TextView;

/**
 * 副屏
 */
public class CustomPresentation extends Presentation {
    private int changeCount;

    private TextView tv_vice_change;


    public void setChange(){
        changeCount ++ ;
        tv_vice_change.setText(String.valueOf(changeCount));
    }


    CustomPresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_presentation);
        tv_vice_change = findViewById(R.id.tv_vice_change);
        Chronometer chronometer = findViewById(R.id.chronometer);
        // 设置开始计时时间
        chronometer.setBase(SystemClock.elapsedRealtime());
        // 启动计时器
        chronometer.start();
    }

    /**
     * 说明副屏已经消失了
     */
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        System.out.println("onDetachedFromWindow");
    }


    /**
     * 副屏发生改变调用(息屏触发)
     */
    @Override
    public void onDisplayChanged() {
        super.onDisplayChanged();
        System.out.println("onDisplayChanged");
    }
}
