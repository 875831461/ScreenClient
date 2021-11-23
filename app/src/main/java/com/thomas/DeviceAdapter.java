package com.thomas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.thomas.client.R;

import java.util.List;

public class DeviceAdapter extends BaseAdapter {
    private List<Device> mData;

    public void setData(List<Device> data) {
        this.mData = data;
        notifyDataSetChanged();
    }

    public List<Device> getData() {
        return mData;
    }

    @Override
    public int getCount() {
        return mData == null ? 0 : mData.size();
    }

    @Override
    public Device getItem(int position) {
        return mData == null ? null : mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.item_device_ip.setText(mData.get(position).getIp());
        viewHolder.item_device_manufacturer.setText(mData.get(position).getManufacture());
        viewHolder.item_device_model.setText(mData.get(position).getModel());
        return convertView;
    }

    private static class ViewHolder {
        TextView item_device_ip;
        TextView item_device_manufacturer;
        TextView item_device_model;
        public ViewHolder(View view) {
            this.item_device_ip = (TextView) view.findViewById(R.id.item_device_ip);
            this.item_device_manufacturer = (TextView) view.findViewById(R.id.item_device_manufacturer);
            this.item_device_model = (TextView) view.findViewById(R.id.item_device_model);
        }
    }
}
