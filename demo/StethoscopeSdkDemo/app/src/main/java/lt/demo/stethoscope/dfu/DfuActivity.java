/*
 * Copyright (c) 2021, Linktop Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package lt.demo.stethoscope.dfu;

import android.app.LoaderManager.LoaderCallbacks;
import android.bluetooth.BluetoothDevice;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.io.File;

import lt.demo.stethoscope.BaseActivity;
import lt.demo.stethoscope.R;
import lt.demo.stethoscope.dfu.adapter.FileBrowserAppsAdapter;
import lt.demo.stethoscope.dfu.fragment.UploadCancelFragment;
import lt.sdk.stethoscope.StethoscopeTool;
import lt.sdk.stethoscope.dfu.OnDfuProgressCallbackAdapter;

/**
 * DfuActivity is the main DFU activity It implements DFUManagerCallbacks to receive callbacks from
 * DfuManager class It implements DeviceScannerFragment.OnDeviceSelectedListener callback to receive callback when device is selected from scanning dialog The activity supports portrait and
 * landscape orientations
 */
public class DfuActivity extends BaseActivity implements LoaderCallbacks<Cursor>,
        UploadCancelFragment.CancelFragmentListener {
    private static final String TAG = "DfuActivity";

    private static final String PREFS_DEVICE_NAME = "lt.demo.stethoscope.dfu.PREFS_DEVICE_NAME";
    private static final String PREFS_DEVICE_ADDRESS = "lt.demo.stethoscope.dfu.PREFS_DEVICE_ADDRESS";
    private static final String PREFS_FILE_NAME = "lt.demo.stethoscope.dfu.PREFS_FILE_NAME";
    private static final String PREFS_FILE_SIZE = "lt.demo.stethoscope.dfu.PREFS_FILE_SIZE";

    private static final String DATA_DEVICE = "device";
    private static final String DATA_FILE_PATH = "file_path";
    private static final String DATA_FILE_STREAM = "file_stream";
    private static final String DATA_STATUS = "status";
    private static final String DATA_DFU_COMPLETED = "dfu_completed";
    private static final String DATA_DFU_ERROR = "dfu_error";

    private static final String EXTRA_URI = "uri";

    private static final int SELECT_FILE_REQ = 1;

    private TextView deviceNameView, deviceAddressView;
    private TextView fileNameView;
    private TextView fileSizeView;
    private TextView fileStatusView;
    private TextView textPercentage;
    private TextView textUploading;
    private ProgressBar progressBar;

    private Button selectFileButton, uploadButton;

    private BluetoothDevice selectedDevice;
    private String filePath;
    private Uri fileStreamUri;
    private boolean statusOk;
    /**
     * Flag set to true in {@link #onRestart()} and to false in {@link #onPause()}.
     */
    private boolean resumed;
    /**
     * Flag set to true if DFU operation was completed while {@link #resumed} was false.
     */
    private boolean dfuCompleted;
    /**
     * The error message received from DFU service while {@link #resumed} was false.
     */
    private String dfuError;

    /**
     * The progress callback receives events from the DFU Service.
     * If is registered in onCreate() and unregistered in onDestroy() so methods here may also be called
     * when the screen is locked or the app went to the background. This is because the UI needs to have the
     * correct information after user comes back to the activity and this information can't be read from the service
     * as it might have been killed already (DFU completed or finished with error).
     */
    private final OnDfuProgressCallbackAdapter mOnDfuProgressCallback = new OnDfuProgressCallbackAdapter() {
        @Override
        public void onDeviceConnecting(@NonNull final String deviceAddress) {
            progressBar.setIndeterminate(true);
            textPercentage.setText(R.string.dfu_status_connecting);
        }

        @Override
        public void onDfuProcessStarting(@NonNull final String deviceAddress) {
            progressBar.setIndeterminate(true);
            textPercentage.setText(R.string.dfu_status_starting);
        }

        @Override
        public void onEnablingDfuMode(@NonNull final String deviceAddress) {
            progressBar.setIndeterminate(true);
            textPercentage.setText(R.string.dfu_status_switching_to_dfu);
        }

        @Override
        public void onFirmwareValidating(@NonNull final String deviceAddress) {
            progressBar.setIndeterminate(true);
            textPercentage.setText(R.string.dfu_status_validating);
        }

        @Override
        public void onDeviceDisconnecting(@NonNull final String deviceAddress) {
            progressBar.setIndeterminate(true);
            textPercentage.setText(R.string.dfu_status_disconnecting);
        }

        @Override
        public void onDfuCompleted(@NonNull final String deviceAddress) {
            textPercentage.setText(R.string.dfu_status_completed);
            if (resumed) {
                // let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
                new Handler().postDelayed(() -> {
                    onTransferCompleted();
                    onBackPressed();
                }, 200);
            } else {
                // Save that the DFU process has finished
                dfuCompleted = true;
                onBackPressed();
            }
        }

        @Override
        public void onDfuAborted(@NonNull final String deviceAddress) {
            textPercentage.setText(R.string.dfu_status_aborted);
            // let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
            new Handler().postDelayed(() -> onUploadCanceled(), 200);
        }

        @Override
        public void onProgressChanged(@NonNull final String deviceAddress, final int percent,
                                      final float speed, final float avgSpeed,
                                      final int currentPart, final int partsTotal) {
            progressBar.setIndeterminate(false);
            progressBar.setProgress(percent);
            textPercentage.setText(getString(R.string.dfu_uploading_percentage, percent));
            if (partsTotal > 1)
                textUploading.setText(getString(R.string.dfu_status_uploading_part, currentPart, partsTotal));
            else
                textUploading.setText(R.string.dfu_status_uploading);
        }

        @Override
        public void onError(@NonNull final String deviceAddress, final int error, final int errorType, final String message) {
            if (resumed) {
                showErrorMessage(message);
            } else {
                dfuError = message;
            }
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature_dfu);
        initUI();

        // restore saved state
        if (savedInstanceState != null) {
            filePath = savedInstanceState.getString(DATA_FILE_PATH);
            fileStreamUri = savedInstanceState.getParcelable(DATA_FILE_STREAM);
            selectedDevice = savedInstanceState.getParcelable(DATA_DEVICE);
            statusOk = statusOk || savedInstanceState.getBoolean(DATA_STATUS);
            uploadButton.setEnabled(selectedDevice != null && statusOk);
            dfuCompleted = savedInstanceState.getBoolean(DATA_DFU_COMPLETED);
            dfuError = savedInstanceState.getString(DATA_DFU_ERROR);
        }

        StethoscopeTool.get().registerDfuProgressCallback(mOnDfuProgressCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StethoscopeTool.get().unregisterDfuProgressCallback(mOnDfuProgressCallback);
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(DATA_FILE_PATH, filePath);
        outState.putParcelable(DATA_FILE_STREAM, fileStreamUri);
        outState.putParcelable(DATA_DEVICE, selectedDevice);
        outState.putBoolean(DATA_STATUS, statusOk);
        outState.putBoolean(DATA_DFU_COMPLETED, dfuCompleted);
        outState.putString(DATA_DFU_ERROR, dfuError);
    }

    private void initUI() {
        displayHomeAsUpEnabled();

        deviceNameView = findViewById(R.id.device_name);
        deviceAddressView = findViewById(R.id.device_address);
        fileNameView = findViewById(R.id.file_name);
        fileSizeView = findViewById(R.id.file_size);
        fileStatusView = findViewById(R.id.file_status);
        selectFileButton = findViewById(R.id.action_select_file);
        uploadButton = findViewById(R.id.action_upload);
        textPercentage = findViewById(R.id.textviewProgress);
        textUploading = findViewById(R.id.textviewUploading);
        progressBar = findViewById(R.id.progressbar_file);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (getApp().isDfuServiceRunning()) {
            // Restore image file information
            deviceNameView.setText(preferences.getString(PREFS_DEVICE_NAME, ""));
            deviceAddressView.setText(preferences.getString(PREFS_DEVICE_ADDRESS, ""));
            fileNameView.setText(preferences.getString(PREFS_FILE_NAME, ""));
            fileSizeView.setText(preferences.getString(PREFS_FILE_SIZE, ""));
            fileStatusView.setText(R.string.dfu_file_status_ok);
            statusOk = true;
            showProgressBar();
        } else {
            Intent intent = getIntent();
            selectedDevice = intent.getParcelableExtra("device");
            if (selectedDevice != null) {
                statusOk = true;
                String name = selectedDevice.getName();
                String address = selectedDevice.getAddress();
                uploadButton.setEnabled(statusOk);
                deviceNameView.setText(name != null ? name : getString(R.string.not_available));
                deviceAddressView.setText(address);
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        if (dfuCompleted)
            onTransferCompleted();
        if (dfuError != null)
            showErrorMessage(dfuError);
        if (dfuCompleted || dfuError != null) {
            dfuCompleted = false;
            dfuError = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;

        if (requestCode == SELECT_FILE_REQ) {// clear previous data
            filePath = null;
            fileStreamUri = null;

            // and read new one
            final Uri uri = data.getData();
            /*
             * The URI returned from application may be in 'file' or 'content' schema. 'File' schema allows us to create a File object and read details from if
             * directly. Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
             */
            if (uri.getScheme().equals("file")) {
                // the direct path to the file has been returned
                final String path = uri.getPath();
                final File file = new File(path);
                filePath = path;

                updateFileInfo(file.getName(), file.length());
            } else if (uri.getScheme().equals("content")) {
                // an Uri has been returned
                fileStreamUri = uri;
                // if application returned Uri for streaming, let's us it. Does it works?
                // FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
                final Bundle extras = data.getExtras();
                if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
                    fileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM);

                // file name and size must be obtained from Content Provider
                final Bundle bundle = new Bundle();
                bundle.putParcelable(EXTRA_URI, uri);
                getLoaderManager().restartLoader(SELECT_FILE_REQ, bundle, this);
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        final Uri uri = args.getParcelable(EXTRA_URI);
        /*
         * Some apps, f.e. Google Drive allow to select file that is not on the device. There is no "_data" column handled by that provider. Let's try to obtain
         * all columns and than check which columns are present.
         */
        // final String[] projection = new String[] { MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATA };
        return new CursorLoader(this, uri, null /* all columns, instead of projection */, null, null, null);
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        fileNameView.setText(null);
        fileSizeView.setText(null);
        filePath = null;
        fileStreamUri = null;
        statusOk = false;
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        if (data != null && data.moveToNext()) {
            /*
             * Here we have to check the column indexes by name as we have requested for all. The order may be different.
             */
            final String fileName = data.getString(data.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)/* 0 DISPLAY_NAME */);
            final int fileSize = data.getInt(data.getColumnIndex(MediaStore.MediaColumns.SIZE) /* 1 SIZE */);
            String filePath = null;
            final int dataIndex = data.getColumnIndex(MediaStore.MediaColumns.DATA);
            if (dataIndex != -1)
                filePath = data.getString(dataIndex /* 2 DATA */);
            if (!TextUtils.isEmpty(filePath))
                this.filePath = filePath;

            updateFileInfo(fileName, fileSize);
        } else {
            fileNameView.setText(null);
            fileSizeView.setText(null);
            filePath = null;
            fileStreamUri = null;
            fileStatusView.setText(R.string.dfu_file_status_error);
            statusOk = false;
        }
    }

    /**
     * Updates the file information on UI
     *
     * @param fileName file name
     * @param fileSize file length
     */
    private void updateFileInfo(final String fileName, final long fileSize) {
        fileNameView.setText(fileName);
        fileSizeView.setText(getString(R.string.dfu_file_size_text, fileSize));
        final boolean statusOk = this.statusOk = MimeTypeMap.getFileExtensionFromUrl(fileName).matches("(?i)ZIP");
        fileStatusView.setText(statusOk ? R.string.dfu_file_status_ok : R.string.dfu_file_status_invalid);
        uploadButton.setEnabled(selectedDevice != null && statusOk);
    }

    /**
     * Called when the question mark was pressed
     *
     * @param view a button that was pressed
     */
    public void onSelectFileHelpClicked(final View view) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dfu_help_title)
                .setMessage(R.string.dfu_help_message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    /**
     * Called when Select File was pressed
     *
     * @param view a button that was pressed
     */
    public void onSelectFileClicked(final View view) {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(DfuProcessService.MIME_TYPE_ZIP);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            // file browser has been found on the device
            startActivityForResult(intent, SELECT_FILE_REQ);
        } else {
            // there is no any file browser app, let's try to download one
            final View customView = getLayoutInflater().inflate(R.layout.app_file_browser, null);
            final ListView appsList = customView.findViewById(android.R.id.list);
            appsList.setAdapter(new FileBrowserAppsAdapter(this));
            appsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            appsList.setItemChecked(0, true);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dfu_alert_no_filebrowser_title)
                    .setView(customView)
                    .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        final int pos = appsList.getCheckedItemPosition();
                        if (pos >= 0) {
                            final String query = getResources().getStringArray(R.array.dfu_app_file_browser_action)[pos];
                            final Intent storeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(query));
                            startActivity(storeIntent);
                        }
                    })
                    .show();
        }
    }

    /**
     * Callback of UPDATE/CANCEL button on DfuActivity
     */
    public void onUploadClicked(final View view) {
        if (getApp().isDfuServiceRunning()) {
            showUploadCancelDialog();
            return;
        }

        // Check whether the selected file is a HEX file (we are just checking the extension)
        if (!statusOk) {
            toast(R.string.dfu_file_status_invalid_message, Toast.LENGTH_LONG);
            return;
        }

        // Save current state in order to restore it if user quit the Activity
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFS_DEVICE_NAME, selectedDevice.getName());
        editor.putString(PREFS_DEVICE_ADDRESS, selectedDevice.getAddress());
        editor.putString(PREFS_FILE_NAME, fileNameView.getText().toString());
        editor.putString(PREFS_FILE_SIZE, fileSizeView.getText().toString());
        editor.apply();

        showProgressBar();

        StethoscopeTool.get().dfuStart(selectedDevice, fileStreamUri, filePath, DfuProcessService.class);
    }

    private void showUploadCancelDialog() {
        StethoscopeTool.get().dfuPause();

        final UploadCancelFragment fragment = UploadCancelFragment.getInstance();
        fragment.show(getSupportFragmentManager(), TAG);
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        textPercentage.setVisibility(View.VISIBLE);
        textPercentage.setText(null);
        textUploading.setText(R.string.dfu_status_uploading);
        textUploading.setVisibility(View.VISIBLE);
        selectFileButton.setEnabled(false);
        uploadButton.setEnabled(true);
        uploadButton.setText(R.string.cancel);
    }

    private void onTransferCompleted() {
        clearUI(true);
        toast(R.string.dfu_success);
    }

    public void onUploadCanceled() {
        clearUI(false);
        toast(R.string.dfu_aborted);
    }

    @Override
    public void onCancelUpload() {
        progressBar.setIndeterminate(true);
        textUploading.setText(R.string.dfu_status_aborting);
        textPercentage.setText(null);
    }

    private void showErrorMessage(final String message) {
        clearUI(false);
        toast("Upload failed: " + message);
    }

    private void clearUI(final boolean clearDevice) {
        progressBar.setVisibility(View.INVISIBLE);
        textPercentage.setVisibility(View.INVISIBLE);
        textUploading.setVisibility(View.INVISIBLE);
        selectFileButton.setEnabled(true);
        uploadButton.setEnabled(false);
        uploadButton.setText(R.string.dfu_action_upload);
        if (clearDevice) {
            selectedDevice = null;
            deviceNameView.setText(R.string.dfu_default_name);
            deviceAddressView.setText("");
        }
        // Application may have lost the right to these files if Activity was closed during upload (grant uri permission). Clear file related values.
        fileNameView.setText(null);
        fileSizeView.setText(null);
        fileStatusView.setText(R.string.dfu_file_status_no_file);
        filePath = null;
        fileStreamUri = null;
        statusOk = false;
    }
}