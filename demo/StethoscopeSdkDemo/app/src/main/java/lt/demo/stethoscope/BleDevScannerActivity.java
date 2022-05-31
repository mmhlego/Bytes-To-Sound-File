package lt.demo.stethoscope;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import lt.demo.stethoscope.utils.BaseRecycleAdapter;
import lt.demo.stethoscope.utils.BluetoothDeviceAdapter;
import lt.sdk.ble.BleDevice;
import lt.sdk.ble.OnBleDeviceScanListener;
import lt.sdk.stethoscope.StethoscopeTool;

/**
 * Bluetooth device discovery list.
 */
public class BleDevScannerActivity extends BaseActivity implements
        BaseRecycleAdapter.OnItemClickListener, OnBleDeviceScanListener {

    public static final int REQUEST_CODE_CONNECT_DEVICE = 555;
    private BluetoothDeviceAdapter mAdapter;

    @Override
    protected boolean containOnBaseLayout() {
        return false;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_device_scanner);
        RecyclerView recyclerView = findViewById(R.id.rv);
        recyclerView.setHasFixedSize(true);
        mAdapter = new BluetoothDeviceAdapter(getApplicationContext());
        mAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        StethoscopeTool.get().startScan(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        StethoscopeTool.get().stopScan();
    }

    @Override
    public void onDestroy() {
        mAdapter = null;
        super.onDestroy();
    }

    @Override
    public void onItemClick(View itemVIew, int position) {
        if (mAdapter != null) {
            BleDevice device = mAdapter.getItem(position);
            Intent data = new Intent();
            data.putExtra(BleDevice.class.getSimpleName(), device);
            setResult(REQUEST_CODE_CONNECT_DEVICE, data);
            finish();
        }
    }

    @Override
    public void onBleDeviceList(List<BleDevice> list) {
        if (mAdapter != null) {
            mAdapter.setItems(list);
        }
    }
}
