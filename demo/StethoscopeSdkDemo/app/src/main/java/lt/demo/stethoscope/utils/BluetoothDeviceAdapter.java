package lt.demo.stethoscope.utils;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;

import lt.demo.stethoscope.R;
import lt.sdk.ble.BleDevice;

public class BluetoothDeviceAdapter extends BaseRecycleAdapter<BluetoothDeviceAdapter.ViewHolder, BleDevice> {

    public BluetoothDeviceAdapter(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_bluetooth_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        BleDevice item = getItem(position);
        String name = item.getName();
        holder.tvName.setText(TextUtils.isEmpty(name) ? getString(R.string.unknown) : name);
        holder.tvAddress.setText(item.getAddress());
        holder.tvRssi.setText(String.format(Locale.getDefault(), "%ddB", item.getRssi()));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvAddress, tvRssi;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvAddress = itemView.findViewById(R.id.tv_address);
            tvRssi = itemView.findViewById(R.id.tv_rssi);
        }
    }
}
