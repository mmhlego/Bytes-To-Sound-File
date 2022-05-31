package lt.demo.stethoscope;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import lt.sdk.obj.Constants;
import lt.sdk.stethoscope.DeviceInfo;
import lt.sdk.stethoscope.OnDeviceInfoListener;
import lt.sdk.stethoscope.StethoscopeTool;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new SettingsFragment())
                    .commit();
        }
        displayHomeAsUpEnabled();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements Handler.Callback {


        private Handler handler;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            handler = new Handler(Looper.getMainLooper(), this);
            StethoscopeTool.get().setOnDeviceInfoListener(new OnDeviceInfoListener() {
                @Override
                public void onDeviceInfo(@NonNull DeviceInfo deviceInfo) {
                    Message.obtain(handler, 10, deviceInfo).sendToTarget();
                }

                @Override
                public void onDeviceVersion(@Constants.VersionType int type, String version) {
                    switch (type) {
                        case Constants.VERSION_TYPE_HARDWARE:
                            Message.obtain(handler, 11, version).sendToTarget();
                            break;
                        case Constants.VERSION_TYPE_SOFTWARE:
                            Message.obtain(handler, 12, version).sendToTarget();
                            break;
                    }
                }
            });
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference prefAppVer = findPreference("key_sdk_version");
            if (prefAppVer != null) {
                prefAppVer.setSummary(StethoscopeTool.getSdkVersion());
            }
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            if (StethoscopeTool.get().getBleState() == Constants.BLE_GATT_CONNECTED) {
                handler.sendEmptyMessageDelayed(0, 1000L);
            }
        }

        @Override
        public void onDestroy() {
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
                handler = null;
            }
            StethoscopeTool.get().setOnDeviceInfoListener(null);
            super.onDestroy();
        }

        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 0:
                    StethoscopeTool.get().readDeviceInfo();
                    return true;
                case 1:
                    StethoscopeTool.get().readDeviceVersion(Constants.VERSION_TYPE_HARDWARE);
                    return true;
                case 2:
                    StethoscopeTool.get().readDeviceVersion(Constants.VERSION_TYPE_SOFTWARE);
                    return true;
                case 10: {
                    DeviceInfo deviceInfo = (DeviceInfo) msg.obj;
                    Preference preference = findPreference("key_sn");
                    if (preference != null) {
                        preference.setSummary(deviceInfo.getSn());
                    }
                    preference = findPreference("key_dev_id");
                    if (preference != null) {
                        preference.setSummary(deviceInfo.getDeviceId());
                    }
                    preference = findPreference("key_akey");
                    if (preference != null) {
                        preference.setSummary(deviceInfo.getAKey());
                    }
                    handler.sendEmptyMessageDelayed(1, 500L);
                    return true;
                }
                case 11: {
                    String hardwareVersion = (String) msg.obj;
                    Preference preference = findPreference("key_hardware_version");
                    if (preference != null) {
                        preference.setSummary(hardwareVersion);
                    }
                    handler.sendEmptyMessageDelayed(2, 500L);
                    return true;
                }
                case 12: {
                    String softwareVersion = (String) msg.obj;
                    Preference preference = findPreference("key_software_version");
                    if (preference != null) {
                        preference.setSummary(softwareVersion);
                    }
                    return true;
                }
            }
            return false;
        }
    }
}