package lt.demo.stethoscope;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.ObservableArrayMap;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableInt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import lt.demo.stethoscope.databinding.ActivityMainBinding;
import lt.demo.stethoscope.dfu.DfuActivity;
import lt.demo.stethoscope.dialog.ProgressDialog;
import lt.demo.stethoscope.utils.AudioTrackPlayer;
import lt.demo.stethoscope.utils.DataKey;
import lt.demo.stethoscope.utils.PlayRecordThread;
import lt.demo.stethoscope.utils.Utils;
import lt.demo.stethoscope.widget.AudioWaveView;
import lt.sdk.ble.BleDevice;
import lt.sdk.ble.OnBleWorkListener;
import lt.sdk.obj.Constants;
import lt.sdk.stethoscope.OnStethoscopeDataListener;
import lt.sdk.stethoscope.StethoscopeTool;

public class MainActivity extends BaseActivity implements OnBleWorkListener {

    private final static String TAG = "MainActivity";

    private static final int REQUEST_LOCATION_PERMISSION = 666;
    private static final boolean SAVE_AUDIO_DATA = true;

    private final ObservableBoolean isPlaying = new ObservableBoolean(false);
    private final ObservableBoolean isRecording = new ObservableBoolean(false);
    private final ObservableBoolean isFilterAudio = new ObservableBoolean(false);
    private final ObservableInt mState = new ObservableInt(BluetoothAdapter.STATE_OFF);
    private final ObservableInt mGattState = new ObservableInt(Constants.BLE_GATT_DISCONNECTED);
    private final ObservableArrayMap<String, Object> dataMap = new ObservableArrayMap<>();

    private FileOutputStream fos;
    private File mFile;

    private final OnStethoscopeDataListener mListener = new OnStethoscopeDataListener() {

        @Override
        public void onFilteredAudioData(short[] data) {
            if (isFilterAudio.get()) {
                dealAudioStream(data);
            }
        }

        @Override
        public void onRawAudioData(short[] data) {
            if (!isFilterAudio.get()) {
                dealAudioStream(data);
            }
        }

        @Override
        public void onSynchronizingDeviceData(boolean inProgress) {
            try {
                if (inProgress) {
                    if (progressDialog == null) {
                        progressDialog = new ProgressDialog();
                        progressDialog.setCancelable(false);
                        progressDialog.setMessage(R.string.synchronizing_data);
                        progressDialog.show(getSupportFragmentManager(), TAG);
                    }
                } else {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        @Override
        public void onBatteryLevelChanged(int level) {
            dataMap.put(DataKey.BATTERY_LEVEL, level);
        }

        @Override
        public void onVolumeLevelChanged(int level) {
            dataMap.put(DataKey.VOLUME_LEVEL, level);
        }

        /**
         * When {@link StethoscopeTool#setEchoModeSwitch)} success, this interface will callback result
         * @param echoMode the current echo mode.
         * */
        @Override
        public void onEchoModeSwitch(@Constants.EchoMode int echoMode) {
            dataMap.put(DataKey.ECHO_MODE, echoMode);
        }

        @Override
        public void onResult(int heartRate) {
            dataMap.put(DataKey.HEART_RATE, heartRate);
        }

        @Override
        public void onException(int exception) {
            switch (exception) {
                case Constants.EXCEPTION_VOLUME_LEVEL_MAX:
                    showAlertDialog(R.string.dialog_msg_adjust_volume_exception_for_max);
                    break;
                case Constants.EXCEPTION_VOLUME_LEVEL_MIN:
                    showAlertDialog(R.string.dialog_msg_adjust_volume_exception_for_min);
                    break;
                case Constants.EXCEPTION_SYNC_DATA_TIMEOUT:
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    toast(R.string.exception_for_sync_data_timeout);
                    break;
            }
        }
    };


    private PlayRecordThread mPlayRecordThread;
    private AudioTrackPlayer mAudioTrackPlayer;

    private ProgressDialog progressDialog;
    private AudioWaveView mAudioWaveView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = dataBindingContentView(R.layout.activity_main);
        binding.setState(mState);
        binding.setGattState(mGattState);
        binding.setDataMap(dataMap);
        binding.setFilterAudio(isFilterAudio);
        binding.setRecording(isRecording);
        binding.setPlaying(isPlaying);
        mAudioWaveView = binding.wsv;

        StethoscopeTool.get().setBleWorkListener(this);
        StethoscopeTool.get().setOnStethoscopeDataListener(mListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        if (mAudioWaveView != null) {
            mAudioWaveView.clearDatas();
        }
        StethoscopeTool.get().disconnect();
        StethoscopeTool.get().setBleWorkListener(null);
        StethoscopeTool.get().setOnStethoscopeDataListener(null);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BleDevScannerActivity.REQUEST_CODE_CONNECT_DEVICE && data != null) {
            BleDevice bleDevice = data.getParcelableExtra(BleDevice.class.getSimpleName());
            //Deprecated
//            StethoscopeTool.get().connect(bleDevice);
            if (bleDevice != null) {
                StethoscopeTool.get().connect(bleDevice.getDevice());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (StethoscopeTool.get().isRecording()) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.msg_exit_app)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        StethoscopeTool.get().stopRecordAudio();
                        super.onBackPressed();
                    })
                    .create().show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (REQUEST_LOCATION_PERMISSION == requestCode) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[0])
                    && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                onClickConnectGatt(null);
            } else {
                Toast.makeText(this, R.string.permission_denied_for_location, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBluetoothStateChanged(int state) {
        mState.set(state);
    }

    @Override
    public void onBleGattStateChanged(int state, BluetoothDevice device) {
        mGattState.set(state);
        if (Constants.BLE_GATT_CONNECTED == state) {
            dataMap.put(DataKey.BLE_DEV, device);
        } else if (Constants.BLE_GATT_DISCONNECTED == state) {
            dataMap.clear();
        }
    }

    private void dealAudioStream(short[] data) {
        if (mAudioWaveView != null) {
            mAudioWaveView.addWaveData(data);
        }
        if (mAudioTrackPlayer != null) {
            mAudioTrackPlayer.write(data);
        }
        try {
            if (fos != null) {
                byte[] dataByte = Utils.shortToBytes(data);
                fos.write(dataByte, 0, dataByte.length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClickBtState(View v) {
        if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            BluetoothAdapter.getDefaultAdapter().disable();
        } else {
            BluetoothAdapter.getDefaultAdapter().enable();
        }
    }

    public void onClickConnectGatt(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}
                        , REQUEST_LOCATION_PERMISSION);
                return;
            }
            if (!Utils.isLocationEnabled(this)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_title_tips)
                        .setMessage(R.string.dialog_msg_turn_on_location)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.dialog_btn_label_turn_on, (dialog, which) -> {
                                    Intent intent = new Intent();
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    try {
                                        intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                        startActivity(intent);
                                    } catch (ActivityNotFoundException ex) {
                                        // The Android SDK doc says that the location settings activity
                                        // may not be found. In that case show the general settings.
                                        // General settings activity
                                        intent.setAction(Settings.ACTION_SETTINGS);
                                        try {
                                            startActivity(intent);
                                        } catch (Exception ignored) {
                                            toast("Can not find the location settings page.");
                                        }
                                    }
                                }
                        ).create().show();
                return;
            }
        }
        if (checkBleState(true)) {
            if (StethoscopeTool.get().getBleState() > Constants.BLE_GATT_DISCONNECTED) {
                StethoscopeTool.get().disconnect();
            } else {
                startActivityForResult(new Intent(getApplicationContext(),
                                BleDevScannerActivity.class),
                        BleDevScannerActivity.REQUEST_CODE_CONNECT_DEVICE);
            }
        }
    }

    public void onClickRecord(View v) {
        if (mPlayRecordThread != null) return;
        if (checkBleState(false)) {
            if (StethoscopeTool.get().isRecording()) {
                StethoscopeTool.get().stopRecordAudio();
                closeFile();
                mAudioTrackPlayer.cancel();
                mAudioTrackPlayer = null;
                isRecording.set(false);
            } else {
                mAudioWaveView.clearDatas();
                isRecording.set(true);
                createNewFile();
                StethoscopeTool.get().startRecordAudio();
                mAudioTrackPlayer = new AudioTrackPlayer();
                mAudioTrackPlayer.play();
            }
        }
    }

    public void onClickPlayRecordFile(View v) {
        if (StethoscopeTool.get().isRecording()) return;
        if (mPlayRecordThread == null) {
            File externalFilesDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            String[] files = externalFilesDir.list();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (files == null || files.length == 0) {
                builder.setMessage(R.string.empty);
            } else {
                builder.setNegativeButton(R.string.clear, (dialog, which) ->
                        new Thread(() -> {
                            File[] filesArr = externalFilesDir.listFiles();
                            if (filesArr != null) {
                                for (File f : filesArr) {
                                    //noinspection ResultOfMethodCallIgnored
                                    f.delete();
                                }
                            }
                            runOnUiThread(() -> toast(getString(R.string.completed)));
                        }).start());
                Arrays.sort(files, String::compareTo);
                builder.setItems(files, (dialog, which) -> {
                    File file = new File(externalFilesDir, files[which]);
                    mAudioWaveView.clearDatas();
                    isPlaying.set(true);
                    mPlayRecordThread = new PlayRecordThread(file,
                            new PlayRecordThread.Callback() {
                                @Override
                                public void onOutputBuffer(short[] buffer) {
                                    mAudioWaveView.addWaveData(buffer);
                                }

                                @Override
                                public void onCompleted() {
                                    isPlaying.set(false);
                                    mPlayRecordThread = null;
                                }
                            });
                    mPlayRecordThread.start();
                    dialog.dismiss();
                });
            }
            builder.create().show();
        } else {
            mPlayRecordThread.interrupt();
        }
    }

    public void onClickDFU(View v) {
        if (getApp().isDfuServiceRunning()) {
            startActivity(new Intent(getApplicationContext(), DfuActivity.class));
        } else {
            if (checkBleState(false)) {
                Object obj = dataMap.getOrDefault(DataKey.BLE_DEV, null);
                if (obj instanceof BluetoothDevice) {
                    startActivity(new Intent(getApplicationContext(), DfuActivity.class)
                            .putExtra("device", (BluetoothDevice) obj));
                }
            }
        }
    }

    private void createNewFile() {
        if (SAVE_AUDIO_DATA) {
            try {
                File externalFilesDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                mFile = new File(externalFilesDir, "audio_" + System.currentTimeMillis() + ".pcm");
                fos = new FileOutputStream(mFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeFile() {
        try {
            if (fos != null) {
                fos.flush();
                fos.close();
                fos = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mFile != null) {
//            if (mFile.length() < 1024) {
//                mFile.delete();
//            }
            mFile = null;
        }
    }

    public void onclickSwitchEchoMode(View v) {
        if (checkBleState(false)) {
            int mode = (int) dataMap.getOrDefault(DataKey.ECHO_MODE, Constants.ECHO_MODE_HS);
            if (mode == Constants.ECHO_MODE_HS) {
                mode = Constants.ECHO_MODE_LS;
            } else {
                mode = Constants.ECHO_MODE_HS;
            }
            StethoscopeTool.get().setEchoModeSwitch(mode);
        }
    }

    public void onClickVolRaise(View v) {
        if (checkBleState(false)) {
            StethoscopeTool.get().adjustVolume(Constants.ADJUST_VOLUME_RAISE);
        }
    }

    public void onClickVolLower(View v) {
        if (checkBleState(false)) {
            StethoscopeTool.get().adjustVolume(Constants.ADJUST_VOLUME_LOWER);
        }
    }
}