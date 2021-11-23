package com.thomas;

import android.os.Parcel;
import android.os.Parcelable;

public class Device implements Parcelable {
    private String ip;
    private String broadcastIp;
    private String broadcastPort;
    private String product;
    private String name;
    private String board;
    private int api;
    private String manufacture;
    private String model;
    private boolean isRecord;

    protected Device(Parcel in) {
        ip = in.readString();
        broadcastIp = in.readString();
        broadcastPort = in.readString();
        product = in.readString();
        name = in.readString();
        board = in.readString();
        api = in.readInt();
        manufacture = in.readString();
        model = in.readString();
        isRecord = in.readByte() != 0;
    }

    public static final Creator<Device> CREATOR = new Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getBroadcastIp() {
        return broadcastIp;
    }

    public void setBroadcastIp(String broadcastIp) {
        this.broadcastIp = broadcastIp;
    }

    public String getBroadcastPort() {
        return broadcastPort;
    }

    public void setBroadcastPort(String broadcastPort) {
        this.broadcastPort = broadcastPort;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    public int getApi() {
        return api;
    }

    public void setApi(int api) {
        this.api = api;
    }

    public String getManufacture() {
        return manufacture;
    }

    public void setManufacture(String manufacture) {
        this.manufacture = manufacture;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isRecord() {
        return isRecord;
    }

    public void setRecord(boolean record) {
        isRecord = record;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(ip);
        dest.writeString(broadcastIp);
        dest.writeString(broadcastPort);
        dest.writeString(product);
        dest.writeString(name);
        dest.writeString(board);
        dest.writeInt(api);
        dest.writeString(manufacture);
        dest.writeString(model);
        dest.writeByte((byte) (isRecord ? 1 : 0));
    }
}
