package com.thomas.client;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.thomas.Device;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class ScreenService extends Service {
    private ExecutorService mExecutorService;
    // 设备ip

    private OutputStream outputStream;
    private MediaCodec mMediaCodec;
    private VirtualDisplay mVirtualDisplay;
    private LinkedBlockingQueue<byte[]> mLinkedBlockingQueue;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case 0:
                    Toast.makeText(getApplicationContext(), R.string.register_socket_server_failed, Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(getApplicationContext(), R.string.timeout, Toast.LENGTH_LONG).show();
                    break;
                case 2:
                    Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
                    break;
            }
            return false;
        }
    });

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mExecutorService = Executors.newCachedThreadPool();
        mLinkedBlockingQueue = new LinkedBlockingQueue<>(160);
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                while (!mExecutorService.isShutdown()){
                    try {
                        byte[] dataFrame = mLinkedBlockingQueue.take();
                        final byte[] head = new byte[ 8];
                        head[1] = (head[0] = -1);
                        head[3] = (head[2] = -1);
                        byte[] lengthByte = intToByteArray(dataFrame.length);
                        head[4] = lengthByte[0];
                        head[5] = lengthByte[1];
                        head[6] = lengthByte[2];
                        head[7] = lengthByte[3];
                        try {
                            outputStream.write(head,0,head.length);
                            outputStream.write(dataFrame,0,dataFrame.length);
                        } catch (IOException e) {
                            e.printStackTrace();
                            try {
                                outputStream.close();
                                mLinkedBlockingQueue.clear();
                                break;
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }

                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }

            }
        });
        createNotificationChannel();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null){
            String action = intent.getAction();
            if (action != null){
                Device device = intent.getParcelableExtra("device");
                if (device != null){
                    switch (action){
                        // 连接设备的命令
                        case "com.thomas.connect":
                            handleConnectCommand(device,intent);
                            break;
                        // 断开连接设备的命令我这里暂时使用了stopService替代,这里保留不写
                        case "com.thomas.disconnect":
                            break;
                    }
                }
            }

        }
        return super.onStartCommand(intent, flags, startId);
    }


    private void handleConnectCommand(final Device device, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int mResultCode = intent.getIntExtra("code", -1);
            Intent data = intent.getParcelableExtra("data");
            if (data != null){
                MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                // 获取MediaProjection实例
                MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(mResultCode, data);
                mediaProjection.registerCallback(new MediaProjection.Callback() {
                    @Override
                    public void onStop() {
                        super.onStop();
                        System.out.println("onStop");
                        // 结束录制
                        Intent intent = new Intent();
                        intent.putExtra("device",device);
                        // 发送结束录制命令
                        intent.setAction("com.thomas.media.projection.stop");
                        sendBroadcast(intent);
                    }
                },new Handler());
                connectDevice(mediaProjection,device);
            }
        }
    }


    private void connectDevice(final MediaProjection mediaProjection,final Device device) {
        if (device.getIp() != null){
            mExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    handleConnects(mediaProjection,device);
                }
            });
        }else {
            Toast.makeText(this, R.string.device_ip_wrong, Toast.LENGTH_SHORT).show();
        }

    }

    private void handleConnects(MediaProjection mediaProjection, Device device) {
        Socket socket = new Socket();
        try {
            SocketAddress address = new InetSocketAddress(device.getIp(), 6789);
            // socket创建超时时间为6000毫秒
            socket.connect(address, 6000);
            System.out.println("连接成功");
            socket.setKeepAlive(true);
            // 设置缓冲区大小
            socket.setSendBufferSize(512000);
            // 代表可以立即向服务器端发送单字节数据
            socket.setOOBInline(true);
            socket.setTcpNoDelay(true);
            // 获取输入输出流
            outputStream = socket.getOutputStream();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                afterConnect(mediaProjection,device);
            }
        } catch (IOException ex) {
            if (ex instanceof SocketTimeoutException){
                mHandler.sendEmptyMessage(1);
            }else {
                mHandler.sendEmptyMessage(0);
            }
        }
    }

    private void afterConnect(MediaProjection mediaProjection, Device device) {
//        int widthPixels = getResources().getDisplayMetrics().widthPixels;
//        int heightPixels = getResources().getDisplayMetrics().heightPixels;
        int densityDpi = getResources().getDisplayMetrics().densityDpi;
        int widthPixels = 720;
        int heightPixels = 1280;
        handleMediaProjection(mediaProjection,device,widthPixels,heightPixels,densityDpi);
    }

    private void handleMediaProjection(MediaProjection mediaProjection, Device device, final int widthPixels, final int heightPixels, final int densityDpi) {
        MediaFormat videoFormat = obtainMediaFormat(widthPixels,heightPixels);
        // 如果服务端是横屏的，所以需要宽高倒置
        // 创建一个MediaCodec的实例
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mMediaCodec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    System.out.println("onInputBufferAvailable");
                }

                /**
                 *
                 * @param index   这里就不用使用过期的方式获取index
                 *                ByteBuffer[] outputBuffers = codec.getOutputBuffers();
                 *                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                 *                int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_TIME);TIMEOUT_TIME 可以设置 1000
                 */
                @Override
                public void onOutputBufferAvailable(@NonNull final MediaCodec codec,final int index, final @NonNull MediaCodec.BufferInfo info) {
                   // System.out.println("onOutputBufferAvailable");
                    handleOutputBufferAvailable(codec,index,info);

                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                    System.out.println("onError");

                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                    System.out.println("[" + Thread.currentThread().getId() + "] AudioEncoder returned new format " + format);
                }
            });
            // 定义这个实例的格式，也就是上面我们定义的format，其他参数不用过于关注//最后一个参数指定是编码器还是解码器。
            mMediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            // 这一步非常关键，它设置的，是MediaCodec的编码源，也就是说，我要告诉mEncoder，你给我解码哪些流。
            // 很出乎大家的意料，MediaCodec并没有要求我们传一个流文件进去，而是要求我们指定一个surface
            // 而这个surface，其实就是我们在上一讲MediaProjection中用来展示屏幕采集数据的surface
            // 需要在createEncoderByType之后和start()之前才能创建，源码注释写的很清楚
            Surface surface = mMediaCodec.createInputSurface();
            VirtualDisplay display = getOrCreateVirtualDisplay(mediaProjection, surface,heightPixels,widthPixels,densityDpi);
            System.out.println(display);
            mMediaCodec.start();
            // 连接成功了并且开始了录制
            Intent intent = new Intent();
            intent.putExtra("device",device);
            // 发送在录制命令
            intent.setAction("com.thomas.media.projection.start");
            sendBroadcast(intent);
        } catch (IOException e) {
            e.printStackTrace();
            Message message = new Message();
            message.obj = e.getMessage();
            message.what = 2;
            mHandler.sendMessage(message);
        }
    }

    private void handleOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
        ByteBuffer outputBuffer = codec.getOutputBuffer(index);
        byte[] data = new byte[info.size];
        assert outputBuffer != null;
        outputBuffer.position(info.offset);
        outputBuffer.get(data);
        mLinkedBlockingQueue.offer(data);
        codec.releaseOutputBuffer(index, false);
    }

    private MediaFormat obtainMediaFormat(int widthPixels, int heightPixels) {
        // MediaFormat这个类是用来定义视频格式相关信息的video/avc,这里的avc是高级视频编码Advanced Video Coding
        // widthPixels 和 heightPixels是视频的尺寸，这个尺寸不能超过视频采集时采集到的尺寸，否则会直接crash
        MediaFormat videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, widthPixels, heightPixels);
        // COLOR_FormatSurface这里表明数据将是一个 GraphicBuffer 元数据 录屏必须配置的参数
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        //videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_Format16bitRGB565);
        // 设置码率，通常码率越高，视频越清晰，但是对应的视频也越大，这个值我默认设置成了2000000，
        // 也就是通常所说的2M，这已经不低了，如果你不想录制这么清晰的，你可以设置成500000，也就是500k
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE,  2000000);

        // 设置帧率，通常这个值越高，视频会显得越流畅，一般默认我设置成30，你最低可以设置成24，不要低于这个值，低于24会明显卡顿
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
//        videoFormat.setInteger(MediaFormat.KEY_WIDTH, 720);
//        videoFormat.setInteger(MediaFormat.KEY_HEIGHT, 1280);
        // i-frame-interval是指的帧间隔，这是个很有意思的值，它指的是，关键帧的间隔时间。通常情况下，
        // 你设置成多少问题都不大。比如你设置成10，那就是10秒一个关键帧。但是，如果你有需求要做视频的预览，
        // 那你最好设置成1因为如果你设置成10，那你会发现，10秒内的预览都是一个截图
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            videoFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel52);
        }
        return videoFormat;
    }


    /**
     VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR：当没有内容显示时，允许将内容镜像到专用显示器上。
     VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY：仅显示此屏幕的内容，不镜像显示其他屏幕的内容。
     VIRTUAL_DISPLAY_FLAG_PRESENTATION：创建演示文稿的屏幕。
     VIRTUAL_DISPLAY_FLAG_PUBLIC：创建公开的屏幕。
     VIRTUAL_DISPLAY_FLAG_SECURE：创建一个安全的屏幕
     */
    private VirtualDisplay getOrCreateVirtualDisplay(MediaProjection mediaProjection,Surface surface, int heightPixels, int widthPixels, int dpi) {

        if (mVirtualDisplay == null) {
            mVirtualDisplay = mediaProjection.createVirtualDisplay("thomas-display",
                    widthPixels, heightPixels, dpi ,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                    surface , null, null);


        } else {
            // resize if size not matched
            Point size = new Point();
            mVirtualDisplay.getDisplay().getSize(size);
            if (size.x != widthPixels || size.y != heightPixels) {
                mVirtualDisplay.resize(widthPixels, heightPixels, dpi);
            }
        }
        return mVirtualDisplay;
    }

    private void createNotificationChannel() {
        Notification.Builder builder = new Notification.Builder(getApplicationContext()); //获取一个Notification构造器
        Intent nfIntent = new Intent(this, MainActivity.class); //点击后跳转的界面，可以设置跳转数据

        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher)) // 设置下拉列表中的图标(大图标)
                //.setContentTitle("SMI InstantView") // 设置下拉列表里的标题
                .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
                .setContentText("is running......") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id");
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        startForeground(110, notification);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("onDestroy");
        if (mMediaCodec != null){
            mMediaCodec.reset();
            mMediaCodec.stop();
            mMediaCodec.release();
        }
        if (mVirtualDisplay != null){
            mVirtualDisplay.release();
        }
        if (outputStream != null){
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private byte[] intToByteArray(int length) {
        return new byte[] {
                (byte) ((length >> 24) & 0xFF),
                (byte) ((length >> 16) & 0xFF),
                (byte) ((length >> 8) & 0xFF),
                (byte) (length & 0xFF)
        };
    }
}
