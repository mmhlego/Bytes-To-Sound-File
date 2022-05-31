package lt.demo.stethoscope;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import lt.sdk.obj.Constants;
import lt.sdk.stethoscope.BuildConfig;
import lt.sdk.stethoscope.StethoscopeTool;

public abstract class BaseActivity extends AppCompatActivity {

    private final static String TAG = "BaseActivity";

    private Toast mToast;

    protected boolean containOnBaseLayout() {
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (containOnBaseLayout()) {
            super.setContentView(R.layout.activity_base);
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        if (containOnBaseLayout()) {
            ViewGroup viewGroup = findViewById(R.id.container);
            View inflate = getLayoutInflater().inflate(layoutResID, viewGroup, false);
            viewGroup.addView(inflate);
        } else {
            super.setContentView(layoutResID);
        }
    }


    protected <T extends ViewDataBinding> T dataBindingContentView(@LayoutRes int layoutResId) {
        ViewGroup viewGroup = findViewById(R.id.container);
        T binding = DataBindingUtil.inflate(getLayoutInflater(), layoutResId, viewGroup, false);
        viewGroup.addView(binding.getRoot());
        return binding;
    }

    protected void displayHomeAsUpEnabled() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    protected void toast(@StringRes int textId) {
        toast(textId, Toast.LENGTH_SHORT);
    }

    protected void toast(@StringRes int textId, int duration) {
        toast(getString(textId), duration);
    }

    protected void toast(CharSequence text) {
        toast(text, Toast.LENGTH_SHORT);
    }

    protected void toast(CharSequence text, int duration) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(this, text, duration);
        mToast.show();
    }


    protected boolean checkBleState(boolean onlyBluetooth) {
        switch (BluetoothAdapter.getDefaultAdapter().getState()) {
            case BluetoothAdapter.STATE_OFF:
                toast(R.string.state_tips_for_bt_off);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                toast(R.string.state_tips_for_bt_turning_off);
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                toast(R.string.state_tips_for_bt_turning_on);
                break;
            case BluetoothAdapter.STATE_ON:
                if (onlyBluetooth) {
                    return true;
                } else {
                    int gattState = StethoscopeTool.get().getBleState();
                    if (Constants.BLE_GATT_DISCONNECTED == gattState) {
                        Log.i(TAG, "checkBleState:BleWorker.BLE_GATT_DISCONNECTED");
                        toast(R.string.state_tips_for_gatt_disconnect);
                    } else if (Constants.BLE_GATT_CONNECTING == gattState) {
                        Log.i(TAG, "checkBleState:BleWorker.BLE_GATT_CONNECTING");
                        toast(R.string.state_tips_for_gatt_connecting);
                    } else {
                        Log.i(TAG, "checkBleState PASS");
                        return true;
                    }
                }
                break;
        }
        return false;
    }

    protected void showAlertDialog(@StringRes int msg) {
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, null)
                .create().show();
    }

    protected App getApp() {
        return (App) getApplication();
    }
}
