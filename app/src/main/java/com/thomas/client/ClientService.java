package com.thomas.client;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.thomas.Device;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientService extends Service {

    private ExecutorService mExecutorService;
    private DatagramSocket mServerSocket;
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
            }
            return false;
        }
    });

    @Override
    public void onCreate() {
        super.onCreate();
        mExecutorService = Executors.newSingleThreadExecutor();
        initServerSocket();
    }


    private void initServerSocket() {
        try {
            mServerSocket = new DatagramSocket();
            final InetAddress inetAddress = InetAddress.getByName("255.255.255.255");
            mExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    sendCommandToServer(inetAddress);
                }
            });
        } catch (SocketException | UnknownHostException e) {
            mHandler.sendEmptyMessage(0);
        }
    }

    /**
     * 这里是用来接收服务端的数据端口
     * @param internetAddress 地址
     */
    private void sendCommandToServer(InetAddress internetAddress) {
        try {
            // 我不需要传递数据到服务器，所以写了一个字节
            byte[] command = new byte[]{0x01};
            DatagramPacket packet = new DatagramPacket(command, command.length, internetAddress, 4567);
            while (!mServerSocket.isClosed()){
                mServerSocket.setBroadcast(true);
                mServerSocket.setSoTimeout(2000);
                mServerSocket.send(packet);
                try {
                    byte[] bytes = new byte[1024];
                    DatagramPacket clientPacket = new DatagramPacket(bytes, 0, bytes.length);
                    mServerSocket.receive(clientPacket);
                    String response = new String(clientPacket.getData(), 0, clientPacket.getLength());
                    System.out.println("client message is " + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        Device device = Device.CREATOR.createFromParcel(Parcel.obtain());
                        InetAddress address = clientPacket.getAddress();
                        String manufacturer = jsonObject.getString("manufacturer");
                        String model = jsonObject.getString("model");
                        int api = jsonObject.getInt("api");
                        device.setIp(address.getHostAddress());
                        device.setApi(api);
                        device.setModel(model);
                        device.setManufacture(manufacturer);
                        Intent intent = new Intent();
                        intent.putExtra("device",device);
                        intent.setAction("com.thomas.device");
                        sendBroadcast(intent);
                        mServerSocket.close();
                        break;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }catch (SocketTimeoutException e){
                    System.out.println("ignore ServerException timeout");
                }
            }
        } catch (IOException e ) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("onDestroy");
        mServerSocket.close();
    }

}
