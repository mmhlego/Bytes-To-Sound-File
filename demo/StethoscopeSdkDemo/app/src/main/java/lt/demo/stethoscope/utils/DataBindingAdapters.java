package lt.demo.stethoscope.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.databinding.BindingAdapter;

import lt.demo.stethoscope.R;
import lt.sdk.obj.Constants;

public class DataBindingAdapters {

    @BindingAdapter("stateChanged")
    public static void setRecyclerAdapter(SwitchCompat swCmt, int state) {
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                swCmt.setChecked(false);
                swCmt.setText(R.string.bluetooth_state_off);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                swCmt.setChecked(false);
                swCmt.setText(R.string.bluetooth_state_turning_off);
                break;
            case BluetoothAdapter.STATE_ON:
                swCmt.setChecked(true);
                swCmt.setText(R.string.bluetooth_state_on);
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                swCmt.setChecked(true);
                swCmt.setText(R.string.bluetooth_state_turning_on);
                break;
        }
    }

    @BindingAdapter("gattState")
    public static void setBleGattState(TextView tv, int state) {
        switch (state) {
            case Constants.BLE_GATT_DISCONNECTED:
            default:
                tv.setText(R.string.ble_gatt_state_disconnected);
                break;
            case Constants.BLE_GATT_CONNECTING:
                tv.setText(R.string.ble_gatt_state_connecting);
                break;
            case Constants.BLE_GATT_CONNECTED:
                tv.setText(R.string.ble_gatt_state_connected);
                break;
        }
    }

    @BindingAdapter("recordBtnState")
    public static void setRecordBtnState(Button btn, int state) {
        if (state == Constants.BLE_GATT_DISCONNECTED) {
            btn.setText(R.string.start_record);
        }
    }

    @BindingAdapter("bleDeviceInfo")
    public static void setBleDeviceInfo(TextView tv, BluetoothDevice device) {
        if (device == null) {
            tv.setText(tv.getResources().getString(R.string.device_info_value
                    , "-", "-"));
        } else {
            tv.setText(tv.getResources().getString(R.string.device_info_value
                    , device.getName(), device.getAddress()));
        }
    }

    @BindingAdapter("switchEchoMode")
    public static void setSwitchEchoMode(Button button, Object mode) {
        int echoMode = (int) mode;
        if (echoMode == Constants.ECHO_MODE_HS) {
            button.setText(R.string.echo_mode_hs);
        } else {
            button.setText(R.string.echo_mode_ls);
        }
    }
}
