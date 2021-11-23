package com.thomas.client;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.media.MediaRouter;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Toast;

import com.thomas.Device;
import com.thomas.DeviceAdapter;
import com.thomas.client.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private CustomPresentation customPresentation;
    // request code for permission
    private final int DISPLAY_PERMISSION = 10;
    private final int REQUEST_SCREEN_CAPTURE = 17;
    private DisplayManager.DisplayListener mDisplayListener;
    private ThomasScreenBroadcast mThomasScreenBroadcast;
    private DeviceAdapter mAdapter;

    private class ThomasScreenBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null){
                Device device = intent.getParcelableExtra("device");
                if (device == null)
                    return;
                switch (intent.getAction()){
                    case "com.thomas.device":
                        handleDevice(device);
                        break;
                    case "com.thomas.media.projection.start":
                        handleMediaProjection(device,true);
                        break;
                    case "com.thomas.media.projection.stop":
                        handleMediaProjection(device,false);
                        break;
                }
            }
        }
    }

    private void handleMediaProjection(Device targetDevice,boolean isRecord) {
        List<Device> devices = mAdapter.getData();
        for (Device data : devices){
            if (targetDevice.getIp().equals(data.getIp())){
                data.setRecord(isRecord);
                break;
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    private void handleDevice(Device device) {
        List<Device> devices = mAdapter.getData();
        if (devices == null){
            devices = new ArrayList<>();
        }
        devices.add(device);
        mAdapter.setData(devices);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            initPermission();
        }
        initView();
        initBroadcast();
        initDisplayListener();
        Intent service = new Intent(this, ClientService.class);
        startService(service);
    }

    private void initView() {
        mAdapter = new DeviceAdapter();
        binding.deviceLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Device device = mAdapter.getItem(position);
                System.out.println(device.isRecord());
                if (device.isRecord()){
                    Intent service = new Intent(MainActivity.this, ScreenService.class);
                    stopService(service);
                    Toast.makeText(getApplicationContext(), R.string.screen_capture_stop, Toast.LENGTH_SHORT).show();
                }else {
                    binding.deviceLv.setTag(device);
                    startMediaProjection();
                }

            }
        });
        binding.deviceLv.setAdapter(mAdapter);
        binding.deviceLv.setEmptyView(binding.deviceIntroduce);
    }


    private void initBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        // 接收设备
        intentFilter.addAction("com.thomas.device");
        // 接收录制命令
        intentFilter.addAction("com.thomas.media.projection.start");
        intentFilter.addAction("com.thomas.media.projection.stop");
        mThomasScreenBroadcast = new ThomasScreenBroadcast();
        registerReceiver(mThomasScreenBroadcast,intentFilter);
    }

    /**
     * 这里创建熄屏时候的监听,当然你也可以使用广播的方式
     */
    private void initDisplayListener() {
        mDisplayListener = new DisplayManager.DisplayListener() {
            /**
             * 客屏屏幕信息发生添加改变会触发一次
             */
            @Override
            public void onDisplayAdded(int displayId) {
                System.out.println("onDisplayAdded" + Thread.currentThread().getName());
                MediaRouter mediaRouter = (MediaRouter) getSystemService(MEDIA_ROUTER_SERVICE);
                MediaRouter.RouteInfo route = mediaRouter.getSelectedRoute(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
                Display display = route.getPresentationDisplay();
                System.out.println("presentation display information" + display);
                if (display != null) {
                    // 如果想要录制主屏注释以下方法即可
                    showPresentationDisplay(display);
                }

            }

            /**
             * 客屏屏幕信息发生添加改变会触发一次
             */
            @Override
            public void onDisplayRemoved(int displayId) {
                System.out.println("onDisplayRemoved" + displayId);

            }

            /**
             * 这个会触发多次尽量请不要在这里做任何操作,部分机型会改变displayId，部分机型是固定的id数值进行变化
             * @param displayId 屏幕ID
             */
            @Override
            public void onDisplayChanged(int displayId) {
                if (customPresentation != null){
                    System.out.println("display changed" + customPresentation.isShowing());
                }else {
                    System.out.println("display changed customPresentation null" + displayId );
                }
            }
        };
        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        displayManager.registerDisplayListener(mDisplayListener,null);
    }

    private void showPresentationDisplay(Display display) {
        customPresentation = new CustomPresentation(MainActivity.this, display);
        if (customPresentation.getWindow() != null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                customPresentation.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            }else {
                customPresentation.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            }
            customPresentation.show();
        }
    }

    /**
     * 开始调用录制的命令
     */
    private void startMediaProjection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            Intent intent = mediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(intent, REQUEST_SCREEN_CAPTURE);
        }else {
            System.out.println("device not support");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_SCREEN_CAPTURE) && (resultCode == RESULT_OK) ){
            if (data == null)
                return;
            Bundle bundle = data.getExtras();
            if (bundle == null)
                return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 拿到需要的投屏设备
                Device device = (Device) binding.deviceLv.getTag();
                if (device == null)
                    return;
                boolean isRecord = device.isRecord();
                if (!isRecord){
                    Intent service = new Intent(this, ScreenService.class);
                    service.putExtra("code", resultCode);
                    service.putExtra("data", data);
                    service.putExtra("device", device);
                    // 发送连接设备的命令
                    service.setAction("com.thomas.connect");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(service);
                    }else {
                        startService(service);
                    }
                }else {
                    Toast.makeText(this, R.string.screen_capture_stop, Toast.LENGTH_SHORT).show();
                }

            }else {
                System.out.println("device not support");
            }
            return;
        }else if (requestCode == DISPLAY_PERMISSION){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)){
                    Toast.makeText(this, R.string.overlay_permission_refuse, Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }
        Toast.makeText(this, R.string.screen_capture_permission_refuse, Toast.LENGTH_SHORT).show();
    }



    private void initPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)){
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:"+getPackageName()));
                startActivityForResult(intent,DISPLAY_PERMISSION);
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mThomasScreenBroadcast);
        if (customPresentation != null){
            customPresentation.dismiss();
        }
        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        displayManager.unregisterDisplayListener(mDisplayListener);
        Intent service = new Intent(this, ScreenService.class);
        stopService(service);
        Intent clientService = new Intent(this, ClientService.class);
        stopService(clientService);

    }

    public void changePresentationDataClick(View view) {
        if (customPresentation != null){
            // 如果想要交互或者使用事件，或者使用以下方式交互
            customPresentation.setChange();
        }
    }

}